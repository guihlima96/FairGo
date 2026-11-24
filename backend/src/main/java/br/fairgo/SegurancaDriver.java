package br.fairgo;

import java.io.IOException;
import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
class SegurancaDriver {
    @Bean
    @Order(1)
    SecurityFilterChain driver(HttpSecurity http, JdbcTemplate db) throws Exception {
        var bearer = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                    throws ServletException, IOException {
                String auth = req.getHeader("Authorization");
                if (auth != null && auth.matches("Bearer drv_[0-9a-f]{64}")) {
                    var ids = db.queryForList("""
                            SELECT usuario_id FROM sessoes_driver
                            WHERE digest=? AND expira_em > now()
                            """, UUID.class, Seguranca.hash(auth.substring(7)));
                    if (!ids.isEmpty()) {
                        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                                ids.get(0), null, List.of(new SimpleGrantedAuthority("ROLE_DRIVER"))));
                    }
                }
                chain.doFilter(req, res);
            }
        };
        return http.securityMatcher("/api/driver/**")
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(c -> c.disable()).requestCache(c -> c.disable())
                .addFilterBefore(bearer, AnonymousAuthenticationFilter.class)
                .exceptionHandling(c -> c.authenticationEntryPoint((req,res,e) -> res.setStatus(401)))
                .authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.POST, "/api/driver/contas",
                        "/api/driver/sessoes", "/api/driver/recuperacao", "/api/driver/recuperacao/confirmacao").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/driver/veiculos").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.PUT, "/api/driver/me/documentos/*", "/api/driver/veiculos/*/dados",
                                "/api/driver/veiculos/*/crlv").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.GET, "/api/driver/me/situacao", "/api/driver/me", "/api/driver/veiculos", "/api/driver/veiculos/*").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.DELETE, "/api/driver/sessoes/atual").hasRole("DRIVER")
                        .anyRequest().denyAll()).build();
    }
}
