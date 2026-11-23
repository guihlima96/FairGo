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
    headers = {"Content-Type": content_type}
    if token:
        headers["Authorization"] = "Bearer " + token
    if isinstance(data, dict):
        data = json.dumps(data).encode()
    request = urllib.request.Request(URL + "/api/admin/" + path, data, headers, method=method)
    try:
        response = urllib.request.urlopen(request, timeout=10)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        assert response.status == status, (method, path, response.status, status)
        body = response.read()
        if body and response.headers.get_content_type() == "application/json":
            return json.loads(body)
        return body


def upload(path, tipo, fields=None, image=JPEG, status=200):
    boundary = uuid.uuid4().hex
    chunks = []
    for key, value in (fields or {}).items():
        chunks.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'.encode())
    chunks.append((f'--{boundary}\r\nContent-Disposition: form-data; name="arquivo"; filename="foto.jpg"\r\n'
                   'Content-Type: image/jpeg\r\n\r\n').encode())
    chunks.extend([image, f'\r\n--{boundary}--\r\n'.encode()])
    return call(path + "/documentos/" + tipo, status, b"".join(chunks), "PUT",
                content_type="multipart/form-data; boundary=" + boundary)


def decidir(path, ids, status, resultado="APROVADO"):
    call(path + "/decisoes", status, {"situacao": resultado, "motivo": "Conferência manual de teste",
                                      "documentosConferidos": ids}, "POST")


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
        ids = [upload(path, tipo)["id"] for tipo in ("ANTECEDENTES", "TOXICOLOGICO")]
        for ear, validade in [("false","2099-01-01"), ("true","2000-01-01")]:
            cnh = upload(path,"CNH",{"ear":ear,"validade":validade})["id"]
            decidir(path, ids + [cnh], 422)
        decidir(path, [], 201, "REJEITADO")
        cnh = upload(path,"CNH",{"ear":"true","validade":"2099-01-01"})["id"]
        self.assertTrue(call(path + "/documentos/" + cnh, 200).startswith(b"\xff\xd8"))
        self.assertFalse(sql(f"SELECT encode(imagem_cifrada,'hex') FROM documentos_cadastro WHERE id='{cnh}'").startswith("ffd8"))
        decidir(path, ids + [str(uuid.uuid4())], 422)
        decidir(path, ids + [cnh], 201)
        decidir(path, ids + [cnh], 409)
        history = call(path + "/historico", 200)
        self.assertEqual([h["situacao"] for h in history], ["REJEITADO","PENDENTE","APROVADO"])
        self.assertTrue(all(h["responsavel"] == "teste-admin" and h["data"] and h["motivo"] for h in history))
        self.assertEqual(call(path,200)["situacao"], "APROVADO")

    def test_veiculo(self):
        placa = "TST" + str(secrets.randbelow(10000)).zfill(4)
        payload = {"placa":placa, "motoristaId":self.usuario()}
        id_veiculo = call("veiculos",201,payload,"POST")["id"]
        call("veiculos",409,payload,"POST")
        path = "validacao/VEICULO/" + id_veiculo
        ids = [upload(path,"CRLV")["id"]]
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


if __name__ == "__main__":
    unittest.main(verbosity=2)
