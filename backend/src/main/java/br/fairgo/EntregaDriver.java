package br.fairgo;

import br.fairgo.notificacoes.*;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
class EntregaDriver {
    private final VerificacaoContato contatos;
    private final RecuperacaoDriver recuperacao;
    private final EmailHomologacao email;
    private final Duration validade;
    private final ThreadPoolExecutor fila = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(16));
    private final System.Logger log = System.getLogger(EntregaDriver.class.getName());

    EntregaDriver(VerificacaoContato contatos, RecuperacaoDriver recuperacao, EmailHomologacao email,
                  @Value("${FAIRGO_RECOVERY_TTL_SECONDS:600}") int segundos) {
        if (segundos < 1 || segundos > 3600) throw new IllegalArgumentException("Prazo de recuperação inválido");
        this.contatos = contatos;
        this.recuperacao = recuperacao;
        this.email = email;
        this.validade = Duration.ofSeconds(segundos);
    }

    UUID verificar(UUID titular, VerificacaoContato.Canal canal) {
        if (canal != VerificacaoContato.Canal.EMAIL || email.autorizados().isEmpty())
            throw new ResponseStatusException(SERVICE_UNAVAILABLE);
        var e = contatos.emitir(titular, canal, email::permite);
        var resultado = enviar(new Mensagem(e.desafio(), e.destino(), "FairGo — verificação de e-mail",
                "Seu código de verificação é " + e.codigo() + ". Validade: 10 minutos."));
        if (resultado != Resultado.ACEITO) throw new ResponseStatusException(SERVICE_UNAVAILABLE);
        return e.desafio();
    }

    void solicitarRecuperacao(String endereco) {
        var destino = endereco.strip().toLowerCase(Locale.ROOT);
        if (!email.permite(destino)) return;
        try {
            fila.execute(() -> {
                try {
                    recuperacao.emitirParaEmail(destino, validade).ifPresent(e ->
                        enviar(new Mensagem(e.referencia(), e.destino(), "FairGo — recuperação de acesso",
                                "Use este token para redefinir sua senha: " + e.token())));
                } catch (RuntimeException e) {
                    log.log(System.Logger.Level.WARNING, "Falha interna na recuperação");
                }
            });
        } catch (RejectedExecutionException e) {
            log.log(System.Logger.Level.WARNING, "Fila de recuperação indisponível");
        }
    }

    private Resultado enviar(Mensagem mensagem) {
        var resultado = email.enviar(mensagem);
        log.log(System.Logger.Level.INFO, "Entrega de e-mail: {0}", resultado);
        return resultado;
    }

    @PreDestroy void encerrar() { fila.shutdownNow(); }
}
