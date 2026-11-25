package br.fairgo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@Service
class TentativasDriver {
    private static final int QUINZE_MINUTOS = 900;
    record Login(ControleTentativas.Reserva rajada, ControleTentativas.Reserva par,
                 ControleTentativas.Reserva conta) {}
    private final ControleTentativas controle;

    TentativasDriver(ControleTentativas controle) {
        this.controle = controle;
    }

    void limitarOrigem(String chave, int maximo) {
        controle.limparExpirados();
        exigir(chave, maximo, QUINZE_MINUTOS);
    }

    private ControleTentativas.Reserva exigir(String chave, int maximo, int segundos) {
        var reserva = controle.reservar(chave, maximo, segundos);
        if (reserva == null) throw new ResponseStatusException(TOO_MANY_REQUESTS);
        return reserva;
    }

    @Transactional
    public Login reservarLogin(String origem, String conta) {
        String par = origem + ":" + conta;
        var rajada = exigir("login-rajada:" + par, 2, 10);
        var porPar = exigir("login-par:" + par, 4, QUINZE_MINUTOS);
        var porConta = exigir("login-conta:" + conta, 8, QUINZE_MINUTOS);
        exigir("login-global", 600, 60);
        return new Login(rajada, porPar, porConta);
    }

    void sucesso(Login login) {
        controle.devolver(login.rajada());
        controle.devolver(login.par());
        controle.devolver(login.conta());
    }
}
