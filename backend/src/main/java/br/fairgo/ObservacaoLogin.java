package br.fairgo;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class ObservacaoLogin extends OncePerRequestFilter {
    private final MetricasLogin metricas;

    ObservacaoLogin(MetricasLogin metricas) {
        this.metricas = metricas;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && "/api/driver/sessoes".equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean encaminhado = request.getHeader("Forwarded") != null || request.getHeader("X-Forwarded-For") != null;
        long inicio = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            metricas.registrarPedido(response.getStatus(), encaminhado, System.nanoTime() - inicio);
        }
    }
}
