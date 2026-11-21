package br.fairgo;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
class Seguranca {
    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        // Clientes REST enviam JSON com autenticação explícita a cada requisição.
        return http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(c -> c.ignoringRequestMatchers(request ->
                        "application/json".equalsIgnoreCase(request.getContentType())))
                .authorizeHttpRequests(a -> a.requestMatchers("/api/admin/**").authenticated()
                        .anyRequest().denyAll())
                .httpBasic(withDefaults()).build();
    }
}
