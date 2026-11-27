# Driver e Admin

| ID | Estado | Pendência e resultado esperado | Origem |
|---|---|---|---|
| DA-01 | Parcial | Estrutura Flutter, abertura e navegação inicial preparadas na PR #24. Validar no Galaxy S10e. Implementar em etapas futuras o cadastro, veículo, envio de documentos e acompanhamento. Capturar a foto pela câmera durante o cadastro, com revisão e nova tentativa. Definir como comprovar a origem da foto. | [#7](https://github.com/guihlima96/FairGo/pull/7), [#9](https://github.com/guihlima96/FairGo/pull/9) |
| DA-02 | Parcial | Estrutura de códigos, confirmação e limites preparada. Aprovar métodos e parâmetros. Contratação e integração dos provedores de SMS e e-mail ficam para a preparação dos testes reais do FairGo Driver. Ver candidatos e arquitetura de entrega abaixo. Conferência visual administrativa implementada e vinculada a nome, CNH e foto. Decidir tratamento de divergências, retenção dos eventos e política de aprovação para novos cadastros. | [Contrato e decisões da Etapa 5](../etapa-5-driver-admin.md#verificação-de-contatos-e-identidade--da-02) |
| DA-03 | Parcial | Emissão interna para e-mail verificado, substituição, consumo único e revogação de sessões implementados. A solicitação pública permanece sem emissão. Integrar a entrega futura e decidir deduplicação, prazo, expurgo e revalidação do titular quando várias origens esgotarem o limite de login. A contratação e integração dos provedores de SMS e e-mail ficam para a preparação dos testes reais do FairGo Driver. Decidir canais autorizados para recuperação, prazo do token e procedimento para perda do contato. O consumo de uso único e a revogação de sessões já existem. | [#8](https://github.com/guihlima96/FairGo/pull/8), [Etapa 5](../etapa-5-driver-admin.md) |
| DA-04 | Parcial | A PR #22 implementa a comunicação por catálogo fechado no próprio cadastro. Definir a comprovação de titularidade para vincular contas a cadastros administrativos legados. A auditoria geral pelo Claude Code fica para etapa futura. | [#6](https://github.com/guihlima96/FairGo/pull/6), [#8](https://github.com/guihlima96/FairGo/pull/8) |
| DA-05 | Definir e implementar | Consultar fontes autorizadas para autenticidade, suspensão de CNH, situação de CRLV, emissão de antecedentes e laudo quando aplicável. Decidir tratamento da assinatura digital antes da conversão ou verificação com o emissor por identificador. | [#3](https://github.com/guihlima96/FairGo/pull/3), [#7](https://github.com/guihlima96/FairGo/pull/7), [#9](https://github.com/guihlima96/FairGo/pull/9) |
| DA-06 | Decidir | Definir se e quando reavaliar aprovações antigas sem os documentos exigidos no fluxo atual. | [#7](https://github.com/guihlima96/FairGo/pull/7), [#9](https://github.com/guihlima96/FairGo/pull/9) |
| DA-07 | Implementar | Definir a fonte e a atualização dos limites municipais e implementar a revalidação anual agendada de documentos e veículos. | [#3](https://github.com/guihlima96/FairGo/pull/3), [Backend](../../backend/README.md) |

### Sessões Driver

A renovação depende de aprovação de prazos absoluto e de inatividade, rotação, revogação da família de tokens e resposta a reutilizações. O login mantém a sessão de oito horas. [Proposta e limites de autenticação](../etapa-5-driver-admin.md#reforço-da-autenticação-driver)

### Provedores candidatos para DA-02 e DA-03

| Canal | Candidatos para avaliação | Alternativa para expansão |
|---|---|---|
| SMS | SMS Dev e SMSMais | Em aberto |
| E-mail | Resend e Brevo | Amazon SES |

A seleção do fornecedor por canal e as condições comerciais serão avaliadas durante a preparação dos testes reais. Até essa etapa, os testes automatizados usam dados fictícios, sem credenciais de fornecedores, pagamentos ou envios reais. A escolha de um fornecedor de SMS não autoriza recuperação de acesso por SMS. [Arquitetura de entrega substituível](../arquitetura.md#entrega-de-sms-e-e-mail)
