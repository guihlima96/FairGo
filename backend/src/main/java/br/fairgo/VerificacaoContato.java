package br.fairgo;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
class VerificacaoContato {
    enum Canal { EMAIL, CELULAR }
    record Emissao(UUID desafio, String destino, String codigo) {
        @Override public String toString() { return "Emissao[protegida]"; }
    }
    private record Contato(String destino, boolean verificado) {}

    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final SecretKeySpec chave;
    private final SecureRandom random = new SecureRandom();

    VerificacaoContato(JdbcTemplate db, TransactionTemplate tx,
                       @Value("${FAIRGO_CONTACT_KEY:}") String segredo) {
        this.db = db;
        this.tx = tx;
        byte[] bytes = Base64.getDecoder().decode(segredo);
        if (bytes.length != 0 && bytes.length != 32)
            throw new IllegalArgumentException("Chave de verificação inválida");
        chave = bytes.length == 0 ? null : new SecretKeySpec(bytes, "HmacSHA256");
    }

    private Contato bloquear(UUID titular, Canal canal) {
        if (chave == null) throw new ResponseStatusException(SERVICE_UNAVAILABLE);
        return db.queryForObject("""
                SELECT u.email, c.celular, c.email_verificado, c.celular_verificado
                FROM usuarios u JOIN contas_driver c ON c.usuario_id=u.id
                WHERE u.id=? FOR UPDATE OF u,c
                """, (rs,n) -> canal == Canal.EMAIL
                ? new Contato(rs.getString("email"), rs.getBoolean("email_verificado"))
                : new Contato(rs.getString("celular"), rs.getBoolean("celular_verificado")), titular);
    }

    private byte[] prova(UUID titular, Canal canal, UUID desafio, String destino, String codigo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(chave);
            String mensagem = String.join("\n", titular.toString(), canal.name(), desafio.toString(), destino, codigo);
            return mac.doFinal(mensagem.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    // Uso interno pelo futuro adaptador de entrega. O segredo nunca integra a resposta HTTP.
    Emissao emitir(UUID titular, Canal canal) {
        return tx.execute(status -> {
            Contato contato = bloquear(titular, canal);
            if (contato.verificado()) throw new ResponseStatusException(CONFLICT);
            UUID desafio = UUID.randomUUID();
            String codigo = String.format(Locale.ROOT, "%08d", random.nextInt(100_000_000));
            int linhas = db.update("""
                    INSERT INTO desafios_contato_driver AS d
                    (usuario_id,canal,desafio,prova,expira_em,reenviar_em,fim_janela,emissoes,restantes)
                    VALUES (?,?,?,?,statement_timestamp()+interval '10 minutes',
                            statement_timestamp()+interval '60 seconds',statement_timestamp()+interval '1 hour',1,5)
                    ON CONFLICT (usuario_id,canal) DO UPDATE SET desafio=excluded.desafio,prova=excluded.prova,
                        expira_em=excluded.expira_em,reenviar_em=excluded.reenviar_em,restantes=5,
                        emissoes=CASE WHEN d.fim_janela<=statement_timestamp() THEN 1 ELSE d.emissoes+1 END,
                        fim_janela=CASE WHEN d.fim_janela<=statement_timestamp() THEN excluded.fim_janela ELSE d.fim_janela END
                    WHERE d.reenviar_em<=statement_timestamp() AND (d.emissoes<5 OR d.fim_janela<=statement_timestamp())
                    """, titular, canal.name(), desafio, prova(titular, canal, desafio, contato.destino(), codigo));
            if (linhas == 0) throw new ResponseStatusException(TOO_MANY_REQUESTS);
            return new Emissao(desafio, contato.destino(), codigo);
        });
    }

    boolean confirmar(UUID titular, Canal canal, UUID desafio, String codigo) {
        return Boolean.TRUE.equals(tx.execute(status -> {
            Contato contato = bloquear(titular, canal);
            if (contato.verificado()) return false;
            var provas = db.queryForList("""
                    UPDATE desafios_contato_driver SET restantes=restantes-1
                    WHERE usuario_id=? AND canal=? AND desafio=? AND restantes>0 AND expira_em>clock_timestamp()
                    RETURNING prova
                    """, byte[].class, titular, canal.name(), desafio);
            if (provas.isEmpty() || !MessageDigest.isEqual(provas.get(0),
                    prova(titular, canal, desafio, contato.destino(), codigo))) return false;
            String coluna = canal == Canal.EMAIL ? "email_verificado" : "celular_verificado";
            db.update("UPDATE contas_driver SET " + coluna + "=true WHERE usuario_id=?", titular);
            db.update("DELETE FROM desafios_contato_driver WHERE usuario_id=? AND canal=?", titular, canal.name());
            return true;
        }));
    }
}
