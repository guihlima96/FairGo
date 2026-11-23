package br.fairgo;

import java.security.*;
import java.time.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/admin/validacao")
class ValidacaoAdmin {
    enum Alvo {
        MOTORISTA("usuarios", "motorista_id", " AND perfil='MOTORISTA'"),
        VEICULO("veiculos", "veiculo_id", "");
        final String tabela, coluna, filtro;
        Alvo(String tabela, String coluna, String filtro) {
            this.tabela = tabela;
            this.coluna = coluna;
            this.filtro = filtro;
        }
    }
    enum Tipo { CNH, ANTECEDENTES, TOXICOLOGICO, CRLV }
    enum Resultado { APROVADO, REJEITADO }
    record Dados(@Min(1) @Max(20) int portas, @NotNull Boolean arCondicionado,
                 @Min(1886) int anoFabricacao, @NotNull @Pattern(regexp="[0-9]{7}") String municipio) {}
    record Decisao(@NotNull Resultado situacao, @NotBlank @Size(max=500) String motivo,
                   @Size(max=4) Set<UUID> documentosConferidos) {}
    record Documento(UUID id, Tipo tipo, LocalDate validade, Boolean ear) {}
    private final JdbcTemplate db;
    private final Cofre cofre;
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    ValidacaoAdmin(JdbcTemplate db, Cofre cofre) {
        this.db = db;
        this.cofre = cofre;
    }

    private String estado(Alvo alvo, UUID id, boolean bloquear) {
        var rows = db.queryForList("SELECT situacao FROM " + alvo.tabela + " WHERE id=?" + alvo.filtro
                + (bloquear ? " FOR UPDATE" : ""), String.class, id);
        if (rows.isEmpty()) throw new ResponseStatusException(NOT_FOUND);
        return rows.get(0);
    }

    @PutMapping("/VEICULO/{id}/dados")
    @Transactional
    @ResponseStatus(NO_CONTENT)
    public void dados(@PathVariable UUID id, @Valid @RequestBody Dados dados, Principal admin) {
        String anterior = estado(Alvo.VEICULO, id, true);
        if (dados.anoFabricacao() > Year.now(FUSO).getValue()) throw new ResponseStatusException(BAD_REQUEST);
        db.update("""
                INSERT INTO dados_veiculo (veiculo_id, portas, ar_condicionado, ano_fabricacao, municipio)
                VALUES (?, ?, ?, ?, ?) ON CONFLICT (veiculo_id) DO UPDATE SET portas=excluded.portas,
                ar_condicionado=excluded.ar_condicionado, ano_fabricacao=excluded.ano_fabricacao,
                municipio=excluded.municipio
                """, id, dados.portas(), dados.arCondicionado(), dados.anoFabricacao(), dados.municipio());
        reabrir(Alvo.VEICULO, id, anterior, admin);
    }

    @PutMapping(value="/{alvo}/{id}/documentos/{tipo}", consumes="multipart/form-data")
    @Transactional
    public Documento enviar(@PathVariable Alvo alvo, @PathVariable UUID id, @PathVariable Tipo tipo,
                    @RequestParam MultipartFile arquivo, @RequestParam(required=false) Boolean ear,
                    @RequestParam(required=false) LocalDate validade, Principal admin) {
        if ((tipo == Tipo.CRLV) != (alvo == Alvo.VEICULO)
                || (tipo == Tipo.CNH) != (ear != null) || (tipo == Tipo.CNH && validade == null))
            throw new ResponseStatusException(BAD_REQUEST);
        String anterior = estado(alvo, id, true);
        UUID revisao = UUID.randomUUID();
        db.update("INSERT INTO documentos_cadastro (id," + alvo.coluna + ",tipo,validade,ear,imagem_cifrada)"
                + " VALUES (?,?,?,?,?,?) ON CONFLICT (" + alvo.coluna + ",tipo) DO UPDATE SET id=excluded.id,"
                + " validade=excluded.validade,ear=excluded.ear,imagem_cifrada=excluded.imagem_cifrada",
                revisao, id, tipo.name(), validade, ear, cofre.preparar(arquivo,id,tipo.name()));
        reabrir(alvo, id, anterior, admin);
        return new Documento(revisao, tipo, validade, ear);
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
        return Map.of("situacao",estado(alvo,id,false), "documentos",documentos(alvo,id),
                "dadosVeiculo", alvo == Alvo.VEICULO ? db.queryForList(
                "SELECT portas, ar_condicionado, ano_fabricacao, municipio FROM dados_veiculo WHERE veiculo_id=?",id) : List.of());
    }

    private List<Documento> documentos(Alvo alvo, UUID id) {
        return db.query("SELECT id,tipo,validade,ear FROM documentos_cadastro WHERE " + alvo.coluna + "=? ORDER BY tipo",
                (r,n) -> new Documento(r.getObject("id",UUID.class),Tipo.valueOf(r.getString("tipo")),
                        r.getObject("validade",LocalDate.class),r.getObject("ear",Boolean.class)), id);
    }

    @GetMapping("/{alvo}/{id}/documentos/{revisao}")
    public ResponseEntity<byte[]> imagem(@PathVariable Alvo alvo, @PathVariable UUID id,
                              @PathVariable UUID revisao) throws GeneralSecurityException {
        var rows = db.queryForList("SELECT tipo, imagem_cifrada FROM documentos_cadastro WHERE "
                + alvo.coluna + "=? AND id=?", id, revisao);
        if (rows.isEmpty()) throw new ResponseStatusException(NOT_FOUND);
        var row = rows.get(0);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_JPEG)
                .header("Content-Disposition","attachment; filename=documento.jpg")
                .body(cofre.abrir((byte[]) row.get("imagem_cifrada"), id, (String) row.get("tipo")));
    }

    @PostMapping("/{alvo}/{id}/decisoes")
    @Transactional
    @ResponseStatus(CREATED)
    public void decidir(@PathVariable Alvo alvo, @PathVariable UUID id, @Valid @RequestBody Decisao decisao,
                        Principal admin) {
        if (!estado(alvo,id,true).equals("PENDENTE")) throw new ResponseStatusException(CONFLICT);
        if (decisao.situacao() == Resultado.APROVADO) {
            var docs = documentos(alvo,id);
            int exigidos = alvo == Alvo.MOTORISTA ? 3 : 1;
            if (docs.size() != exigidos || decisao.documentosConferidos() == null
                    || decisao.documentosConferidos().size() != exigidos
                    || docs.stream().anyMatch(d -> !decisao.documentosConferidos().contains(d.id())
                    || Boolean.FALSE.equals(d.ear())
                    || (d.validade() != null && d.validade().isBefore(LocalDate.now(FUSO)))))
                throw new ResponseStatusException(UNPROCESSABLE_ENTITY);
            if (alvo == Alvo.VEICULO && !veiculoElegivel(id)) throw new ResponseStatusException(UNPROCESSABLE_ENTITY);
        }
        registrar(alvo,id,decisao.situacao().name(),decisao.motivo().strip(),admin);
    }

    private boolean veiculoElegivel(UUID id) {
        return Boolean.TRUE.equals(db.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM dados_veiculo v JOIN limites_municipais m ON m.ibge=v.municipio
                WHERE v.veiculo_id=? AND portas>=4 AND ar_condicionado
                AND ? - ano_fabricacao BETWEEN 0 AND LEAST(11,m.idade_maxima))
                """, Boolean.class, id, Year.now(FUSO).getValue()));
    }

    private void reabrir(Alvo alvo, UUID id, String anterior, Principal admin) {
        if (!anterior.equals("PENDENTE")) registrar(alvo,id,"PENDENTE","Cadastro atualizado",admin);
    }

    private void registrar(Alvo alvo, UUID id, String status, String motivo, Principal admin) {
        db.update("UPDATE " + alvo.tabela + " SET situacao=? WHERE id=?", status, id);
        db.update("INSERT INTO decisoes_cadastro (" + alvo.coluna + ",situacao,motivo,responsavel) VALUES (?,?,?,?)",
                id,status,motivo,admin.getName());
    }

    @GetMapping("/{alvo}/{id}/historico")
    public List<Map<String,Object>> historico(@PathVariable Alvo alvo, @PathVariable UUID id) {
        estado(alvo,id,false);
        return db.queryForList("SELECT sequencia,situacao,motivo,responsavel,data FROM decisoes_cadastro WHERE "
                + alvo.coluna + "=? ORDER BY sequencia",id);
    }
}
