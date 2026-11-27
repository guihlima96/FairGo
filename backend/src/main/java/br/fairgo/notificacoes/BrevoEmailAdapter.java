package br.fairgo.notificacoes;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class BrevoEmailAdapter implements EmailAdapter {
    private final HttpClient client;
    private final URI endpoint;
    private final ObjectMapper json;
    private final String chave, remetente, nome;

    public BrevoEmailAdapter(ObjectMapper json, String chave, String remetente, String nome) {
        this(json, chave, remetente, nome, URI.create("https://api.brevo.com/v3/smtp/email"));
    }

    BrevoEmailAdapter(ObjectMapper json, String chave, String remetente, String nome, URI endpoint) {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        this.endpoint = endpoint;
        this.json = json;
        this.chave = chave;
        this.remetente = remetente;
        this.nome = nome;
    }

    @Override public Resultado enviar(Mensagem mensagem) {
        try {
            var body = Map.of("sender", Map.of("email", remetente, "name", nome),
                    "to", List.of(Map.of("email", mensagem.destino())), "subject", mensagem.assunto(),
                    "textContent", mensagem.texto());
            var pedido = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(6))
                    .header("api-key", chave).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body))).build();
            int status = client.send(pedido, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status == 201) return Resultado.ACEITO;
            if (status >= 400 && status < 500 && status != 408) return Resultado.RECUSADO;
            return Resultado.INDETERMINADO;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Resultado.INDETERMINADO;
        } catch (IOException e) {
            return Resultado.INDETERMINADO;
        }
    }
}
