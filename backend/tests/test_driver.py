"""Integração do Driver com identidades fictícias e banco exclusivo."""
import concurrent.futures
import base64
import hashlib
import hmac
import json
import os
import secrets
import unittest
import urllib.error
import urllib.request
import uuid

from test_validacao import URL, sql


def api(metodo, rota, dados=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(URL + rota, json.dumps(dados).encode() if dados else None,
                                 headers, method=metodo)
    try:
        resposta = urllib.request.urlopen(req, timeout=15)
    except urllib.error.HTTPError as erro:
        resposta = erro
    with resposta:
        corpo = resposta.read()
        return resposta.status, json.loads(corpo) if corpo else None


def cadastro():
    numeros = [secrets.randbelow(10) for _ in range(9)]
    for peso in (10, 11):
        resto = sum(n * (peso - i) for i, n in enumerate(numeros)) % 11
        numeros.append(0 if resto < 2 else 11 - resto)
    return {"nome": "Motorista Fictício", "cpf": "".join(map(str, numeros)), "celular": "(12) 99999-0000",
            "email": uuid.uuid4().hex + "@example.com", "senha": secrets.token_hex(16)}


class Driver(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if not os.environ["PGDATABASE"].endswith("_test"):
            raise RuntimeError("Use banco exclusivo com sufixo _test")
        sql("DELETE FROM limites_driver")

    def criar(self, dados):
        status, conta = api("POST", "/api/driver/contas", dados)
        self.assertEqual(status, 201)
        return conta["id"]

    def login(self, identificador, senha, status=200):
        codigo, corpo = api("POST", "/api/driver/sessoes", {"identificador": identificador, "senha": senha})
        self.assertEqual(codigo, status)
        return corpo["token"] if codigo == 200 else None

    def test_verificacao_sem_provedor(self):
        dados = cadastro()
        titular = self.criar(dados)
        token = self.login(titular, dados["senha"])
        for canal in ("EMAIL", "CELULAR"):
            base = "/api/driver/me/verificacoes/" + canal
            self.assertEqual(api("POST", base + "/solicitacoes")[0], 401)
            self.assertEqual(api("POST", base + "/solicitacoes", token=token)[0], 503)
            tentativa = {"desafio": str(uuid.uuid4()), "codigo": "01234567"}
            self.assertEqual(api("POST", base + "/confirmacao", tentativa)[0], 401)
            self.assertIn(api("POST", base + "/confirmacao", tentativa, token)[0], (400, 503))
            self.assertEqual(api("POST", base + "/confirmacao", dict(tentativa, codigo=""), token)[0], 400)
        status, me = api("GET", "/api/driver/me", token=token)
        self.assertEqual(status, 200)
        self.assertFalse(any(me[k] for k in ("nomeVerificado", "emailVerificado", "celularVerificado")))
        self.assertEqual(sql(f"SELECT count(*) FROM desafios_contato_driver WHERE usuario_id='{titular}'"), "0")

    @unittest.skipUnless(os.environ.get("FAIRGO_CONTACT_KEY"), "Chave efêmera de teste ausente")
    def test_confirmacao_concorrente_e_isolamento(self):
        dados, outros = cadastro(), cadastro()
        titular = self.criar(dados)
        token = self.login(titular, dados["senha"])
        outro = self.login(self.criar(outros), outros["senha"])
        rota = "/api/driver/me/verificacoes/EMAIL/confirmacao"
        # Fixture local de desafio já emitido. Nenhum canal externo participa do teste.
        desafio = str(uuid.uuid4())
        codigo = f"{secrets.randbelow(100_000_000):08d}"
        mensagem = "\n".join((titular, "EMAIL", desafio, dados["email"], codigo)).encode()
        chave = base64.b64decode(os.environ["FAIRGO_CONTACT_KEY"])
        prova = hmac.digest(chave, mensagem, "sha256").hex()
        sql(f"""INSERT INTO desafios_contato_driver
            (usuario_id,canal,desafio,prova,expira_em,reenviar_em,fim_janela,emissoes,restantes)
            VALUES ('{titular}','EMAIL','{desafio}',decode('{prova}','hex'),
                    now()+interval '10 minutes',now(),now()+interval '1 hour',1,5)""")
        pedido = {"desafio": desafio, "codigo": codigo}
        self.assertEqual(api("POST", rota, pedido, outro)[0], 400)
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
            respostas = [executor.submit(api, "POST", rota, pedido, token) for _ in range(2)]
            self.assertEqual(sorted(r.result()[0] for r in respostas), [204, 400])
        me = api("GET", "/api/driver/me", token=token)[1]
        self.assertTrue(me["emailVerificado"])
        self.assertFalse(me["celularVerificado"] or me["nomeVerificado"])
        self.assertFalse(api("GET", "/api/driver/me", token=outro)[1]["emailVerificado"])

    def test_contas(self):
        a, b = cadastro(), cadastro()
        for campo, invalido in (("cpf", "00000000000"), ("celular", "123"), ("senha", "curta"), ("nome", "")):
            self.assertEqual(api("POST", "/api/driver/contas", dict(a, **{campo: invalido}))[0], 400)
        ida = self.criar(a)
        idb = self.criar(b)
        for repetido in (dict(cadastro(), cpf=a["cpf"]), dict(cadastro(), email=a["email"].upper())):
            self.assertEqual(api("POST", "/api/driver/contas", repetido)[0], 409)
        token = self.login(a["email"].upper(), a["senha"])
        outro = self.login(idb, b["senha"])
        for sessao, titular in ((token, ida), (outro, idb)):
            status, me = api("GET", "/api/driver/me?id=" + idb, token=sessao)
            self.assertEqual((status, me["id"], me["situacao"]), (200, titular, "PENDENTE"))
            self.assertEqual(set(me), {"id", "nome", "email", "situacao", "cpf", "celular",
                                       "nomeVerificado", "emailVerificado", "celularVerificado"})
            self.assertFalse(any(me[k] for k in ("emailVerificado", "nomeVerificado", "celularVerificado")))
            self.assertNotIn("senha_hash", me)
        self.assertEqual(api("GET", "/api/driver/me")[0], 401)
        self.assertEqual(api("GET", "/api/admin/validacao/pendentes", token=token)[0], 401)
        admin = secrets.token_hex(32)
        digest = hashlib.sha256(admin.encode()).hexdigest()
        sql(f"INSERT INTO credenciais_admin VALUES ('{digest}','teste-driver',now()+interval '1 hour',false)")
        self.assertEqual(api("GET", "/api/driver/me", token=admin)[0], 401)
        self.assertEqual(api("DELETE", "/api/driver/sessoes/atual", token=token)[0], 204)
        self.assertEqual(api("GET", "/api/driver/me", token=token)[0], 401)
        sql(f"UPDATE sessoes_driver SET expira_em=now() WHERE usuario_id='{idb}'")
        self.assertEqual(api("GET", "/api/driver/me", token=outro)[0], 401)
        for _ in range(8):
            self.login(a["email"], "senha incorreta", 401)
        self.login(ida, a["senha"], 429)

    def test_concorrencia_e_legado(self):
        dados = cadastro()
        def solicitar(_):
            return api("POST", "/api/driver/contas", dados)[0]
        with concurrent.futures.ThreadPoolExecutor(2) as pool:
            self.assertEqual(sorted(pool.map(solicitar, range(2))), [201,409])
        tentativa = dict(cadastro(), cpf=dados["cpf"])
        self.assertEqual(api("POST", "/api/driver/contas", tentativa)[0], 409)
        self.assertEqual(sql(f"SELECT count(*) FROM usuarios WHERE email='{tentativa['email']}'"), "0")
        legado = cadastro()
        id_legado = uuid.uuid4()
        sql(f"INSERT INTO usuarios(id,nome,email,perfil) "
            f"VALUES ('{id_legado}','Pessoa Legada','{legado['email']}','MOTORISTA')")
        self.assertEqual(api("POST", "/api/driver/contas", legado)[0], 409)
        self.assertEqual(sql(f"SELECT count(*) FROM contas_driver WHERE usuario_id='{id_legado}'"), "0")

    def test_recuperacao(self):
        a = cadastro()
        b = cadastro()
        outro = self.criar(b)
        sessao_alheia = self.login(outro, b["senha"])
        usuario = self.criar(a)
        sessao = self.login(usuario, a["senha"])
        for email in (a["email"], cadastro()["email"]):
            self.assertEqual(api("POST", "/api/driver/recuperacao", {"email":email}), (202,None))
        self.assertEqual(sql(f"SELECT count(*) FROM recuperacoes_driver WHERE usuario_id='{usuario}'"), "0")
        # Fixture do futuro emissor, criada somente no banco descartável.
        token = "rec_" + secrets.token_hex(32)
        digest = hashlib.sha256(token.encode()).hexdigest()
        vinculo = hashlib.sha256("\n".join((usuario, a["email"], token)).encode()).hexdigest()
        sql(f"""INSERT INTO recuperacoes_driver(digest,usuario_id,expira_em,vinculo)
                VALUES ('{digest}','{usuario}',now()+interval '1 hour','{vinculo}')""")
        dados = {"token":token, "novaSenha":secrets.token_hex(16)}
        def confirmar():
            return api("POST", "/api/driver/recuperacao/confirmacao", dados)[0]
        self.assertEqual(confirmar(), 400)
        sql(f"UPDATE contas_driver SET email_verificado=true WHERE usuario_id='{usuario}'")
        sql(f"UPDATE recuperacoes_driver SET expira_em=now() WHERE digest='{digest}'")
        self.assertEqual(confirmar(), 400)
        sql(f"UPDATE recuperacoes_driver SET expira_em=now()+interval '1 hour' WHERE digest='{digest}'")
        with concurrent.futures.ThreadPoolExecutor(2) as pool:
            self.assertEqual(sorted(pool.map(lambda _: confirmar(), range(2))), [204,400])
        self.assertEqual(confirmar(), 400)
        self.assertEqual(api("GET", "/api/driver/me", token=sessao)[0], 401)
        self.login(usuario, a["senha"], 401)
        self.login(usuario, dados["novaSenha"])
        self.assertEqual(api("GET", "/api/driver/me", token=sessao_alheia)[0], 200)
        self.login(outro, b["senha"])
        # Solicitações públicas, inclusive simultâneas para contato verificado, continuam sem emissão.
        with concurrent.futures.ThreadPoolExecutor(2) as pool:
            respostas = list(pool.map(lambda _: api("POST", "/api/driver/recuperacao",
                                                     {"email": a["email"]}), range(2)))
        self.assertEqual(respostas, [(202, None), (202, None)])
        self.assertEqual(sql(f"SELECT count(*) FROM recuperacoes_driver WHERE usuario_id='{usuario}'"), "0")
        invalido = "rec_" + secrets.token_hex(32)
        for valor in (invalido, invalido + "x"):
            codigo, resposta = api("POST", "/api/driver/recuperacao/confirmacao",
                                   {"token": valor, "novaSenha": dados["novaSenha"]})
            self.assertEqual(codigo, 400)
            for segredo in (invalido, dados["novaSenha"]):
                self.assertNotIn(segredo, str(resposta))
        if os.environ.get("FAIRGO_TEST_BACKEND_LOG"):
            with open(os.environ["FAIRGO_TEST_BACKEND_LOG"], encoding="utf-8") as arquivo:
                log = arquivo.read()
            self.assertFalse(any(s in log for s in (token, invalido, dados["novaSenha"], a["email"])),
                             "Dado confidencial encontrado no log do backend")


if __name__ == "__main__":
    unittest.main(verbosity=2)
