package br.fairgo.seguranca;

import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProvaCodigoTest {
    private final UUID titular = new UUID(0, 1);
    private final UUID desafio = new UUID(0, 2);
    private final String destino = "aluna.é@example.invalid";
    private final String codigo = "00001234";
    private final ProvaCodigo componente = new ProvaCodigo(new byte[32]);

    // Produzido pelo método prova de VerificacaoContato em e2864f9 com uma chave fictícia de 32 zeros.
    private final byte[] provaAnterior = HexFormat.of().parseHex(
            "2bd2dbf928c7470a5d490f27d3190e53cdf23bdffda814612ef3b3f06c48df29");

    @Test
    void preservaVetorAnteriorComZeroInicialEDestinoUtf8() {
        assertArrayEquals(provaAnterior, componente.calcular(titular, "EMAIL", desafio, destino, codigo));
        assertTrue(componente.conferir(titular, "EMAIL", desafio, destino, codigo, provaAnterior));
    }

    @Test
    void vinculaCadaCampoIndividualmente() {
        assertFalse(componente.conferir(new UUID(0, 3), "EMAIL", desafio, destino, codigo, provaAnterior));
        assertFalse(componente.conferir(titular, "CELULAR", desafio, destino, codigo, provaAnterior));
        assertFalse(componente.conferir(titular, "EMAIL", new UUID(0, 3), destino, codigo, provaAnterior));
        assertFalse(componente.conferir(titular, "EMAIL", desafio, "outra@example.invalid", codigo, provaAnterior));
        assertFalse(componente.conferir(titular, "EMAIL", desafio, destino, "1234", provaAnterior));
    }

    @Test
    void rejeitaOutraChaveOuProvaAdulterada() {
        byte[] outraChave = new byte[32];
        outraChave[0] = 1;
        assertFalse(new ProvaCodigo(outraChave).conferir(titular, "EMAIL", desafio, destino, codigo, provaAnterior));
        byte[] adulterada = provaAnterior.clone();
        adulterada[0] ^= 1;
        assertFalse(componente.conferir(titular, "EMAIL", desafio, destino, codigo, adulterada));
        assertThrows(IllegalArgumentException.class, () -> new ProvaCodigo(new byte[31]));
        assertThrows(IllegalArgumentException.class, () -> new ProvaCodigo(new byte[33]));
    }

    @Test
    void bibliotecaPodeVerificarRetiradaDeReservaSemInfraestrutura() {
        String codigoRetirada = componente.gerarCodigo();
        assertTrue(codigoRetirada.matches("[0-9]{8}"));
        byte[] prova = componente.calcular(titular, "BALCAO", desafio, "biblioteca-central", codigoRetirada);
        assertTrue(componente.conferir(titular, "BALCAO", desafio, "biblioteca-central", codigoRetirada, prova));
    }
}
