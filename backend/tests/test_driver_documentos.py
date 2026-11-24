"""Integração entre as duas APIs com evidências fictícias."""
import datetime
import os
import secrets
import string
import unittest
from concurrent.futures import ThreadPoolExecutor
from threading import Barrier

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

    def placa(self):
        return "".join(secrets.choice(string.ascii_uppercase) for _ in range(3)) + str(secrets.randbelow(10000)).zfill(4)

    def documentos_obrigatorios(self, token):
        rota = "/api/driver/me/documentos/"
        cnh = enviar_arquivo(rota + "CNH", token, {"ear": "true", "validade": "2099-01-01"})["id"]
        return [cnh] + [enviar_arquivo(rota + tipo, token)["id"]
                        for tipo in ("ANTECEDENTES", "FOTO_PERFIL", "RESIDENCIA")]

    def test_veiculos_e_revisoes_por_titular(self):
        motorista, token = self.motorista()
        _, outro = self.motorista()
        decidir("validacao/MOTORISTA/" + motorista, self.documentos_obrigatorios(token), 201)
        situacao = "/api/driver/me/situacao"
        veiculos = "/api/driver/veiculos"
        dados = {"portas": 4, "arCondicionado": True, "anoFabricacao": datetime.date.today().year,
                 "municipio": "0000002"}

        def veiculo_aprovado(dono):
            veiculo = request(veiculos, 201, {"placa": self.placa()}, "POST", dono)["id"]
            rota = veiculos + "/" + veiculo
            request(rota + "/dados", 204, dados, "PUT", dono)
            crlv = enviar_arquivo(rota + "/crlv", dono)["id"]
            decidir("validacao/VEICULO/" + veiculo, [crlv], 201)
            return veiculo, crlv

        alheio, crlv_alheio = veiculo_aprovado(outro)
        consulta = request(situacao, 200, token=token)
        self.assertEqual(consulta["motorista"]["analise"]["situacao"], "APROVADO")
        self.assertEqual(consulta["veiculos"], [])
        self.assertFalse(any(consulta["preElegibilidade"].values()))
        self.assertNotIn(crlv_alheio, str(consulta))
        enviar_arquivo(veiculos + "/" + alheio + "/crlv", token, status=404)

        primeiro, antigo = veiculo_aprovado(token)
        segundo, intacto = veiculo_aprovado(token)
        rota = veiculos + "/" + primeiro
        novo = enviar_arquivo(rota + "/crlv", token)["id"]
        consulta = request(situacao, 200, token=token)
        por_id = {v["id"]: v for v in consulta["veiculos"]}
        self.assertEqual(set(por_id), {primeiro, segundo})
        self.assertEqual(por_id[primeiro]["crlv"]["revisaoAtual"], novo)
        self.assertEqual(por_id[primeiro]["analise"]["situacao"], "PENDENTE")
        self.assertEqual(por_id[segundo]["crlv"]["revisaoAtual"], intacto)
        self.assertEqual(por_id[segundo]["analise"]["situacao"], "APROVADO")
        self.assertEqual(consulta["preElegibilidade"],
                         {"motoristaEVeiculoAprovados": True, "autorizadoParaCorridas": False})
        self.assertNotIn(antigo, str(consulta))
        alvo = "validacao/VEICULO/" + primeiro
        for revisao in (antigo, intacto, crlv_alheio):
            call(alvo + "/documentos/" + revisao, 404)
            decidir(alvo, [revisao], 422)
        self.assertEqual(request(veiculos + "/" + alheio, 200, token=outro)["revisaoCrlv"], crlv_alheio)

    def test_consulta_durante_substituicao_e_decisao(self):
        motorista, token = self.motorista()
        alvo = "validacao/MOTORISTA/" + motorista
        ids = self.documentos_obrigatorios(token)
        antiga = ids[-1]
        inicio = Barrier(3, timeout=10)

        def substituir():
            inicio.wait()
            return enviar_arquivo("/api/driver/me/documentos/RESIDENCIA", token)["id"]

        def aprovar():
            inicio.wait()
            # Se a substituição vencer o bloqueio, a revisão apresentada já estará obsoleta.
            decidir(alvo, ids, (201, 422))

        def consultar():
            inicio.wait()
            return [request("/api/driver/me/situacao", 200, token=token)["motorista"] for _ in range(8)]

        with ThreadPoolExecutor(max_workers=3) as executor:
            envio = executor.submit(substituir)
            decisao = executor.submit(aprovar)
            leituras = executor.submit(consultar)
            nova = envio.result()
            decisao.result()
            amostras = leituras.result()

        historico = call(alvo + "/historico", 200)
        estados = [h["situacao"] for h in historico]
        self.assertIn(estados, ([], ["APROVADO", "PENDENTE"]))
        if historico:
            self.assertEqual(set(historico[0]["revisoes_conferidas"]), set(ids))
        for motorista in amostras:
            doc, = [d for d in motorista["documentos"] if d["tipo"] == "RESIDENCIA"]
            estado = motorista["analise"]["situacao"]
            self.assertIn((estado, doc["revisaoAtual"]),
                          {("PENDENTE", antiga), ("PENDENTE", nova), ("APROVADO", antiga)})
        final = request("/api/driver/me/situacao", 200, token=token)["motorista"]
        self.assertEqual(final["analise"]["situacao"], "PENDENTE")
        residencia, = [d for d in final["documentos"] if d["tipo"] == "RESIDENCIA"]
        self.assertEqual(residencia["revisaoAtual"], nova)

    def test_situacao_e_rejeicao(self):
        titular, token = self.motorista()
        _, outro = self.motorista()
        rota = "/api/driver/me/situacao"
        request(rota, 401)
        request(rota, 401, token=TOKEN)
        inicial = request(rota, 200, token=token)
        motorista = inicial["motorista"]
        self.assertEqual(motorista["id"], titular)
        self.assertFalse(any(motorista[k] for k in ("nomeVerificado", "emailVerificado", "celularVerificado")))
        self.assertEqual(inicial["veiculos"], [])
        self.assertFalse(any(inicial["preElegibilidade"].values()))
        docs = motorista["documentos"]
        self.assertEqual({d["tipo"] for d in docs if d["obrigatorio"]},
                         {"CNH", "FOTO_PERFIL", "RESIDENCIA", "ANTECEDENTES"})
        self.assertEqual([d["tipo"] for d in docs if not d["obrigatorio"]], ["TOXICOLOGICO"])
        self.assertTrue(all(d["revisaoAtual"] is None for d in docs))

        envio = "/api/driver/me/documentos/RESIDENCIA"
        antiga = enviar_arquivo(envio, token)["id"]
        decidir("validacao/MOTORISTA/" + titular, [], 201, "REJEITADO")
        rejeitado = request(rota, 200, token=token)
        analise = rejeitado["motorista"]["analise"]
        self.assertEqual(analise, {"situacao": "REJEITADO",
                                 "orientacaoPublica": "Revise os dados e documentos deste cadastro."})
        self.assertNotIn("Conferência manual de teste", str(rejeitado))
        self.assertNotIn("admin-integracao", str(rejeitado))
        alheio = request(rota + "?motoristaId=" + titular, 200, token=outro)
        self.assertNotIn(titular, str(alheio))
        self.assertNotIn(antiga, str(alheio))

        atual = enviar_arquivo(envio, token)["id"]
        reaberto = request(rota, 200, token=token)["motorista"]
        self.assertEqual(reaberto["analise"], {"situacao": "PENDENTE", "orientacaoPublica": None})
        residencia, = [d for d in reaberto["documentos"] if d["tipo"] == "RESIDENCIA"]
        self.assertEqual(residencia["revisaoAtual"], atual)
        self.assertNotIn(antiga, str(reaberto))

    def test_driver_admin(self):
        id_, dono = self.motorista()
        outro_id, outro = self.motorista()
        veiculos = "/api/driver/veiculos"
        docs = "/api/driver/me/documentos/"
        placa = self.placa()
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
        situacao = "/api/driver/me/situacao"
        pendente, = request(situacao, 200, token=dono)["veiculos"]
        self.assertFalse(pendente["dadosTecnicosInformados"])
        self.assertTrue(pendente["crlv"]["obrigatorio"])
        self.assertIsNone(pendente["crlv"]["revisaoAtual"])
        self.assertEqual(request(situacao, 200, token=outro)["veiculos"], [])
        decidir("validacao/VEICULO/" + v, [], 201, "REJEITADO")
        rejeitado, = request(situacao, 200, token=dono)["veiculos"]
        self.assertEqual(rejeitado["analise"]["situacao"], "REJEITADO")
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
        aprovado = request(situacao, 200, token=dono)
        self.assertEqual(aprovado["preElegibilidade"],
                         {"motoristaEVeiculoAprovados": True, "autorizadoParaCorridas": False})
        self.assertFalse(aprovado["motorista"]["nomeVerificado"])
        self.assertTrue(aprovado["veiculos"][0]["dadosTecnicosInformados"])
        novo = enviar_arquivo(rota + "/crlv", dono)["id"]
        revisado = request(situacao, 200, token=dono)
        self.assertFalse(revisado["preElegibilidade"]["motoristaEVeiculoAprovados"])
        self.assertEqual(revisado["veiculos"][0]["crlv"]["revisaoAtual"], novo)
        self.assertNotIn(crlv, str(revisado))
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
            reaberturas = [h for h in historico if h["situacao"] == "PENDENTE"]
            self.assertEqual(len(reaberturas), 2 if alvo == veiculo else 1)
            self.assertTrue(all(h["responsavel"] == "driver:" + id_ for h in reaberturas))


if __name__ == "__main__":
    unittest.main(verbosity=2)
