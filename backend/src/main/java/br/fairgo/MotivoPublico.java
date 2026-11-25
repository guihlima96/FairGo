package br.fairgo;

import java.util.*;
import br.fairgo.CadastroCompartilhado.Alvo;
import br.fairgo.CadastroCompartilhado.Tipo;
import static br.fairgo.CadastroCompartilhado.Tipo.*;

enum MotivoPublico {
    CNH_VENCIDA("Envie sua CNH dentro da validade.", Alvo.MOTORISTA, CNH),
    CNH_SEM_EAR("Envie sua CNH com a observação EAR.", Alvo.MOTORISTA, CNH),
    CNH_NAO_DEFINITIVA("Envie sua CNH definitiva.", Alvo.MOTORISTA, CNH),
    DOCUMENTO_ILEGIVEL("Envie uma versão legível do documento indicado.", null, Tipo.values()),
    FOTO_INADEQUADA("Atualize sua foto de perfil com uma imagem nítida do rosto.", Alvo.MOTORISTA, FOTO_PERFIL),
    DIVERGENCIA_CADASTRAL("Confira os dados do cadastro e corrija as divergências.", null),
    RESIDENCIA_INVALIDA("Envie um comprovante de residência em seu nome.", Alvo.MOTORISTA, RESIDENCIA),
    CRLV_INVALIDO("Envie o CRLV atualizado do veículo cadastrado.", Alvo.VEICULO, CRLV),
    VEICULO_INELEGIVEL("Confira os dados técnicos e os requisitos do veículo.", Alvo.VEICULO),
    OUTROS("Revise os dados e documentos deste cadastro.", null);

    record Selecao(@jakarta.validation.constraints.NotBlank String codigo, String documento, UUID revisao) {}

    final String mensagem;
    private final Alvo alvo;
    private final List<Tipo> tipos;

    MotivoPublico(String mensagem, Alvo alvo, Tipo... tipos) {
        this.mensagem = mensagem;
        this.alvo = alvo;
        this.tipos = List.of(tipos);
    }

    boolean aceita(Alvo cadastro) { return alvo == null || alvo == cadastro; }

    List<Tipo> documentos(Alvo cadastro) {
        return tipos.stream().filter(t -> (t == CRLV) == (cadastro == Alvo.VEICULO)).toList();
    }
}
