package br.fairgo.documentos;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import org.junit.jupiter.api.Test;

class DocumentosTest {
    private static byte[] pdf(int paginas) throws IOException {
        try (var documento = new PDDocument(); var saida = new ByteArrayOutputStream()) {
            for (int i = 0; i < paginas; i++) documento.addPage(new PDPage());
            documento.save(saida);
            return saida.toByteArray();
        }
    }

    private static byte[] jpeg(int largura, int altura) throws IOException {
        var pixels = new BufferedImage(largura, altura, BufferedImage.TYPE_BYTE_GRAY);
        var saida = new ByteArrayOutputStream();
        ImageIO.write(pixels, "jpeg", saida);
        return saida.toByteArray();
    }

    @Test
    void certificadoDeCursoPodeSerProcessadoProtegidoELidoSemAplicacao() throws Exception {
        byte[] certificado = PdfSeguro.converter(pdf(1));
        try (var documento = Loader.loadPDF(certificado)) {
            assertEquals(1, documento.getNumberOfPages());
        }
        byte[] contexto = "curso/42/certificado".getBytes(StandardCharsets.UTF_8);
        var cifra = new CifraDocumental(new byte[32]);
        byte[] protegido = cifra.cifrar(certificado, contexto);
        assertArrayEquals(certificado, cifra.abrir(protegido, contexto));
        assertFalse(Arrays.equals(Arrays.copyOf(protegido, 12),
                Arrays.copyOf(cifra.cifrar(certificado, contexto), 12)));
        assertThrows(GeneralSecurityException.class, () -> cifra.abrir(protegido, new byte[]{1}));
        byte[] outraChave = new byte[32];
        outraChave[0] = 1;
        assertThrows(GeneralSecurityException.class, () -> new CifraDocumental(outraChave).abrir(protegido, contexto));
        protegido[12] ^= 1;
        assertThrows(GeneralSecurityException.class, () -> cifra.abrir(protegido, contexto));
        assertThrows(IllegalArgumentException.class, () -> new CifraDocumental(new byte[16]));
    }

    @Test
    void imagemRegravadaPreservaDimensoesEAplicaLimites() throws Exception {
        byte[] foto = jpeg(32, 24);
        assertFalse(FormatoDocumento.ehPdf(foto));
        assertEquals(32, ImageIO.read(new ByteArrayInputStream(JpegSeguro.regravar(foto))).getWidth());
        assertThrows(IOException.class, () -> JpegSeguro.regravar(new byte[]{1, 2}));
        assertThrows(IOException.class, () -> JpegSeguro.regravar(jpeg(4000, 2001)));
        byte[] grande = new byte[FormatoDocumento.LIMITE_BYTES + 1];
        assertThrows(IOException.class, () -> JpegSeguro.regravar(grande));
        assertThrows(IOException.class, () -> PdfSeguro.converter(grande));
    }

    @Test
    void falhasDoConversorLiberamVagas() throws Exception {
        byte[] invalido = "%PDF-invalido".getBytes(StandardCharsets.US_ASCII);
        assertTrue(FormatoDocumento.ehPdf(invalido));
        assertThrows(IOException.class, () -> PdfSeguro.converter(invalido));
        assertThrows(IOException.class, () -> PdfSeguro.converter(pdf(11)));
        assertTrue(FormatoDocumento.ehPdf(PdfSeguro.converter(pdf(1))));
    }

    @Test
    void conversoesSimultaneasAplicamContrapressao() throws Exception {
        int pedidos = 6;
        byte[] documento = pdf(10);
        var inicio = new CyclicBarrier(pedidos);
        var pool = Executors.newFixedThreadPool(pedidos);
        try {
            List<Future<Boolean>> respostas = new ArrayList<>();
            for (int i = 0; i < pedidos; i++) {
                respostas.add(pool.submit(() -> {
                    inicio.await(10, TimeUnit.SECONDS);
                    try {
                        return FormatoDocumento.ehPdf(PdfSeguro.converter(documento));
                    } catch (PdfSeguro.Ocupado e) {
                        return false;
                    }
                }));
            }
            int aceitos = 0;
            for (var resposta : respostas) if (resposta.get(30, TimeUnit.SECONDS)) aceitos++;
            assertTrue(aceitos > 0 && aceitos < pedidos);
        } finally {
            pool.shutdownNow();
        }
    }
}
