package br.fairgo;

import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class ControleTentativas {
    record Reserva(String chave, Timestamp fim) {}
    private final JdbcTemplate db;

    ControleTentativas(JdbcTemplate db) {
        this.db = db;
    }

    void limparExpirados() {
        db.update("""
                DELETE FROM limites_driver WHERE chave IN (
                    SELECT chave FROM limites_driver WHERE ate<=statement_timestamp()
                    ORDER BY ate,chave LIMIT 64 FOR UPDATE SKIP LOCKED)
                """);
    }

    // Em uma transação, o chamador pode reservar várias cotas ou desfazer o conjunto.
    Reserva reservar(String chave, int maximo, int segundos) {
        String digest = TokenOpaco.hash(chave);
        var fins = db.queryForList("""
                INSERT INTO limites_driver AS l (chave,contador,ate)
                VALUES (?,1,statement_timestamp()+? * interval '1 second')
                ON CONFLICT (chave) DO UPDATE SET
                    contador=CASE WHEN l.ate<=statement_timestamp() THEN 1 ELSE l.contador+1 END,
                    ate=CASE WHEN l.ate<=statement_timestamp() THEN excluded.ate ELSE l.ate END
                WHERE l.ate<=statement_timestamp() OR l.contador<?
                RETURNING ate
                """, Timestamp.class, digest, segundos, maximo);
        return fins.isEmpty() ? null : new Reserva(digest, fins.get(0));
    }

    void devolver(Reserva reserva) {
        db.update("""
                UPDATE limites_driver SET contador=contador-1
                WHERE chave=? AND ate=? AND contador>0
                """, reserva.chave(), reserva.fim());
    }
}
