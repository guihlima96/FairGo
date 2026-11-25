package br.fairgo.documentos;

import java.nio.ByteBuffer;
import java.security.*;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.*;

public final class CifraDocumental {
    private static final int NONCE_BYTES = 12;
    private final SecretKey chave;
    private final SecureRandom random = new SecureRandom();

    public CifraDocumental(byte[] chave) {
        if (chave.length != 32) throw new IllegalArgumentException("Chave de documentos inválida");
        this.chave = new SecretKeySpec(chave, "AES");
    }

    public byte[] cifrar(byte[] claro, byte[] contexto) throws GeneralSecurityException {
        byte[] nonce = new byte[NONCE_BYTES];
        random.nextBytes(nonce);
        byte[] cifrado = cifra(Cipher.ENCRYPT_MODE, nonce, contexto).doFinal(claro);
        return ByteBuffer.allocate(nonce.length + cifrado.length).put(nonce).put(cifrado).array();
    }

    public byte[] abrir(byte[] dados, byte[] contexto) throws GeneralSecurityException {
        if (dados.length < NONCE_BYTES + 16) throw new GeneralSecurityException("Documento truncado");
        return cifra(Cipher.DECRYPT_MODE, Arrays.copyOf(dados, NONCE_BYTES), contexto)
                .doFinal(dados, NONCE_BYTES, dados.length - NONCE_BYTES);
    }

    private Cipher cifra(int modo, byte[] nonce, byte[] contexto) throws GeneralSecurityException {
        Cipher cifra = Cipher.getInstance("AES/GCM/NoPadding");
        cifra.init(modo, chave, new GCMParameterSpec(128, nonce));
        cifra.updateAAD(contexto);
        return cifra;
    }
}
