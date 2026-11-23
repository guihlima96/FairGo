"""Integração do Driver com identidades fictícias e banco exclusivo."""
import concurrent.futures
import hashlib
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

    def criar(self, dados):
        status, conta = api("POST", "/api/driver/contas", dados)
        self.assertEqual(status, 201)
        return conta["id"]

    def login(self, identificador, senha, status=200):
        codigo, corpo = api("POST", "/api/driver/sessoes", {"identificador": identificador, "senha": senha})
        self.assertEqual(codigo, status)
        return corpo["token"] if codigo == 200 else None

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
            self.assertFalse(any(me[k] for k in ("email_verificado","nome_verificado","celular_verificado")))
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
        usuario = self.criar(a)
        sessao = self.login(usuario, a["senha"])
        for email in (a["email"], cadastro()["email"]):
            self.assertEqual(api("POST", "/api/driver/recuperacao", {"email":email}), (202,None))
        self.assertEqual(sql(f"SELECT count(*) FROM recuperacoes_driver WHERE usuario_id='{usuario}'"), "0")
        # Fixture do futuro emissor, criada somente no banco descartável.
        token = "rec_" + secrets.token_hex(32)
        digest = hashlib.sha256(token.encode()).hexdigest()
        sql(f"INSERT INTO recuperacoes_driver VALUES ('{digest}','{usuario}',now()+interval '1 hour')")
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


if __name__ == "__main__":
    unittest.main(verbosity=2)
