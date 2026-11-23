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
        var pasta = Files.createTempDirectory("fairgo-pdf-");
        var entrada = pasta.resolve("entrada.pdf");
        var saida = pasta.resolve("leitura.pdf");
        Process processo = null;
        try {
            Files.write(entrada, dados);
            String classe = PdfSeguro.class.getName();
            boolean jar = PdfSeguro.class.getResource("PdfSeguro.class").toString().contains("BOOT-INF/");
            var pb = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-Xmx160m", "-XX:MaxDirectMemorySize=16m", "-Djava.awt.headless=true",
                    "-Dloader.main=" + classe, "-cp", System.getProperty("java.class.path"),
                    jar ? "org.springframework.boot.loader.launch.PropertiesLauncher" : classe,
                    entrada.toString(), saida.toString());
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.environment().keySet().removeIf(k -> !Set.of("SYSTEMROOT","WINDIR","TMP","TEMP","LANG").contains(k.toUpperCase(Locale.ROOT)));
            processo = pb.start();
            if (!processo.waitFor(15, TimeUnit.SECONDS) || processo.exitValue() != 0
                    || Files.size(saida) > 4194304) throw new IOException("PDF fora dos limites");
            return Files.readAllBytes(saida);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } finally {
            if (processo != null && processo.isAlive()) processo.destroyForcibly().onExit().join();
            Files.deleteIfExists(saida);
            Files.deleteIfExists(entrada);
            Files.deleteIfExists(pasta);
        }
    }

    public static void main(String[] args) throws IOException {
        try (var original = Loader.loadPDF(new File(args[0])); var limpo = new PDDocument()) {
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
            limpo.save(args[1]);
        }
    }
}
