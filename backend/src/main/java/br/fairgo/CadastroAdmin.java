package br.fairgo;

import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
class CadastroAdmin {
    enum Perfil { PASSAGEIRO, MOTORISTA }
    record Usuario(@NotBlank @Size(max = 160) String nome,
                   @NotBlank @Email @Size(max = 254) String email,
                   @NotNull Perfil perfil) {}
    record Veiculo(@NotNull UUID motoristaId,
                   @NotNull @Pattern(regexp = Placa.PADRAO) String placa) {}

    private final JdbcTemplate db;

    private final CadastroCompartilhado cadastros;

    CadastroAdmin(JdbcTemplate db, CadastroCompartilhado cadastros) {
        this.cadastros = cadastros;
        this.db = db;
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    ResultadoCadastro usuario(@Valid @RequestBody Usuario usuario) {
        UUID id = UUID.randomUUID();
        db.update("INSERT INTO usuarios (id, nome, email, perfil) VALUES (?, ?, ?, ?)",
                id, usuario.nome().strip(), EmailCadastro.normalizar(usuario.email()),
                usuario.perfil().name());
        return new ResultadoCadastro(id, "PENDENTE");
    }

    @PostMapping("/veiculos")
    @ResponseStatus(HttpStatus.CREATED)
    ResultadoCadastro veiculo(@Valid @RequestBody Veiculo veiculo) {
        return cadastros.criarVeiculo(veiculo.motoristaId(), veiculo.placa());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    void cadastroDuplicado() {}
}
