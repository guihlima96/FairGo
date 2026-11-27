# Arquitetura do FairGo

## Decis�o e escopo

O FairGo evolui como um **mon�lito modular**. Admin, Driver e Passenger s�o frentes do produto. A base ap�s a PR #19 (`9efa32b`) tem um backend Java 17 com Spring Boot 3.5.7, um artefato Maven e um banco PostgreSQL. Os fluxos da aplica��o ficam em `br.fairgo`; o n�cleo documental ocupa o subpacote `br.fairgo.documentos`. Os limites descritos abaixo orientam mudan�as incrementais dentro dessa aplica��o.

| Frente | Implementa��o na base de refer�ncia | Evolu��o prevista |
|---|---|---|
| Admin | Painel HTML/JavaScript em `/admin/index.html` e API REST em `/api/admin/**`. Cadastra pessoas e ve�culos, consulta evid�ncias, registra decis�es e audita a confer�ncia visual de identidade. | Evolu��o das ferramentas de an�lise e revalida��o. |
| Driver | API REST em `/api/driver/**` para contas, sess�es, ve�culos, documentos, confirma��o de contatos e consulta de situa��o e identidade. | Entrega real de c�digos e tokens, ativa��o das pol�ticas aprovadas e evolu��o do cliente Flutter. |
| Passenger | O Admin pode cadastrar um usu�rio com perfil `PASSAGEIRO`. | Aplicativo Flutter e opera��es pr�prias do passageiro. |

Corridas, tarifas e cancelamentos t�m documenta��o de produto. O uso de WebSocket depende de um caso de uso definido. Consulte a [especifica��o](../FAIRGO_SPEC.md) e o [contrato REST Driver/Admin](etapa-5-driver-admin.md).

## Como o cadastro funciona hoje

O painel e os clientes Driver usam controladores HTTP distintos. Spring Security aplica as permiss�es de cada frente. A persist�ncia usa SQL expl�cito com `JdbcTemplate`. Configura��o e inicializa��o do banco ficam em [resources](../backend/src/main/resources/), nos scripts `schema.sql`, `validacao.sql`, `driver.sql` e `identidade.sql`.

| Responsabilidade | Componentes atuais | Depend�ncias importantes |
|---|---|---|
| Criar pessoa e conta | `CadastroAdmin`, `ContaDriver` | Ambos gravam em `usuarios`. O autocadastro cria tamb�m a conta Driver na mesma transa��o. O cadastro administrativo legado tem fluxo pr�prio. |
| Autenticar e autorizar | `Seguranca`, `SegurancaDriver`, `ContaDriver` | Admin e Driver usam pap�is, tokens e consultas distintos. Ambas as cadeias usam `TokenOpaco.hash`. |
| Cadastrar ve�culo e receber evid�ncias | `CadastroAdmin`, `CadastroVeiculoDriver`, `ValidacaoAdmin`, `CadastroCompartilhado` | As duas frentes usam o mesmo servi�o de escrita para ve�culo, dados t�cnicos e documentos. O servi�o usa o cofre e mant�m a reabertura e o hist�rico. |
| Preparar e proteger documentos | `Cofre`, `PdfSeguro` | O adaptador Cofre aplica a pol�tica FAIRGO e delega o processamento e a cifra ao pacote documentos. |
| Decidir e consultar | `ValidacaoAdmin`, `SituacaoDriver` | A decis�o administrativa verifica as revis�es atuais. A consulta Driver usa uma transa��o de leitura consistente e apresenta a situa��o do titular e de seus ve�culos. |

Na substitui��o documental, a aplica��o verifica o titular, prepara o arquivo e s� ent�o bloqueia o cadastro para reconferir a titularidade e gravar a revis�o. Quando aplic�vel, a mesma transa��o reabre o cadastro e registra o evento. O hist�rico referencia as revis�es conferidas. A consulta de pr�-elegibilidade considera motorista e ve�culo pr�prio aprovados e devolve `autorizadoParaCorridas=false`. Esses comportamentos s�o refer�ncias de compatibilidade em futuras extra��es. [C�digo do backend](../backend/src/main/java/br/fairgo/)

## Organiza��o para reaproveitamento

| Candidato | Responsabilidade aproveit�vel em outro produto | Pol�tica que pertence ao FairGo |
|---|---|---|
| Cadastro e contas | Identifica��o da conta, normaliza��o de contatos, sess�es e recupera��o de acesso. | Perfil de mobilidade, v�nculo com cadastros legados e condi��es de acesso Driver. |
| Verifica��o de contatos | Emiss�o interna e confirma��o de desafios por canal, com registro do resultado. | Contatos exigidos, ordem das etapas do motorista e crit�rios para verificar o nome. |
| Documentos | Prepara��o de arquivos, limites de processamento, cifragem e leitura autorizada. | CNH, EAR, CRLV, formatos por tipo e evid�ncias exigidas por cadastro. |
| An�lise administrativa | Registro de decis�o, respons�vel, momento, motivo e revis�es conferidas. | Crit�rios de aprova��o, v�nculo do ve�culo, limites municipais e pr�-elegibilidade. |

**Resultado do cadastro e placa.** Admin, Driver e o servi�o de cadastro usam o record `ResultadoCadastro` ao devolver `id` e `situacao`. O formul�rio de entrada do autocadastro continua em `ContaDriver.Cadastro`. A valida��o do formato de placa nas duas APIs usa `Placa.PADRAO`, e `CadastroCompartilhado.criarVeiculo` normaliza o valor antes da grava��o. Os indicadores de identidade e contatos s�o consultados separadamente da resposta de cria��o.

**Crit�rio para extra��o futura.** Manter o componente dentro do mon�lito enquanto seus consumidores forem as frentes do FairGo. Quando outro software precisar dele, demonstrar o uso com dados fict�cios, testes independentes e uma pol�tica do segundo produto. Documentar entradas, sa�das, erros e depend�ncias antes de decidir sobre distribui��o como biblioteca. Credenciais, documentos reais e configura��es de produ��o ficam fora do componente.

## Refer�ncias para as pr�ximas frentes

- **Identidade e contatos do Driver.** Hoje a API exp�e indicadores separados para nome, e-mail e celular. A confer�ncia usada na decis�o administrativa e a verifica��o de um canal s�o opera��es distintas. A emiss�o e entrega de recupera��o dependem de um canal verificado. As decis�es sobre m�todos e provedores est�o em [DA-02 e DA-03](pendencias/driver-admin.md).
- **Contratos e testes.** Ao alterar comportamento compartilhado, identificar os consumidores Admin e Driver, preservar titularidade, revis�es e hist�rico e conferir o fluxo pelas respectivas APIs. Os [testes de integra��o e painel](../backend/tests/) e o [workflow](../.github/workflows/admin.yml) registram as verifica��es existentes.
- **Opera��o e dados.** Isolamento do conversor, reten��o, rota��o da chave, restaura��o e evolu��o do esquema s�o acompanhados em [opera��o e seguran�a](pendencias/operacao-seguranca.md). Ambientes, observabilidade, uso de WebSocket e sele��o de ve�culo para corridas est�o em [produto e arquitetura](pendencias/produto-arquitetura.md).

### Confer�ncia visual

O registro de autoria (`AutoriaRegistro`) representa respons�vel e instante, sem depend�ncia de documentos, pap�is HTTP ou regras de mobilidade. `ConferenciaIdentidade` aplica a pol�tica espec�fica do motorista e serializa a grava��o com o mesmo bloqueio do cadastro usado na substitui��o documental. Os controladores de consulta compartilham a proje��o SQL `identidade_motorista_atual`, que compara a �ltima confer�ncia com as vers�es atuais. A API administrativa recebe o parecer e fornece a auditoria. A API Driver apresenta apenas o indicador do titular.

### Entrega de SMS e e-mail

Para DA-02 e DA-03, a integra��o futura ter� um contrato interno de entrega por canal. O fluxo de verifica��o ou recupera��o passar� uma refer�ncia �nica da mensagem, a finalidade, o destino definido pelo backend e o conte�do a entregar. O adaptador do fornecedor traduzir� essa solicita��o para a API externa e devolver� um resultado comum: aceito, recusado ou indeterminado. A sele��o do adaptador ficar� na configura��o de infraestrutura. Assim, trocar o fornecedor de um canal preserva o cadastro, as regras de verifica��o e recupera��o e os contratos HTTP do Driver.

| Responsabilidade | Onde ficar� |
|---|---|
| Determinar o destino eleg�vel, emitir e consumir o c�digo ou token, aplicar expira��o e limites | Servi�o de verifica��o ou recupera��o |
| Traduzir a mensagem, autenticar a chamada externa e interpretar a resposta do fornecedor | Adaptador de SMS ou e-mail |
| Decidir se uma falha admite nova tentativa usando a mesma refer�ncia e evitando envio duplicado | Coordena��o da entrega, fora das regras de cadastro |

A aceita��o pelo fornecedor indica apenas que ele recebeu a solicita��o. A confirma��o do contato continua exigindo a apresenta��o do c�digo v�lido. A recupera��o segue condicionada ao canal previamente verificado. Um resultado indeterminado exige concilia��o antes de repetir o envio ou recorrer a outro fornecedor.

Enquanto a integra��o estiver pendente, a solicita��o de verifica��o segue respondendo 503 e o pedido de recupera��o mant�m o 202 uniforme sem envio. Na futura implementa��o dos adaptadores, os testes de contrato dever�o cobrir aceita��o, recusa e resposta indeterminada com um adaptador em mem�ria restrito � su�te, destinos fict�cios e nenhuma chamada de rede. C�digos, tokens e destinos devem ficar fora de logs. [Candidatos e etapa de contrata��o](pendencias/driver-admin.md#provedores-candidatos-para-da-02-e-da-03)

### Recupera��o de acesso

`TokenOpaco` concentra gera��o aleat�ria e resumo SHA-256 reutilizados em sess�o e recupera��o, com prefixos distintos. `RecuperacaoDriver` aplica a pol�tica de conta e e-mail verificado e coordena a troca de senha e a revoga��o das sess�es na mesma transa��o. O login e a redefini��o compartilham o codificador de senhas. O emissor devolve ao chamador interno uma refer�ncia de mensagem, o destino obtido do banco e o token em mem�ria. A finalidade � recupera��o e o canal preparado � e-mail. Esses dados ser�o a entrada da coordena��o de entrega prevista acima. Nenhuma rota serializa esse objeto. O futuro coordenador precisar� tratar falhas ap�s a emiss�o e evitar que uma repeti��o envie um token j� substitu�do.

### Controle de tentativas

`ControleTentativas` implementa reservas de janela fixa, limpeza limitada e devolu��o associada ao vencimento original em PostgreSQL. N�o conhece perfis, sess�es ou transporte HTTP. `TentativasDriver` comp�e as cotas por opera��o, conta, origem e sistema para a API Driver. O controlador identifica a conex�o e o titular e confirma a devolu��o das reservas pessoais na transa��o de abertura da sess�o. A deriva��o da senha ocorre fora da transa��o de admiss�o. O or�amento global � compartilhado pelas inst�ncias e sua linha pode sofrer conten��o; medir em carga antes da exposi��o p�blica.

### ARQ-802 - processamento e prote��o documental

Antes da extra��o, Cofre recebia MultipartFile, interpretava tipos de mobilidade, recodificava JPEG e executava AES-GCM. PdfSeguro dependia do cofre para reconhecer a sa�da e lan�ava exce��es HTTP. Agora o pacote `br.fairgo.documentos` opera sobre bytes. Admin e Driver continuam usando o mesmo servi�o de grava��o.

| Componente | Entrada e sa�da | Depend�ncias, limites e erros |
|---|---|---|
| FormatoDocumento | Bytes  indica��o de assinatura PDF | JDK. O prefixo identifica um candidato, cuja validade � conferida pelo conversor. As demais entradas passam pelo decodificador JPEG. |
| JpegSeguro | Bytes JPEG  nova imagem JPEG | ImageIO em mem�ria. Entrada de at� 4 MiB e at� 8 milh�es de pixels. Formato inv�lido ou excesso gera IOException. |
| PdfSeguro | Bytes PDF  PDF reconstru�do com imagens | JDK e PDFBox. Processo separado via stdin/stdout. At� duas convers�es por JVM, 15 s, heap de 160 MiB e mem�ria direta de 16 MiB. Entrada e sa�da de at� 4 MiB, 1-10 p�ginas, lado da caixa de at� 14.400 pontos e renderiza��o limitada a 1.800 pixels no maior lado. PDF cifrado ou inv�lido gera IOException. Ocupado e SaidaExcedida distinguem satura��o e c�pia visual grande. Em classes soltas executa o main diretamente e no JAR Spring Boot usa PropertiesLauncher. |
| CifraDocumental | Bytes e contexto autenticado  bytes cifrados, ou opera��o inversa | JCA, chave de 32 bytes, AES-GCM com nonce aleat�rio de 12 bytes no in�cio do registro e tag de 128 bits. Falha de autentica��o gera GeneralSecurityException. O chamador fornece o contexto exato nas duas opera��es. |
| Cofre (adaptador FAIRGO) | Upload, titular e tipo  formato e conte�do protegido | Aplica a lista de tipos permitidos para PDF e converte os erros de documento, sa�da excessiva e satura��o em HTTP 400, 413 e 503. Mant�m AAD ASCII titular:tipo:formato e, para registros com formato nulo, titular:tipo. A leitura legada remove o marcador 1 quando presente, solicita reenvio do PDF bruto de desenvolvimento (422) e mant�m os demais bytes. |

O teste de certificado de curso usa o n�cleo sem cadastro, Spring ou banco. A regress�o de compatibilidade l� um JPEG sint�tico produzido pelo Cofre do commit de01322 com uma chave exclusiva de teste. Grava��o, titularidade, revis�es e reabertura continuam no fluxo transacional descrito acima.

O processo do conversor herda as permiss�es do usu�rio do backend. Cotas por conta, isolamento no ambiente de destino, legibilidade, reten��o, rota��o de chave e restaura��o continuam no [registro operacional](pendencias/operacao-seguranca.md). A autenticidade do documento e o tratamento de assinaturas digitais seguem em [DA-05](pendencias/driver-admin.md).

## Cat�logo e diagn�stico ARQ-803

Base analisada em 25/11/2026: `9efa32b95847de0cf0ee860ba2c7c7df3cca9dcc`. A [PR #19](https://github.com/guihlima96/FairGo/pull/19) integrou a ARQ-802. O n�cleo documental est� implementado e tem teste independente em contexto educacional. A ARQ-803 est� em planejamento. O cat�logo abaixo indica o grau de reutiliza��o dispon�vel hoje dentro do mon�lito.

| Componente | Reuso atual ou potencial | Depend�ncia que delimita o uso |
|---|---|---|
| `br.fairgo.documentos` | `FormatoDocumento`, `JpegSeguro`, `PdfSeguro` e `CifraDocumental` j� processam bytes ou operam com contexto autenticado fornecido pelo chamador. | JDK, ImageIO, JCA, PDFBox e subprocesso para PDF; o JAR usa o lan�ador do Spring Boot. Tipos aceitos, AAD hist�rico e tradu��o HTTP pertencem a `Cofre`. |
| `TokenOpaco` | Gera��o aleat�ria com prefixo e digest SHA-256 j� compartilhados por autentica��o e recupera��o. | Apenas JDK. Prefixos, prazo, v�nculo e finalidade pertencem aos consumidores. |
| `ResultadoCadastro`, `AutoriaRegistro`, `Placa` | Resultado de cria��o e autoria s�o dados simples; a valida��o de placa � compartilhada entre Admin e Driver. | Placa pertence a ve�culos. A decis�o de identidade vincula nome, CNH e foto ao cadastro do motorista. |
| `br.fairgo.seguranca.ProvaCodigo` | Gera c�digos de oito d�gitos com SecureRandom, calcula HMAC-SHA256 e confere a prova com MessageDigest.isEqual. Pode ser usado, por exemplo, na retirada de reservas de uma biblioteca. | Apenas JDK. Recebe uma chave de 32 bytes. O chamador fornece titular, canal, desafio, destino e c�digo. |
| `VerificacaoContato` | Usa o componente de prova nos dois canais do Driver. | L� FAIRGO_CONTACT_KEY, controla disponibilidade, expira��o, reenvio e tentativas, bloqueia contas, atualiza os indicadores e persiste desafios na mesma transa��o JDBC. |
| `ControleTentativas` | Reserva e devolu��o por chave e janela j� est�o separadas dos limites decididos por `TentativasDriver` e `PoliticaLogin`. | Spring JDBC e PostgreSQL: tabela `limites_driver`, rel�gio do banco, upsert, `RETURNING` e limpeza em lotes com `SKIP LOCKED`. |
| `RecuperacaoDriver` | O padr�o reutiliz�vel � emitir um segredo de uso �nico vinculado ao titular e destino atual, com expira��o. | Usa `PasswordEncoder`, transa��o, perfil MOTORISTA, e-mail verificado, `recuperacoes_driver` e revoga��o das sess�es do titular. |
| `ContaDriver`, `SegurancaDriver`, `Seguranca` | H� opera��es de sess�o e trechos de integra��o Spring Security candidatos a organiza��o interna. | Conta e sess�o Driver, credencial administrativa e decis�o de acesso t�m formatos, titulares, permiss�es e persist�ncia pr�prios. |
| `ObservacaoLogin`, `MetricasLogin` | Contagens e medidas agregadas podem orientar outros fluxos se surgir demanda. | Filtro Servlet espec�fico de POST de sess�o Driver; preservar categorias agregadas e aus�ncia de identificadores nos registros. |

### Fluxos, duplica��es e limites transacionais

- **Cadastro.** Admin e Driver repetem a normaliza��o de nome e e-mail e a inser��o em `usuarios`. O Admin escolhe o perfil. O autocadastro Driver valida CPF e celular brasileiro, gera o hash da senha e grava `usuarios` e `contas_driver` atomicamente. A normaliza��o de e-mail � um candidato pequeno. Compartilhar a cria��o integral poderia deixar usu�rio sem conta ou atribuir perfil incorreto.
- **Autentica��o.** `ContaDriver.entrar` re�ne HTTP, interpreta��o do identificador, busca por e-mail/UUID, verifica��o da senha, reservas e emiss�o de sess�o. Uma conta conhecida usa a mesma chave de cota nas duas formas de login. A senha � conferida antes da transa��o e seu hash � reconferido sob bloqueio antes de emitir a sess�o. Esse limite protege a troca de senha concorrente.
- **Sess�es e autoriza��o.** Os filtros repetem leitura do Bearer, consulta do digest e cria��o do principal Spring. No Admin, o token hexadecimal identifica uma credencial administrativa com expira��o e revoga��o. No Driver, o prefixo `drv_` e a tabela `sessoes_driver` identificam uma sess�o de oito horas. Logout elimina apenas a sess�o do titular; redefini��o elimina todas as sess�es dele. Manter cadeias e pol�ticas separadas. Uma fun��o de parsing s� se justifica diante de manuten��o duplicada real.
- **Contato.** `ContatoDriver` usa o titular autenticado. A emiss�o interna vincula HMAC ao titular, canal, desafio, destino e c�digo. PostgreSQL controla expira��o, reenvio, or�amento e consumo. A confirma��o decrementa a tentativa e, no sucesso, atualiza o contato e elimina o desafio na mesma transa��o. A solicita��o p�blica continua em 503 enquanto a entrega estiver desabilitada.
- **Recupera��o.** O pedido p�blico responde 202 ap�s o controle por origem. A emiss�o interna s� produz um envelope para e-mail verificado. A confirma��o exige o v�nculo com o destino atual e consome o token na transa��o que troca a senha e revoga sess�es. A resposta p�blica e a disponibilidade do provedor s�o contratos distintos. Os mecanismos de recupera��o e de contato t�m segredos, provas e efeitos diferentes.
- **Tentativas.** O or�amento por origem come�a antes da verifica��o da conta. As reservas de rajada/par, conta e sistema s�o obtidas em conjunto; o sucesso devolve apenas as pr�prias reservas de rajada, par e conta na janela original. Uma falha ou um sucesso concorrente n�o deve limpar as demais tentativas. A pol�tica de origem, os limiares e as m�tricas continuam sob responsabilidade do Driver.

### Escopo proposto e crit�rios de aprova��o

| Entrega independente | Benef�cio e risco | Crit�rio de aceite |
|---|---|---|
| **A - prioridade alta. Isolar a gera��o e a prova do desafio de contato.** Extrair apenas c�digo Java puro para gerar o c�digo e calcular/conferir a prova. Manter o canal, destino atual, rel�gio SQL, or�amentos e decis�o de atualizar flags no fluxo existente. | Reuso concreto entre canais e teste independente do banco. Alterar ordem dos campos, separador, capitaliza��o do canal ou codifica��o invalidaria desafios em andamento. | Vetor fixo compat�vel com a vers�o anterior, inclusive c�digo com zero inicial. Variar titular, canal, desafio, destino e c�digo individualmente. Usar compara��o de prova em tempo constante. Preservar chave ausente, 503/400/204 e consumo �nico sob concorr�ncia. |
| **B - prioridade m�dia. Compartilhar normaliza��o de e-mail no cadastro.** Substituir apenas o comportamento id�ntico de Admin e Driver, mantendo valida��o HTTP e grava��es pr�prias. | Evita diverg�ncia no valor persistido. Normalizar antes da valida��o pode alterar as entradas aceitas. | Mesmas respostas para entradas v�lidas, inv�lidas e duplicadas antes/depois, mesmo valor gravado, conflito 409 e rollback integral do autocadastro. Comparar os dois caminhos com a mesma entrada. |
| **C - prioridade m�dia. Separar o caso de uso de login do controlador Driver.** Mover a orquestra��o como uma unidade; manter convers�o HTTP na borda e consultas JDBC internas. | Reduz concentra��o de responsabilidades no controlador. Mover a fronteira transacional ou as devolu��es pode emitir sess�o ap�s redefini��o ou apagar falhas concorrentes. | Preservar 200/401/429, token e expira��o, hash fict�cio, cota compartilhada entre e-mail/UUID, ordena��o de reservas e teste concorrente entre login e troca de senha. |

A ARQ-803.1 implementa a proposta **A**. O formato persistido usa os cinco campos na ordem titular, canal, desafio, destino e c�digo, unidos por LF e codificados em UTF-8. UUIDs usam a representa��o textual e o canal usa o nome exato do enum. O c�digo � texto de largura oito. Preservar esses bytes e a chave permite confirmar desafios anteriores. Para novos consumidores, usar campos can�nicos sem LF. A aplica��o mant�m os segredos fora das respostas e dos logs. **B** e **C** continuam pendentes. Cada entrega pode partir da main vigente por si s�. Recupera��o, filtros de pap�is, armazenamento de cotas e integra��o com provedores permanecem fora do primeiro recorte. N�o h� necessidade demonstrada de biblioteca externa, novo esquema ou servi�o geral de identidade.

### Contratos e persist�ncia de refer�ncia

As rotas abaixo usam o contrato vigente. Os c�digos indicam os comportamentos centrais de cada fluxo. O contrato completo e os casos de autoriza��o est�o na [Etapa 5](etapa-5-driver-admin.md).

| Opera��o | Contrato observ�vel | Tabelas, proje��es e regress�es existentes |
|---|---|---|
| POST `/api/admin/usuarios` e `/api/driver/contas` | 201 com `id` e `situacao`; duplicidade retorna 409. O perfil e os campos de entrada dependem da frente. | `usuarios`, `contas_driver`, cotas em `limites_driver`. Cobertura em `test_validacao.py`, `test_driver.py` e `test_driver_documentos.py`. |
| POST `/api/driver/sessoes`, GET `/api/driver/me` e DELETE `/api/driver/sessoes/atual` | Login por e-mail ou UUID retorna 200 com token e expira��o; credenciais inv�lidas retornam 401 e cota excedida, 429. Consulta restrita ao titular e logout 204. | `usuarios`, `contas_driver`, `sessoes_driver`, `limites_driver` e vis�o `identidade_motorista_atual`. A autentica��o do Admin usa `credenciais_admin`. Ver `test_driver.py`, `MetricasLoginTest` e testes administrativos de autoriza��o. |
| POST em `/api/driver/me/verificacoes/{canal}/solicitacoes` e `/confirmacao` sob o mesmo prefixo | A solicita��o autenticada responde 503 enquanto a entrega est� desabilitada. Na confirma��o, desafio e c�digo v�lidos retornam 204; tentativa inv�lida retorna 400. Preservar tamb�m o comportamento sem chave configurada. | `usuarios`, `contas_driver`, `desafios_contato_driver`. Ver `VerificacaoContatoTest` e `test_driver.py`. |
| POST `/api/driver/recuperacao` e `/api/driver/recuperacao/confirmacao` | Pedido v�lido dentro da cota recebe 202 uniforme sem envio. Confirma��o v�lida recebe 204; token inv�lido recebe 400. A resposta p�blica nunca cont�m o segredo. | `usuarios`, `contas_driver`, `recuperacoes_driver`, `sessoes_driver` e `limites_driver`. Ver `RecuperacaoDriverTest` e `test_driver.py`. |

Para documentos e identidade, preservar os v�nculos em `documentos_cadastro`, `decisoes_cadastro` e `conferencias_identidade`, a vis�o usada por Admin e Driver e a compatibilidade de leitura. As refer�ncias s�o `CofreCompatibilidadeTest`, `DocumentosTest`, `test_driver_documentos.py`, `test_validacao.py` e `test_painel.py`.

**Decis�es pendentes.** A escolha e a contrata��o de provedores, a ativa��o de envio, a recupera��o por perda de contato e o efeito das verifica��es na aprova��o dependem de decis�o de produto (DA-02 e DA-03). Os limites vigentes s�o a refer�ncia de compatibilidade; eventuais calibra��es pertencem a OS-06. Os recortes B e C seguem sujeitos a autoriza��o individual. A prepara��o documental ARQ-803.0 n�o aprova essas mudan�as.

### Regress�o e pend�ncias

Usar PostgreSQL descart�vel nos testes JUnit de contato e recupera��o. Preservar as su�tes HTTP para conta legada, login por dois identificadores, redes compartilhadas, concorr�ncia, expira��o, logout, isolamento de pap�is, reenvio e mudan�a de destino. Incluir as respostas de recupera��o 202/204/400, rejei��o do segundo consumo, revoga��o das sess�es do titular e sobreviv�ncia das sess�es de outra conta. Na CI, conferir Maven verify, as tr�s rodadas Driver, Admin, Selenium e integra��o Driver/Admin. Esses s�o crit�rios para mudan�as futuras; o diagn�stico � est�tico.

Continuam abertas **OS-01** (cotas, abuso e isolamento operacional do conversor), **OS-02** (legibilidade e tempo com amostras autorizadas), **OS-03** (reten��o, acesso e backups), **OS-04** (rota��o, recifragem e restaura��o), **OS-05** (avalia��o de antiv�rus) e **DA-05** (autenticidade e assinaturas digitais). O subprocesso herda as permiss�es do backend. Para autentica��o, **OS-06** acompanha proxy, redes compartilhadas, calibra��o e conten��o da cota global. [Opera��o e seguran�a](pendencias/operacao-seguranca.md) � [Driver e Admin](pendencias/driver-admin.md)

### Aplicativo FairGo Driver

O cliente Android reside em `apps/driver`. A entrada apresenta a marca e as rotas Entrar e Criar conta. Em `lib/core` ficam o tema, as rotas e a configura��o do prefixo REST `/api/driver/`. As telas ficam em `features` e o s�mbolo visual em `shared`. A navega��o funciona offline. A �rea Entrar usa DriverApi (dart:io) para criar sess�o, consultar /me e revogar a sess�o. Um canal Flutter guarda a credencial cifrada com AES-GCM, chave do Android Keystore e arquivo privado fora do backup. A origem da API acompanha o token para impedir seu uso em outro ambiente. A identifica��o do titular vem exclusivamente do servidor. O backend compartilhado continua respons�vel pelas regras e pela autoriza��o. [Execu��o e teste por USB](../apps/driver/README.md)
