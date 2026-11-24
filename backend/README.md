# FAIRGO • validação administrativa

## Executar localmente

Pré-requisitos: JDK 17, Maven e PostgreSQL. No PowerShell, na raiz do projeto, configure o banco existente ou crie um banco e um usuário proprietário para desenvolvimento. Use uma chave AES de 32 bytes em Base64, estável entre reinícios. Gere-a **uma vez** com Python 3 e armazene a saída em um gerenciador de segredos:

```powershell
python -c "import secrets,base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"
$env:FAIRGO_DB_URL = 'jdbc:postgresql://localhost:5432/fairgo'
$env:FAIRGO_DB_USER = 'fairgo_app'
$env:FAIRGO_DB_PASSWORD = [Net.NetworkCredential]::new('', (Read-Host 'Senha do banco' -AsSecureString)).Password
$env:FAIRGO_DOCUMENT_KEY = [Net.NetworkCredential]::new('', (Read-Host 'Chave Base64 do cofre' -AsSecureString)).Password
mvn -f backend/pom.xml spring-boot:run
```

A inicialização adiciona tabelas às existentes. A chave perdida torna os documentos ilegíveis. Faça backup da chave separado do banco. A troca de chave exige recifrar as imagens existentes.

## Identidade administrativa

Cada pessoa recebe um token aleatório individual com prazo e revogação. A API aceita apenas o cabeçalho `Authorization: Bearer <token>`. Para gerar localmente um token e seu digest, execute no computador do operador autorizado (Python 3):

```powershell
python -c "import secrets,hashlib; t=secrets.token_hex(32); print('Token:',t); print('Digest:',hashlib.sha256(t.encode()).hexdigest())"
```

Entregue o token pelo canal de segredos da equipe. No psql, como responsável pelo provisionamento, insira **apenas o digest** e uma identidade única da pessoa. Exemplo com prompts:

```sql
\prompt 'Digest SHA-256: ' digest
\prompt 'Identificador individual: ' pessoa
INSERT INTO credenciais_admin (digest,admin_id,expira_em)
VALUES (:'digest', :'pessoa', now()+interval '8 hours');
```

Para revogar, execute `UPDATE credenciais_admin SET revogada=true WHERE digest=:'digest';`. O próximo pedido já consulta a revogação. Os antigos POST /api/admin/usuarios e POST /api/admin/veiculos mantêm seus corpos, respostas e códigos. Clientes que usavam Basic precisam enviar o token. As antigas variáveis FAIRGO_ADMIN_USER/PASSWORD são dispensadas.

## Conferência manual

Prefixo: `/api/admin/validacao`. Substitua `{alvo}` por `MOTORISTA` ou `VEICULO` e `{id}` pelo UUID do respectivo cadastro.

| Método e caminho relativo | Operação |
|---|---|
| PUT /VEICULO/{id}/dados | JSON com portas (inteiro), arCondicionado (booleano), anoFabricacao e municipio (código IBGE de 7 dígitos). |
| PUT /{alvo}/{id}/documentos/{tipo} | multipart/form-data. Campo arquivo com documento legível de até 4 MiB e 8 milhões de pixels. Para CNH, enviar ear e validade (AAAA-MM-DD). Para os demais, validade quando aplicável. |
| GET /{alvo}/{id} | Situação, dados técnicos e metadados para revisão, incluindo UUID de cada versão atual. |
| GET /{alvo}/{id}/documentos/{revisao} | Download autenticado do JPEG ou da cópia visual em PDF para conferência. |
| POST /{alvo}/{id}/decisoes | JSON com situacao, motivo e, para aprovação, documentosConferidos com os UUIDs de todas as revisões obrigatórias e das facultativas conferidas. |
| GET /{alvo}/{id}/historico | Decisões e reaberturas em ordem, com motivo, data do servidor e identidade do token. |

Cada motorista exige CNH definitiva com EAR e validade vigente, antecedentes, FOTO_PERFIL e RESIDENCIA. O toxicológico é facultativo. CNH, CRLV, antecedentes e residência aceitam JPEG e PDF. Foto e toxicológico aceitam JPEG. O limite de 4 MiB vale por envio e o limite de pixels se aplica às imagens. Na aprovação de motorista, envie identidadeConferida=true e os UUIDs das revisões obrigatórias. O painel permite baixar o PDF para leitura. Confira o nome na CNH, a foto frontal própria e a titularidade do comprovante de residência. O histórico registra as revisões conferidas. A verificação automatizada de veracidade e autenticidade junto aos emissores é uma evolução futura. O CRLV pertence ao veículo. A pessoa revisora deve verificar legibilidade, titularidade, correspondência de placa, EAR e conteúdo de cada comprovante antes de incluir sua versão. Descreva o motivo sem números de documento, resultados médicos ou outros dados pessoais. Exemplo de rejeição:

```json
{"situacao":"REJEITADO","motivo":"Imagem ilegível. Solicitar nova foto."}
```

A gravação de evidência após uma decisão reabre o cadastro. O servidor serializa decisões e alterações sobre o mesmo cadastro e rejeita versões documentais substituídas. Clientes antigos que enviam somente as três revisões anteriores recebem 422 na aprovação do motorista. A confirmação manual inclui verificar que a CNH é definitiva, sem campo separado para PPD. Cadastros anteriormente aprovados mantêm a situação. Uma campanha de reavaliação depende de decisão de produto. O histórico devolve revisoes_conferidas como array de UUIDs para novas aprovações de motorista e veículo e null quando não há registro. Dados insuficientes ou inelegíveis retornam 422; nova decisão sobre cadastro já decidido retorna 409.

## Limite municipal

O operador responsável deve conferir a regra municipal e registrar `ibge` e `idade_maxima` em `limites_municipais`. Registre 11 quando tiver verificado que o teto FAIRGO é aplicável. Valores entre 0 e 11 são aceitos. Município sem regra cadastrada bloqueia a aprovação. Na decisão, idade = ano atual em São Paulo menos ano de fabricação, incluindo o limite. Exigem-se quatro ou mais portas e ar-condicionado. Aprovar o veículo não aprova o motorista.

## Teste integrado

Use banco **exclusivo e descartável** com nome terminado em `_test`. A suíte grava cadastros fictícios e regras municipais sintéticas. Inicie o backend como acima apontando FAIRGO_DB_URL para esse banco. Em outro terminal configure PGHOST, PGPORT, PGDATABASE, PGUSER e, se necessário, PGPASSWORD para o mesmo banco. A conta de teste deve poder provisionar regras e credenciais. Defina PSQL com o caminho do executável se ele estiver fora do Path.

```powershell
$env:FAIRGO_TEST_URL = 'http://127.0.0.1:8080'
python backend/tests/test_validacao.py
```

## Operação e dependências externas

Documentos ficam em PostgreSQL como bytes cifrados com AES-256-GCM, nonce aleatório por gravação e vínculo autenticado ao cadastro e tipo. O servidor identifica, decodifica e recodifica JPEG, descartando metadados e o nome original. Para PDF, um processo separado renderiza até dez páginas e reconstrói um arquivo com imagens das páginas. A cópia visual descarta metadados originais, ações, anexos e links interativos. Assinaturas digitais do original não são preservadas. A entrada e a saída trafegam por pipes em memória. A conversão ocorre antes da transação que grava a revisão e reabre o cadastro. A coluna formato identifica JPEG e PDF nas novas gravações. Documentos JPEG anteriores e cópias PDF já normalizadas continuam legíveis. PDFs brutos da versão intermediária de desenvolvimento exigem reenvio. Cada conversão tem até 15 segundos, heap de 160 MiB, lado maior de até 1800 pixels e saída de até 4 MiB. Há duas conversões simultâneas por instância. Cópias acima de 4 MiB retornam HTTP 413. O processo filho compartilha as permissões de disco e rede do usuário do backend. Em produção, use container com usuário sem privilégios, escrita restrita e rede limitada aos serviços necessários. Antes de liberar o Driver, valide manualmente legibilidade e tempo de conversão no ambiente de destino com CNH digital e comprovante digitalizado autorizados, incluindo documento de dez páginas. Registre apenas tempos, tamanhos e resultado da leitura. Downloads administrativos usam no-store. Dados completos de CNH, CRLV e laudos permanecem dentro da imagem. Não registrar tokens, imagens ou corpos multipart nos logs.

Em produção use HTTPS, banco em rede privada, cofre de segredos e backup testado. Aplique, em ordem, `schema.sql`, `validacao.sql` e `driver.sql` com credencial de migração e defina SPRING_SQL_INIT_MODE=never na aplicação. Conceda à conta da API SELECT em credenciais_admin e limites_municipais, SELECT/INSERT em decisoes_cadastro, uso na sequência e apenas as permissões de leitura e gravação necessárias nas tabelas de cadastro. Restrinja provisionamento e alteração do histórico a funções operacionais separadas.

A conferência é manual. Autenticidade, suspensão de CNH, situação oficial de CRLV, emissão de antecedentes e confirmação do laudo dependem de consulta ao emissor ou de integração autorizada. Também ficam para operação posterior atualização oficial por município, antivírus externo, política de retenção e revalidação anual agendada. O status reflete a última decisão registrada.

## API do motorista

A API `/api/driver` oferece cadastro, login, consulta ao próprio perfil e logout. O pedido de recuperação tem resposta uniforme. A emissão e entrega do token aguardam a definição do canal verificado. O [contrato da Etapa 5.2](../docs/etapa-5-driver-admin.md) descreve rotas, corpos e códigos HTTP.

Na conta de execução do backend, acrescente às permissões do Admin:

| Tabela | Permissões para o Driver |
|---|---|
| usuarios | SELECT e INSERT para o perfil do motorista |
| contas_driver | SELECT, INSERT e UPDATE apenas em senha_hash para a redefinição de senha |
| sessoes_driver | SELECT, INSERT e DELETE para autenticação, expiração e logout |
| limites_driver | SELECT, INSERT, UPDATE em contador e DELETE para contabilizar e limpar tentativas |
| recuperacoes_driver | SELECT e DELETE para consumir o token |

Use uma credencial de migração separada para a estrutura. A emissão futura de tokens e a marcação de contatos verificados dependem de fluxos próprios.

Com o backend apontado para o banco descartável terminado em `_test`, execute `python backend/tests/test_driver.py` na raiz. A suíte limpa os contadores de tentativas ao iniciar. Use o banco exclusivamente para essa suíte durante a execução.

## Testes automáticos e painel

O workflow Testes do FairGo Admin executa Maven e a suíte HTTP em cada push e pull request, usando PostgreSQL descartável e uma chave de documentos gerada para a execução. A aba Actions mostra os resultados.

Com o backend iniciado, abra http://127.0.0.1:8080/admin/index.html. Informe o token individual do administrador, o tipo e o UUID do cadastro. A consulta mostra situação, documentos cadastrados e histórico. Limpar encerra a consulta e remove o token do formulário.

### Análise de pendências

Informe o token e clique em **Carregar pendentes**. A fila identifica motoristas pelo nome e veículos pela placa. Selecione o cadastro e abra os documentos para conferência. A imagem fica disponível dentro da página durante a análise. Para aprovar, marque os documentos conferidos. Para ambas as decisões, informe uma justificativa e confirme. A fila, a situação e o histórico são recarregados após a decisão. Cadastros já decididos continuam disponíveis em **Consultar por ID**.

O GitHub Actions também executa tests/test_painel.py em Chrome com cadastros e JPEG fictícios no banco exclusivo da suíte. O teste simula rejeição de motorista e aprovação de veículo.

## Veículos e documentos pelo Driver

O [contrato da Etapa 5.3](../docs/etapa-5-driver-admin.md) descreve as rotas autenticadas de veículos e documentos. Elas usam as mesmas tabelas, permissões de gravação, cofre e histórico do fluxo administrativo existente. Para testar a integração com o banco exclusivo, execute `python backend/tests/test_driver_documentos.py` na raiz, após iniciar o backend.
