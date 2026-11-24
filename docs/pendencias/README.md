# Pendências do FairGo

Registro consolidado em 24/11/2026 a partir das descrições e discussões das PRs #1 a #9, da documentação da `main` e das revisões registradas na conversa do projeto. Use os IDs para referenciar os itens em futuras PRs. Ao concluir um item, registre o resultado e o link da PR ou da decisão. A [especificação mestre](../../FAIRGO_SPEC.md) é a referência para regras aprovadas.

| Área | Registro |
|---|---|
| Driver e Admin | [Cadastro, autenticação e análise de documentos](driver-admin.md) |
| Operação e segurança | [Exposição pública, manutenção e privacidade](operacao-seguranca.md) |
| Produto e arquitetura | [Decisões para corridas, tarifas e evolução dos módulos](produto-arquitetura.md) |

## Estado das PRs revisadas

| PRs | Situação na consolidação |
|---|---|
| [#1](https://github.com/guihlima96/FairGo/pull/1) e [#2](https://github.com/guihlima96/FairGo/pull/2) | Integradas. A validação do backend iniciada na #2 foi coberta pelas suítes das PRs posteriores. |
| [#3](https://github.com/guihlima96/FairGo/pull/3), [#4](https://github.com/guihlima96/FairGo/pull/4) e [#5](https://github.com/guihlima96/FairGo/pull/5) | Integradas. Conferência manual, histórico, CI e fila do Admin entregues. Dependências externas e operação estão nos registros temáticos. |
| [#6](https://github.com/guihlima96/FairGo/pull/6), [#7](https://github.com/guihlima96/FairGo/pull/7) e [#8](https://github.com/guihlima96/FairGo/pull/8) | Integradas. Planejamento do Driver, conferência manual com PDF, autocadastro, login e parte da recuperação entregues. |
| [#9](https://github.com/guihlima96/FairGo/pull/9) | Aberta na data do levantamento, com [testes aprovados](https://github.com/guihlima96/FairGo/actions/runs/45432980139). Veículos e documentos pelo Driver aguardam revisão e merge. Os itens de exposição pública seguem acompanhados neste registro. |

As revisões anteriores já trataram a conversão de PDF antes da transação, os arquivos temporários do conversor, as revisões auditadas, o formato de `/me` e a repetição da suíte Driver. Os itens dos registros temáticos são trabalhos futuros ou decisões em aberto.
