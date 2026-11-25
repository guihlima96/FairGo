package br.fairgo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

final class TokenOpaco {
    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenOpaco() {}

    static String gerar(String prefixo) {
        byte[] segredo = new byte[32];
        RANDOM.nextBytes(segredo);
        return prefixo + HexFormat.of().formatHex(segredo);
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

