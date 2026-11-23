package br.fairgo;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Component
class Cofre {
    private final SecretKey chave;
    private final SecureRandom random = new SecureRandom();

    Cofre(@Value("${fairgo.document-key}") String base64) {
        byte[] key = Base64.getDecoder().decode(base64);
        if (key.length != 32) throw new IllegalArgumentException("Chave de documentos inválida");
        chave = new SecretKeySpec(key, "AES");
    }

    byte[] preparar(MultipartFile arquivo, UUID dono, String tipo) {
        if (arquivo.isEmpty() || arquivo.getSize() > 4194304)
            throw new ResponseStatusException(BAD_REQUEST);
        try (var input = new MemoryCacheImageInputStream(arquivo.getInputStream())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException();
            var reader = readers.next();
            try {
                if (!reader.getFormatName().equalsIgnoreCase("JPEG")) throw new IOException();
                reader.setInput(input, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > 8000000) throw new IOException();
                BufferedImage photo = reader.read(0);
                var bytes = new ByteArrayOutputStream();
                try (var out = new MemoryCacheImageOutputStream(bytes)) {
                    if (!ImageIO.write(photo, "jpeg", out)) throw new IOException();
                }
                byte[] nonce = new byte[12];
                random.nextBytes(nonce);
                byte[] encrypted = cipher(Cipher.ENCRYPT_MODE, nonce, dono, tipo).doFinal(bytes.toByteArray());
                return ByteBuffer.allocate(12 + encrypted.length).put(nonce).put(encrypted).array();
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, "JPEG inválido");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha no cofre", e);
        }
    }

    byte[] abrir(byte[] data, UUID dono, String tipo) throws GeneralSecurityException {
        return cipher(Cipher.DECRYPT_MODE, Arrays.copyOf(data,12), dono, tipo)
                .doFinal(data, 12, data.length - 12);
    }

    private Cipher cipher(int modo, byte[] nonce, UUID dono, String tipo) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(modo, chave, new GCMParameterSpec(128, nonce));
        cipher.updateAAD((dono + ":" + tipo).getBytes(StandardCharsets.US_ASCII));
        return cipher;
    }
}
