package br.fairgo.documentos;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.*;
import javax.imageio.stream.*;

public final class JpegSeguro {
    private JpegSeguro() {}

    public static byte[] regravar(byte[] dados) throws IOException {
        FormatoDocumento.conferirEntrada(dados);
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(dados))) {
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
                return bytes.toByteArray();
            } finally {
                reader.dispose();
            }
        }
    }
}
