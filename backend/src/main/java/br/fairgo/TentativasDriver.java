package br.fairgo;

import org.springframework.stereotype.Service;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@Service
@EnableConfigurationProperties(PoliticaLogin.class)
class TentativasDriver {
    private static final int QUINZE_MINUTOS = 900;
    record Login(ControleTentativas.Reserva rajada, ControleTentativas.Reserva par,
                 ControleTentativas.Reserva conta) {}
    private final ControleTentativas controle;

    private final PoliticaLogin politica;
    private final MetricasLogin metricas;

    TentativasDriver(ControleTentativas controle, PoliticaLogin politica, MetricasLogin metricas) {
        this.politica = politica;
        this.metricas = metricas;
        this.controle = controle;
    }

    void limitarOrigem(String chave, int maximo) {
        controle.limparExpirados();
        exigir(chave, maximo, QUINZE_MINUTOS);
    }

    void limitarOrigemLogin(String origem) {
        controle.limparExpirados();
        exigir("login-ip:" + origem, politica.origem());
    }

    private ControleTentativas.Reserva exigir(String chave, PoliticaLogin.Cota cota) {
        return exigir(chave, cota.maximo(), cota.segundos());
    }

    private ControleTentativas.Reserva exigir(String chave, int maximo, int segundos) {
        var reserva = controle.reservar(chave, maximo, segundos);
        if (reserva == null) throw new ResponseStatusException(TOO_MANY_REQUESTS);
        return reserva;
    }

    @Transactional
    public Login reservarLogin(String origem, String conta) {
        String par = origem + ":" + conta;
        var rajada = exigir("login-rajada:" + par, politica.rajada());
        var porPar = exigir("login-par:" + par, politica.par());
        var porConta = exigir("login-conta:" + conta, politica.conta());
        long inicio = System.nanoTime();
        try {
            exigir("login-global", politica.global());
        } finally {
            metricas.registrarReservaGlobal(System.nanoTime() - inicio);
        }
        return new Login(rajada, porPar, porConta);
    }

    void sucesso(Login login) {
        controle.devolver(login.rajada());
        controle.devolver(login.par());
        controle.devolver(login.conta());
    }
}
