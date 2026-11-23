package br.fairgo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
class Seguranca {
    @Bean
    SecurityFilterChain api(HttpSecurity http, JdbcTemplate db) throws Exception {
        var bearer = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                    throws ServletException, IOException {
                String auth = req.getHeader("Authorization");
                if (auth != null && auth.matches("Bearer [0-9a-f]{64}")) {
                    var ids = db.queryForList("""
                            SELECT admin_id FROM credenciais_admin
                            WHERE digest = ? AND expira_em > CURRENT_TIMESTAMP AND NOT revogada
                            """, String.class, hash(auth.substring(7)));
                    if (!ids.isEmpty()) {
                        var principal = new UsernamePasswordAuthenticationToken(ids.get(0), null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                        SecurityContextHolder.getContext().setAuthentication(principal);
                    }
                }
                chain.doFilter(req, res);
            }
        };
        return http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(c -> c.disable())
                .requestCache(c -> c.disable())
                .addFilterBefore(bearer, AnonymousAuthenticationFilter.class)
                .exceptionHandling(c -> c.authenticationEntryPoint((req,res,e) -> res.setStatus(401)))
                .authorizeHttpRequests(a -> a.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll().requestMatchers("/admin/index.html", "/admin/painel.js").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().denyAll()).build();
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

