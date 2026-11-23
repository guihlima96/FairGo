const campo = id => document.getElementById(id);
let consulta;
function limpar() {
    consulta?.abort();
    campo("resultado").hidden = true;
    campo("aviso").textContent = "";
    campo("documentos").replaceChildren();
    campo("historico").replaceChildren();
}
function listar(id, linhas, vazio) {
    for (const texto of linhas.length ? linhas : [vazio]) {
        const item = document.createElement("li");
        item.textContent = texto;
        campo(id).append(item);
    }
}
campo("consulta").addEventListener("input", limpar);
campo("consulta").addEventListener("reset", limpar);
campo("consulta").addEventListener("submit", async evento => {
    evento.preventDefault();
    limpar();
    consulta = new AbortController();
    const opcoes = {signal: consulta.signal, cache: "no-store",
        headers: {Authorization: "Bearer " + campo("token").value.trim()}};
    const rota = "/api/admin/validacao/" + campo("tipo").value + "/" + encodeURIComponent(campo("codigo").value);
    campo("aviso").textContent = "Consultando…";
    try {
        const respostas = await Promise.all([fetch(rota, opcoes), fetch(rota + "/historico", opcoes)]);
        const falha = respostas.find(r => !r.ok);
        if (falha) {
            const mensagens = {401: "Confira o token administrativo.", 404: "Cadastro não encontrado."};
            throw new Error(mensagens[falha.status] || "Falha ao consultar o cadastro.");
        }
        const [dados, historico] = await Promise.all(respostas.map(r => r.json()));
        if (opcoes.signal.aborted) return;
        campo("situacao").textContent = "Situação do cadastro: " + dados.situacao;
        listar("documentos", dados.documentos.map(d => d.tipo), "Nenhum documento cadastrado.");
        listar("historico", historico.map(h => [new Date(h.data).toLocaleString("pt-BR"), h.situacao,
            h.responsavel, h.motivo].join(" · ")), "Nenhuma decisão registrada.");
        campo("resultado").hidden = false;
        campo("aviso").textContent = "Consulta concluída.";
    } catch (erro) {
        if (!opcoes.signal.aborted) campo("aviso").textContent = erro.message;
    }
});
