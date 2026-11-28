package br.fairgo;

import java.util.Locale;

final class EmailCadastro {
    private EmailCadastro() {}

    static String normalizar(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
