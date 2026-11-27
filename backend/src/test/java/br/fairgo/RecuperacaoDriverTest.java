package br.fairgo;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="PGDATABASE", matches=".+_test")
class RecuperacaoDriverTest {
    private final UUID titular = UUID.randomUUID();
    private final String email = titular + "@example.invalid";
    private static final Duration PRAZO_TESTE = Duration.ofMinutes(2);
    private JdbcTemplate db;
    private RecuperacaoDriver servico;

    @BeforeEach void preparar() {
        var fonte = new DriverManagerDataSource(System.getenv("FAIRGO_DB_URL"),
                System.getenv("FAIRGO_DB_USER"), System.getenv("FAIRGO_DB_PASSWORD"));
        var banco = new JdbcTemplate(fonte);
        assertEquals(System.getenv("PGDATABASE"), banco.queryForObject("SELECT current_database()", String.class));
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql"),
                new ClassPathResource("driver.sql")).execute(fonte);
        db = banco;
        servico = new RecuperacaoDriver(db, new TransactionTemplate(new DataSourceTransactionManager(fonte)),
                new SegurancaDriver().senhasDriver());
        db.update("INSERT INTO usuarios(id,nome,email,perfil) VALUES (?,'Teste',?,'MOTORISTA')", titular,email);
        String cpf = String.format(Locale.ROOT,"%011d",new SecureRandom().nextLong(100_000_000_000L));
        db.update("INSERT INTO contas_driver(usuario_id,cpf,celular,senha_hash) VALUES (?,?,'+5511999990000','fixture')",
                titular,cpf);
    }

    @AfterEach void limpar() {
        if (db == null) return;
        db.update("DELETE FROM recuperacoes_driver WHERE usuario_id=?", titular);
        db.update("DELETE FROM contas_driver WHERE usuario_id=?", titular);
        db.update("DELETE FROM usuarios WHERE id=?", titular);
    }

    private void verificar() {
        db.update("UPDATE contas_driver SET email_verificado=true WHERE usuario_id=?", titular);
    }

    @Test void entregaAssincronaPermiteRecuperarComTokenDoEmail() throws Exception {
        verificar();
        var caixa = new ArrayBlockingQueue<br.fairgo.notificacoes.Mensagem>(1);
        var emailTeste = new br.fairgo.notificacoes.EmailHomologacao(m -> {
            caixa.add(m);
            return br.fairgo.notificacoes.Resultado.ACEITO;
        }, Set.of(email));
        var fluxo = new EntregaDriver(null, servico, emailTeste, 600);
        try {
            fluxo.solicitarRecuperacao(email);
            var mensagem = caixa.poll(5, TimeUnit.SECONDS);
            assertNotNull(mensagem);
            assertEquals(email, mensagem.destino());
            var token = java.util.regex.Pattern.compile("rec_[0-9a-f]{64}").matcher(mensagem.texto());
            assertTrue(token.find());
            assertTrue(servico.redefinir(token.group(), "Senha de teste ficticia"));
            assertFalse(servico.redefinir(token.group(), "Senha de teste ficticia"));
        } finally { fluxo.encerrar(); }
    }

    @Test void recuperacaoPublicaConcorrenteEmiteUmaVezParaDestinoVerificado() throws Exception {
        assertTrue(servico.emitirParaEmail(email, PRAZO_TESTE).isEmpty());
        verificar();
        var barreira = new CyclicBarrier(2);
        Callable<Optional<RecuperacaoDriver.Entrega>> chamada = () -> {
            barreira.await(10,TimeUnit.SECONDS);
            return servico.emitirParaEmail(email, PRAZO_TESTE);
        };
        var pool = Executors.newFixedThreadPool(2);
        try {
            var primeira = pool.submit(chamada);
            var segunda = pool.submit(chamada);
            var entregas = java.util.stream.Stream.of(primeira.get(15, TimeUnit.SECONDS),
                    segunda.get(15, TimeUnit.SECONDS)).flatMap(Optional::stream).toList();
            assertEquals(1, entregas.size());
            assertEquals(email, entregas.get(0).destino());
            assertTrue(servico.redefinir(entregas.get(0).token(), "Senha ficticia de teste"));
        } finally { pool.shutdownNow(); }
    }

    @Test void destinoSubstituicaoEExpiracao() {
        assertTrue(servico.emitir(titular,PRAZO_TESTE).isEmpty());
        verificar();
        var anterior = servico.emitir(titular,PRAZO_TESTE).orElseThrow();
        var atual = servico.emitir(titular,PRAZO_TESTE).orElseThrow();
        assertTrue(atual.token().matches("rec_[0-9a-f]{64}"));
        assertEquals(email, atual.destino());
        assertEquals("Entrega[protegida]", atual.toString());
        assertFalse(servico.redefinir(anterior.token(),"Senha fictícia anterior"));
        assertEquals(TokenOpaco.hash(atual.token()), db.queryForObject(
                "SELECT digest FROM recuperacoes_driver WHERE usuario_id=?",String.class,titular));
        db.update("UPDATE recuperacoes_driver SET expira_em=now() WHERE usuario_id=?",titular);
        assertFalse(servico.redefinir(atual.token(),"Senha fictícia vencida"));
        var antesDaTroca = servico.emitir(titular,PRAZO_TESTE).orElseThrow();
        db.update("UPDATE usuarios SET email=? WHERE id=?", "novo-"+email,titular);
        assertTrue(servico.emitir(titular,PRAZO_TESTE).isEmpty());
        db.update("UPDATE usuarios SET email=? WHERE id=?", email,titular);
        verificar();
        assertFalse(servico.redefinir(antesDaTroca.token(),"Senha fictícia antiga"));
    }

    @Test void emissoesConcorrentesMantemUmTokenConsumivel() throws Exception {
        verificar();
        var inicio = new CyclicBarrier(2);
        Callable<RecuperacaoDriver.Entrega> emitir = () -> {
            inicio.await(10,TimeUnit.SECONDS);
            return servico.emitir(titular,PRAZO_TESTE).orElseThrow();
        };
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(emitir);
            var b = pool.submit(emitir);
            var primeira = a.get(15,TimeUnit.SECONDS);
            var segunda = b.get(15,TimeUnit.SECONDS);
            assertNotEquals(primeira.referencia(), segunda.referencia());
            assertTrue(servico.redefinir(primeira.token(),"Senha fictícia de teste") ^
                       servico.redefinir(segunda.token(),"Senha fictícia de teste"));
            assertFalse(servico.redefinir(primeira.token(),"Senha fictícia repetida"));
            assertFalse(servico.redefinir(segunda.token(),"Senha fictícia repetida"));
        } finally {
            pool.shutdownNow();
        }
    }
}
