const el = id => document.getElementById(id);
const main = document.querySelector("main");
const nomes = {MOTORISTA: "Motorista", VEICULO: "Veículo"};
let selecionado;
let imagemUrl;
let ocupado = false;

function fecharImagem() {
    el("visor").hidden = true;
    el("imagem").removeAttribute("src");
    if (imagemUrl) URL.revokeObjectURL(imagemUrl);
    imagemUrl = null;
}
function limparDetalhe() {
    fecharImagem();
    selecionado = null;
    el("resultado").hidden = true;
    el("documentos").replaceChildren();
    el("historico").replaceChildren();
    el("motivo").value = "";
}
function limpar() {
    limparDetalhe();
    el("fila").replaceChildren();
    el("busca").reset();
    el("aviso").textContent = "";
}
async function executar(acao) {
    if (ocupado) return;
    ocupado = true;
    main.inert = true;
    el("aviso").textContent = "Carregando…";
    try {
        await acao();
    } catch (erro) {
        el("aviso").textContent = erro.message;
    } finally {
        main.inert = false;
        ocupado = false;
    }
}
async function api(caminho, opcoes = {}) {
    const resposta = await fetch("/api/admin/validacao/" + caminho, {
        ...opcoes, cache: "no-store", signal: AbortSignal.timeout(20000),
        headers: {Authorization: "Bearer " + el("token").value.trim(), ...opcoes.headers}
    });
    if (!resposta.ok) {
        if (resposta.status === 401) limpar();
        const mensagens = {401: "Confira o token administrativo.", 404: "Cadastro ou documento indisponível. Atualize a consulta.",
            409: "Este cadastro já recebeu uma decisão. Consulte novamente.",
            422: "Confira os documentos atuais e os requisitos de elegibilidade."};
        throw new Error(mensagens[resposta.status] || "Falha na solicitação. Consulte novamente.");
    }
    return resposta;
}
function item(lista, texto) {
    const li = document.createElement("li");
    li.textContent = texto;
    el(lista).append(li);
    return li;
}
function botao(texto, acao) {
    const b = document.createElement("button");
    b.type = "button";
    b.textContent = texto;
    b.onclick = () => executar(acao);
    return b;
}
async function fila() {
    el("fila").replaceChildren();
    const linhas = await (await api("pendentes")).json();
    for (const cadastro of linhas) {
        const nome = nomes[cadastro.tipo] + " · " + cadastro.descricao;
        item("fila", "").append(botao(nome, () => consultar(cadastro.tipo, cadastro.id, nome)));
    }
    if (!linhas.length) item("fila", "Nenhum cadastro pendente.");
}
async function consultar(tipo, id, nome) {
    limparDetalhe();
    const rota = tipo + "/" + encodeURIComponent(id);
    const cadastro = await (await api(rota)).json();
    const historico = await (await api(rota + "/historico")).json();
    selecionado = {tipo, id, nome, rota};
    el("titulo").textContent = nome;
    el("situacao").textContent = "Situação: " + cadastro.situacao;
    const v = cadastro.dadosVeiculo[0];
    el("dados").textContent = v ? v.portas + " portas · Ar-condicionado: " + (v.ar_condicionado ? "sim" : "não")
        + " · Fabricação: " + v.ano_fabricacao + " · Município (IBGE): " + v.municipio : "";
    const pendente = cadastro.situacao === "PENDENTE";
    for (const doc of cadastro.documentos) {
        const linha = item("documentos", doc.tipo + (doc.validade ? " · Validade: " + doc.validade : "")
            + (doc.ear == null ? "" : " · EAR: " + (doc.ear ? "sim" : "não")));
        const conferir = document.createElement("input");
        conferir.type = "checkbox";
        conferir.value = doc.id;
        conferir.disabled = true;
        linha.append(botao("Visualizar " + doc.tipo, async () => {
            fecharImagem();
            const arquivo = await (await api(rota + "/documentos/" + doc.id)).blob();
            if (arquivo.type !== "image/jpeg") throw new Error("Formato de documento inesperado.");
            imagemUrl = URL.createObjectURL(arquivo);
            el("imagem").src = imagemUrl;
            el("imagem").alt = doc.tipo + " do cadastro selecionado";
            try {
                await el("imagem").decode();
            } catch {
                fecharImagem();
                throw new Error("Não foi possível abrir a imagem.");
            }
            el("visor").hidden = false;
            conferir.disabled = false;
            el("aviso").textContent = "Documento aberto.";
        }));
        if (pendente) {
            const label = document.createElement("label");
            label.append(conferir, " Conferi este documento");
            linha.append(label);
        }
    }
    if (!cadastro.documentos.length) item("documentos", "Nenhum documento cadastrado.");
    for (const h of historico) item("historico", [new Date(h.data).toLocaleString("pt-BR"), h.situacao, h.responsavel, h.motivo].join(" · "));
    if (!historico.length) item("historico", "Nenhuma decisão registrada.");
    el("decisao").hidden = !pendente;
    el("resultado").hidden = false;
    el("aviso").textContent = "Consulta concluída.";
}
el("acesso").onsubmit = e => {
    e.preventDefault();
    executar(async () => { limparDetalhe(); await fila(); el("aviso").textContent = "Fila atualizada."; });
};
el("acesso").onreset = limpar;
el("token").oninput = limpar;
el("fechar").onclick = fecharImagem;
el("busca").onsubmit = e => {
    e.preventDefault();
    if (el("token").reportValidity()) executar(() => consultar(el("tipo").value, el("codigo").value,
        nomes[el("tipo").value] + " · " + el("codigo").value));
};
el("decisao").onsubmit = e => {
    e.preventDefault();
    const {tipo, id, nome, rota} = selecionado;
    const motivo = el("motivo").value.trim();
    const situacao = e.submitter.value;
    const documentosConferidos = Array.from(el("documentos").querySelectorAll("input:checked"), c => c.value);
    if (!motivo) { el("aviso").textContent = "Informe uma justificativa."; return; }
    if (situacao === "APROVADO" && documentosConferidos.length !== (tipo === "MOTORISTA" ? 3 : 1)) {
        el("aviso").textContent = "Visualize e marque todos os documentos exigidos antes de aprovar."; return;
    }
    if (!window.confirm("Confirmar " + (situacao === "APROVADO" ? "aprovação" : "rejeição") + " de " + nome + "?\n" + motivo)) return;
    executar(async () => {
        await api(rota + "/decisoes", {method: "POST", headers: {"Content-Type": "application/json"},
            body: JSON.stringify({situacao, motivo, documentosConferidos})});
        limparDetalhe();
        try {
            await fila();
            await consultar(tipo, id, nome);
        } catch (erro) {
            throw new Error("Decisão registrada. Atualize a consulta. " + erro.message);
        }
        el("aviso").textContent = "Decisão registrada.";
    });
};
window.addEventListener("pagehide", () => { limpar(); el("token").value = ""; });
