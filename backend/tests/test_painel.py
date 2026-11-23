"""Fluxo fictício do painel com Chrome e o backend/banco exclusivos da suíte HTTP."""
import datetime
import unittest
import uuid

from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from test_validacao import URL, TOKEN, DIGEST, call, upload, sql
import os


class Painel(unittest.TestCase):
    def test_revisao_no_navegador(self):
        self.assertTrue(os.environ["PGDATABASE"].endswith("_test"))
        sql(f"INSERT INTO credenciais_admin VALUES ('{DIGEST}','painel-teste',now()+interval '10 minutes',false)")
        self.addCleanup(sql, f"DELETE FROM credenciais_admin WHERE digest='{DIGEST}'")
        nome = "Motorista " + uuid.uuid4().hex
        motorista = call("usuarios", 201, {"nome": nome, "email": uuid.uuid4().hex + "@example.com",
                                           "perfil": "MOTORISTA"}, "POST")["id"]
        placa = "WEB" + str(uuid.uuid4().int % 10000).zfill(4)
        veiculo = call("veiculos", 201, {"placa": placa, "motoristaId": motorista}, "POST")["id"]
        for tipo in ("ANTECEDENTES", "TOXICOLOGICO", "CNH"):
            upload("validacao/MOTORISTA/" + motorista, tipo,
                   {"ear": "true", "validade": "2099-12-31"} if tipo == "CNH" else {})
        rota = "validacao/VEICULO/" + veiculo
        doc = upload(rota, "CRLV")
        call(rota + "/dados", 204, {"portas": 4, "arCondicionado": True,
                                    "anoFabricacao": datetime.date.today().year, "municipio": "0000002"}, "PUT")
        call("validacao/pendentes", 401, token=None)
        call(rota + "/documentos/" + doc["id"], 401, token=None)

        opcoes = webdriver.ChromeOptions()
        opcoes.add_argument("--headless=new")
        opcoes.add_argument("--no-sandbox")
        navegador = webdriver.Chrome(options=opcoes)
        self.addCleanup(navegador.quit)
        espera = WebDriverWait(navegador, 15)
        def campo(id_elemento):
            return navegador.find_element(By.ID, id_elemento)

        navegador.get(URL + "/admin/index.html")
        campo("token").send_keys(TOKEN)
        campo("acesso").find_element(By.TAG_NAME, "button").click()
        for descricao, resultado in [(nome, "REJEITADO"), (placa, "APROVADO")]:
            espera.until(lambda _: descricao in campo("fila").text)
            campo("fila").find_element(By.XPATH, ".//button[contains(.,'" + descricao + "')]").click()
            espera.until(lambda _: descricao in campo("titulo").text)
            campo("documentos").find_element(By.TAG_NAME, "button").click()
            espera.until(lambda _: campo("visor").is_displayed())
            campo("documentos").find_element(By.TAG_NAME, "input").click()
            campo("fechar").click()
            botao = campo("decisao").find_element(By.CSS_SELECTOR, "button[value='" + resultado + "']")
            botao.click()
            self.assertFalse(campo("motivo").get_property("validity")["valid"])
            campo("motivo").send_keys("Análise fictícia")
            botao.click()
            navegador.switch_to.alert.dismiss()
            self.assertIn("PENDENTE", campo("situacao").text)
            botao.click()
            navegador.switch_to.alert.accept()
            espera.until(lambda _: campo("aviso").text == "Decisão registrada.")
            self.assertIn(resultado, campo("situacao").text)
            self.assertIn("Análise fictícia", campo("historico").text)
            self.assertIn("painel-teste", campo("historico").text)
            self.assertNotIn(descricao, campo("fila").text)


if __name__ == "__main__":
    unittest.main(verbosity=2)
