package br.fairgo.notificacoes;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration
class ConfiguracaoNotificacoes {
    @Bean EmailHomologacao emailHomologacao(Environment env, ObjectMapper json) {
        if (!"disabled".equals(env.getProperty("FAIRGO_SMS_PROVIDER", "disabled")))
            throw new IllegalStateException("Fornecedor SMS indisponível");
        return switch (env.getProperty("FAIRGO_EMAIL_PROVIDER", "disabled")) {
            case "disabled" -> new EmailHomologacao(m -> Resultado.RECUSADO, Set.of());
            case "brevo" -> {
                var destinos = Arrays.stream(exigir(env,"FAIRGO_EMAIL_ALLOWED_RECIPIENTS").split(","))
                        .map(String::strip).map(s -> s.toLowerCase(Locale.ROOT)).filter(s -> !s.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
                if (destinos.isEmpty() || destinos.stream().anyMatch(s -> !s.matches("[^\\s@*,]+@[^\\s@*,]+")))
                    throw new IllegalStateException("Lista de homologação inválida");
                yield new EmailHomologacao(new BrevoEmailAdapter(json, exigir(env,"FAIRGO_BREVO_EMAIL_API_KEY"),
                        exigir(env,"FAIRGO_EMAIL_SENDER"), exigir(env,"FAIRGO_EMAIL_SENDER_NAME")), destinos);
            }
            default -> throw new IllegalStateException("Fornecedor de e-mail desconhecido");
        };
    }

    private static String exigir(Environment env, String nome) {
        String valor = env.getProperty(nome, "").strip();
        if (valor.isEmpty()) throw new IllegalStateException("Configuração ausente: " + nome);
        return valor;
    }
}
