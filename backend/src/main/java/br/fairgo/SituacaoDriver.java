package br.fairgo;

import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import br.fairgo.CadastroCompartilhado.Alvo;
import br.fairgo.CadastroCompartilhado.Tipo;

@RestController
@RequestMapping("/api/driver/me")
class SituacaoDriver {
    record Evidencia(Tipo tipo, boolean obrigatorio, UUID revisaoAtual, LocalDate validade, Boolean ear) {}
    record Analise(String situacao, String orientacaoPublica) {
        static Analise de(String situacao) {
            return new Analise(situacao, "REJEITADO".equals(situacao)
                    ? "Revise os dados e documentos deste cadastro." : null);
        }
    }
    record Motorista(UUID id, Analise analise, boolean nomeVerificado, boolean emailVerificado,
                     boolean celularVerificado, List<Evidencia> documentos) {}
    record Veiculo(UUID id, String placa, Analise analise, boolean dadosTecnicosInformados, Evidencia crlv) {}
    record PreElegibilidade(boolean motoristaEVeiculoAprovados, boolean autorizadoParaCorridas) {}
    record Resposta(Motorista motorista, List<Veiculo> veiculos, PreElegibilidade preElegibilidade) {}

    private final JdbcTemplate db;

    SituacaoDriver(JdbcTemplate db) {
        this.db = db;
    }

    @GetMapping("/situacao")
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Resposta consultar(Principal sessao) {
        UUID titular = UUID.fromString(sessao.getName());
        var documentos = documentosPessoais(titular);
        var motorista = db.queryForObject("""
                SELECT u.situacao, c.nome_verificado, c.email_verificado, c.celular_verificado
                FROM usuarios u JOIN contas_driver c ON c.usuario_id=u.id
                WHERE u.id=? AND u.perfil='MOTORISTA'
                """, (rs, n) -> new Motorista(titular, Analise.de(rs.getString("situacao")),
                        rs.getBoolean("nome_verificado"), rs.getBoolean("email_verificado"),
                        rs.getBoolean("celular_verificado"), documentos), titular);
        var veiculos = db.query("""
                SELECT v.id, v.placa, v.situacao, d.veiculo_id IS NOT NULL AS informado,
                       c.id AS revisao, c.validade
                FROM veiculos v LEFT JOIN dados_veiculo d ON d.veiculo_id=v.id
                LEFT JOIN documentos_cadastro c ON c.veiculo_id=v.id AND c.tipo='CRLV'
                WHERE v.motorista_id=? ORDER BY v.placa, v.id
                """, (rs, n) -> new Veiculo(rs.getObject("id", UUID.class), rs.getString("placa"),
                        Analise.de(rs.getString("situacao")), rs.getBoolean("informado"),
                        new Evidencia(Tipo.CRLV, true, rs.getObject("revisao", UUID.class),
                                rs.getObject("validade", LocalDate.class), null)), titular);
        boolean parAprovado = "APROVADO".equals(motorista.analise().situacao())
                && veiculos.stream().anyMatch(v -> "APROVADO".equals(v.analise().situacao()));
        return new Resposta(motorista, veiculos, new PreElegibilidade(parAprovado, false));
    }

    private List<Evidencia> documentosPessoais(UUID titular) {
        var exigidos = CadastroCompartilhado.obrigatorios(Alvo.MOTORISTA);
        var documentos = new EnumMap<Tipo, Evidencia>(Tipo.class);
        for (Tipo tipo : exigidos) {
            documentos.put(tipo, new Evidencia(tipo, true, null, null, null));
        }
        documentos.put(Tipo.TOXICOLOGICO, new Evidencia(Tipo.TOXICOLOGICO, false, null, null, null));
        db.query("""
                SELECT tipo, id, validade, ear FROM documentos_cadastro WHERE motorista_id=?
                """, rs -> {
                    Tipo tipo = Tipo.valueOf(rs.getString("tipo"));
                    documentos.put(tipo, new Evidencia(tipo, exigidos.contains(tipo),
                            rs.getObject("id", UUID.class), rs.getObject("validade", LocalDate.class),
                            rs.getObject("ear", Boolean.class)));
                }, titular);
        return List.copyOf(documentos.values());
    }
}
