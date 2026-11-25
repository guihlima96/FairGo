package br.fairgo;

import static org.junit.jupiter.api.Assertions.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CofreCompatibilidadeTest {
    // Chave pública de teste 00..1f, titular fictício e registros fixos.
    private final Cofre cofre = new Cofre("AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=");
    private final UUID dono = new UUID(0, 1);

    @Test
    void leImagemProduzidaPeloCofreAnterior() throws Exception {
        try (var recurso = getClass().getResourceAsStream("/documentos/jpeg-de01322.b64")) {
            byte[] cifrado = Base64.getMimeDecoder().decode(recurso.readAllBytes());
            byte[] jpeg = cofre.abrir(cifrado, dono, "CNH", "JPEG");
            assertEquals("6e8444592300eaa27a591ff9bdff5167a007cc08ecd8d037d81b1766d4706155",
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(jpeg)));
        }
    }

    @Test
    void preservaInterpretacaoDosRegistrosLegados() throws Exception {
        var hex = HexFormat.of();
        byte[] jpeg = hex.parseHex("0202030405060708090A0B0C9995F64FB6482AE85A4F3172301B4B99C25A84A8");
        assertArrayEquals(hex.parseHex("FFD8FFD9"), cofre.abrir(jpeg, dono, "CNH", null));
        byte[] marcado = hex.parseHex("0302030405060708090A0B0CFB64ABD079E6A060400D3F94E7089E9F70166B063DDE");
        assertArrayEquals(hex.parseHex("255044462D"), cofre.abrir(marcado, dono, "CNH", null));
        byte[] bruto = hex.parseHex("0402030405060708090A0B0CC268CCA14E9D60FE3E9C845FF5E276DA23D90D9B1E");
        var erro = assertThrows(ResponseStatusException.class, () -> cofre.abrir(bruto, dono, "CNH", null));
        assertEquals(422, erro.getStatusCode().value());
    }
}
