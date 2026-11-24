package br.fairgo;

import java.security.Principal;
import java.time.*;
import java.util.*;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
class CadastroCompartilhado {
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
    static EnumSet<Tipo> obrigatorios(Alvo alvo) {
        return alvo == Alvo.MOTORISTA
                ? EnumSet.of(Tipo.CNH, Tipo.ANTECEDENTES, Tipo.FOTO_PERFIL, Tipo.RESIDENCIA)
                : EnumSet.of(Tipo.CRLV);
    }

    enum Tipo { CNH, ANTECEDENTES, TOXICOLOGICO, FOTO_PERFIL, RESIDENCIA, CRLV }
    record Dados(@Min(1) @Max(20) int portas, @NotNull Boolean arCondicionado,
                 @Min(1886) int anoFabricacao, @NotNull @Pattern(regexp="[0-9]{7}") String municipio) {}
    record Documento(UUID id, Tipo tipo, LocalDate validade, Boolean ear) {}
    private final JdbcTemplate db;
    private final Cofre cofre;
    private final TransactionTemplate transacao;
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    CadastroCompartilhado(JdbcTemplate db, Cofre cofre, TransactionTemplate transacao) {
        this.transacao = transacao;
        this.db = db;
        this.cofre = cofre;
    }

    String estado(Alvo alvo, UUID id, boolean bloquear) {
        return estado(alvo, id, bloquear, null);
    }

    private String estado(Alvo alvo, UUID id, boolean bloquear, UUID motorista) {
        boolean conferirDono = motorista != null && alvo == Alvo.VEICULO;
        if (motorista != null && alvo == Alvo.MOTORISTA && !motorista.equals(id))
            throw new ResponseStatusException(NOT_FOUND);
        var rows = db.queryForList("SELECT situacao FROM " + alvo.tabela + " WHERE id=?" + alvo.filtro
                + (conferirDono ? " AND motorista_id=?" : "") + (bloquear ? " FOR UPDATE" : ""),
                String.class, conferirDono ? new Object[]{id,motorista} : new Object[]{id});
        if (rows.isEmpty()) throw new ResponseStatusException(NOT_FOUND);
        return rows.get(0);
    }

    CadastroAdmin.Cadastro criarVeiculo(UUID motorista, String valor) {
        UUID id = UUID.randomUUID();
        String placa = valor.replace("-", "").toUpperCase(Locale.ROOT);
        int inseridos = db.update("""
                INSERT INTO veiculos (id, motorista_id, placa)
                SELECT ?, id, ? FROM usuarios WHERE id = ? AND perfil = 'MOTORISTA'
                """, id, placa, motorista);
        if (inseridos == 0) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Motorista inválido");
        }
        return new CadastroAdmin.Cadastro(id, "PENDENTE");
    }

    @Transactional
    public void dados(UUID id, Dados dados, Principal responsavel, UUID motorista) {
        String anterior = estado(Alvo.VEICULO, id, true, motorista);
        if (dados.anoFabricacao() > Year.now(FUSO).getValue()) throw new ResponseStatusException(BAD_REQUEST);
        db.update("""
                INSERT INTO dados_veiculo (veiculo_id, portas, ar_condicionado, ano_fabricacao, municipio)
                VALUES (?, ?, ?, ?, ?) ON CONFLICT (veiculo_id) DO UPDATE SET portas=excluded.portas,
                ar_condicionado=excluded.ar_condicionado, ano_fabricacao=excluded.ano_fabricacao,
                municipio=excluded.municipio
                """, id, dados.portas(), dados.arCondicionado(), dados.anoFabricacao(), dados.municipio());
        reabrir(Alvo.VEICULO, id, anterior, responsavel);
    }

    Documento enviar(Alvo alvo, UUID id, Tipo tipo, MultipartFile arquivo, Boolean ear,
                     LocalDate validade, Principal responsavel, UUID motorista) {
        if ((tipo == Tipo.CRLV) != (alvo == Alvo.VEICULO)
                || (tipo == Tipo.CNH) != (ear != null) || (tipo == Tipo.CNH && validade == null))
            throw new ResponseStatusException(BAD_REQUEST);
        estado(alvo, id, false, motorista);
        var documento = cofre.preparar(arquivo, id, tipo.name());
        return transacao.execute(status -> {
            String anterior = estado(alvo, id, true, motorista);
            UUID revisao = UUID.randomUUID();
            db.update("INSERT INTO documentos_cadastro (id," + alvo.coluna + ",tipo,validade,ear,imagem_cifrada,formato)"
                    + " VALUES (?,?,?,?,?,?,?) ON CONFLICT (" + alvo.coluna + ",tipo) DO UPDATE SET id=excluded.id,"
                    + " validade=excluded.validade,ear=excluded.ear,imagem_cifrada=excluded.imagem_cifrada, formato=excluded.formato",
                    revisao, id, tipo.name(), validade, ear, documento.cifrado(), documento.formato());
            reabrir(alvo, id, anterior, responsavel);
            return new Documento(revisao, tipo, validade, ear);
        });
    }

    private void reabrir(Alvo alvo, UUID id, String anterior, Principal responsavel) {
        if (!anterior.equals("PENDENTE")) registrar(alvo,id,"PENDENTE","Cadastro atualizado",responsavel,null);
    }

    void registrar(Alvo alvo, UUID id, String status, String motivo, Principal responsavel, UUID[] revisoes) {
        db.update("UPDATE " + alvo.tabela + " SET situacao=? WHERE id=?", status, id);
        db.update("INSERT INTO decisoes_cadastro (" + alvo.coluna + ",situacao,motivo,responsavel,revisoes_conferidas) VALUES (?,?,?,?,?)",
                id,status,motivo,responsavel.getName(),revisoes);
    }

}
