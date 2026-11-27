package br.fairgo.notificacoes;

import java.util.UUID;

public record Mensagem(UUID referencia, String destino, String assunto, String texto) {
    @Override public String toString() { return "Mensagem[protegida]"; }
}
