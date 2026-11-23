package br.fairgo;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.rendering.*;

public final class PdfSeguro {
    private static final int LIMITE = 4 * 1024 * 1024;
    private static final Semaphore VAGAS = new Semaphore(2);
    static byte[] converter(byte[] dados) throws IOException {
        if (!VAGAS.tryAcquire()) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Tente novamente");
        try {
            return executar(dados);
        } finally {
            VAGAS.release();
        }
    }

    private static byte[] executar(byte[] dados) throws IOException {
        String classe = PdfSeguro.class.getName();
        boolean jar = PdfSeguro.class.getResource("PdfSeguro.class").toString().contains("BOOT-INF/");
        var pb = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx160m", "-XX:MaxDirectMemorySize=16m", "-Djava.awt.headless=true",
                "-Dloader.main=" + classe, "-cp", System.getProperty("java.class.path"),
                jar ? "org.springframework.boot.loader.launch.PropertiesLauncher" : classe);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        pb.environment().keySet().removeIf(k -> !Set.of("SYSTEMROOT","WINDIR","TMP","TEMP","LANG").contains(k.toUpperCase(Locale.ROOT)));
        Process processo = pb.start();
        var io = Executors.newFixedThreadPool(2);
        try {
            var envio = io.submit(() -> {
                try (var destino = processo.getOutputStream()) {
                    destino.write(dados);
                }
                return null;
            });
            var leitura = io.submit(() -> {
                try (var origem = processo.getInputStream()) {
                    return origem.readNBytes(LIMITE + 1);
                }
            });
            if (!processo.waitFor(15, TimeUnit.SECONDS)) throw new IOException("Tempo de conversão excedido");
            if (processo.exitValue() == 2) throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, "Cópia visual acima de 4 MiB");
            if (processo.exitValue() != 0) throw new IOException("PDF inválido");
            envio.get();
            byte[] copia = leitura.get();
            if (copia.length > LIMITE || !Cofre.pdf(copia)) throw new IOException("Saída inválida");
            return copia;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (ExecutionException e) {
            throw new IOException(e.getCause());
        } finally {
            if (processo.isAlive()) processo.destroyForcibly().onExit().join();
            io.shutdownNow();
        }
    }

    public static void main(String[] args) throws IOException {
        var saida = System.out;
        System.setOut(System.err);
        javax.imageio.ImageIO.setUseCache(false);
        byte[] entrada = System.in.readNBytes(LIMITE + 1);
        if (entrada.length > LIMITE) throw new IOException();
        try (var original = Loader.loadPDF(entrada); var limpo = new PDDocument()) {
            int paginas = original.getNumberOfPages();
            if (original.isEncrypted() || paginas < 1 || paginas > 10) throw new IOException();
            var renderer = new PDFRenderer(original);
            renderer.setSubsamplingAllowed(true);
            for (int i = 0; i < paginas; i++) {
                var caixa = original.getPage(i).getCropBox();
                float lado = Math.max(caixa.getWidth(), caixa.getHeight());
                if (!Float.isFinite(lado) || lado <= 0 || lado > 14400) throw new IOException();
                var imagem = renderer.renderImage(i, Math.min(2f, 1800f / lado), ImageType.RGB);
                var pagina = new PDPage(new PDRectangle(imagem.getWidth(), imagem.getHeight()));
                limpo.addPage(pagina);
                try (var conteudo = new PDPageContentStream(limpo, pagina)) {
                    conteudo.drawImage(JPEGFactory.createFromImage(limpo, imagem, 0.86f), 0, 0);
                } finally {
                    imagem.flush();
                }
            }
            var bytes = new ByteArrayOutputStream();
            limpo.save(bytes);
            if (bytes.size() > LIMITE) System.exit(2);
            bytes.writeTo(saida);
            saida.flush();
        }
    }
}
