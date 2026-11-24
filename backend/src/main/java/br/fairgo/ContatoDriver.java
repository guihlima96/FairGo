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

    ContatoDriver(VerificacaoContato verificacao) {
        this.verificacao = verificacao;
    }

    @PostMapping("/solicitacoes")
    public void solicitar(@PathVariable Canal canal) {
        // Habilitar a emissão somente após integrar a entrega ao contato da conta.
        throw new ResponseStatusException(SERVICE_UNAVAILABLE);
    }

    @PostMapping("/confirmacao")
    @ResponseStatus(NO_CONTENT)
    public void confirmar(@PathVariable Canal canal, @Valid @RequestBody Confirmacao pedido, Principal sessao) {
        if (!verificacao.confirmar(UUID.fromString(sessao.getName()), canal, pedido.desafio(), pedido.codigo()))
            throw new ResponseStatusException(BAD_REQUEST);
    }
}
