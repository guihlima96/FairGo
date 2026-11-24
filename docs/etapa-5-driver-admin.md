# Etapa 5 — FairGo Driver e FairGo Admin

**Proposta para revisão.** A primeira entrega é documental. As próximas entregas começam pelo backend compartilhado. A interface Flutter fica para uma etapa posterior. As regras vigentes estão na [especificação mestre](../FAIRGO_SPEC.md) e em [cadastro e segurança](cadastro-e-seguranca.md).

## Sequência proposta de implementação

| Entrega | Comportamento proposto | Critério de aceite futuro |
|---|---|---|
| 1. Cadastro próprio | Coletar nome completo, celular ativo, e-mail e CPF, com verificação do nome e dos contatos. Criar conta e perfil MOTORISTA de forma atômica. O servidor atribui o perfil. | Reenvios e pedidos simultâneos preservam a unicidade. Um cadastro administrativo anterior exige comprovação de titularidade antes de qualquer vínculo. |
| 2. Acesso e recuperação | Autenticar na área Driver e associar a sessão ao motorista. | A sessão permite completar o cadastro pendente. Uma credencial Driver não acessa a fila, imagens administrativas nem decisões. |
| 3. Veículo | Receber placa e dados técnicos e vincular o veículo ao motorista autenticado. | O servidor deriva o proprietário da sessão e verifica esse vínculo a cada leitura ou alteração. |
| 4. Documentos | Receber foto de perfil, comprovante de residência, CNH definitiva com EAR, antecedentes obrigatórios e toxicológico facultativo no cadastro do motorista e CRLV no veículo. | Validar titularidade antes da gravação. Usar as mesmas validações, cofre e revisões do fluxo administrativo. |
| 5. Análise | Disponibilizar os cadastros na fila existente da Etapa 4. | O administrador abre a revisão, informa motivo, confirma e decide pelo painel atual. |
| 6. Situação | Consultar o estado do próprio motorista e de cada veículo vinculado. | Exibir PENDENTE, APROVADO ou REJEITADO por cadastro. Uma decisão sobre o veículo não altera a do motorista. |

## Conta, autenticação e recuperação

Como opção para avaliação, usar e-mail verificado e senha armazenada com hash adaptativo. Manter credenciais e sessões do motorista separadas dos tokens individuais do Admin. A API Driver deve identificar o titular no servidor, inclusive quando o cliente informar um UUID. Uma conta autenticada pode acompanhar a análise sem que o login conceda aprovação cadastral.

Para recuperação, propõe-se enviar ao contato previamente verificado um token aleatório, de uso único e com prazo. Guardar somente seu resumo criptográfico, responder de modo uniforme para contas existentes e inexistentes e limitar solicitações e tentativas. Ao definir a nova senha, consumir o token atomicamente e revogar as sessões anteriores. Testar expiração, reutilização e duas confirmações simultâneas. Excluir tokens, senhas e conteúdo documental dos logs.

**Decisões para revisão antes de implementar:** identificador de login, canal e provedor de verificação e recuperação, política de senha, tipo e duração de sessão, prazos e limites de tentativas, procedimento para perda do contato e comprovação para vincular cadastros legados. Confirmar também como apresentar a justificativa ao motorista e como registrar a verificação de identidade. A existência de uma conta com o mesmo e-mail não comprova titularidade.

## Integração com o fluxo da Etapa 4

A proposta é oferecer operações próprias ao Driver e reutilizar a gravação transacional do backend. O aplicativo não deve receber um token administrativo. Motorista e veículo conservam seus UUIDs e estados no mesmo banco. A fila atual lista pendências e a decisão continua exclusiva do Admin. Não introduzir uma segunda fila ou um segundo histórico.

- **Envio.** Preservar o contrato atual de imagem JPEG de até 4 MiB e 8 milhões de pixels, a recodificação e a cifragem. Para CNH, registrar EAR e validade. Para o veículo, manter portas, ar-condicionado, ano de fabricação e município. Adequar a análise do Driver à regra aprovada para a Etapa 5. Desde a PR #7, o Admin permite aprovar sem toxicológico. Na integração, testar aprovação sem esse exame e bloqueio da aprovação enquanto faltar a verificação de antecedentes.
- **Ampliação prevista.** Planejar o recebimento da foto de perfil e do comprovante de residência, com conferência pelo Admin e registro da revisão. Incluir nos testes PPD, foto reaproveitada da CNH e comprovante em nome de terceiro. O Admin aceita JPEG e PDF para CNH, CRLV, comprovante de residência e antecedentes.
- **Revisão.** Vincular cada versão ao alvo e tipo corretos. A conferência administrativa usa as versões atuais. Preservar o bloqueio de aprovação incompleta, a rejeição de revisão substituída e o registro da justificativa, data e identidade administrativa.
- **Correção.** Nova evidência após decisão reabre o cadastro afetado conforme a Etapa 4. Em envio simultâneo a uma decisão, preservar a serialização atual. A consulta Driver lê a situação resultante. O conteúdo do histórico interno e os dados de outros titulares ficam restritos.
- **Evolução de dados.** Se a conta exigir estruturas adicionais, propor mudanças aditivas em JDBC com plano de compatibilidade para cadastros existentes. Submeter o esquema a revisão antes de executar alterações.

## Validação para as futuras entregas

Manter o workflow e as suítes atuais do Admin como regressão. Acrescentar cenários Driver com banco exclusivo e identidades fictícias. Percorrer cadastro, autenticação, vínculo do veículo e envio; verificar a presença no painel, rejeitar um documento ilegível, corrigir, conferir a nova revisão, aprovar e consultar os dois estados pelo Driver. Conferir o histórico após cada decisão.

Testar também acesso cruzado entre dois motoristas, tentativa de autoaprovação, cadastro duplicado e recuperação de acesso. Confirmar que cadastros criados antes da Etapa 5 continuam consultáveis no painel. Contratos de API, esquema de credenciais e política de retenção permanecem sujeitos a revisão.

## Captura da foto pelo motorista

Na etapa da foto, abrir a câmera e permitir tirar, revisar e refazer a foto antes do envio em JPEG. Se o acesso à câmera estiver indisponível, informar como tentar novamente. Não oferecer galeria nem seletor de arquivos nesse fluxo. Nos testes do aplicativo, conferir a captura, a nova tentativa e o caso de permissão negada. O endpoint administrativo atual recebe JPEG e a conferência visual pelo Admin não comprova a origem da imagem.

## Autenticidade do documento digital

A implementação atual guarda a cópia visual cifrada. A assinatura do arquivo recebido só poderá ser validada durante o envio, antes da conversão. Para uma revisão posterior, será necessário consultar o emissor com um QR code ou outro identificador legível, quando disponível, ou solicitar o original novamente. A escolha do provedor e a política de validação automática seguem pendentes.

## Etapa 5.2 — Contrato proposto para revisão

Prefixo `/api/driver`. Requisições e respostas com corpo usam JSON. Clientes devem usar HTTPS em produção. Enviar o token apenas no cabeçalho `Authorization: Bearer <token>`. O número de cadastro desta proposta é o UUID entregue no cadastro.

| Método e rota | Entrada | Resultado |
|---|---|---|
| POST /contas | nome, cpf, celular, email, senha | 201 com id e situacao=PENDENTE. Dados inválidos retornam 400. CPF ou e-mail duplicado retorna 409 sem identificar o campo. |
| POST /sessoes | identificador (e-mail ou UUID) e senha | 200 com token e expiraEm. Falha de credencial retorna 401 e limite de tentativas retorna 429. |
| GET /me | Bearer Driver | 200 com id, nome, email, situacao, cpf, celular e os booleanos nomeVerificado, emailVerificado e celularVerificado. Sessão inválida retorna 401. |
| DELETE /sessoes/atual | Bearer Driver | 204. Revoga a sessão apresentada. |
| POST /recuperacao | email | 202 vazio para qualquer endereço sintaticamente válido. Nesta entrega nenhum token é emitido e nenhuma mensagem é enviada. |
| POST /recuperacao/confirmacao | token e novaSenha | 204 ao consumir uma credencial válida associada a e-mail verificado. Token inválido, expirado ou consumido retorna 400. A troca revoga todas as sessões. |

A criação do usuário e da conta ocorre em uma transação. O servidor define o perfil MOTORISTA. O CPF é validado pelos dígitos verificadores e guardado sem pontuação. O celular brasileiro com DDD é normalizado para +55. O e-mail é convertido para minúsculas e nunca vincula automaticamente um cadastro anterior. Nome, e-mail e celular iniciam sem verificação. O login concede acesso apenas à área de cadastro.

**Parâmetros técnicos para revisão.** Senhas de 15 a 128 caracteres, PBKDF2-HMAC-SHA256 com salt aleatório e 600 mil iterações. Sessões opacas com 256 bits aleatórios, digest no servidor e validade de oito horas. O banco compartilha os limites entre instâncias. Em quinze minutos, o login admite oito tentativas por conta e quarenta por endereço de origem. Um login válido limpa o contador da conta. O endereço é o do par da conexão HTTP. Atrás de proxy, validar o encaminhamento confiável e os limites antes da exposição pública.

**Banco e operação.** Aplicar `backend/src/main/resources/driver.sql` após os scripts existentes com a credencial de migração em produção. As tabelas são aditivas. Restringir as permissões de execução aos dados necessários, proteger banco e backups e evitar registrar corpos de requisições e cabeçalhos de autorização. Cadastros legados exigem comprovação de titularidade em fluxo futuro.

**Recuperação pendente de integração.** O futuro emissor deverá confirmar o contato verificado, gerar um segredo aleatório de uso único com prefixo rec_, persistir apenas seu SHA-256 com prazo e entregar pelo canal aprovado. A confirmação e a revogação estão implementadas. Ainda dependem de decisão o método de verificação, o provedor, o prazo do token e o atendimento a quem perdeu o contato. Os testes provisionam credenciais exclusivamente no banco descartável.

## Etapa 5.3 — Veículos e documentos

Todas as rotas abaixo exigem Bearer Driver. O servidor deriva o motorista da sessão. Veículo inexistente ou de outro titular retorna 404.

| Método e rota em /api/driver | Contrato |
|---|---|
| POST /veiculos | JSON com placa. Retorna 201 com id e situacao. Placa duplicada retorna 409. |
| GET /veiculos | Lista os veículos do titular. |
| GET /veiculos/{id} | Retorna id, placa, situacao, dados técnicos e revisaoCrlv. Dados e revisão podem ser nulos. |
| PUT /veiculos/{id}/dados | Portas, ar-condicionado, ano de fabricação e município nos campos portas, arCondicionado, anoFabricacao e municipio. Retorna 204. |
| PUT /veiculos/{id}/crlv | Multipart com arquivo JPEG/PDF e validade opcional. |
| PUT /me/documentos/{tipo} | Multipart com arquivo. CNH requer ear e validade. ANTECEDENTES e RESIDENCIA aceitam JPEG/PDF, assim como CNH. FOTO_PERFIL e TOXICOLOGICO aceitam JPEG. O toxicológico é facultativo. |

Os envios retornam a revisão (id, tipo, validade e ear). A fila e as decisões continuam no Admin. Substituir uma evidência depois de decisão reabre apenas o cadastro afetado e registra o responsável como driver:<UUID>. A conversão ocorre fora da transação e a gravação revalida titularidade e estado sob bloqueio.

**Pendências antes da exposição pública.** O backend recebe o JPEG da foto e não atesta captura pela câmera. O aplicativo deverá realizar essa captura. Seguem pendentes verificação dos contatos, validação de autenticidade com emissor, política de retenção e reavaliação de aprovações antigas. Revisar cotas de upload, concorrência e isolamento do conversor no ambiente de produção. A sanitização descarta assinaturas digitais e os testes fictícios não substituem os ensaios de legibilidade e latência com documentos reais autorizados.

## Etapa 5.4 — Consulta consolidada

`GET /api/driver/me/situacao` usa exclusivamente o titular da sessão Driver. Responde 200 com uma leitura consistente do cadastro e 401 para sessão ausente, inválida ou credencial do Admin.

| Campo | Interpretação |
|---|---|
| motorista | ID próprio, análise atual, indicadores reais de verificação de nome, e-mail e celular e lista de documentos pessoais. Um indicador falso significa verificação pendente. |
| motorista.documentos | Cada item contém tipo, obrigatorio, revisaoAtual, validade e ear. Revisão nula indica ausência. Um obrigatório ausente precisa ser enviado. O toxicológico é facultativo. |
| veiculos | Uma entrada por veículo do titular com id, placa, análise, dadosTecnicosInformados e metadados do CRLV. Lista vazia indica ausência de veículo. |
| analise | Situação administrativa PENDENTE, APROVADO ou REJEITADO. Apenas REJEITADO recebe orientação pública fixa. A reabertura remove essa orientação. |
| preElegibilidade | motoristaEVeiculoAprovados indica motorista aprovado com ao menos um veículo próprio aprovado. Conta decisões administrativas vigentes, inclusive legadas. autorizadoParaCorridas permanece falso nesta etapa. |

A consulta não reavalia decisões nem atesta autenticidade, validade atual ou identidade. O cliente pode indicar o documento ausente pela combinação de obrigatorio e revisaoAtual. A aprovação administrativa não altera os indicadores de verificação.

**Proposta de comunicação da rejeição.** Exibir “Revise os dados e documentos deste cadastro.” Para informar uma causa específica em etapa futura, propor um catálogo fechado de códigos públicos com mensagens previamente revisadas e seleção explícita pelo Admin. Decisões antigas ou sem código recebem a orientação genérica. O texto livre, os responsáveis e o histórico continuam exclusivos do Admin.

O [registro de pendências do Driver](pendencias/driver-admin.md) acompanha câmera, autenticidade, verificação de contatos e aprovações antigas. Os [itens de operação e segurança](pendencias/operacao-seguranca.md) incluem cotas de upload, isolamento do conversor, retenção e validação com documentos reais antes da exposição pública.

### Estados da pré-elegibilidade

| Motorista | Veículos do titular | motoristaEVeiculoAprovados | autorizadoParaCorridas |
|---|---|---|---|
| PENDENTE ou REJEITADO | Qualquer combinação | false | false |
| APROVADO | Nenhum, ou todos PENDENTE ou REJEITADO | false | false |
| APROVADO | Um ou mais APROVADO, independentemente dos demais | true | false |

Cada CRLV pertence ao veículo indicado na rota. A gravação confere o titular sob bloqueio e substitui a revisão pela chave veículo e tipo. A leitura associa o CRLV ao mesmo veículo e lista somente veículos do titular. Tipos pessoais ainda ausentes do contrato são omitidos desta consulta durante uma atualização de versões.

O resumo não seleciona veículo de trabalho. Para uma etapa futura, a proposta é o motorista escolher um veículo próprio e o backend avaliar aquele vínculo e as condições operacionais no momento de autorizar corridas. A escolha, a troca e os critérios adicionais dependem da decisão PA-08.

## Verificação de contatos e identidade — DA-02

Proposta de contrato para o Driver autenticado. O servidor identifica a conta pela sessão e obtém o destino do cadastro. Os canais são `EMAIL` e `CELULAR`.

| Operação em /api/driver/me/verificacoes/{canal} | Comportamento |
|---|---|
| POST /solicitacoes | Hoje retorna 503 e não cria desafio. Após escolher o provedor, propõe-se 202 com o UUID do desafio depois da aceitação da entrega, 429 para limites e 409 para contato já verificado. O código segue somente para o contato cadastrado. |
| POST /confirmacao | JSON com desafio (UUID) e codigo (string de oito dígitos). 204 confirma o canal da sessão. 400 para entrada, código ou desafio inválido, substituído, esgotado, vencido ou consumido. Com a chave ausente, a confirmação fica indisponível (503). |
| Ambas | Sem sessão Driver válida, 401. A resposta não contém o destino nem o código secreto. GET /me e GET /me/situacao mantêm os indicadores independentes. |

**Parâmetros técnicos propostos para revisão.** A emissão interna gera oito dígitos aleatórios, com zero inicial permitido, dez minutos de validade, cinco tentativas, sessenta segundos entre envios e cinco emissões por janela de uma hora por conta e canal. O reenvio substitui o desafio anterior. O banco guarda HMAC-SHA256 com chave separada, vinculado à conta, canal, UUID, destino e código. Uma confirmação válida consome o desafio e marca somente o canal correspondente na mesma transação. Falhas de código consomem uma tentativa. O cadastro e a aprovação documental continuam independentes da emissão. Não há provedor, adaptador de teste em produção nem marcação por solicitação.

**Entrega e operação pendentes.** Antes de ativar a emissão pública, decidir provedores e remetentes, tratamento de falhas ambíguas, idempotência da entrega e limites por destino, origem e orçamento global. Planejar invalidação atômica do indicador e dos desafios em qualquer futuro fluxo de troca do contato. A chave deve vir do gerenciador de segredos. A rotação invalida desafios em trânsito. Não registrar códigos, destinos ou corpos de confirmação. Definir expurgo dos desafios expirados e retenção de auditoria.

**Conferência administrativa proposta.** Registrar evento do titular com resultado, operador autenticado, data do servidor, revisão da CNH e da foto e referência à versão do nome cadastrado. Vincular a confirmação do nome à evidência e à comparação documental daquela revisão. Registrar correções ou divergências em eventos separados. Uma substituição da evidência vinculada ou alteração do nome deverá invalidar a conferência correspondente. O checkbox e as decisões históricas do Admin, por si, não atualizam nomeVerificado. Métodos adicionais de prova de identidade e mensagens públicas dependem de decisão.

**Bloqueio futuro de aprovação.** Proposta de política versionada atribuída pelo servidor a novos cadastros a partir de um corte explícito, incluindo os iniciados pelo Admin. Na transação de aprovação, conferir as revisões e o nome atuais e ambos os contatos verificados. Se faltar requisito, responder 422 com a situação ainda pendente. Definir previamente o tratamento de contas legadas, pendentes no corte e cadastros administrativos sem conta Driver. A ativação depende de canais operacionais e da aprovação dessa política. Esta entrega preserva a regra administrativa atual e não habilita corridas.
