package br.fairgo;

import java.util.Locale;
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
                   @NotNull @Pattern(regexp = "(?i)[a-z]{3}-?[0-9][a-z0-9][0-9]{2}") String placa) {}
    record Cadastro(UUID id, String situacao) {}

    private final JdbcTemplate db;

    CadastroAdmin(JdbcTemplate db) {
        this.db = db;
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    Cadastro usuario(@Valid @RequestBody Usuario usuario) {
        UUID id = UUID.randomUUID();
        db.update("INSERT INTO usuarios (id, nome, email, perfil) VALUES (?, ?, ?, ?)",
                id, usuario.nome().strip(), usuario.email().strip().toLowerCase(Locale.ROOT),
                usuario.perfil().name());
        return new Cadastro(id, "PENDENTE");
    }

    @PostMapping("/veiculos")
    @ResponseStatus(HttpStatus.CREATED)
    Cadastro veiculo(@Valid @RequestBody Veiculo veiculo) {
        UUID id = UUID.randomUUID();
        String placa = veiculo.placa().replace("-", "").toUpperCase(Locale.ROOT);
        int inseridos = db.update("""
                INSERT INTO veiculos (id, motorista_id, placa)
                SELECT ?, id, ? FROM usuarios WHERE id = ? AND perfil = 'MOTORISTA'
                """, id, placa, veiculo.motoristaId());
        if (inseridos == 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Motorista inválido");
        }
        return new Cadastro(id, "PENDENTE");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    void cadastroDuplicado() {}
}
