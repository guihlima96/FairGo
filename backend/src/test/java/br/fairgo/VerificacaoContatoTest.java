package br.fairgo;

import java.security.SecureRandom;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import static br.fairgo.VerificacaoContato.Canal.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="PGDATABASE", matches=".+_test")
class VerificacaoContatoTest {
    private final UUID titular = UUID.randomUUID();
    private JdbcTemplate db;
    private VerificacaoContato servico;

    @BeforeEach void preparar() {
        var fonte = new DriverManagerDataSource(System.getenv("FAIRGO_DB_URL"),
                System.getenv("FAIRGO_DB_USER"), System.getenv("FAIRGO_DB_PASSWORD"));
        db = new JdbcTemplate(fonte);
        assertEquals(System.getenv("PGDATABASE"), db.queryForObject("SELECT current_database()", String.class));
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql"),
                new ClassPathResource("driver.sql")).execute(fonte);
        byte[] chave = new byte[32];
        new SecureRandom().nextBytes(chave);
        servico = new VerificacaoContato(db, new TransactionTemplate(new DataSourceTransactionManager(fonte)),
                Base64.getEncoder().encodeToString(chave));
        db.update("INSERT INTO usuarios(id,nome,email,perfil) VALUES (?,'Teste',?,'MOTORISTA')",
                titular, titular + "@example.invalid");
        String cpf = String.format(Locale.ROOT, "%011d", new SecureRandom().nextLong(100_000_000_000L));
        db.update("INSERT INTO contas_driver(usuario_id,cpf,celular,senha_hash) VALUES (?,?,'+5511999990000','fixture')",
                titular, cpf);
    }

    @AfterEach void limpar() {
        if (db == null || !System.getenv("PGDATABASE").equals(db.queryForObject("SELECT current_database()", String.class))) return;
        db.update("DELETE FROM desafios_contato_driver WHERE usuario_id=?", titular);
        db.update("DELETE FROM contas_driver WHERE usuario_id=?", titular);
        db.update("DELETE FROM usuarios WHERE id=?", titular);
    }

    @Test void limitesReenvioExpiracaoEUsoUnico() {
        var antigo = servico.emitir(titular, EMAIL);
        assertEquals(429, assertThrows(ResponseStatusException.class,
                () -> servico.emitir(titular, EMAIL)).getStatusCode().value());
        liberarReenvio();
        var atual = servico.emitir(titular, EMAIL);
        assertFalse(servico.confirmar(titular, EMAIL, antigo.desafio(), antigo.codigo()));
        db.update("UPDATE desafios_contato_driver SET expira_em=now() WHERE usuario_id=?", titular);
        assertFalse(servico.confirmar(titular, EMAIL, atual.desafio(), atual.codigo()));
        for (int i=0; i<3; i++) {
            liberarReenvio();
            atual = servico.emitir(titular, EMAIL);
        }
        liberarReenvio();
        assertEquals(429, assertThrows(ResponseStatusException.class,
                () -> servico.emitir(titular, EMAIL)).getStatusCode().value());
        assertTrue(servico.confirmar(titular, EMAIL, atual.desafio(), atual.codigo()));
        assertFalse(servico.confirmar(titular, EMAIL, atual.desafio(), atual.codigo()));
        assertEquals(Boolean.TRUE, db.queryForObject("""
                SELECT email_verificado AND NOT celular_verificado AND NOT nome_verificado
                FROM contas_driver WHERE usuario_id=?
                """, Boolean.class, titular));
    }

    private void liberarReenvio() {
        db.update("UPDATE desafios_contato_driver SET reenviar_em=now() WHERE usuario_id=?", titular);
    }

    @Test void tentativasCanalEDestino() {
        var emissao = servico.emitir(titular, CELULAR);
        assertFalse(servico.confirmar(titular, EMAIL, emissao.desafio(), emissao.codigo()));
        db.update("UPDATE contas_driver SET celular='+5511999990001' WHERE usuario_id=?", titular);
        assertFalse(servico.confirmar(titular, CELULAR, emissao.desafio(), emissao.codigo()));
        db.update("UPDATE contas_driver SET celular=? WHERE usuario_id=?", emissao.destino(), titular);
        String errado = emissao.codigo().equals("00000000") ? "00000001" : "00000000";
        for (int i=0; i<4; i++) assertFalse(servico.confirmar(titular, CELULAR, emissao.desafio(), errado));
        assertFalse(servico.confirmar(titular, CELULAR, emissao.desafio(), emissao.codigo()));
        liberarReenvio();
        var novo = servico.emitir(titular, CELULAR);
        assertTrue(servico.confirmar(titular, CELULAR, novo.desafio(), novo.codigo()));
        assertEquals(Boolean.TRUE, db.queryForObject("""
                SELECT celular_verificado AND NOT email_verificado AND NOT nome_verificado
                FROM contas_driver WHERE usuario_id=?
                """, Boolean.class, titular));
    }
}
