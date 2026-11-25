const el = id => document.getElementById(id);
const main = document.querySelector("main");
const nomes = {MOTORISTA: "Motorista", VEICULO: "Veículo"};
let selecionado;
let imagemUrl;
let ocupado = false;

function fecharImagem() {
    el("visor").hidden = true;
    el("pdf").removeAttribute("href");
    el("pdf").hidden = true;
    el("imagem").hidden = true;
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
    el("conferencias").replaceChildren();
    el("conferencia").reset();
    el("motivo").value = "";
}
function limpar() {
    limparDetalhe();
    limparFila();
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
            409: "Os dados ou a situação mudaram. Consulte novamente antes de registrar.",
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
const grupos = ["incompletos", "prontos", "aprovados", "rejeitados"];
let linhasFila = null;
let versaoFila = 0;
let atualizando = false;

function limparFila() {
    versaoFila++;
    linhasFila = null;
    for (const grupo of grupos) {
        el(grupo).replaceChildren();
        document.querySelector('[data-total="' + grupo + '"]').textContent = "";
    }
}
function mostrarFila() {
    if (!linhasFila) return;
    const contagens = Object.fromEntries(grupos.map(g => [g, 0]));
    for (const grupo of grupos) el(grupo).replaceChildren();
    for (const cadastro of linhasFila) {
        if (el("filtro").value && el("filtro").value !== cadastro.tipo) continue;
        const grupo = cadastro.situacao === "PENDENTE"
            ? (cadastro.prontoParaAnalise ? "prontos" : "incompletos") : cadastro.situacao.toLowerCase() + "s";
        contagens[grupo]++;
        const nome = nomes[cadastro.tipo] + " · " + cadastro.descricao;
        const linha = item(grupo, "");
        linha.append(botao(nome, () => consultar(cadastro.tipo, cadastro.id, nome)));
        const evidencias = document.createElement("small");
        evidencias.style.display = "block";
        evidencias.textContent = "Enviados: " + (cadastro.enviados.join(", ") || "nenhum") +
            ". Ausentes: " + (cadastro.ausentes.join(", ") || "nenhum") + "." +
            (cadastro.tipo === "VEICULO" ? " Dados técnicos " + (cadastro.dadosTecnicosPresentes ? "presentes." : "ausentes.") : "");
        linha.append(evidencias);
        if (cadastro.codigoPublico) linha.append("Motivo público: " + cadastro.codigoPublico);
    }
    for (const grupo of grupos) document.querySelector('[data-total="' + grupo + '"]').textContent = contagens[grupo];
}
async function fila() {
    const versao = ++versaoFila;
    const linhas = await (await api("fila")).json();
    if (versao !== versaoFila) return;
    if (JSON.stringify(linhas) === JSON.stringify(linhasFila)) return;
    linhasFila = linhas;
    mostrarFila();
}
el("filtro").onchange = mostrarFila;
setInterval(async () => {
    if (!linhasFila || ocupado || atualizando || document.hidden) return;
    atualizando = true;
    try {
        await fila();
    } catch (erro) {
        el("aviso").textContent = erro.message;
    } finally {
        atualizando = false;
    }
}, 15000);
async function consultar(tipo, id, nome) {
    limparDetalhe();
    const rota = tipo + "/" + encodeURIComponent(id);
    const cadastro = await (await api(rota)).json();
    const historico = await (await api(rota + "/historico")).json();
    const motivos = cadastro.situacao === "PENDENTE" ? await (await api("motivos-publicos/" + tipo)).json() : [];
    selecionado = {tipo, id, nome, rota, versaoNome: cadastro.versaoNome, motivos, documentos: cadastro.documentos};
    el("codigoPublico").replaceChildren(new Option("Selecione para rejeitar", ""));
    for (const m of motivos) el("codigoPublico").add(new Option(m.codigo.replaceAll("_", " "), m.codigo));
    mostrarOrientacao();
    el("conferencia").hidden = tipo !== "MOTORISTA";
    if (tipo === "MOTORISTA") {
        const eventos = await (await api(rota + "/conferencias-identidade")).json();
        const ultimo = eventos[0];
        const revisao = tipoDoc => cadastro.documentos.find(d => d.tipo === tipoDoc)?.id;
        const vigente = ultimo && ultimo.versaoNome === cadastro.versaoNome
            && ultimo.revisaoCnh === revisao("CNH") && ultimo.revisaoFoto === revisao("FOTO_PERFIL");
        el("vigencia").textContent = vigente ? "Resultado vigente: " + ultimo.resultado : "Sem conferência vigente.";
        for (const h of eventos) item("conferencias", [new Date(h.autoria.data).toLocaleString("pt-BR"),
            h.autoria.responsavel, h.resultado, h.nomeConferido, "versão " + h.versaoNome,
            "CNH " + h.revisaoCnh, "Foto " + h.revisaoFoto].join(" · "));
    }
    el("titulo").textContent = cadastro.nomeMotorista || nome;
    el("identidade").checked = false;
    el("identificacao").hidden = tipo !== "MOTORISTA";
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
        conferir.dataset.tipo = doc.tipo;
        conferir.disabled = true;
        linha.append(botao("Visualizar " + doc.tipo, async () => {
            fecharImagem();
            const arquivo = await (await api(rota + "/documentos/" + doc.id)).blob();
            if (arquivo.type === "application/pdf") {
                imagemUrl = URL.createObjectURL(arquivo);
                el("pdf").href = imagemUrl;
                el("pdf").hidden = false;
                el("visor").hidden = false;
                el("pdf").onclick = () => { conferir.disabled = false; };
                el("aviso").textContent = "Baixe e confira o PDF antes de marcar o documento.";
                return;
            }
            if (arquivo.type !== "image/jpeg") throw new Error("Formato de documento inesperado.");
            imagemUrl = URL.createObjectURL(arquivo);
            el("imagem").hidden = false;
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
        if (pendente || (tipo === "MOTORISTA" && ["CNH", "FOTO_PERFIL"].includes(doc.tipo))) {
            const label = document.createElement("label");
            label.append(conferir, " Conferi este documento");
            linha.append(label);
        }
    }
    if (!cadastro.documentos.length) item("documentos", "Nenhum documento cadastrado.");
    for (const h of historico) item("historico", [new Date(h.data).toLocaleString("pt-BR"), h.situacao, h.responsavel, h.motivo, h.codigo_publico, h.documento_publico].filter(Boolean).join(" · "));
    if (!historico.length) item("historico", "Nenhuma decisão registrada.");
    el("decisao").hidden = !pendente;
    el("resultado").hidden = false;
    el("aviso").textContent = "Consulta concluída.";
}
function mostrarOrientacao() {
    const opcao = selecionado.motivos.find(m => m.codigo === el("codigoPublico").value);
    el("mensagemPublica").textContent = opcao?.mensagem || "";
    const documentos = selecionado.documentos.filter(d => opcao?.documentos.includes(d.tipo));
    el("rotuloDocumento").hidden = !opcao?.documentos.length;
    el("docPublico").replaceChildren(new Option("Selecione o documento", ""));
    for (const doc of documentos) el("docPublico").add(new Option(doc.tipo, doc.id));
    if (documentos.length === 1) el("docPublico").value = documentos[0].id;
}
el("codigoPublico").onchange = mostrarOrientacao;
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
el("conferencia").onsubmit = e => {
    e.preventDefault();
    const {tipo, id, nome, rota, versaoNome} = selecionado;
    const marcados = el("documentos").querySelectorAll("input:checked");
    const revisoes = Object.fromEntries(Array.from(marcados, c => [c.dataset.tipo, c.value]));
    if (!revisoes.CNH || !revisoes.FOTO_PERFIL) {
        el("aviso").textContent = "Abra e marque a CNH e a foto comparadas."; return;
    }
    const resultado = el("parecer").value;
    if (!window.confirm("Registrar a conferência visual de " + el("titulo").textContent + "?")) return;
    executar(async () => {
        await api(rota + "/conferencias-identidade", {method: "POST", headers: {"Content-Type": "application/json"},
            body: JSON.stringify({versaoNome, revisaoCnh: revisoes.CNH, revisaoFoto: revisoes.FOTO_PERFIL, resultado})});
        await consultar(tipo, id, nome);
        el("aviso").textContent = "Conferência registrada.";
    });
};
el("decisao").onsubmit = e => {
    e.preventDefault();
    const {tipo, id, nome, rota} = selecionado;
    const motivo = el("motivo").value.trim();
    const situacao = e.submitter.value;
    const documentosConferidos = Array.from(el("documentos").querySelectorAll("input:checked"), c => c.value);
    if (!motivo) { el("aviso").textContent = "Informe uma justificativa."; return; }
    const exigidos = tipo === "MOTORISTA" ? ["CNH", "ANTECEDENTES", "FOTO_PERFIL", "RESIDENCIA"] : ["CRLV"];
    const marcados = Array.from(el("documentos").querySelectorAll("input:checked"), c => c.dataset.tipo);
    const identidadeConferida = el("identidade").checked;
    if (situacao === "APROVADO" && tipo === "MOTORISTA" && !identidadeConferida) {
        el("aviso").textContent = "Confirme a conferência de identidade."; return;
    }
    if (situacao === "APROVADO" && !exigidos.every(t => marcados.includes(t))) {
        el("aviso").textContent = "Visualize e marque todos os documentos exigidos antes de aprovar."; return;
    }
    let orientacao;
    if (situacao === "REJEITADO") {
        const opcao = selecionado.motivos.find(m => m.codigo === el("codigoPublico").value);
        if (!opcao) { el("aviso").textContent = "Selecione o motivo público."; return; }
        const doc = selecionado.documentos.find(d => d.id === el("docPublico").value);
        if (opcao.documentos.length && !doc) { el("aviso").textContent = "Indique a revisão do documento."; return; }
        orientacao = {codigo: opcao.codigo, documento: doc?.tipo, revisao: doc?.id};
    }
    if (!window.confirm("Confirmar " + (situacao === "APROVADO" ? "aprovação" : "rejeição") + " de " + nome + "?\n" + motivo)) return;
    executar(async () => {
        await api(rota + "/decisoes", {method: "POST", headers: {"Content-Type": "application/json"},
            body: JSON.stringify({situacao, motivo, documentosConferidos, identidadeConferida, orientacao})});
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
