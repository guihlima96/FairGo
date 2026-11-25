package br.fairgo.documentos;

import java.io.IOException;

public final class FormatoDocumento {
    public static final int LIMITE_BYTES = 4 * 1024 * 1024;

    private FormatoDocumento() {}

    public static boolean ehPdf(byte[] dados) {
        return dados.length >= 5 && dados[0] == '%' && dados[1] == 'P'
                && dados[2] == 'D' && dados[3] == 'F' && dados[4] == '-';
    }

    static void conferirEntrada(byte[] dados) throws IOException {
        if (dados.length == 0 || dados.length > LIMITE_BYTES)
            throw new IOException("Tamanho de entrada inválido");
    }
}
