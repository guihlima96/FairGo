package br.fairgo.seguranca;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class ProvaCodigo {
    private static final String ALGORITMO = "HmacSHA256";
    private final SecretKeySpec chave;
    private final SecureRandom random = new SecureRandom();

    public ProvaCodigo(byte[] chave) {
        if (chave.length != 32) throw new IllegalArgumentException("Chave de verificação inválida");
        this.chave = new SecretKeySpec(chave, ALGORITMO);
    }

    public String gerarCodigo() {
        return String.format(Locale.ROOT, "%08d", random.nextInt(100_000_000));
    }

    public byte[] calcular(UUID titular, String canal, UUID desafio, String destino, String codigo) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(chave);
            String mensagem = String.join("\n", titular.toString(), canal, desafio.toString(), destino, codigo);
            return mac.doFinal(mensagem.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public boolean conferir(UUID titular, String canal, UUID desafio, String destino, String codigo, byte[] prova) {
        return MessageDigest.isEqual(calcular(titular, canal, desafio, destino, codigo), prova);
    }
}
