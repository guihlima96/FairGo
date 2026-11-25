package br.fairgo;

import br.fairgo.documentos.FormatoDocumento;

import java.security.*;
import java.time.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import br.fairgo.CadastroCompartilhado.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/admin/validacao")
class ValidacaoAdmin {
    enum Resultado { APROVADO, REJEITADO }
    record Decisao(@NotNull Resultado situacao, @NotBlank @Size(max=500) String motivo,
                   Boolean identidadeConferida, @Size(max=5) Set<UUID> documentosConferidos) {}
    private final JdbcTemplate db;
    private final Cofre cofre;
    private final CadastroCompartilhado cadastros;
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    ValidacaoAdmin(JdbcTemplate db, Cofre cofre, CadastroCompartilhado cadastros) {
        this.db = db;
        this.cofre = cofre;
        this.cadastros = cadastros;
    }

    @PutMapping("/VEICULO/{id}/dados")
    @Transactional
    @ResponseStatus(NO_CONTENT)
    public void dados(@PathVariable UUID id, @Valid @RequestBody Dados dados, Principal admin) {
        cadastros.dados(id, dados, admin, null);
    }

    @PutMapping(value="/{alvo}/{id}/documentos/{tipo}", consumes="multipart/form-data")
    public Documento enviar(@PathVariable Alvo alvo, @PathVariable UUID id, @PathVariable Tipo tipo,
                    @RequestParam MultipartFile arquivo, @RequestParam(required=false) Boolean ear,
                    @RequestParam(required=false) LocalDate validade, Principal admin) {
        return cadastros.enviar(alvo,id,tipo,arquivo,ear,validade,admin,null);
    }

    record ItemFila(UUID id, Alvo tipo, String descricao, String situacao, boolean prontoParaAnalise,
                    Set<Tipo> enviados, Set<Tipo> ausentes, Boolean dadosTecnicosPresentes) {}

    @GetMapping("/fila")
    public List<ItemFila> fila() {
        return db.query("""
                SELECT u.id, 'MOTORISTA' AS alvo, u.nome AS descricao, u.situacao, NULL::boolean AS tecnicos,
                       ARRAY(SELECT d.tipo FROM documentos_cadastro d WHERE d.motorista_id=u.id) AS tipos
                FROM usuarios u WHERE u.perfil='MOTORISTA'
                UNION ALL
                SELECT v.id, 'VEICULO', v.placa, v.situacao, t.veiculo_id IS NOT NULL,
                       ARRAY(SELECT d.tipo FROM documentos_cadastro d WHERE d.veiculo_id=v.id)
                FROM veiculos v LEFT JOIN dados_veiculo t ON t.veiculo_id=v.id
                ORDER BY alvo, descricao, id
                """, (r,n) -> {
            Alvo alvo = Alvo.valueOf(r.getString("alvo"));
            var ausentes = CadastroCompartilhado.obrigatorios(alvo);
            var enviados = EnumSet.noneOf(Tipo.class);
            for (String nome : (String[]) r.getArray("tipos").getArray()) {
                Tipo tipo = Tipo.valueOf(nome);
                if (ausentes.remove(tipo)) enviados.add(tipo);
            }
            Boolean tecnicos = r.getObject("tecnicos", Boolean.class);
            return new ItemFila(r.getObject("id", UUID.class), alvo, r.getString("descricao"),
                    r.getString("situacao"), ausentes.isEmpty() && !Boolean.FALSE.equals(tecnicos),
                    enviados, ausentes, tecnicos);
        });
    }

    @GetMapping("/pendentes")
    public List<Map<String,Object>> pendentes() {
        return db.queryForList("""
                SELECT id, 'MOTORISTA' AS tipo, nome AS descricao FROM usuarios
                WHERE perfil='MOTORISTA' AND situacao='PENDENTE'
                UNION ALL
                SELECT id, 'VEICULO' AS tipo, placa AS descricao FROM veiculos WHERE situacao='PENDENTE'
                ORDER BY tipo, descricao, id
                """);
    }

    @GetMapping("/{alvo}/{id}")
    public Map<String,Object> consultar(@PathVariable Alvo alvo, @PathVariable UUID id) {
        String situacao = cadastros.estado(alvo,id,false);
        var detalhe = new LinkedHashMap<String,Object>();
        detalhe.put("nomeMotorista", "");
        if (alvo == Alvo.MOTORISTA) detalhe.putAll(db.queryForMap(
                "SELECT nome AS \"nomeMotorista\", versao_nome AS \"versaoNome\" FROM usuarios WHERE id=?", id));
        detalhe.putAll(Map.of("situacao",situacao, "documentos",documentos(alvo,id),
                "dadosVeiculo", alvo == Alvo.VEICULO ? db.queryForList(
                "SELECT portas, ar_condicionado, ano_fabricacao, municipio FROM dados_veiculo WHERE veiculo_id=?",id) : List.of()));
        return detalhe;
    }

    private List<Documento> documentos(Alvo alvo, UUID id) {
        return db.query("SELECT id,tipo,validade,ear FROM documentos_cadastro WHERE " + alvo.coluna + "=? ORDER BY tipo",
                (r,n) -> new Documento(r.getObject("id",UUID.class),Tipo.valueOf(r.getString("tipo")),
                        r.getObject("validade",LocalDate.class),r.getObject("ear",Boolean.class)), id);
    }

    @GetMapping("/{alvo}/{id}/documentos/{revisao}")
    public ResponseEntity<byte[]> imagem(@PathVariable Alvo alvo, @PathVariable UUID id,
                              @PathVariable UUID revisao) throws GeneralSecurityException {
        var rows = db.queryForList("SELECT tipo, formato, imagem_cifrada FROM documentos_cadastro WHERE "
                + alvo.coluna + "=? AND id=?", id, revisao);
        if (rows.isEmpty()) throw new ResponseStatusException(NOT_FOUND);
        var row = rows.get(0);
        byte[] dados = cofre.abrir((byte[]) row.get("imagem_cifrada"), id, (String) row.get("tipo"), (String) row.get("formato"));
        boolean pdf = FormatoDocumento.ehPdf(dados);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(pdf ? MediaType.APPLICATION_PDF : MediaType.IMAGE_JPEG)
                .header("Content-Disposition", pdf ? "attachment; filename=documento.pdf" : "attachment; filename=documento.jpg")
                .body(dados);
    }

    @PostMapping("/{alvo}/{id}/decisoes")
    @Transactional
    @ResponseStatus(CREATED)
    public void decidir(@PathVariable Alvo alvo, @PathVariable UUID id, @Valid @RequestBody Decisao decisao,
                        Principal admin) {
        if (!cadastros.estado(alvo,id,true).equals("PENDENTE")) throw new ResponseStatusException(CONFLICT);
        if (decisao.situacao() == Resultado.APROVADO) {
            var docs = documentos(alvo,id);
            var exigidos = CadastroCompartilhado.obrigatorios(alvo);
            var conferidos = decisao.documentosConferidos();
            var atuais = docs.stream().map(Documento::id).toList();
            if (conferidos == null || !atuais.containsAll(conferidos)
                    || (alvo == Alvo.MOTORISTA && !Boolean.TRUE.equals(decisao.identidadeConferida())))
                throw new ResponseStatusException(UNPROCESSABLE_ENTITY);
            for (Documento doc : docs) {
                if (!exigidos.remove(doc.tipo())) continue;
                if (!conferidos.contains(doc.id()) || Boolean.FALSE.equals(doc.ear())
                        || (doc.validade() != null && doc.validade().isBefore(LocalDate.now(FUSO))))
                    throw new ResponseStatusException(UNPROCESSABLE_ENTITY);
            }
            if (!exigidos.isEmpty()) throw new ResponseStatusException(UNPROCESSABLE_ENTITY);
            if (alvo == Alvo.VEICULO && !veiculoElegivel(id)) throw new ResponseStatusException(UNPROCESSABLE_ENTITY);
        }
        cadastros.registrar(alvo,id,decisao.situacao().name(),decisao.motivo().strip(),admin,
                decisao.situacao() == Resultado.APROVADO
                        ? decisao.documentosConferidos().toArray(UUID[]::new) : null);
    }

    private boolean veiculoElegivel(UUID id) {
        return Boolean.TRUE.equals(db.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM dados_veiculo v JOIN limites_municipais m ON m.ibge=v.municipio
                WHERE v.veiculo_id=? AND portas>=4 AND ar_condicionado
                AND ? - ano_fabricacao BETWEEN 0 AND LEAST(11,m.idade_maxima))
                """, Boolean.class, id, Year.now(FUSO).getValue()));
    }

    @GetMapping("/{alvo}/{id}/historico")
    public List<Map<String,Object>> historico(@PathVariable Alvo alvo, @PathVariable UUID id) {
        cadastros.estado(alvo,id,false);
        return db.query("SELECT sequencia,situacao,motivo,responsavel,data,revisoes_conferidas FROM decisoes_cadastro WHERE "
                + alvo.coluna + "=? ORDER BY sequencia", (rs, n) -> {
                    var evento = new LinkedHashMap<String,Object>();
                    for (String campo : List.of("sequencia","situacao","motivo","responsavel","data"))
                        evento.put(campo, rs.getObject(campo));
                    var revisoes = rs.getArray("revisoes_conferidas");
                    evento.put("revisoes_conferidas", revisoes == null ? null : revisoes.getArray());
                    return evento;
                },id);
    }
}
