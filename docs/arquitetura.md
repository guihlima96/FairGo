# Arquitetura do FairGo

## Decisão e escopo

O FairGo evolui como um **monólito modular**. Admin, Driver e Passenger são frentes do produto. A base após a PR #11 (`50c3f4d`) tem um backend Java 17 com Spring Boot 3.5.7, um artefato Maven e um banco PostgreSQL. As classes de produção ainda compartilham o pacote `br.fairgo`. Os limites descritos abaixo orientam mudanças incrementais dentro dessa aplicação.

| Frente | Implementação na base de referência | Evolução prevista |
|---|---|---|
| Admin | Painel HTML/JavaScript em `/admin/index.html` e API REST em `/api/admin/**`. Cadastra pessoas e veículos, consulta evidências e registra decisões. | Evolução das ferramentas de análise e revalidação. |
| Driver | API REST em `/api/driver/**` para contas, sessões, veículos, documentos e consulta de situação. | Verificação de identidade e contatos, entrega da recuperação de conta e aplicativo Flutter. |
| Passenger | O Admin pode cadastrar um usuário com perfil `PASSAGEIRO`. | Aplicativo Flutter e operações próprias do passageiro. |

Corridas, tarifas e cancelamentos têm documentação de produto. O uso de WebSocket depende de um caso de uso definido. Consulte a [especificação](../FAIRGO_SPEC.md) e o [contrato REST Driver/Admin](etapa-5-driver-admin.md).

## Como o cadastro funciona hoje

O painel e os clientes Driver usam controladores HTTP distintos. Spring Security aplica as permissões de cada frente. A persistência usa SQL explícito com `JdbcTemplate`. Configuração e inicialização do banco ficam em [resources](../backend/src/main/resources/), nos scripts `schema.sql`, `validacao.sql` e `driver.sql`.

| Responsabilidade | Componentes atuais | Dependências importantes |
|---|---|---|
| Criar pessoa e conta | `CadastroAdmin`, `ContaDriver` | Ambos gravam em `usuarios`. O autocadastro cria também a conta Driver na mesma transação. O cadastro administrativo legado tem fluxo próprio. |
| Autenticar e autorizar | `Seguranca`, `SegurancaDriver`, `ContaDriver` | Admin e Driver usam papéis, tokens e consultas distintos. O Driver usa o cálculo de digest definido em `Seguranca`. |
| Cadastrar veículo e receber evidências | `CadastroAdmin`, `CadastroVeiculoDriver`, `ValidacaoAdmin`, `CadastroCompartilhado` | As duas frentes usam o mesmo serviço de escrita para veículo, dados técnicos e documentos. O serviço usa o cofre e mantém a reabertura e o histórico. |
| Preparar e proteger documentos | `Cofre`, `PdfSeguro` | O cofre valida o formato, encaminha PDFs para conversão e cifra o conteúdo com contexto de titular e tipo. A política de formatos ainda menciona tipos documentais do FairGo. |
| Decidir e consultar | `ValidacaoAdmin`, `SituacaoDriver` | A decisão administrativa verifica as revisões atuais. A consulta Driver usa uma transação de leitura consistente e apresenta a situação do titular e de seus veículos. |

Na substituição documental, a aplicação verifica o titular, prepara o arquivo e só então bloqueia o cadastro para reconferir a titularidade e gravar a revisão. Quando aplicável, a mesma transação reabre o cadastro e registra o evento. O histórico referencia as revisões conferidas. A consulta de pré-elegibilidade considera motorista e veículo próprio aprovados e devolve `autorizadoParaCorridas=false`. Esses comportamentos são referências de compatibilidade em futuras extrações. [Código do backend](../backend/src/main/java/br/fairgo/)

## Organização para reaproveitamento

| Candidato | Responsabilidade aproveitável em outro produto | Política que pertence ao FairGo |
|---|---|---|
| Cadastro e contas | Identificação da conta, normalização de contatos, sessões e recuperação de acesso. | Perfil de mobilidade, vínculo com cadastros legados e condições de acesso Driver. |
| Verificação de contatos | Quando implementados, emissão e consumo de desafios e registro do resultado da verificação. | Contatos exigidos, ordem das etapas do motorista e critérios para verificar o nome. |
| Documentos | Preparação de arquivos, limites de processamento, cifragem e leitura autorizada. | CNH, EAR, CRLV, formatos por tipo e evidências exigidas por cadastro. |
| Análise administrativa | Registro de decisão, responsável, momento, motivo e revisões conferidas. | Critérios de aprovação, vínculo do veículo, limites municipais e pré-elegibilidade. |

**Resultado do cadastro e placa.** Admin, Driver e o serviço de cadastro usam o record `ResultadoCadastro` ao devolver `id` e `situacao`. O formulário de entrada do autocadastro continua em `ContaDriver.Cadastro`. A validação do formato de placa nas duas APIs usa `Placa.PADRAO`, e `CadastroCompartilhado.criarVeiculo` normaliza o valor antes da gravação. A futura verificação de identidade e contatos pode usar os indicadores próprios do Driver sem ampliar a resposta de criação.

**Critério para extração futura.** Manter o componente dentro do monólito enquanto seus consumidores forem as frentes do FairGo. Quando outro software precisar dele, demonstrar o uso com dados fictícios, testes independentes e uma política do segundo produto. Documentar entradas, saídas, erros e dependências antes de decidir sobre distribuição como biblioteca. Credenciais, documentos reais e configurações de produção ficam fora do componente.

## Referências para as próximas frentes

- **Identidade e contatos do Driver.** Hoje a API expõe indicadores separados para nome, e-mail e celular. A conferência usada na decisão administrativa e a verificação de um canal são operações distintas. A emissão e entrega de recuperação dependem de um canal verificado. As decisões sobre métodos e provedores estão em [DA-02 e DA-03](pendencias/driver-admin.md).
- **Contratos e testes.** Ao alterar comportamento compartilhado, identificar os consumidores Admin e Driver, preservar titularidade, revisões e histórico e conferir o fluxo pelas respectivas APIs. Os [testes de integração e painel](../backend/tests/) e o [workflow](../.github/workflows/admin.yml) registram as verificações existentes.
- **Operação e dados.** Isolamento do conversor, retenção, rotação da chave, restauração e evolução do esquema são acompanhados em [operação e segurança](pendencias/operacao-seguranca.md). Ambientes, observabilidade, uso de WebSocket e seleção de veículo para corridas estão em [produto e arquitetura](pendencias/produto-arquitetura.md).
