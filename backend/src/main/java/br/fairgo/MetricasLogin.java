package br.fairgo;

import java.util.concurrent.atomic.LongAdder;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

@Component
@ManagedResource(objectName="br.fairgo:type=LoginDriver")
public class MetricasLogin {
    private final LongAdder pedidos = new LongAdder();
    private final LongAdder invalidos = new LongAdder();
    private final LongAdder limitados = new LongAdder();
    private final LongAdder encaminhados = new LongAdder();
    private final LongAdder nanosLogin = new LongAdder();
    private final LongAdder reservas = new LongAdder();
    private final LongAdder nanosReserva = new LongAdder();

    void registrarPedido(int status, boolean cabecalhoEncaminhado, long nanos) {
        pedidos.increment();
        if (status == 401) invalidos.increment();
        if (status == 429) limitados.increment();
        if (cabecalhoEncaminhado) encaminhados.increment();
        nanosLogin.add(nanos);
    }

    void registrarReservaGlobal(long nanos) {
        reservas.increment();
        nanosReserva.add(nanos);
    }

    @ManagedAttribute public long getPedidos() { return pedidos.sum(); }
    @ManagedAttribute public long getRespostas401() { return invalidos.sum(); }
    @ManagedAttribute public long getRespostas429() { return limitados.sum(); }
    @ManagedAttribute public long getPedidosComCabecalhoEncaminhado() { return encaminhados.sum(); }
    @ManagedAttribute public long getNanosLogin() { return nanosLogin.sum(); }
    @ManagedAttribute public long getReservasGlobais() { return reservas.sum(); }
    @ManagedAttribute public long getNanosReservaGlobal() { return nanosReserva.sum(); }
}
