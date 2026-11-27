package br.fairgo.notificacoes;

public interface SmsAdapter {
    Resultado enviar(Mensagem mensagem);
}
