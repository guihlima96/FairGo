package br.fairgo;

import java.lang.management.ManagementFactory;
import javax.management.ObjectName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricasLoginTest {
    @Configuration
    @EnableMBeanExport
    static class Exportacao {}

    @Test void expoeSomenteAgregadosDeLogin() throws Exception {
        try (var contexto = new AnnotationConfigApplicationContext(Exportacao.class, MetricasLogin.class)) {
            var metricas = contexto.getBean(MetricasLogin.class);
            metricas.registrarPedido(401, false, 30);
            metricas.registrarPedido(429, true, 10);
            metricas.registrarPedido(200, false, 60);
            metricas.registrarReservaGlobal(20);
            var servidor = ManagementFactory.getPlatformMBeanServer();
            var nome = new ObjectName("br.fairgo:type=LoginDriver");
            assertEquals(3L, servidor.getAttribute(nome, "Pedidos"));
            assertEquals(1L, servidor.getAttribute(nome, "Respostas401"));
            assertEquals(1L, servidor.getAttribute(nome, "Respostas429"));
            assertEquals(1L, servidor.getAttribute(nome, "PedidosComCabecalhoEncaminhado"));
            assertEquals(100L, servidor.getAttribute(nome, "NanosLogin"));
            assertEquals(1L, servidor.getAttribute(nome, "ReservasGlobais"));
            assertEquals(20L, servidor.getAttribute(nome, "NanosReservaGlobal"));
        }
    }
}
