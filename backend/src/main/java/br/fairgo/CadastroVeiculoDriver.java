package br.fairgo;

import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import br.fairgo.CadastroCompartilhado.*;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/driver")
class CadastroVeiculoDriver {
    record NovoVeiculo(@NotNull @Pattern(regexp=CadastroAdmin.PLACA) String placa) {}
    record Veiculo(UUID id, String placa, String situacao, Dados dados, UUID revisaoCrlv) {}

    private final CadastroCompartilhado cadastros;
    private final JdbcTemplate db;
    private static final String CONSULTA = """
            SELECT v.id,v.placa,v.situacao,d.portas,d.ar_condicionado,d.ano_fabricacao,d.municipio,c.id AS crlv
            FROM veiculos v LEFT JOIN dados_veiculo d ON d.veiculo_id=v.id
            LEFT JOIN documentos_cadastro c ON c.veiculo_id=v.id AND c.tipo='CRLV'
            WHERE v.motorista_id=?
            """;
    private static final RowMapper<Veiculo> VEICULO = (rs,n) -> new Veiculo(rs.getObject("id",UUID.class),
            rs.getString("placa"),rs.getString("situacao"), rs.getObject("portas") == null ? null :
            new Dados(rs.getInt("portas"), rs.getBoolean("ar_condicionado"),rs.getInt("ano_fabricacao"),
                    rs.getString("municipio")), rs.getObject("crlv",UUID.class));

    CadastroVeiculoDriver(CadastroCompartilhado cadastros, JdbcTemplate db) {
        this.cadastros = cadastros;
        this.db = db;
    }

    @PostMapping("/veiculos")
    @ResponseStatus(CREATED)
    public CadastroAdmin.Cadastro cadastrar(@Valid @RequestBody NovoVeiculo veiculo, Principal sessao) {
        return cadastros.criarVeiculo(UUID.fromString(sessao.getName()), veiculo.placa());
    }

    @GetMapping("/veiculos")
    public List<Veiculo> listar(Principal sessao) {
        return db.query(CONSULTA + " ORDER BY v.placa,v.id", VEICULO, UUID.fromString(sessao.getName()));
    }

    @GetMapping("/veiculos/{id}")
    public Veiculo consultar(@PathVariable UUID id, Principal sessao) {
        return db.query(CONSULTA + " AND v.id=?", VEICULO, UUID.fromString(sessao.getName()), id)
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }

    @PutMapping("/veiculos/{id}/dados")
    @ResponseStatus(NO_CONTENT)
    public void dados(@PathVariable UUID id, @Valid @RequestBody Dados dados, Principal sessao) {
        UUID motorista = UUID.fromString(sessao.getName());
        cadastros.dados(id, dados, () -> "driver:" + motorista, motorista);
    }

    @PutMapping(value="/me/documentos/{tipo}", consumes="multipart/form-data")
    public Documento documento(@PathVariable Tipo tipo, @RequestParam MultipartFile arquivo,
                @RequestParam(required=false) Boolean ear, @RequestParam(required=false) LocalDate validade,
                Principal sessao) {
        UUID motorista = UUID.fromString(sessao.getName());
        return cadastros.enviar(Alvo.MOTORISTA, motorista, tipo, arquivo, ear, validade,
                () -> "driver:" + motorista, motorista);
    }

    @PutMapping(value="/veiculos/{id}/crlv", consumes="multipart/form-data")
    public Documento crlv(@PathVariable UUID id, @RequestParam MultipartFile arquivo,
                          @RequestParam(required=false) LocalDate validade, Principal sessao) {
        UUID motorista = UUID.fromString(sessao.getName());
        return cadastros.enviar(Alvo.VEICULO, id, Tipo.CRLV, arquivo, null, validade,
                () -> "driver:" + motorista, motorista);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(CONFLICT)
    void duplicado() {}
}
