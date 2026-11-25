"""Fluxo fictício do painel com Chrome e o backend/banco exclusivos da suíte HTTP."""
import datetime
import unittest
import uuid

from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select, WebDriverWait
from test_validacao import URL, TOKEN, DIGEST, call, upload, sql, pdf_teste
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
        def entrada(id_cadastro):
            return next(i for i in call("validacao/fila", 200) if i["id"] == id_cadastro)

        call("validacao/fila", 401, token=None)
        inicial = entrada(motorista)
        self.assertFalse(inicial["prontoParaAnalise"])
        self.assertEqual(set(inicial["ausentes"]), {"CNH", "ANTECEDENTES", "RESIDENCIA", "FOTO_PERFIL"})
        self.assertEqual(inicial["enviados"], [])
        self.assertFalse(entrada(veiculo)["dadosTecnicosPresentes"])
        for tipo in ("ANTECEDENTES", "FOTO_PERFIL", "RESIDENCIA", "CNH"):
            upload("validacao/MOTORISTA/" + motorista, tipo,
                   {"ear": "true", "validade": "2099-12-31"} if tipo == "CNH" else {})
        rota = "validacao/VEICULO/" + veiculo
        doc = upload(rota, "CRLV")
        self.assertFalse(entrada(veiculo)["prontoParaAnalise"])
        self.assertEqual(entrada(veiculo)["enviados"], ["CRLV"])
        self.assertTrue(entrada(motorista)["prontoParaAnalise"])
        self.assertEqual(entrada(motorista)["situacao"], "PENDENTE")
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
        espera.until(lambda _: placa in campo("prontos").text)
        self.assertTrue(entrada(veiculo)["prontoParaAnalise"])
        filtro = Select(campo("filtro"))
        filtro.select_by_value("VEICULO")
        self.assertNotIn(nome, campo("fila").text)
        self.assertIn(placa, campo("prontos").text)
        filtro.select_by_value("")
        def total(grupo):
            return int(navegador.find_element(By.CSS_SELECTOR, "[data-total='" + grupo + "']").text)

        # Envio externo muda os indicadores com o painel aberto.
        extra = "Motorista incompleto " + uuid.uuid4().hex
        id_extra = call("usuarios", 201, {"nome": extra, "email": uuid.uuid4().hex + "@example.com",
                                         "perfil": "MOTORISTA"}, "POST")["id"]
        WebDriverWait(navegador, 35).until(lambda _: extra in campo("incompletos").text)
        incompletos, prontos = total("incompletos"), total("prontos")
        for tipo_doc in ("CNH", "ANTECEDENTES", "RESIDENCIA", "FOTO_PERFIL"):
            upload("validacao/MOTORISTA/" + id_extra, tipo_doc,
                   {"ear": "true", "validade": "2099-12-31"} if tipo_doc == "CNH" else {})
        WebDriverWait(navegador, 35).until(lambda _: extra in campo("prontos").text)
        self.assertEqual(total("incompletos"), incompletos - 1)
        self.assertEqual(total("prontos"), prontos + 1)
        for descricao, resultado in [(nome, "REJEITADO"), (placa, "APROVADO"), (nome, "APROVADO")]:
            conferir_identidade = descricao == nome and resultado == "APROVADO"
            if conferir_identidade:
                upload("validacao/MOTORISTA/" + motorista, "RESIDENCIA", image=pdf_teste())
                WebDriverWait(navegador, 35).until(lambda _: nome in campo("prontos").text)
            espera.until(lambda _: descricao in campo("fila").text)
            campo("fila").find_element(By.XPATH, ".//button[contains(.,'" + descricao + "')]").click()
            espera.until(lambda _: descricao in campo("titulo").text)
            linhas = campo("documentos").find_elements(By.TAG_NAME, "li")
            for linha in linhas if conferir_identidade else linhas[:1]:
                linha.find_element(By.TAG_NAME, "button").click()
                espera.until(lambda _: campo("visor").is_displayed())
                if campo("pdf").is_displayed():
                    campo("pdf").click()
                linha.find_element(By.TAG_NAME, "input").click()
                campo("fechar").click()
            if conferir_identidade:
                campo("identidade").click()
            if resultado == "REJEITADO":
                Select(campo("codigoPublico")).select_by_value("RESIDENCIA_INVALIDA")
                self.assertTrue(campo("docPublico").get_attribute("value"))
            botao = campo("decisao").find_element(By.CSS_SELECTOR, "button[value='" + resultado + "']")
            botao.click()
            self.assertFalse(campo("motivo").get_property("validity")["valid"])
            campo("motivo").send_keys("Análise fictícia")
            botao.click()
            navegador.switch_to.alert.dismiss()
            grupo = "aprovados" if resultado == "APROVADO" else "rejeitados"
            antes, prontos = total(grupo), total("prontos")
            self.assertIn("PENDENTE", campo("situacao").text)
            botao.click()
            navegador.switch_to.alert.accept()
            espera.until(lambda _: campo("aviso").text == "Decisão registrada.")
            self.assertIn(resultado, campo("situacao").text)
            self.assertIn("Análise fictícia", campo("historico").text)
            self.assertIn("painel-teste", campo("historico").text)
            if resultado == "REJEITADO":
                self.assertIn("RESIDENCIA_INVALIDA", campo("historico").text)
                self.assertIn("RESIDENCIA_INVALIDA", campo("rejeitados").text)
            self.assertNotIn(descricao, campo("fila").text)
            self.assertIn(descricao, campo(grupo).text)
            self.assertEqual(total(grupo), antes + 1)
            self.assertEqual(total("prontos"), prontos - 1)
        self.assertEqual(campo("vigencia").text, "Sem conferência vigente.")
        for tipo in ("CNH", "FOTO_PERFIL"):
            linha = campo("documentos").find_element(By.XPATH, ".//li[.//input[@data-tipo='" + tipo + "']]")
            linha.find_element(By.TAG_NAME, "button").click()
            espera.until(lambda _: campo("visor").is_displayed())
            linha.find_element(By.TAG_NAME, "input").click()
            campo("fechar").click()
        Select(campo("parecer")).select_by_value("CONFERE")
        campo("conferencia").find_element(By.TAG_NAME, "button").click()
        navegador.switch_to.alert.accept()
        espera.until(lambda _: campo("aviso").text == "Conferência registrada.")
        self.assertIn("CONFERE", campo("vigencia").text)
        self.assertIn("APROVADO", campo("situacao").text)


if __name__ == "__main__":
    unittest.main(verbosity=2)
