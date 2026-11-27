package br.fairgo.notificacoes;

import java.util.Set;

public record EmailHomologacao(EmailAdapter adaptador, Set<String> autorizados) {
    public EmailHomologacao { autorizados = Set.copyOf(autorizados); }
    public boolean permite(String destino) { return autorizados.contains(destino); }
    public Resultado enviar(Mensagem mensagem) {
        return permite(mensagem.destino()) ? adaptador.enviar(mensagem) : Resultado.RECUSADO;
    }
    @Override public String toString() { return "EmailHomologacao[protegido]"; }
}
