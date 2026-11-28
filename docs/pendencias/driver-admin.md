# Driver e Admin

| ID | Estado | Pendência e resultado esperado | Origem |
|---|---|---|---|
| DA-01 | Parcial | Estrutura Flutter, abertura e navegação inicial preparadas na PR #24. Validar no Galaxy S10e. Implementar em etapas futuras o cadastro, veículo, envio de documentos e acompanhamento. Capturar a foto pela câmera durante o cadastro, com revisão e nova tentativa. Definir como comprovar a origem da foto. | [#7](https://github.com/guihlima96/FairGo/pull/7), [#9](https://github.com/guihlima96/FairGo/pull/9) |
| DA-02 | Parcial | Estrutura de códigos, confirmação e limites preparada. Aprovar métodos e parâmetros. Brevo preparado para homologação de e-mail; SMS adiado em INT-001.2. Ver candidatos e arquitetura de entrega abaixo. Conferência visual administrativa implementada e vinculada a nome, CNH e foto. Decidir tratamento de divergências, retenção dos eventos e política de aprovação para novos cadastros. | [Contrato e decisões da Etapa 5](../etapa-5-driver-admin.md#verificação-de-contatos-e-identidade--da-02) |
| DA-03 | Parcial | Emissão interna para e-mail verificado, substituição, consumo único e revogação de sessões implementados. A solicitação pública mantém 202 uniforme e admite emissão na homologação de e-mail autorizada. Validar a entrega Brevo e decidir prazo, expurgo e revalidação do titular quando várias origens esgotarem o limite de login. A integração real de SMS segue adiada em INT-001.2. Decidir canais autorizados para recuperação, prazo do token e procedimento para perda do contato. O consumo de uso único e a revogação de sessões já existem. | [#8](https://github.com/guihlima96/FairGo/pull/8), [Etapa 5](../etapa-5-driver-admin.md) |
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

### Homologação do login Android — PR #25

Validar no Galaxy S10e a persistência após encerramento do processo, a expiração, a revogação e a perda do túnel USB. Renovação e recuperação por canais externos continuam pendentes em DA-03.

## INT-001.1 e INT-001.2

DA-02 e DA-03 recebem entrega Brevo somente para endereços de homologação explicitamente autorizados. Confirmar o fluxo completo, limites e expiração com a caixa real antes de ampliar a exposição. Registrar saturação da fila, incerteza de entrega e conectividade IPv6. [Roteiro de homologação](../homologacao-email.md)

**INT-001.2 — pendente.** A contratação, homologação e integração real de SMS foram adiadas. Manter o celular sujeito à confirmação e as regras cadastrais vigentes.

## Cadastro Flutter — PR #28

Fluxo disponível para desenvolvimento e homologação interna. Validar no Galaxy S10e; lançamento público depende das pendências regulatórias e operacionais. Nome e contatos começam sem confirmação. SMS continua em INT-001.2. Documentos e análise administrativa permanecem etapas posteriores.

### PR #29 — homologação interna
DA-01: cadastro Flutter homologado no S10e pelo responsável. DA-02: jornada de verificação de e-mail integrada ao perfil, com homologação física pendente. DA-03: solicitação e redefinição pelo aplicativo implementadas; validar recebimento e revogação no roteiro de homologação. Uso público depende das pendências operacionais e regulatórias. SMS segue em INT-001.2.
