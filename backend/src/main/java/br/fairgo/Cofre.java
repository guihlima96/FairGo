package br.fairgo;

import br.fairgo.documentos.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Component
class Cofre {
    private final CifraDocumental cifra;

    Cofre(@Value("${fairgo.document-key}") String base64) {
        cifra = new CifraDocumental(Base64.getDecoder().decode(base64));
    }

    record Preparado(String formato, byte[] cifrado) {}

    Preparado preparar(MultipartFile arquivo, UUID dono, String tipo) {
        if (arquivo.isEmpty() || arquivo.getSize() > FormatoDocumento.LIMITE_BYTES)
            throw new ResponseStatusException(BAD_REQUEST);
        try {
            byte[] dados = arquivo.getBytes();
            String formato = FormatoDocumento.ehPdf(dados) ? "PDF" : "JPEG";
            if (formato.equals("PDF")) {
                if (!Set.of("CNH","CRLV","RESIDENCIA","ANTECEDENTES").contains(tipo))
                    throw new IOException();
                dados = PdfSeguro.converter(dados);
            } else {
                dados = JpegSeguro.regravar(dados);
            }
            return new Preparado(formato, cifra.cifrar(dados, contexto(dono, tipo, formato)));
        } catch (PdfSeguro.Ocupado e) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Tente novamente");
        } catch (PdfSeguro.SaidaExcedida e) {
            throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "Cópia visual acima de 4 MiB");
        } catch (IOException | IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Documento inválido");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha no cofre", e);
        }
    }

    byte[] abrir(byte[] data, UUID dono, String tipo, String formato) throws GeneralSecurityException {
        byte[] claro = cifra.abrir(data, contexto(dono, tipo, formato));
        if (formato != null) return claro;
        if (claro[0] == 1) return Arrays.copyOfRange(claro, 1, claro.length);
        if (FormatoDocumento.ehPdf(claro)) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Reenvie o PDF de desenvolvimento");
        }
        return claro;
    }

    private static byte[] contexto(UUID dono, String tipo, String formato) {
        String valor = dono + ":" + tipo;
        if (formato != null) valor += ":" + formato;
        return valor.getBytes(StandardCharsets.US_ASCII);
    }
}
