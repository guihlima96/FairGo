package br.fairgo;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
class ConferenciaIdentidade {
    enum Resultado { CONFERE, DIVERGENTE }
    record Pedido(@Positive long versaoNome, @NotNull UUID revisaoCnh, @NotNull UUID revisaoFoto,
                  @NotNull Resultado resultado) {}
    record Evento(long sequencia, AutoriaRegistro autoria, Resultado resultado,
                  long versaoNome, String nomeConferido, UUID revisaoCnh, UUID revisaoFoto) {}

    private final JdbcTemplate db;
    private final CadastroCompartilhado cadastros;

    ConferenciaIdentidade(JdbcTemplate db, CadastroCompartilhado cadastros) {
        this.db = db;
        this.cadastros = cadastros;
    }

    @Transactional
    public void registrar(UUID motorista, Pedido pedido, String responsavel) {
        // Mesmo bloqueio da substituição documental. O SELECT seguinte vê a última gravação confirmada.
        cadastros.estado(CadastroCompartilhado.Alvo.MOTORISTA, motorista, true);
        int inseridos = db.update("""
                INSERT INTO conferencias_identidade
                    (motorista_id, versao_nome, nome_conferido, revisao_cnh, revisao_foto, resultado, responsavel)
                SELECT u.id, i.versao_nome, u.nome, i.revisao_cnh, i.revisao_foto, ?, ?
                FROM identidade_motorista_atual i JOIN usuarios u ON u.id=i.id
                WHERE i.id=? AND i.versao_nome=? AND i.revisao_cnh=? AND i.revisao_foto=?
                """, pedido.resultado().name(), responsavel, motorista, pedido.versaoNome(),
                pedido.revisaoCnh(), pedido.revisaoFoto());
        if (inseridos == 0) throw new ResponseStatusException(CONFLICT);
    }

    List<Evento> historico(UUID motorista) {
        cadastros.estado(CadastroCompartilhado.Alvo.MOTORISTA, motorista, false);
        return db.query("""
                SELECT sequencia, responsavel, data, resultado, versao_nome, nome_conferido,
                       revisao_cnh, revisao_foto FROM conferencias_identidade
                WHERE motorista_id=? ORDER BY sequencia DESC
                """, (rs,n) -> new Evento(rs.getLong("sequencia"),
                        new AutoriaRegistro(rs.getString("responsavel"), rs.getTimestamp("data").toInstant()),
                        Resultado.valueOf(rs.getString("resultado")),
                        rs.getLong("versao_nome"), rs.getString("nome_conferido"),
                        rs.getObject("revisao_cnh", UUID.class), rs.getObject("revisao_foto", UUID.class)), motorista);
    }
}
