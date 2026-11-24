"""Teste HTTP em um backend local iniciado com um banco exclusivo de testes."""
import base64
import datetime
import hashlib
import json
import os
import secrets
import subprocess
import unittest
import urllib.error
import urllib.request
import uuid

URL = os.environ["FAIRGO_TEST_URL"].rstrip("/")
TOKEN = secrets.token_hex(32)
DIGEST = hashlib.sha256(TOKEN.encode()).hexdigest()
JPEG = base64.b64decode("/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAACAAIDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD5/ooooA//2Q==")


def sql(statement):
    args = [os.environ.get("PSQL", "psql"), "-XAt", "-v", "ON_ERROR_STOP=1", "-c", statement]
    return subprocess.check_output(args, text=True).strip()


def call(path, status, data=None, method="GET", token=TOKEN, content_type="application/json"):
    return request("/api/admin/" + path, status, data, method, token, content_type)


def request(path, status, data=None, method="GET", token=None, content_type="application/json"):
    headers = {"Content-Type": content_type}
    if token:
        headers["Authorization"] = "Bearer " + token
    if isinstance(data, dict):
        data = json.dumps(data).encode()
    request = urllib.request.Request(URL + path, data, headers, method=method)
    try:
        response = urllib.request.urlopen(request, timeout=25)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        expected = status if isinstance(status, tuple) else (status,)
        assert response.status in expected, (method, path, response.status, status)
        body = response.read()
        if body and response.headers.get_content_type() == "application/json":
            return json.loads(body)
        return body


def upload(path, tipo, fields=None, image=JPEG, status=200):
    return enviar_arquivo("/api/admin/" + path + "/documentos/" + tipo, TOKEN, fields, image, status)


def enviar_arquivo(rota, token, fields=None, image=JPEG, status=200):
    boundary = uuid.uuid4().hex
    chunks = []
    for key, value in (fields or {}).items():
        chunks.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'.encode())
    chunks.append((f'--{boundary}\r\nContent-Disposition: form-data; name="arquivo"; filename="foto.jpg"\r\n'
                   'Content-Type: image/jpeg\r\n\r\n').encode())
    chunks.extend([image, f'\r\n--{boundary}--\r\n'.encode()])
    return request(rota, status, b"".join(chunks), "PUT", token,
                content_type="multipart/form-data; boundary=" + boundary)


def pdf_teste(paginas=1, catalogo=b""):
    filhos = b" ".join(f"{i + 3} 0 R".encode() for i in range(paginas))
    objetos = [b"<< /Type /Catalog /Pages 2 0 R " + catalogo + b" >>",
               b"<< /Type /Pages /Count " + str(paginas).encode() + b" /Kids [" + filhos + b"] >>"]
    fonte, figura, conteudo = paginas + 3, paginas + 4, paginas + 5
    pagina = (f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
              f"/Resources << /Font << /F1 {fonte} 0 R >> /XObject << /Im {figura} 0 R >> >> "
              f"/Contents {conteudo} 0 R >>").encode()
    objetos.extend([pagina] * paginas)
    objetos.append(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
    objetos.append(f"<< /Type /XObject /Subtype /Image /Width 2 /Height 2 /ColorSpace /DeviceRGB "
                   f"/BitsPerComponent 8 /Filter /DCTDecode /Length {len(JPEG)} >>\nstream\n".encode()
                   + JPEG + b"\nendstream")
    comandos = b"BT /F1 15 Tf 72 720 Td (Cadastro ficticio FairGo) Tj ET q 80 0 0 80 72 600 cm /Im Do Q"
    objetos.append(f"<< /Length {len(comandos)} >>\nstream\n".encode() + comandos + b"\nendstream")
    dados = bytearray(b"%PDF-1.4\n")
    offsets = []
    for i, obj in enumerate(objetos, 1):
        offsets.append(len(dados))
        dados.extend(f"{i} 0 obj\n".encode() + obj + b"\nendobj\n")
    inicio = len(dados)
    dados.extend(f"xref\n0 {len(objetos)+1}\n0000000000 65535 f \n".encode())
    for offset in offsets:
        dados.extend(f"{offset:010d} 00000 n \n".encode())
    dados.extend(f"trailer\n<< /Size {len(objetos)+1} /Root 1 0 R >>\nstartxref\n{inicio}\n%%EOF\n".encode())
    return bytes(dados)


def decidir(path, ids, status, resultado="APROVADO", identidade=True):
    call(path + "/decisoes", status, {"situacao": resultado, "motivo": "Conferência manual de teste",
                                      "identidadeConferida": identidade, "documentosConferidos": ids}, "POST")


class Validacao(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if not os.environ["PGDATABASE"].endswith("_test"):
            raise RuntimeError("Use um banco exclusivo com sufixo _test")
        sql(f"INSERT INTO credenciais_admin VALUES ('{DIGEST}','teste-admin',now()+interval '10 minutes',false)")
        sql("INSERT INTO limites_municipais VALUES ('0000001',8),('0000002',11) ON CONFLICT DO NOTHING")

    @classmethod
    def tearDownClass(cls):
        sql(f"DELETE FROM credenciais_admin WHERE digest='{DIGEST}'")

    def usuario(self, perfil="MOTORISTA"):
        return call("usuarios", 201, {"nome": "Pessoa Teste", "perfil": perfil,
                                     "email": uuid.uuid4().hex + "@example.com"}, "POST")["id"]

    def test_autorizacao(self):
        path = "validacao/MOTORISTA/" + str(uuid.uuid4())
        call(path, 401, token=None)
        call(path, 401, token=secrets.token_hex(32))
        call(path, 404)
        passageiro = self.usuario("PASSAGEIRO")
        call("validacao/MOTORISTA/" + passageiro, 404)

    def test_credenciais_invalidas(self):
        for vencida, revogada in [(True, False), (False, True)]:
            token = secrets.token_hex(32)
            digest = hashlib.sha256(token.encode()).hexdigest()
            ttl = "-1 minute" if vencida else "1 minute"
            sql(f"INSERT INTO credenciais_admin VALUES ('{digest}','expiracao',now()+interval '{ttl}',{revogada})")
            try:
                call("validacao/MOTORISTA/" + str(uuid.uuid4()), 401, token=token)
            finally:
                sql(f"DELETE FROM credenciais_admin WHERE digest='{digest}'")

    def test_motorista(self):
        path = "validacao/MOTORISTA/" + self.usuario()
        decidir(path, [], 422)
        upload(path, "CNH", status=400)
        upload(path, "ANTECEDENTES", image=b"conteudo invalido", status=400)
        upload(path, "FOTO_PERFIL", image=pdf_teste(), status=400)
        upload(path, "RESIDENCIA", image=b"%PDF-invalido", status=400)
        upload(path, "CNH", {"ear":"true","validade":"2099-01-01"}, image=pdf_teste(11), status=400)
        marcado = pdf_teste(catalogo=b"/OpenAction << /S /JavaScript /JS () >>")
        doc = upload(path, "CNH", {"ear":"true","validade":"2099-01-01"}, image=marcado)
        copia = call(path + "/documentos/" + doc["id"], 200)
        self.assertTrue(copia.startswith(b"%PDF-"))
        self.assertNotEqual(copia, marcado)
        self.assertNotIn(b"/JavaScript", copia)
        ids = [upload(path, "FOTO_PERFIL")["id"]]
        for tipo in ("ANTECEDENTES", "RESIDENCIA"):
            revisao = upload(path, tipo, image=pdf_teste())["id"]
            self.assertTrue(call(path + "/documentos/" + revisao, 200).startswith(b"%PDF-"))
            ids.append(revisao)
        for ear, validade in [("false","2099-01-01"), ("true","2000-01-01")]:
            cnh = upload(path,"CNH",{"ear":ear,"validade":validade})["id"]
            decidir(path, ids + [cnh], 422)
        decidir(path, [], 201, "REJEITADO")
        cnh = upload(path,"CNH",{"ear":"true","validade":"2099-01-01"})["id"]
        self.assertTrue(call(path + "/documentos/" + cnh, 200).startswith(b"\xff\xd8"))
        self.assertFalse(sql(f"SELECT encode(imagem_cifrada,'hex') FROM documentos_cadastro WHERE id='{cnh}'").startswith("ffd8"))
        decidir(path, ids + [str(uuid.uuid4())], 422)
        decidir(path, ids + [cnh], 422, identidade=False)
        decidir(path, [ids[0], ids[2], cnh], 422)
        decidir(path, ids + [cnh], 201)
        decidir(path, ids + [cnh], 409)
        history = call(path + "/historico", 200)
        self.assertEqual([h["situacao"] for h in history], ["REJEITADO","PENDENTE","APROVADO"])
        self.assertTrue(all(h["responsavel"] == "teste-admin" and h["data"] and h["motivo"] for h in history))
        self.assertCountEqual(history[-1]["revisoes_conferidas"], ids + [cnh])
        self.assertEqual(call(path,200)["situacao"], "APROVADO")
        tox = upload(path, "TOXICOLOGICO", {"validade":"2000-01-01"})
        decidir(path, ids + [cnh, tox["id"]], 201)

    def test_veiculo(self):
        placa = "TST" + str(secrets.randbelow(10000)).zfill(4)
        payload = {"placa":placa, "motoristaId":self.usuario()}
        id_veiculo = call("veiculos",201,payload,"POST")["id"]
        call("veiculos",409,payload,"POST")
        path = "validacao/VEICULO/" + id_veiculo
        ids = [upload(path,"CRLV", image=pdf_teste(10))["id"]]
        decidir(path, ids, 422)
        ano = datetime.date.today().year
        for portas, ar, idade, cidade, status in [
            (3,True,1,"0000002",422), (4,False,1,"0000002",422), (4,True,1,"0000000",422),
            (4,True,9,"0000001",422), (4,True,12,"0000002",422), (4,True,11,"0000002",201),
            (4,True,8,"0000001",201)
        ]:
            call(path + "/dados",204,{"portas":portas,"arCondicionado":ar,"anoFabricacao":ano-idade,
                                      "municipio":cidade},"PUT")
            decidir(path,ids,status)
            if status == 201:
                self.assertEqual(call(path + "/historico", 200)[-1]["revisoes_conferidas"], ids)


if __name__ == "__main__":
    unittest.main(verbosity=2)
