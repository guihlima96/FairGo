# Pendências do FairGo

Índice atualizado em 25/11/2026 até a integração da PR #19 em `9efa32b`. Use os IDs para referenciar os itens em futuras PRs. Ao concluir um item, registre o resultado e o link da PR ou da decisão. A [especificação mestre](../../FAIRGO_SPEC.md) é a referência para regras aprovadas.

| Área | Registro |
|---|---|
| Driver e Admin | [Cadastro, autenticação e análise de documentos](driver-admin.md) |
| Operação e segurança | [Exposição pública, manutenção e privacidade](operacao-seguranca.md) |
| Produto e arquitetura | [Decisões para corridas, tarifas e evolução dos módulos](produto-arquitetura.md) |

## Estado das PRs revisadas

| PRs | Situação após a integração da PR #19 |
|---|---|
| [#1](https://github.com/guihlima96/FairGo/pull/1) e [#2](https://github.com/guihlima96/FairGo/pull/2) | Integradas. A validação do backend iniciada na #2 foi coberta pelas suítes das PRs posteriores. |
| [#3](https://github.com/guihlima96/FairGo/pull/3), [#4](https://github.com/guihlima96/FairGo/pull/4) e [#5](https://github.com/guihlima96/FairGo/pull/5) | Integradas. Conferência manual, histórico, CI e fila do Admin entregues. Dependências externas e operação estão nos registros temáticos. |
| [#6](https://github.com/guihlima96/FairGo/pull/6), [#7](https://github.com/guihlima96/FairGo/pull/7) e [#8](https://github.com/guihlima96/FairGo/pull/8) | Integradas. Planejamento do Driver, conferência manual com PDF, autocadastro, login e parte da recuperação entregues. |
| [#9](https://github.com/guihlima96/FairGo/pull/9) | Integrada à main. Veículos e documentos do Driver seguem para análise na fila do Admin. Os itens de exposição pública seguem acompanhados neste registro. |
| #10 e #11 | Integradas. Registro de pendências e consulta de pré-elegibilidade entregues. Comunicação específica de rejeições, verificação real e escolha futura do veículo continuam em DA-02, DA-04 e PA-08. |
| #12 e #13 | Integradas. Diagnóstico arquitetural e primeira extração de componentes de cadastro. |
| [#14](https://github.com/guihlima96/FairGo/pull/14) | Integrada. Desafios de contato preparados. Provedores, entrega real e ativação da política de aprovação seguem em DA-02. |
| [#15](https://github.com/guihlima96/FairGo/pull/15) | Integrada. Conferência visual administrativa vinculada ao nome e às revisões de CNH e foto. Divergências e critérios de aprovação seguem em DA-02. |
| [#16](https://github.com/guihlima96/FairGo/pull/16) | Integrada. Planejamento da entrega de SMS e e-mail. Seleção e contratação dos provedores seguem em DA-02 e DA-03. |
| [#17](https://github.com/guihlima96/FairGo/pull/17) | Integrada. Emissão interna e consumo de token de recuperação vinculado ao e-mail verificado, com revogação de sessões. Entrega real pendente em DA-03. |
| [#18](https://github.com/guihlima96/FairGo/pull/18) | Integrada. Reservas de tentativas de login e métricas agregadas. Calibração das cotas, contenção e validação do proxy seguem em OS-06. |
| [#19](https://github.com/guihlima96/FairGo/pull/19) | Integrada. ARQ-802 concluída. Processamento e cifra documental isolados no monólito. OS-01 a OS-05 e DA-05 continuam acompanhados. |

As revisões anteriores já trataram a conversão de PDF antes da transação, os arquivos temporários do conversor, as revisões auditadas, o formato de `/me` e a repetição da suíte Driver. Os itens dos registros temáticos são trabalhos futuros ou decisões em aberto.

O [diagnóstico ARQ-803](../arquitetura.md#catálogo-e-diagnóstico-arq-803) registra o catálogo, os riscos e as propostas de extração aguardando autorização.

## Fila administrativa após a ARQ-803.0

A PR #20 consolidou o diagnóstico documental. A PR #21 propõe a classificação operacional de cadastros. Permanecem em DA-02 a política de contatos e em DA-05 a verificação de autenticidade. As propostas de extração da ARQ-803 continuam independentes desta entrega.

## Atualização até a PR #31 — base 12b292c

As PRs #20–23 consolidaram o diagnóstico, a fila administrativa, a comunicação pública de rejeições e a prova de códigos. As PRs #24 e #25 introduziram Flutter, login e sessão protegida. A #26 integrou Brevo em homologação. A #27 documentou conformidade municipal. As PRs #28–29 entregaram autocadastro, verificação de e-mail e recuperação no app. As PRs #30–31 revisaram a máscara, a visibilidade de campos e a política de novas senhas.

| Tipo de evidência | Situação |
|---|---|
| CI na main após #31 | [Backend/Admin](https://github.com/guihlima96/FairGo/actions/runs/46162783105) e [Flutter/Android](https://github.com/guihlima96/FairGo/actions/runs/46162783022) aprovados em 12b292c. |
| Galaxy S10e | Responsável confirmou cadastro, login, verificação de e-mail e recuperação na #30. A confirmação física específica da política de espaços da #31 continua sem evidência neste registro. |
| Exposição pública | SMS desativado, e-mail limitado à homologação. Permanecem revisão da política de senha e MFA, contingência para perda de contato, cotas e operação, pendências documentais OS-01–05 e DA-05, e avaliações da [frente Compliance](../compliance/README.md). |
