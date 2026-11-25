package br.fairgo;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
class RecuperacaoDriver {
    // Envelope interno para a futura entrega por e-mail. Nunca serializar em uma resposta pública.
    record Entrega(UUID referencia, String destino, String token) {
        @Override public String toString() { return "Entrega[protegida]"; }
    }

    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final PasswordEncoder senhas;

    RecuperacaoDriver(JdbcTemplate db, TransactionTemplate tx, PasswordEncoder senhas) {
        this.db = db;
        this.tx = tx;
        this.senhas = senhas;
    }

    private static String vinculo(UUID titular, String destino, String token) {
        return TokenOpaco.hash(titular + "\n" + destino + "\n" + token);
    }

    Optional<Entrega> emitir(UUID titular, Duration validade) {
        long segundos = validade.toSeconds();
        if (segundos < 1) throw new IllegalArgumentException("Validade deve ter ao menos um segundo");
        return tx.execute(status -> {
            var contatos = db.queryForList("""
                    SELECT u.email FROM usuarios u JOIN contas_driver c ON c.usuario_id=u.id
                    WHERE u.id=? AND u.perfil='MOTORISTA' AND c.email_verificado FOR UPDATE OF u,c
                    """, String.class, titular);
            if (contatos.isEmpty()) return Optional.empty();
            String email = contatos.get(0);
            String token = TokenOpaco.gerar("rec_");
            db.update("""
                    INSERT INTO recuperacoes_driver(usuario_id,digest,vinculo,expira_em)
                    VALUES (?,?,?,clock_timestamp() + ? * interval '1 second')
                    ON CONFLICT (usuario_id) DO UPDATE SET digest=excluded.digest,
                        vinculo=excluded.vinculo,expira_em=excluded.expira_em
                    """, titular, TokenOpaco.hash(token), vinculo(titular,email,token), segundos);
            return Optional.of(new Entrega(UUID.randomUUID(), email, token));
        });
    }

    boolean redefinir(String token, String novaSenha) {
        String digest = TokenOpaco.hash(token);
        return Boolean.TRUE.equals(tx.execute(status -> {
            record Destinatario(UUID id, String email) {}
            List<Destinatario> contas = db.query("""
                    SELECT c.usuario_id,u.email FROM recuperacoes_driver r
                    JOIN contas_driver c USING(usuario_id) JOIN usuarios u ON u.id=c.usuario_id
                    WHERE r.digest=? AND r.expira_em>clock_timestamp() AND c.email_verificado
                    AND u.perfil='MOTORISTA' FOR UPDATE OF u,c
                    """, (rs,n) -> new Destinatario(rs.getObject(1,UUID.class), rs.getString(2)), digest);
            if (contas.isEmpty()) return false;
            Destinatario dono = contas.get(0);
            int consumidos = db.update("""
                    DELETE FROM recuperacoes_driver WHERE usuario_id=? AND digest=? AND vinculo=?
                    AND expira_em>clock_timestamp()
                    """, dono.id(), digest, vinculo(dono.id(),dono.email(),token));
            if (consumidos == 0) return false;
            db.update("UPDATE contas_driver SET senha_hash=? WHERE usuario_id=?", senhas.encode(novaSenha), dono.id());
            db.update("DELETE FROM sessoes_driver WHERE usuario_id=?", dono.id());
            return true;
        }));
    }
}
