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
    enum Estado { PENDENTE, APROVADO, REJEITADO }

    record Analise(Estado situacao, String orientacaoPublica,
                   @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                   String codigoPublico,
                   @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                   String documentoAfetado) {
        static Analise de(java.sql.ResultSet rs) throws java.sql.SQLException {
            Estado estado = Estado.valueOf(rs.getString("situacao"));
            if (estado != Estado.REJEITADO) return new Analise(estado, null, null, null);
            String codigo = rs.getString("codigo_publico");
            String mensagem = codigo == null ? MotivoPublico.OUTROS.mensagem : MotivoPublico.valueOf(codigo).mensagem;
            return new Analise(estado, mensagem, codigo, rs.getString("documento_publico"));
        }
    }
    record Motorista(UUID id, Analise analise, boolean nomeVerificado, boolean emailVerificado,
                     boolean celularVerificado, List<Evidencia> documentos) {
        @com.fasterxml.jackson.annotation.JsonProperty
        public boolean identidadeDocumentalVerificada() { return nomeVerificado; }
    }
    record Veiculo(UUID id, String placa, Analise analise, boolean dadosTecnicosInformados, Evidencia crlv) {}
    record PreElegibilidade(boolean motoristaEVeiculoAprovados, boolean autorizadoParaCorridas) {
        static PreElegibilidade cadastral(Motorista motorista, List<Veiculo> veiculosProprios) {
            boolean parAprovado = motorista.analise().situacao() == Estado.APROVADO
                    && veiculosProprios.stream().anyMatch(v -> v.analise().situacao() == Estado.APROVADO);
            return new PreElegibilidade(parAprovado, false);
        }
    }
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
                SELECT u.situacao, r.codigo_publico, r.documento_publico, i.verificada AS nome_verificado, c.email_verificado, c.celular_verificado
                FROM usuarios u JOIN contas_driver c ON c.usuario_id=u.id
                JOIN identidade_motorista_atual i ON i.id=u.id
                LEFT JOIN orientacao_rejeicao_atual r ON r.motorista_id=u.id
                WHERE u.id=? AND u.perfil='MOTORISTA'
                """, (rs, n) -> new Motorista(titular, Analise.de(rs),
                        rs.getBoolean("nome_verificado"), rs.getBoolean("email_verificado"),
                        rs.getBoolean("celular_verificado"), documentos), titular);
        var veiculos = db.query("""
                SELECT r.codigo_publico, r.documento_publico, v.id, v.placa, v.situacao, d.veiculo_id IS NOT NULL AS informado,
                       c.id AS revisao, c.validade
                FROM veiculos v LEFT JOIN dados_veiculo d ON d.veiculo_id=v.id
                LEFT JOIN documentos_cadastro c ON c.veiculo_id=v.id AND c.tipo='CRLV'
                LEFT JOIN orientacao_rejeicao_atual r ON r.veiculo_id=v.id
                WHERE v.motorista_id=? ORDER BY v.placa, v.id
                """, (rs, n) -> new Veiculo(rs.getObject("id", UUID.class), rs.getString("placa"),
                        Analise.de(rs), rs.getBoolean("informado"),
                        new Evidencia(Tipo.CRLV, true, rs.getObject("revisao", UUID.class),
                                rs.getObject("validade", LocalDate.class), null)), titular);
        return new Resposta(motorista, veiculos, PreElegibilidade.cadastral(motorista, veiculos));
    }

    private List<Evidencia> documentosPessoais(UUID titular) {
        var exigidos = CadastroCompartilhado.obrigatorios(Alvo.MOTORISTA);
        var documentos = new EnumMap<Tipo, Evidencia>(Tipo.class);
        for (Tipo tipo : exigidos) {
            documentos.put(tipo, new Evidencia(tipo, true, null, null, null));
        }
        documentos.put(Tipo.TOXICOLOGICO, new Evidencia(Tipo.TOXICOLOGICO, false, null, null, null));
        db.query("""
                SELECT tipo, id, validade, ear FROM documentos_cadastro
                WHERE motorista_id=? AND tipo = ANY (?)
                """, rs -> {
                    Tipo tipo = Tipo.valueOf(rs.getString("tipo"));
                    documentos.put(tipo, new Evidencia(tipo, exigidos.contains(tipo),
                            rs.getObject("id", UUID.class), rs.getObject("validade", LocalDate.class),
                            rs.getObject("ear", Boolean.class)));
                }, titular, documentos.keySet().stream().map(Enum::name).toArray(String[]::new));
        return List.copyOf(documentos.values());
    }
}
