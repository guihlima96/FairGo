"""Integração entre as duas APIs com evidências fictícias."""
import datetime
import os
import secrets
import string
import unittest

from test_driver import cadastro
from test_validacao import TOKEN, DIGEST, call, decidir, enviar_arquivo, pdf_teste, request, sql


class DocumentosDriver(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if not os.environ["PGDATABASE"].endswith("_test"):
            raise RuntimeError("Use banco exclusivo com sufixo _test")
        sql("DELETE FROM limites_driver")
        sql(f"INSERT INTO credenciais_admin VALUES ('{DIGEST}','admin-integracao',now()+interval '10 minutes',false)")
        sql("INSERT INTO limites_municipais VALUES ('0000002',11) ON CONFLICT DO NOTHING")

    @classmethod
    def tearDownClass(cls):
        sql(f"DELETE FROM credenciais_admin WHERE digest='{DIGEST}'")

    def motorista(self):
        dados = cadastro()
        id_ = request("/api/driver/contas", 201, dados, "POST")["id"]
        token = request("/api/driver/sessoes", 200, {"identificador":id_, "senha":dados["senha"]}, "POST")["token"]
        return id_, token

    def test_driver_admin(self):
        id_, dono = self.motorista()
        outro_id, outro = self.motorista()
        veiculos = "/api/driver/veiculos"
        docs = "/api/driver/me/documentos/"
        placa = "".join(secrets.choice(string.ascii_uppercase) for _ in range(3)) + str(secrets.randbelow(10000)).zfill(4)
        request(veiculos, 401, {"placa":placa}, "POST")
        request(veiculos, 401, token=TOKEN)
        v = request(veiculos, 201, {"placa":placa,"motoristaId":outro_id}, "POST", dono)["id"]
        rota = veiculos + "/" + v
        dados = {"portas":4,"arCondicionado":True,"anoFabricacao":datetime.date.today().year,"municipio":"0000002"}
        self.assertEqual(request(veiculos, 200, token=outro), [])
        self.assertEqual(request(veiculos, 200, token=dono)[0]["id"], v)
        request(rota, 404, token=outro)
        request(rota + "/dados", 404, dados, "PUT", outro)
        enviar_arquivo(rota + "/crlv", outro, status=404)
        enviar_arquivo(docs + "CNH", None, status=401)
        call("validacao/pendentes", 401, token=dono)
        request(veiculos, 409, {"placa":placa}, "POST", outro)
        request(rota + "/dados", 204, dados, "PUT", dono)
        crlv = enviar_arquivo(rota + "/crlv", dono, image=pdf_teste())["id"]
        self.assertEqual(request(rota, 200, token=dono)["dados"], dados)
        self.assertEqual(request(rota, 200, token=dono)["revisaoCrlv"], crlv)
        enviar_arquivo(docs + "CRLV", dono, status=400)
        enviar_arquivo(docs + "FOTO_PERFIL", dono, image=pdf_teste(), status=400)
        cnh = enviar_arquivo(docs + "CNH", dono, {"ear":"true","validade":"2099-01-01"}, pdf_teste())["id"]
        ids = [enviar_arquivo(docs + tipo, dono)["id"] for tipo in ("ANTECEDENTES","FOTO_PERFIL","RESIDENCIA")]
        motorista = "validacao/MOTORISTA/" + id_
        veiculo = "validacao/VEICULO/" + v
        fila = {item["id"] for item in call("validacao/pendentes", 200)}
        self.assertTrue({id_, v}.issubset(fila))
        self.assertEqual(call("validacao/MOTORISTA/" + outro_id, 200)["documentos"], [])
        self.assertTrue(call(veiculo + "/documentos/" + crlv, 200).startswith(b"%PDF-"))
        decidir(motorista, ids + [cnh], 201)
        decidir(veiculo, [crlv], 201)
        self.assertEqual(request(rota, 200, token=dono)["situacao"], "APROVADO")
        novo = enviar_arquivo(rota + "/crlv", dono)["id"]
        self.assertNotEqual(novo, crlv)
        self.assertEqual(request(rota, 200, token=dono)["situacao"], "PENDENTE")
        self.assertEqual(request("/api/driver/me", 200, token=dono)["situacao"], "APROVADO")
        decidir(veiculo, [crlv], 422)
        decidir(veiculo, [novo], 201)
        enviar_arquivo(docs + "RESIDENCIA", dono, image=pdf_teste())
        enviar_arquivo(docs + "TOXICOLOGICO", dono)
        self.assertEqual(request("/api/driver/me", 200, token=dono)["situacao"], "PENDENTE")
        for alvo in (motorista, veiculo):
            historico = call(alvo + "/historico", 200)
            reabertura, = [h for h in historico if h["situacao"] == "PENDENTE"]
            self.assertEqual(reabertura["responsavel"], "driver:" + id_)


if __name__ == "__main__":
    unittest.main(verbosity=2)
