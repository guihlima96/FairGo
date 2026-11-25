# Orientações para trabalhar no FairGo

## Arquitetura existente

O backend em `backend/` usa Java 17, Spring Boot 3.5.7, Spring Web, Security e Validation. A persistência usa Spring JDBC com JdbcTemplate e SQL explícito em PostgreSQL. O Maven lê `backend/pom.xml`. O painel administrativo é servido pelo próprio backend em `/admin/index.html`.

Preserve os contratos REST e os DTOs existentes. Prefira injeção por construtor e valide as entradas no servidor. Mantenha a persistência em JDBC. Consulte `backend/src/main/resources/schema.sql` e `validacao.sql` antes de propor mudanças de dados. Preserve os registros existentes e use variáveis de ambiente para credenciais e chave do cofre.

## Build e testes

Na raiz do repositório, com JDK 17 e Maven no PATH:

```powershell
mvn -f backend/pom.xml compile
mvn -B -f backend/pom.xml verify
```

A fase `verify` inclui testes Maven e empacotamento. As suítes JUnit de verificação de contatos e recuperação interna usam PostgreSQL quando `PGDATABASE` termina em `_test`. Configure as variáveis de teste antes de `verify` para executá-las. Para reproduzir a regressão completa:

1. Prepare PostgreSQL e psql com um banco descartável exclusivo terminado em `_test`. Configure as variáveis JDBC e a chave de documentos conforme o [guia do backend](backend/README.md). Inicie o servidor com `mvn -f backend/pom.xml spring-boot:run`.
2. Em outro terminal, na raiz, aponte `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER` e `PGPASSWORD` para o mesmo banco. Configure `PSQL` se necessário. Para a suíte de confirmação HTTP, forneça a mesma `FAIRGO_CONTACT_KEY` temporária (32 bytes aleatórios em Base64) ao servidor e ao processo Python. Aguarde o painel responder. Use Python 3 em ambiente virtual e Chrome com driver compatível disponível para Selenium.
3. Execute em sequência:

```powershell
$env:FAIRGO_TEST_URL = 'http://127.0.0.1:8080'
python -m pip install selenium==4.29.0
python backend/tests/test_validacao.py
python backend/tests/test_painel.py
1..3 | ForEach-Object { python backend/tests/test_driver.py; if ($LASTEXITCODE) { throw "Falha na suíte Driver" } }
python backend/tests/test_driver_documentos.py
```

As suítes cobrem a API Admin, o painel Selenium, as contas e contatos Driver e a integração documental Driver/Admin. As três rodadas consecutivas da suíte de contas verificam isolamento e repetibilidade. O [workflow Admin](.github/workflows/admin.yml) reproduz a sequência no CI.

## Fluxo de trabalho

Consulte [FAIRGO_SPEC.md](FAIRGO_SPEC.md) para regras aprovadas. Registre propostas e pendências explicitamente. Antes de mudanças complexas, identifique os arquivos e contratos afetados. Trabalhe em branch própria, com commits focados no padrão Conventional Commits. Nas mudanças de backend ou painel, valide o build e todas as suítes de regressão.

O [plano da Etapa 5](docs/etapa-5-driver-admin.md) descreve a integração proposta para revisão.
