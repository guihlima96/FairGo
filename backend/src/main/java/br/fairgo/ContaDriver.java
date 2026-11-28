package br.fairgo;

import java.security.Principal;
import java.time.Instant;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/driver")
class ContaDriver {
    record Cadastro(@NotBlank @Size(max=160) String nome, @NotBlank @CPF String cpf,
                    @NotBlank @Pattern(regexp="(?:[+]55 ?)?(?:[(][1-9][0-9][)]|[1-9][0-9]) ?9[0-9]{4}-?[0-9]{4}") String celular,
                    @NotBlank @Email @Size(max=254) String email, @NotBlank @Size(min=8,max=32) String senha) {}
    record Login(@NotBlank @Size(max=254) String identificador, @NotBlank @Size(max=128) String senha) {}
    record Pedido(@NotBlank @Email @Size(max=254) String email) {}
    record Redefinicao(@NotNull @Pattern(regexp="rec_[0-9a-f]{64}") String token,
                       @NotBlank @Size(min=8,max=32) String novaSenha) {
        @Override public String toString() { return "Redefinicao[protegida]"; }
    }
    record Credencial(UUID id, String hash) {}
    record MinhaConta(UUID id, String nome, String email, String situacao, String cpf, String celular,
                      boolean nomeVerificado, boolean emailVerificado, boolean celularVerificado) {}
    record Sessao(String token, Instant expiraEm) {}

    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final PasswordEncoder senhas;
    private final String hashFicticio;
    private final RecuperacaoDriver recuperacao;
    private final EntregaDriver entrega;
    private final TentativasDriver tentativas;

    ContaDriver(JdbcTemplate db, TransactionTemplate tx, PasswordEncoder senhas, RecuperacaoDriver recuperacao,
                TentativasDriver tentativas, EntregaDriver entrega) {
        this.entrega = entrega;
        this.db = db;
        this.tx = tx;
        this.senhas = senhas;
        this.hashFicticio = senhas.encode(UUID.randomUUID().toString());
        this.recuperacao = recuperacao;
        this.tentativas = tentativas;
    }

    @PostMapping("/contas")
    @ResponseStatus(CREATED)
    public ResultadoCadastro cadastrar(@Valid @RequestBody Cadastro dados, HttpServletRequest req) {
        tentativas.limitarOrigem("cadastro:" + req.getRemoteAddr(), 20);
        String cpf = dados.cpf().replace(".", "").replace("-", "");
        if (!cpf.matches("[0-9]{11}")) throw new ResponseStatusException(BAD_REQUEST);
        String celular = dados.celular().replaceAll("[^0-9]", "");
        String telefone = celular.length() == 11 ? "+55" + celular : "+" + celular;
        String email = dados.email().strip().toLowerCase(Locale.ROOT);
        String hash = senhas.encode(dados.senha());
        UUID id = UUID.randomUUID();
        try {
            tx.executeWithoutResult(status -> {
                db.update("INSERT INTO usuarios(id,nome,email,perfil) VALUES (?,?,?,'MOTORISTA')",
                        id, dados.nome().strip(), email);
                db.update("INSERT INTO contas_driver(usuario_id,cpf,celular,senha_hash) VALUES (?,?,?,?)",
                        id, cpf, telefone, hash);
            });
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(CONFLICT);
        }
        return new ResultadoCadastro(id, "PENDENTE");
    }

    @PostMapping("/sessoes")
    public Sessao entrar(@Valid @RequestBody Login dados, HttpServletRequest req) {
        String origem = req.getRemoteAddr();
        tentativas.limitarOrigemLogin(origem);
        String login = dados.identificador().strip().toLowerCase(Locale.ROOT);
        String coluna = login.contains("@") ? "u.email" : "u.id";
        Object valor = login;
        if (coluna.equals("u.id")) {
            try { valor = UUID.fromString(login); }
            catch (IllegalArgumentException ex) { valor = null; }
        }
        var contas = db.query("SELECT u.id,c.senha_hash FROM usuarios u JOIN contas_driver c ON c.usuario_id=u.id "
                + "WHERE u.perfil='MOTORISTA' AND " + coluna + "=?",
                (rs,n) -> new Credencial(rs.getObject(1,UUID.class), rs.getString(2)), valor);
        Credencial conta = contas.isEmpty() ? null : contas.get(0);
        var reserva = tentativas.reservarLogin(origem, conta == null ? login : conta.id().toString());
        if (!senhas.matches(dados.senha(), conta == null ? hashFicticio : conta.hash()) || conta == null)
            throw new ResponseStatusException(UNAUTHORIZED);
        return tx.execute(status -> {
            // Serializa emissão de sessão e troca de senha.
            String atual = db.queryForObject("SELECT senha_hash FROM contas_driver WHERE usuario_id=? FOR UPDATE",
                    String.class, conta.id());
            if (!conta.hash().equals(atual)) throw new ResponseStatusException(UNAUTHORIZED);
            tentativas.sucesso(reserva);
            db.update("DELETE FROM sessoes_driver WHERE usuario_id=? AND expira_em<=now()", conta.id());
            String token = TokenOpaco.gerar("drv_");
            Instant fim = db.queryForObject("""
                    INSERT INTO sessoes_driver(digest,usuario_id,expira_em) VALUES (?,?,now()+interval '8 hours')
                    RETURNING expira_em
                    """, java.sql.Timestamp.class, TokenOpaco.hash(token), conta.id()).toInstant();
            return new Sessao(token, fim);
        });
    }

    @GetMapping("/me")
    public MinhaConta minhaConta(Principal titular) {
        return db.queryForObject("""
                SELECT u.id,u.nome,u.email,u.situacao,c.cpf,c.celular,i.verificada AS nome_verificado,c.email_verificado,c.celular_verificado
                FROM usuarios u JOIN contas_driver c ON c.usuario_id=u.id
                JOIN identidade_motorista_atual i ON i.id=u.id WHERE u.id=?
                """, (rs, n) -> new MinhaConta(rs.getObject("id", UUID.class), rs.getString("nome"),
                        rs.getString("email"), rs.getString("situacao"), rs.getString("cpf"), rs.getString("celular"),
                        rs.getBoolean("nome_verificado"), rs.getBoolean("email_verificado"), rs.getBoolean("celular_verificado")),
                UUID.fromString(titular.getName()));
    }

    @DeleteMapping("/sessoes/atual")
    @ResponseStatus(NO_CONTENT)
    public void sair(@RequestHeader("Authorization") String bearer, Principal titular) {
        db.update("DELETE FROM sessoes_driver WHERE usuario_id=? AND digest=?",
                UUID.fromString(titular.getName()), TokenOpaco.hash(bearer.substring(7)));
    }

    @PostMapping("/recuperacao")
    @ResponseStatus(ACCEPTED)
    public void solicitar(@Valid @RequestBody Pedido pedido, HttpServletRequest req) {
        tentativas.limitarOrigem("recuperacao-ip:" + req.getRemoteAddr(), 15);
        entrega.solicitarRecuperacao(pedido.email());
    }

    @PostMapping("/recuperacao/confirmacao")
    @ResponseStatus(NO_CONTENT)
    public void redefinir(@Valid @RequestBody Redefinicao dados, HttpServletRequest req) {
        tentativas.limitarOrigem("redefinir-ip:" + req.getRemoteAddr(), 15);
        if (!recuperacao.redefinir(dados.token(), dados.novaSenha())) throw new ResponseStatusException(BAD_REQUEST);
    }

    // A descrição padrão de erros de binding pode conter valores rejeitados (senha ou token).
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    @ResponseStatus(BAD_REQUEST)
    void entradaInvalida() {}
}
