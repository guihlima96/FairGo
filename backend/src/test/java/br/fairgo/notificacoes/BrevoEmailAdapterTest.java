package br.fairgo.notificacoes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrevoEmailAdapterTest {
    @Test void traduzRespostasSemRepetirPostEProtegeDestino() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var chamadas = new AtomicInteger();
        var status = new AtomicInteger();
        var corpo = new AtomicReference<String>();
        var chave = new AtomicReference<String>();
        server.createContext("/v3/smtp/email", troca -> {
            chamadas.incrementAndGet();
            chave.set(troca.getRequestHeaders().getFirst("api-key"));
            corpo.set(new String(troca.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            troca.sendResponseHeaders(status.get(), -1);
            troca.close();
        });
        server.start();
        try {
            var json = new ObjectMapper();
            var email = new EmailHomologacao(new BrevoEmailAdapter(json, "fixture", "teste@example.invalid", "Teste",
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v3/smtp/email")), Set.of("a@example.invalid"));
            var mensagem = new Mensagem(UUID.randomUUID(), "a@example.invalid", "Teste", "Mensagem fictícia");
            for (int codigo : List.of(201, 400, 429, 500, 408)) {
                status.set(codigo);
                var esperado = codigo == 201 ? Resultado.ACEITO :
                        codigo == 400 || codigo == 429 ? Resultado.RECUSADO : Resultado.INDETERMINADO;
                assertEquals(esperado, email.enviar(mensagem));
            }
            assertEquals(Resultado.RECUSADO, email.enviar(new Mensagem(UUID.randomUUID(),"b@example.invalid","Teste","Teste")));
            assertEquals(5, chamadas.get());
            assertEquals("fixture", chave.get());
            assertEquals(mensagem.destino(), json.readTree(corpo.get()).at("/to/0/email").asText());
            assertEquals(mensagem.texto(), json.readTree(corpo.get()).get("textContent").asText());
        } finally { server.stop(0); }
    }
}
