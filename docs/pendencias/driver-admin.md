# Driver e Admin

| ID | Estado | Pend�ncia e resultado esperado | Origem |
|---|---|---|---|
| DA-01 | Parcial | Estrutura Flutter, abertura e navega��o inicial preparadas na PR #24. Validar no Galaxy S10e. Implementar em etapas futuras o cadastro, ve�culo, envio de documentos e acompanhamento. Capturar a foto pela c�mera durante o cadastro, com revis�o e nova tentativa. Definir como comprovar a origem da foto. | [#7](https://github.com/guihlima96/FairGo/pull/7), [#9](https://github.com/guihlima96/FairGo/pull/9) |
| DA-02 | Parcial | Estrutura de c�digos, confirma��o e limites preparada. Aprovar m�todos e par�metros. Contrata��o e integra��o dos provedores de SMS e e-mail ficam para a prepara��o dos testes reais do FairGo Driver. Ver candidatos e arquitetura de entrega abaixo. Confer�ncia visual administrativa implementada e vinculada a nome, CNH e foto. Decidir tratamento de diverg�ncias, reten��o dos eventos e pol�tica de aprova��o para novos cadastros. | [Contrato e decis�es da Etapa 5](../etapa-5-driver-admin.md#verifica��o-de-contatos-e-identidade--da-02) |
| DA-03 | Parcial | Emiss�o interna para e-mail verificado, substitui��o, consumo �nico e revoga��o de sess�es implementados. A solicita��o p�blica permanece sem emiss�o. Integrar a entrega futura e decidir deduplica��o, prazo, expurgo e revalida��o do titular quando v�rias origens esgotarem o limite de login. A contrata��o e integra��o dos provedores de SMS e e-mail ficam para a prepara��o dos testes reais do FairGo Driver. Decidir canais autorizados para recupera��o, prazo do token e procedimento para perda do contato. O consumo de uso �nico e a revoga��o de sess�es j� existem. | [#8](https://github.com/guihlima96/FairGo/pull/8), [Etapa 5](../etapa-5-driver-admin.md) |
| DA-04 | Parcial | A PR #22 implementa a comunica��o por cat�logo fechado no pr�prio cadastro. Definir a comprova��o de titularidade para vincular contas a cadastros administrativos legados. A auditoria geral pelo Claude Code fica para etapa futura. | [#6](https://github.com/guihlima96/FairGo/pull/6), [#8](https://github.com/guihlima96/FairGo/pull/8) |
| DA-05 | Definir e implementar | Consultar fontes autorizadas para autenticidade, suspens�o de CNH, situa��o de CRLV, emiss�o de antecedentes e laudo quando aplic�vel. Decidir tratamento da assinatura digital antes da convers�o ou verifica��o com o emissor por identificador. | [#3](https://github.com/guihlima96/FairGo/pull/3), [#7](https://github.com/guihlima96/FairGo/pull/7), [#9](https://github.com/guihlima96/FairGo/pull/9) |
| DA-06 | Decidir | Definir se e quando reavaliar aprova��es antigas sem os documentos exigidos no fluxo atual. | [#7](https://github.com/guihlima96/FairGo/pull/7), [#9](https://github.com/guihlima96/FairGo/pull/9) |
| DA-07 | Implementar | Definir a fonte e a atualiza��o dos limites municipais e implementar a revalida��o anual agendada de documentos e ve�culos. | [#3](https://github.com/guihlima96/FairGo/pull/3), [Backend](../../backend/README.md) |

### Sess�es Driver

A renova��o depende de aprova��o de prazos absoluto e de inatividade, rota��o, revoga��o da fam�lia de tokens e resposta a reutiliza��es. O login mant�m a sess�o de oito horas. [Proposta e limites de autentica��o](../etapa-5-driver-admin.md#refor�o-da-autentica��o-driver)

### Provedores candidatos para DA-02 e DA-03

| Canal | Candidatos para avalia��o | Alternativa para expans�o |
|---|---|---|
| SMS | SMS Dev e SMSMais | Em aberto |
| E-mail | Resend e Brevo | Amazon SES |

A sele��o do fornecedor por canal e as condi��es comerciais ser�o avaliadas durante a prepara��o dos testes reais. At� essa etapa, os testes automatizados usam dados fict�cios, sem credenciais de fornecedores, pagamentos ou envios reais. A escolha de um fornecedor de SMS n�o autoriza recupera��o de acesso por SMS. [Arquitetura de entrega substitu�vel](../arquitetura.md#entrega-de-sms-e-e-mail)

### Homologa��o do login Android - PR #25

Validar no Galaxy S10e a persist�ncia ap�s encerramento do processo, a expira��o, a revoga��o e a perda do t�nel USB. Renova��o e recupera��o por canais externos continuam pendentes em DA-03.
