package br.fairgo;

import java.security.Principal;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import br.fairgo.VerificacaoContato.Canal;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/driver/me/verificacoes/{canal}")
class ContatoDriver {
    record Confirmacao(@NotNull UUID desafio, @NotNull @Pattern(regexp="[0-9]{8}") String codigo) {
        @Override public String toString() { return "Confirmacao[protegida]"; }
    }
    private final VerificacaoContato verificacao;

    record Desafio(UUID desafio) {}
    private final EntregaDriver entrega;

    ContatoDriver(VerificacaoContato verificacao, EntregaDriver entrega) {
        this.verificacao = verificacao;
        this.entrega = entrega;
    }

    @PostMapping("/solicitacoes")
    @ResponseStatus(ACCEPTED)
    public Desafio solicitar(@PathVariable Canal canal, Principal sessao) {
        return new Desafio(entrega.verificar(UUID.fromString(sessao.getName()), canal));
    }

    @PostMapping("/confirmacao")
    @ResponseStatus(NO_CONTENT)
    public void confirmar(@PathVariable Canal canal, @Valid @RequestBody Confirmacao pedido, Principal sessao) {
        if (!verificacao.confirmar(UUID.fromString(sessao.getName()), canal, pedido.desafio(), pedido.codigo()))
            throw new ResponseStatusException(BAD_REQUEST);
    }
}
