# Etapa 5 - FairGo Driver e FairGo Admin

**Proposta para revis�o.** A primeira entrega � documental. As pr�ximas entregas come�am pelo backend compartilhado. A interface Flutter fica para uma etapa posterior. As regras vigentes est�o na [especifica��o mestre](../FAIRGO_SPEC.md) e em [cadastro e seguran�a](cadastro-e-seguranca.md).

## Sequ�ncia proposta de implementa��o

| Entrega | Comportamento proposto | Crit�rio de aceite futuro |
|---|---|---|
| 1. Cadastro pr�prio | Coletar nome completo, celular ativo, e-mail e CPF, com verifica��o do nome e dos contatos. Criar conta e perfil MOTORISTA de forma at�mica. O servidor atribui o perfil. | Reenvios e pedidos simult�neos preservam a unicidade. Um cadastro administrativo anterior exige comprova��o de titularidade antes de qualquer v�nculo. |
| 2. Acesso e recupera��o | Autenticar na �rea Driver e associar a sess�o ao motorista. | A sess�o permite completar o cadastro pendente. Uma credencial Driver n�o acessa a fila, imagens administrativas nem decis�es. |
| 3. Ve�culo | Receber placa e dados t�cnicos e vincular o ve�culo ao motorista autenticado. | O servidor deriva o propriet�rio da sess�o e verifica esse v�nculo a cada leitura ou altera��o. |
| 4. Documentos | Receber foto de perfil, comprovante de resid�ncia, CNH definitiva com EAR, antecedentes obrigat�rios e toxicol�gico facultativo no cadastro do motorista e CRLV no ve�culo. | Validar titularidade antes da grava��o. Usar as mesmas valida��es, cofre e revis�es do fluxo administrativo. |
| 5. An�lise | Disponibilizar os cadastros na fila existente da Etapa 4. | O administrador abre a revis�o, informa motivo, confirma e decide pelo painel atual. |
| 6. Situa��o | Consultar o estado do pr�prio motorista e de cada ve�culo vinculado. | Exibir PENDENTE, APROVADO ou REJEITADO por cadastro. Uma decis�o sobre o ve�culo n�o altera a do motorista. |

## Conta, autentica��o e recupera��o

Como op��o para avalia��o, usar e-mail verificado e senha armazenada com hash adaptativo. Manter credenciais e sess�es do motorista separadas dos tokens individuais do Admin. A API Driver deve identificar o titular no servidor, inclusive quando o cliente informar um UUID. Uma conta autenticada pode acompanhar a an�lise sem que o login conceda aprova��o cadastral.

Para recupera��o, prop�e-se enviar ao contato previamente verificado um token aleat�rio, de uso �nico e com prazo. Guardar somente seu resumo criptogr�fico, responder de modo uniforme para contas existentes e inexistentes e limitar solicita��es e tentativas. Ao definir a nova senha, consumir o token atomicamente e revogar as sess�es anteriores. Testar expira��o, reutiliza��o e duas confirma��es simult�neas. Excluir tokens, senhas e conte�do documental dos logs.

**Decis�es para revis�o antes de implementar:** identificador de login, canal e provedor de verifica��o e recupera��o, pol�tica de senha, tipo e dura��o de sess�o, prazos e limites de tentativas, procedimento para perda do contato e comprova��o para vincular cadastros legados. Confirmar tamb�m como apresentar a justificativa ao motorista e como registrar a verifica��o de identidade. A exist�ncia de uma conta com o mesmo e-mail n�o comprova titularidade.

## Integra��o com o fluxo da Etapa 4

A proposta � oferecer opera��es pr�prias ao Driver e reutilizar a grava��o transacional do backend. O aplicativo n�o deve receber um token administrativo. Motorista e ve�culo conservam seus UUIDs e estados no mesmo banco. A fila atual lista pend�ncias e a decis�o continua exclusiva do Admin. N�o introduzir uma segunda fila ou um segundo hist�rico.

- **Envio.** Preservar o contrato atual de imagem JPEG de at� 4 MiB e 8 milh�es de pixels, a recodifica��o e a cifragem. Para CNH, registrar EAR e validade. Para o ve�culo, manter portas, ar-condicionado, ano de fabrica��o e munic�pio. Adequar a an�lise do Driver � regra aprovada para a Etapa 5. Desde a PR #7, o Admin permite aprovar sem toxicol�gico. Na integra��o, testar aprova��o sem esse exame e bloqueio da aprova��o enquanto faltar a verifica��o de antecedentes.
- **Amplia��o prevista.** Planejar o recebimento da foto de perfil e do comprovante de resid�ncia, com confer�ncia pelo Admin e registro da revis�o. Incluir nos testes PPD, foto reaproveitada da CNH e comprovante em nome de terceiro. O Admin aceita JPEG e PDF para CNH, CRLV, comprovante de resid�ncia e antecedentes.
- **Revis�o.** Vincular cada vers�o ao alvo e tipo corretos. A confer�ncia administrativa usa as vers�es atuais. Preservar o bloqueio de aprova��o incompleta, a rejei��o de revis�o substitu�da e o registro da justificativa, data e identidade administrativa.
- **Corre��o.** Nova evid�ncia ap�s decis�o reabre o cadastro afetado conforme a Etapa 4. Em envio simult�neo a uma decis�o, preservar a serializa��o atual. A consulta Driver l� a situa��o resultante. O conte�do do hist�rico interno e os dados de outros titulares ficam restritos.
- **Evolu��o de dados.** Se a conta exigir estruturas adicionais, propor mudan�as aditivas em JDBC com plano de compatibilidade para cadastros existentes. Submeter o esquema a revis�o antes de executar altera��es.

## Valida��o para as futuras entregas

Manter o workflow e as su�tes atuais do Admin como regress�o. Acrescentar cen�rios Driver com banco exclusivo e identidades fict�cias. Percorrer cadastro, autentica��o, v�nculo do ve�culo e envio; verificar a presen�a no painel, rejeitar um documento ileg�vel, corrigir, conferir a nova revis�o, aprovar e consultar os dois estados pelo Driver. Conferir o hist�rico ap�s cada decis�o.

Testar tamb�m acesso cruzado entre dois motoristas, tentativa de autoaprova��o, cadastro duplicado e recupera��o de acesso. Confirmar que cadastros criados antes da Etapa 5 continuam consult�veis no painel. Contratos de API, esquema de credenciais e pol�tica de reten��o permanecem sujeitos a revis�o.

## Captura da foto pelo motorista

Na etapa da foto, abrir a c�mera e permitir tirar, revisar e refazer a foto antes do envio em JPEG. Se o acesso � c�mera estiver indispon�vel, informar como tentar novamente. N�o oferecer galeria nem seletor de arquivos nesse fluxo. Nos testes do aplicativo, conferir a captura, a nova tentativa e o caso de permiss�o negada. O endpoint administrativo atual recebe JPEG e a confer�ncia visual pelo Admin n�o comprova a origem da imagem.

## Autenticidade do documento digital

A implementa��o atual guarda a c�pia visual cifrada. A assinatura do arquivo recebido s� poder� ser validada durante o envio, antes da convers�o. Para uma revis�o posterior, ser� necess�rio consultar o emissor com um QR code ou outro identificador leg�vel, quando dispon�vel, ou solicitar o original novamente. A escolha do provedor e a pol�tica de valida��o autom�tica seguem pendentes.

## Etapa 5.2 - Contrato proposto para revis�o

Prefixo `/api/driver`. Requisi��es e respostas com corpo usam JSON. Clientes devem usar HTTPS em produ��o. Enviar o token apenas no cabe�alho `Authorization: Bearer <token>`. O n�mero de cadastro desta proposta � o UUID entregue no cadastro.

| M�todo e rota | Entrada | Resultado |
|---|---|---|
| POST /contas | nome, cpf, celular, email, senha | 201 com id e situacao=PENDENTE. Dados inv�lidos retornam 400. CPF ou e-mail duplicado retorna 409 sem identificar o campo. |
| POST /sessoes | identificador (e-mail ou UUID) e senha | 200 com token e expiraEm. Falha de credencial retorna 401 e limite de tentativas retorna 429. |
| GET /me | Bearer Driver | 200 com id, nome, email, situacao, cpf, celular e os booleanos nomeVerificado, emailVerificado e celularVerificado. Sess�o inv�lida retorna 401. |
| DELETE /sessoes/atual | Bearer Driver | 204. Revoga a sess�o apresentada. |
| POST /recuperacao | email | 202 vazio para qualquer endere�o sintaticamente v�lido. Nesta entrega nenhum token � emitido e nenhuma mensagem � enviada. |
| POST /recuperacao/confirmacao | token e novaSenha | 204 ao consumir uma credencial v�lida associada a e-mail verificado. Token inv�lido, expirado ou consumido retorna 400. A troca revoga todas as sess�es. |

A cria��o do usu�rio e da conta ocorre em uma transa��o. O servidor define o perfil MOTORISTA. O CPF � validado pelos d�gitos verificadores e guardado sem pontua��o. O celular brasileiro com DDD � normalizado para +55. O e-mail � convertido para min�sculas e nunca vincula automaticamente um cadastro anterior. Nome, e-mail e celular iniciam sem verifica��o. O login concede acesso apenas � �rea de cadastro.

**Par�metros t�cnicos para revis�o.** Senhas de 15 a 128 caracteres, PBKDF2-HMAC-SHA256 com salt aleat�rio e 600 mil itera��es. Sess�es opacas com 256 bits aleat�rios, digest no servidor e validade de oito horas. O banco compartilha os limites entre inst�ncias. A pol�tica de admiss�o de login e o tratamento dos contadores est�o na se��o de refor�o da autentica��o abaixo. O endere�o � o do par da conex�o HTTP. Atr�s de proxy, validar o encaminhamento confi�vel e os limites antes da exposi��o p�blica.

**Banco e opera��o.** Aplicar `backend/src/main/resources/driver.sql` ap�s os scripts existentes com a credencial de migra��o em produ��o. As tabelas s�o aditivas. Restringir as permiss�es de execu��o aos dados necess�rios, proteger banco e backups e evitar registrar corpos de requisi��es e cabe�alhos de autoriza��o. Cadastros legados exigem comprova��o de titularidade em fluxo futuro.

**Recupera��o pendente de integra��o.** O emissor interno confirma o contato verificado e gera um segredo aleat�rio de uso �nico com prefixo rec_, persistindo apenas resumos e validade. A entrega pelo canal aprovado continua pendente. A confirma��o e a revoga��o est�o implementadas. Ainda dependem de decis�o o m�todo de verifica��o, o provedor, o prazo do token e o atendimento a quem perdeu o contato. Os testes provisionam credenciais exclusivamente no banco descart�vel.

## Etapa 5.3 - Ve�culos e documentos

Todas as rotas abaixo exigem Bearer Driver. O servidor deriva o motorista da sess�o. Ve�culo inexistente ou de outro titular retorna 404.

| M�todo e rota em /api/driver | Contrato |
|---|---|
| POST /veiculos | JSON com placa. Retorna 201 com id e situacao. Placa duplicada retorna 409. |
| GET /veiculos | Lista os ve�culos do titular. |
| GET /veiculos/{id} | Retorna id, placa, situacao, dados t�cnicos e revisaoCrlv. Dados e revis�o podem ser nulos. |
| PUT /veiculos/{id}/dados | Portas, ar-condicionado, ano de fabrica��o e munic�pio nos campos portas, arCondicionado, anoFabricacao e municipio. Retorna 204. |
| PUT /veiculos/{id}/crlv | Multipart com arquivo JPEG/PDF e validade opcional. |
| PUT /me/documentos/{tipo} | Multipart com arquivo. CNH requer ear e validade. ANTECEDENTES e RESIDENCIA aceitam JPEG/PDF, assim como CNH. FOTO_PERFIL e TOXICOLOGICO aceitam JPEG. O toxicol�gico � facultativo. |

Os envios retornam a revis�o (id, tipo, validade e ear). A fila e as decis�es continuam no Admin. Substituir uma evid�ncia depois de decis�o reabre apenas o cadastro afetado e registra o respons�vel como driver:<UUID>. A convers�o ocorre fora da transa��o e a grava��o revalida titularidade e estado sob bloqueio.

**Pend�ncias antes da exposi��o p�blica.** O backend recebe o JPEG da foto e n�o atesta captura pela c�mera. O aplicativo dever� realizar essa captura. Seguem pendentes verifica��o dos contatos, valida��o de autenticidade com emissor, pol�tica de reten��o e reavalia��o de aprova��es antigas. Revisar cotas de upload, concorr�ncia e isolamento do conversor no ambiente de produ��o. A sanitiza��o descarta assinaturas digitais e os testes fict�cios n�o substituem os ensaios de legibilidade e lat�ncia com documentos reais autorizados.

## Etapa 5.4 - Consulta consolidada

`GET /api/driver/me/situacao` usa exclusivamente o titular da sess�o Driver. Responde 200 com uma leitura consistente do cadastro e 401 para sess�o ausente, inv�lida ou credencial do Admin.

| Campo | Interpreta��o |
|---|---|
| motorista | ID pr�prio, an�lise atual, indicadores reais de verifica��o de nome, e-mail e celular e lista de documentos pessoais. Um indicador falso significa verifica��o pendente. |
| motorista.documentos | Cada item cont�m tipo, obrigatorio, revisaoAtual, validade e ear. Revis�o nula indica aus�ncia. Um obrigat�rio ausente precisa ser enviado. O toxicol�gico � facultativo. |
| veiculos | Uma entrada por ve�culo do titular com id, placa, an�lise, dadosTecnicosInformados e metadados do CRLV. Lista vazia indica aus�ncia de ve�culo. |
| analise | Situa��o administrativa PENDENTE, APROVADO ou REJEITADO. Apenas REJEITADO recebe orienta��o p�blica vigente. A reabertura remove essa orienta��o. |
| preElegibilidade | motoristaEVeiculoAprovados indica motorista aprovado com ao menos um ve�culo pr�prio aprovado. Conta decis�es administrativas vigentes, inclusive legadas. autorizadoParaCorridas permanece falso nesta etapa. |

A consulta n�o reavalia decis�es nem atesta autenticidade, validade atual ou identidade. O cliente pode indicar o documento ausente pela combina��o de obrigatorio e revisaoAtual. A aprova��o administrativa n�o altera os indicadores de verifica��o.

**Comunica��o da rejei��o (PR #22).** O Admin consulta `GET /api/admin/validacao/motivos-publicos/{alvo}`. Cada op��o cont�m c�digo, mensagem padronizada e tipos de documento aplic�veis. Na decis�o REJEITADO o painel envia, por exemplo, `"orientacao":{"codigo":"CRLV_INVALIDO","documento":"CRLV","revisao":"<UUID atual>"}`, junto da justificativa interna obrigat�ria. Causas gerais usam somente o c�digo. Clientes anteriores podem omitir a orienta��o. Combina��es inv�lidas retornam 400 e uma revis�o substitu�da ou alheia retorna 409. O registro da decis�o, do motivo p�blico e da revis�o � at�mico.

| C�digo p�blico | Aplica��o |
|---|---|
| CNH_VENCIDA, CNH_SEM_EAR, CNH_NAO_DEFINITIVA | Revis�o atual da CNH do motorista |
| FOTO_INADEQUADA, RESIDENCIA_INVALIDA | Foto ou comprovante de resid�ncia do motorista, respectivamente |
| CRLV_INVALIDO | Revis�o atual do CRLV do ve�culo |
| DOCUMENTO_ILEGIVEL | Documento atual do cadastro selecionado |
| VEICULO_INELEGIVEL | Dados do ve�culo |
| DIVERGENCIA_CADASTRAL, OUTROS | Cadastro de motorista ou ve�culo |

Em `GET /api/driver/me/situacao`, `analise.orientacaoPublica` cont�m a mensagem padronizada. Uma rejei��o com c�digo acrescenta `codigoPublico` e, quando aplic�vel, `documentoAfetado`. A sele��o considera o �ltimo evento daquele cadastro. Na rejei��o legada sem c�digo a mensagem continua "Revise os dados e documentos deste cadastro.". Ao reabrir, a mensagem volta a null e os campos espec�ficos deixam de aparecer. Justificativa, autoria, hist�rico e v�nculo hist�rico com a revis�o permanecem na API administrativa.

O [registro de pend�ncias do Driver](pendencias/driver-admin.md) acompanha c�mera, autenticidade, verifica��o de contatos e aprova��es antigas. Os [itens de opera��o e seguran�a](pendencias/operacao-seguranca.md) incluem cotas de upload, isolamento do conversor, reten��o e valida��o com documentos reais antes da exposi��o p�blica.

### Estados da pr�-elegibilidade

| Motorista | Ve�culos do titular | motoristaEVeiculoAprovados | autorizadoParaCorridas |
|---|---|---|---|
| PENDENTE ou REJEITADO | Qualquer combina��o | false | false |
| APROVADO | Nenhum, ou todos PENDENTE ou REJEITADO | false | false |
| APROVADO | Um ou mais APROVADO, independentemente dos demais | true | false |

Cada CRLV pertence ao ve�culo indicado na rota. A grava��o confere o titular sob bloqueio e substitui a revis�o pela chave ve�culo e tipo. A leitura associa o CRLV ao mesmo ve�culo e lista somente ve�culos do titular. Tipos pessoais ainda ausentes do contrato s�o omitidos desta consulta durante uma atualiza��o de vers�es.

O resumo n�o seleciona ve�culo de trabalho. Para uma etapa futura, a proposta � o motorista escolher um ve�culo pr�prio e o backend avaliar aquele v�nculo e as condi��es operacionais no momento de autorizar corridas. A escolha, a troca e os crit�rios adicionais dependem da decis�o PA-08.

## Verifica��o de contatos e identidade - DA-02

Proposta de contrato para o Driver autenticado. O servidor identifica a conta pela sess�o e obt�m o destino do cadastro. Os canais s�o `EMAIL` e `CELULAR`.

| Opera��o em /api/driver/me/verificacoes/{canal} | Comportamento |
|---|---|
| POST /solicitacoes | Hoje retorna 503 e n�o cria desafio. Ap�s escolher o provedor, prop�e-se 202 com o UUID do desafio depois da aceita��o da entrega, 429 para limites e 409 para contato j� verificado. O c�digo segue somente para o contato cadastrado. |
| POST /confirmacao | JSON com desafio (UUID) e codigo (string de oito d�gitos). 204 confirma o canal da sess�o. 400 para entrada, c�digo ou desafio inv�lido, substitu�do, esgotado, vencido ou consumido. Com a chave ausente, a confirma��o fica indispon�vel (503). |
| Ambas | Sem sess�o Driver v�lida, 401. A resposta n�o cont�m o destino nem o c�digo secreto. GET /me e GET /me/situacao mant�m os indicadores independentes. |

**Par�metros t�cnicos propostos para revis�o.** A emiss�o interna gera oito d�gitos aleat�rios, com zero inicial permitido, dez minutos de validade, cinco tentativas, sessenta segundos entre envios e cinco emiss�es por janela de uma hora por conta e canal. O reenvio substitui o desafio anterior. O banco guarda HMAC-SHA256 com chave separada, vinculado � conta, canal, UUID, destino e c�digo. Uma confirma��o v�lida consome o desafio e marca somente o canal correspondente na mesma transa��o. Falhas de c�digo consomem uma tentativa. O cadastro e a aprova��o documental continuam independentes da emiss�o. N�o h� provedor, adaptador de teste em produ��o nem marca��o por solicita��o.

**Entrega e opera��o pendentes.** Antes de ativar a emiss�o p�blica, decidir provedores e remetentes, tratamento de falhas amb�guas, idempot�ncia da entrega e limites por destino, origem e or�amento global. Planejar invalida��o at�mica do indicador e dos desafios em qualquer futuro fluxo de troca do contato. A chave deve vir do gerenciador de segredos. A rota��o invalida desafios em tr�nsito. N�o registrar c�digos, destinos ou corpos de confirma��o. Definir expurgo dos desafios expirados e reten��o de auditoria.

**Proposta de origem (implementa��o detalhada abaixo).** Registrar evento do titular com resultado, operador autenticado, data do servidor, revis�o da CNH e da foto e refer�ncia � vers�o do nome cadastrado. Vincular a confirma��o do nome � evid�ncia e � compara��o documental daquela revis�o. Registrar corre��es ou diverg�ncias em eventos separados. Uma substitui��o da evid�ncia vinculada ou altera��o do nome dever� invalidar a confer�ncia correspondente. O checkbox e as decis�es hist�ricas do Admin, por si, n�o atualizam nomeVerificado. M�todos adicionais de prova de identidade e mensagens p�blicas dependem de decis�o.

**Bloqueio futuro de aprova��o.** Proposta de pol�tica versionada atribu�da pelo servidor a novos cadastros a partir de um corte expl�cito, incluindo os iniciados pelo Admin. Na transa��o de aprova��o, conferir as revis�es e o nome atuais e ambos os contatos verificados. Se faltar requisito, responder 422 com a situa��o ainda pendente. Definir previamente o tratamento de contas legadas, pendentes no corte e cadastros administrativos sem conta Driver. A ativa��o depende de canais operacionais e da aprova��o dessa pol�tica. Esta entrega preserva a regra administrativa atual e n�o habilita corridas.

## Confer�ncia administrativa de identidade e nome

No painel, o administrador abre e marca a CNH e a foto de perfil, compara ambas com o nome exibido, escolhe CONFERE ou DIVERGENTE e confirma o registro. Essa a��o tamb�m est� dispon�vel para cadastros j� decididos. A decis�o cadastral e seu checkbox continuam independentes.

| Opera��o administrativa em /api/admin/validacao/MOTORISTA/{id} | Contrato |
|---|---|
| GET do detalhe | nomeMotorista e versaoNome identificam o nome apresentado. documentos cont�m os UUIDs das revis�es. |
| POST /conferencias-identidade | JSON com versaoNome, revisaoCnh, revisaoFoto e resultado. Retorna 201. Refer�ncias ausentes no corpo ou resultado inv�lido retornam 400. Revis�o ausente no cadastro, de outro titular, substitu�da ou vers�o do nome desatualizada retorna 409. Atualizar a consulta antes de repetir. |
| GET /conferencias-identidade | Hist�rico do titular em ordem decrescente de sequ�ncia, incluindo nome conferido, vers�o, revis�es, resultado e autoria (responsavel e data). Titular inexistente ou passageiro retorna 404. As duas rotas exigem token Admin. |

O servidor registra o respons�vel da sess�o administrativa e o instante do PostgreSQL. Cada registro expl�cito acrescenta um evento. A sequ�ncia define o �ltimo parecer, inclusive quando o mesmo conjunto de evid�ncias recebe um parecer divergente. Os UUIDs hist�ricos continuam dispon�veis ap�s substitui��o, sem duplicar imagens.

Em GET /api/driver/me e GET /api/driver/me/situacao, nomeVerificado depende exclusivamente do �ltimo evento CONFERE correspondente ao nome e �s duas revis�es atuais. A consulta consolidada tamb�m exp�e motorista.identidadeDocumentalVerificada com o mesmo valor, pois a confer�ncia � conjunta. O indicador antigo isolado de contas_driver e decis�es hist�ricas n�o constituem evid�ncia. A troca de CNH ou foto e qualquer mudan�a efetiva do nome invalidam a confer�ncia. Retornar ao nome anterior requer nova confer�ncia. E-mail, celular, situa��o administrativa e autoriza��o para corridas conservam suas regras.

A confer�ncia registra compara��o visual humana. Autenticidade junto ao emissor, biometria, reten��o do nome hist�rico, pol�tica para diverg�ncias e eventual obrigatoriedade na aprova��o precisam de decis�o pr�pria.

## Recupera��o interna de acesso - DA-03

A solicita��o p�blica da PR #8 mant�m o limite por origem, retorna 202 sem corpo para qualquer e-mail v�lido dentro da cota e n�o consulta contas nem emite credencial. O endpoint de confirma��o mant�m os c�digos HTTP 204 e 400. As respostas e os logs normais da aplica��o omitem o token e a senha.

O emissor � interno, recebe o titular e uma dura��o positiva de pelo menos um segundo. A dura��o utilizada em testes � fict�cia. A conta e o e-mail verificado s�o lidos e bloqueados no banco. Sem conta eleg�vel, n�o h� emiss�o. Um token `rec_` cont�m 32 bytes aleat�rios. Armazenam-se a conta, o SHA-256 do token, a validade e um segundo resumo que vincula o segredo ao UUID e ao e-mail. A tabela conserva uma credencial corrente por titular. Emiss�es concorrentes s�o serializadas e apenas a �ltima credencial pode ser consumida. A troca do e-mail invalida sua verifica��o e a revoga��o da verifica��o remove o token. Confirmar um token v�lido consome a credencial, atualiza o hash da senha e encerra todas as sess�es da conta na mesma transa��o. Credenciais sem v�nculo, vencidas, substitu�das ou consumidas s�o recusadas.

### Abuso e bloqueio provocado por terceiros

Na base da PR #17, uma �nica origem podia consumir as oito tentativas de qualquer conta em quinze minutos. A pol�tica abaixo trata esse cen�rio. A recupera��o p�blica tem contadores separados para solicita��o e confirma��o por endere�o de origem. O limite pode atingir usu�rios sob o mesmo IP. A resposta 429 depende da origem mesmo quando o e-mail n�o existe. Solicitar recupera��o nesta etapa n�o consome tentativas de login, encerra sess�es ou substitui um token.

**Propostas para decis�o antes de ativar entrega.** Avaliar atraso progressivo por conta e origem combinado com limite global, e um caminho de revalida��o por contato j� comprovado para o titular bloqueado. Evitar que pedidos an�nimos substituam continuamente uma credencial v�lida. Avaliar intervalo m�nimo e deduplica��o por conta e destino com resposta uniforme. Medir falsos bloqueios em redes compartilhadas, validar a origem atr�s do proxy e definir atendimento para perda do contato. Decidir prazo, expurgo, pol�tica de substitui��o e tratamento de entrega amb�gua. A integra��o da entrega permanece pendente.

## Refor�o da autentica��o Driver

Limites t�cnicos da implementa��o para revis�o e calibra��o antes da exposi��o p�blica. As janelas fixas come�am na primeira reserva daquela chave. O rel�gio � o do PostgreSQL. O primeiro limite esgotado devolve 429 sem verificar a senha. Dentro das cotas, senha incorreta e conta inexistente retornam 401 e passam pela mesma fun��o de verifica��o de senha, usando um hash fict�cio para a conta ausente.

| Escopo | Or�amento | Finalidade |
|---|---|---|
| Origem da conex�o | 40 requisi��es em 15 minutos | Proteger a consulta de conta antes da verifica��o da senha. Conta todo pedido admitido desta origem. |
| Conta e origem | 2 em 10 segundos, at� 4 em 15 minutos | Conter rajadas e reincid�ncia de uma origem contra o mesmo titular. |
| Conta em todas as origens | 8 em 15 minutos | Conter tentativas distribu�das. E-mail e UUID do mesmo titular compartilham o contador. |
| Todas as inst�ncias que usam o banco | 600 verifica��es em 1 minuto | Limitar o trabalho agregado de deriva��o de senha. Inclui sucessos. |

As reservas do par, conta e or�amento global s�o at�micas. Uma recusa desfaz esse conjunto. A reserva de origem permanece contabilizada. Um login confirmado devolve exatamente uma unidade de suas janelas de par e conta durante a transa��o que cria a sess�o. Falhas anteriores permanecem contabilizadas. Uma devolu��o atrasada s� afeta a janela original. Os limites de cadastro e recupera��o mant�m as cotas anteriores e usam chaves distintas.

Em uma rede compartilhada, tentativas contra um motorista n�o consomem a cota pessoal de outro. O teto de origem continua compartilhado, inclusive para sucessos. O titular em outra rede pode entrar enquanto houver or�amento da conta e do sistema. V�rias origens coordenadas ainda podem esgotar a conta; um ataque volumoso pode esgotar o or�amento global. Nas bordas de janelas fixas, rajadas pr�ximas podem ser admitidas. Avaliar taxas de 401/429 e lat�ncia em tr�fego representativo antes de aprovar os valores, sem registrar senhas, tokens ou identificadores pessoais nas m�tricas.

**Origem atr�s de proxy.** Na configura��o entregue, o servidor usa o par da conex�o e ignora Forwarded e X-Forwarded-For. Atr�s de um proxy, muitos usu�rios podem aparecer como uma �nica origem. Antes de alterar o comportamento, definir os endere�os de proxies confi�veis, restringir o acesso direto � aplica��o, remover cabe�alhos fornecidos pelo cliente na borda e testar a cadeia de saltos, IPv4 e IPv6 na topologia final. Cotas por sub-rede tamb�m dependem de avalia��o de falsos bloqueios.

**Sess�o e revalida��o propostas.** O token atual expira em oito horas e segue revog�vel por logout ou redefini��o de senha. N�o h� renova��o autom�tica. Para revis�o: introduzir uma credencial de renova��o distinta, de uso �nico, guardada por resumo e rotacionada atomicamente, com prazo absoluto e limite de inatividade. Tratar reutiliza��o com revoga��o da fam�lia e exigir autentica��o recente para a��es sens�veis. Definir dura��o, toler�ncia a repeti��o leg�tima e revoga��o no logout e na recupera��o antes de implementar. Para titular temporariamente limitado, avaliar uma revalida��o por contato previamente verificado, com resposta uniforme, limites pr�prios e prova antes de qualquer libera��o. A entrega real e o procedimento para quem perdeu o contato dependem de DA-02 e DA-03.

## Cliente Flutter inicial - PR #24

A abertura do FairGo Driver oferece Entrar e Criar conta. As duas rotas apresentam o estado de implementa��o e permitem voltar. O primeiro build Android para desenvolvimento usa ARM64 e API m�nima 29. O contrato REST Driver permanece no backend. [Guia de execu��o e valida��o f�sica](../apps/driver/README.md)

## Login Flutter - PR #25

Ao abrir Entrar, o app valida a sess�o armazenada consultando o perfil. O prazo absoluto vem de expiraEm. O cliente descarta a sess�o vencida ou recusada e exige novo login. Sair apaga a credencial local e solicita revoga��o da sess�o apresentada. Uma falha de rede no logout pode deixar a sess�o remota v�lida at� a expira��o. A �rea autenticada exibe apenas o nome.
