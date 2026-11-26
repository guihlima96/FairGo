# Arquitetura do FairGo

## Decisão e escopo

O FairGo evolui como um **monólito modular**. Admin, Driver e Passenger são frentes do produto. A base após a PR #19 (`9efa32b`) tem um backend Java 17 com Spring Boot 3.5.7, um artefato Maven e um banco PostgreSQL. Os fluxos da aplicação ficam em `br.fairgo`; o núcleo documental ocupa o subpacote `br.fairgo.documentos`. Os limites descritos abaixo orientam mudanças incrementais dentro dessa aplicação.

| Frente | Implementação na base de referência | Evolução prevista |
|---|---|---|
| Admin | Painel HTML/JavaScript em `/admin/index.html` e API REST em `/api/admin/**`. Cadastra pessoas e veículos, consulta evidências, registra decisões e audita a conferência visual de identidade. | Evolução das ferramentas de análise e revalidação. |
| Driver | API REST em `/api/driver/**` para contas, sessões, veículos, documentos, confirmação de contatos e consulta de situação e identidade. | Entrega real de códigos e tokens, ativação das políticas aprovadas e evolução do cliente Flutter. |
| Passenger | O Admin pode cadastrar um usuário com perfil `PASSAGEIRO`. | Aplicativo Flutter e operações próprias do passageiro. |

Corridas, tarifas e cancelamentos têm documentação de produto. O uso de WebSocket depende de um caso de uso definido. Consulte a [especificação](../FAIRGO_SPEC.md) e o [contrato REST Driver/Admin](etapa-5-driver-admin.md).

## Como o cadastro funciona hoje

O painel e os clientes Driver usam controladores HTTP distintos. Spring Security aplica as permissões de cada frente. A persistência usa SQL explícito com `JdbcTemplate`. Configuração e inicialização do banco ficam em [resources](../backend/src/main/resources/), nos scripts `schema.sql`, `validacao.sql`, `driver.sql` e `identidade.sql`.

| Responsabilidade | Componentes atuais | Dependências importantes |
|---|---|---|
| Criar pessoa e conta | `CadastroAdmin`, `ContaDriver` | Ambos gravam em `usuarios`. O autocadastro cria também a conta Driver na mesma transação. O cadastro administrativo legado tem fluxo próprio. |
| Autenticar e autorizar | `Seguranca`, `SegurancaDriver`, `ContaDriver` | Admin e Driver usam papéis, tokens e consultas distintos. Ambas as cadeias usam `TokenOpaco.hash`. |
| Cadastrar veículo e receber evidências | `CadastroAdmin`, `CadastroVeiculoDriver`, `ValidacaoAdmin`, `CadastroCompartilhado` | As duas frentes usam o mesmo serviço de escrita para veículo, dados técnicos e documentos. O serviço usa o cofre e mantém a reabertura e o histórico. |
| Preparar e proteger documentos | `Cofre`, `PdfSeguro` | O adaptador Cofre aplica a política FAIRGO e delega o processamento e a cifra ao pacote documentos. |
| Decidir e consultar | `ValidacaoAdmin`, `SituacaoDriver` | A decisão administrativa verifica as revisões atuais. A consulta Driver usa uma transação de leitura consistente e apresenta a situação do titular e de seus veículos. |

Na substituição documental, a aplicação verifica o titular, prepara o arquivo e só então bloqueia o cadastro para reconferir a titularidade e gravar a revisão. Quando aplicável, a mesma transação reabre o cadastro e registra o evento. O histórico referencia as revisões conferidas. A consulta de pré-elegibilidade considera motorista e veículo próprio aprovados e devolve `autorizadoParaCorridas=false`. Esses comportamentos são referências de compatibilidade em futuras extrações. [Código do backend](../backend/src/main/java/br/fairgo/)

## Organização para reaproveitamento

| Candidato | Responsabilidade aproveitável em outro produto | Política que pertence ao FairGo |
|---|---|---|
| Cadastro e contas | Identificação da conta, normalização de contatos, sessões e recuperação de acesso. | Perfil de mobilidade, vínculo com cadastros legados e condições de acesso Driver. |
| Verificação de contatos | Emissão interna e confirmação de desafios por canal, com registro do resultado. | Contatos exigidos, ordem das etapas do motorista e critérios para verificar o nome. |
| Documentos | Preparação de arquivos, limites de processamento, cifragem e leitura autorizada. | CNH, EAR, CRLV, formatos por tipo e evidências exigidas por cadastro. |
| Análise administrativa | Registro de decisão, responsável, momento, motivo e revisões conferidas. | Critérios de aprovação, vínculo do veículo, limites municipais e pré-elegibilidade. |

**Resultado do cadastro e placa.** Admin, Driver e o serviço de cadastro usam o record `ResultadoCadastro` ao devolver `id` e `situacao`. O formulário de entrada do autocadastro continua em `ContaDriver.Cadastro`. A validação do formato de placa nas duas APIs usa `Placa.PADRAO`, e `CadastroCompartilhado.criarVeiculo` normaliza o valor antes da gravação. Os indicadores de identidade e contatos são consultados separadamente da resposta de criação.

**Critério para extração futura.** Manter o componente dentro do monólito enquanto seus consumidores forem as frentes do FairGo. Quando outro software precisar dele, demonstrar o uso com dados fictícios, testes independentes e uma política do segundo produto. Documentar entradas, saídas, erros e dependências antes de decidir sobre distribuição como biblioteca. Credenciais, documentos reais e configurações de produção ficam fora do componente.

## Referências para as próximas frentes

- **Identidade e contatos do Driver.** Hoje a API expõe indicadores separados para nome, e-mail e celular. A conferência usada na decisão administrativa e a verificação de um canal são operações distintas. A emissão e entrega de recuperação dependem de um canal verificado. As decisões sobre métodos e provedores estão em [DA-02 e DA-03](pendencias/driver-admin.md).
- **Contratos e testes.** Ao alterar comportamento compartilhado, identificar os consumidores Admin e Driver, preservar titularidade, revisões e histórico e conferir o fluxo pelas respectivas APIs. Os [testes de integração e painel](../backend/tests/) e o [workflow](../.github/workflows/admin.yml) registram as verificações existentes.
- **Operação e dados.** Isolamento do conversor, retenção, rotação da chave, restauração e evolução do esquema são acompanhados em [operação e segurança](pendencias/operacao-seguranca.md). Ambientes, observabilidade, uso de WebSocket e seleção de veículo para corridas estão em [produto e arquitetura](pendencias/produto-arquitetura.md).

### Conferência visual

O registro de autoria (`AutoriaRegistro`) representa responsável e instante, sem dependência de documentos, papéis HTTP ou regras de mobilidade. `ConferenciaIdentidade` aplica a política específica do motorista e serializa a gravação com o mesmo bloqueio do cadastro usado na substituição documental. Os controladores de consulta compartilham a projeção SQL `identidade_motorista_atual`, que compara a última conferência com as versões atuais. A API administrativa recebe o parecer e fornece a auditoria. A API Driver apresenta apenas o indicador do titular.

### Entrega de SMS e e-mail

Para DA-02 e DA-03, a integração futura terá um contrato interno de entrega por canal. O fluxo de verificação ou recuperação passará uma referência única da mensagem, a finalidade, o destino definido pelo backend e o conteúdo a entregar. O adaptador do fornecedor traduzirá essa solicitação para a API externa e devolverá um resultado comum: aceito, recusado ou indeterminado. A seleção do adaptador ficará na configuração de infraestrutura. Assim, trocar o fornecedor de um canal preserva o cadastro, as regras de verificação e recuperação e os contratos HTTP do Driver.

| Responsabilidade | Onde ficará |
|---|---|
| Determinar o destino elegível, emitir e consumir o código ou token, aplicar expiração e limites | Serviço de verificação ou recuperação |
| Traduzir a mensagem, autenticar a chamada externa e interpretar a resposta do fornecedor | Adaptador de SMS ou e-mail |
| Decidir se uma falha admite nova tentativa usando a mesma referência e evitando envio duplicado | Coordenação da entrega, fora das regras de cadastro |

A aceitação pelo fornecedor indica apenas que ele recebeu a solicitação. A confirmação do contato continua exigindo a apresentação do código válido. A recuperação segue condicionada ao canal previamente verificado. Um resultado indeterminado exige conciliação antes de repetir o envio ou recorrer a outro fornecedor.

Enquanto a integração estiver pendente, a solicitação de verificação segue respondendo 503 e o pedido de recuperação mantém o 202 uniforme sem envio. Na futura implementação dos adaptadores, os testes de contrato deverão cobrir aceitação, recusa e resposta indeterminada com um adaptador em memória restrito à suíte, destinos fictícios e nenhuma chamada de rede. Códigos, tokens e destinos devem ficar fora de logs. [Candidatos e etapa de contratação](pendencias/driver-admin.md#provedores-candidatos-para-da-02-e-da-03)

### Recuperação de acesso

`TokenOpaco` concentra geração aleatória e resumo SHA-256 reutilizados em sessão e recuperação, com prefixos distintos. `RecuperacaoDriver` aplica a política de conta e e-mail verificado e coordena a troca de senha e a revogação das sessões na mesma transação. O login e a redefinição compartilham o codificador de senhas. O emissor devolve ao chamador interno uma referência de mensagem, o destino obtido do banco e o token em memória. A finalidade é recuperação e o canal preparado é e-mail. Esses dados serão a entrada da coordenação de entrega prevista acima. Nenhuma rota serializa esse objeto. O futuro coordenador precisará tratar falhas após a emissão e evitar que uma repetição envie um token já substituído.

### Controle de tentativas

`ControleTentativas` implementa reservas de janela fixa, limpeza limitada e devolução associada ao vencimento original em PostgreSQL. Não conhece perfis, sessões ou transporte HTTP. `TentativasDriver` compõe as cotas por operação, conta, origem e sistema para a API Driver. O controlador identifica a conexão e o titular e confirma a devolução das reservas pessoais na transação de abertura da sessão. A derivação da senha ocorre fora da transação de admissão. O orçamento global é compartilhado pelas instâncias e sua linha pode sofrer contenção; medir em carga antes da exposição pública.

### ARQ-802 — processamento e proteção documental

Antes da extração, Cofre recebia MultipartFile, interpretava tipos de mobilidade, recodificava JPEG e executava AES-GCM. PdfSeguro dependia do cofre para reconhecer a saída e lançava exceções HTTP. Agora o pacote `br.fairgo.documentos` opera sobre bytes. Admin e Driver continuam usando o mesmo serviço de gravação.

| Componente | Entrada e saída | Dependências, limites e erros |
|---|---|---|
| FormatoDocumento | Bytes → indicação de assinatura PDF | JDK. O prefixo identifica um candidato, cuja validade é conferida pelo conversor. As demais entradas passam pelo decodificador JPEG. |
| JpegSeguro | Bytes JPEG → nova imagem JPEG | ImageIO em memória. Entrada de até 4 MiB e até 8 milhões de pixels. Formato inválido ou excesso gera IOException. |
| PdfSeguro | Bytes PDF → PDF reconstruído com imagens | JDK e PDFBox. Processo separado via stdin/stdout. Até duas conversões por JVM, 15 s, heap de 160 MiB e memória direta de 16 MiB. Entrada e saída de até 4 MiB, 1–10 páginas, lado da caixa de até 14.400 pontos e renderização limitada a 1.800 pixels no maior lado. PDF cifrado ou inválido gera IOException. Ocupado e SaidaExcedida distinguem saturação e cópia visual grande. Em classes soltas executa o main diretamente e no JAR Spring Boot usa PropertiesLauncher. |
| CifraDocumental | Bytes e contexto autenticado → bytes cifrados, ou operação inversa | JCA, chave de 32 bytes, AES-GCM com nonce aleatório de 12 bytes no início do registro e tag de 128 bits. Falha de autenticação gera GeneralSecurityException. O chamador fornece o contexto exato nas duas operações. |
| Cofre (adaptador FAIRGO) | Upload, titular e tipo → formato e conteúdo protegido | Aplica a lista de tipos permitidos para PDF e converte os erros de documento, saída excessiva e saturação em HTTP 400, 413 e 503. Mantém AAD ASCII titular:tipo:formato e, para registros com formato nulo, titular:tipo. A leitura legada remove o marcador 1 quando presente, solicita reenvio do PDF bruto de desenvolvimento (422) e mantém os demais bytes. |

O teste de certificado de curso usa o núcleo sem cadastro, Spring ou banco. A regressão de compatibilidade lê um JPEG sintético produzido pelo Cofre do commit de01322 com uma chave exclusiva de teste. Gravação, titularidade, revisões e reabertura continuam no fluxo transacional descrito acima.

O processo do conversor herda as permissões do usuário do backend. Cotas por conta, isolamento no ambiente de destino, legibilidade, retenção, rotação de chave e restauração continuam no [registro operacional](pendencias/operacao-seguranca.md). A autenticidade do documento e o tratamento de assinaturas digitais seguem em [DA-05](pendencias/driver-admin.md).

## Catálogo e diagnóstico ARQ-803

Base analisada em 25/11/2026: `9efa32b95847de0cf0ee860ba2c7c7df3cca9dcc`. A [PR #19](https://github.com/guihlima96/FairGo/pull/19) integrou a ARQ-802. O núcleo documental está implementado e tem teste independente em contexto educacional. A ARQ-803 está em planejamento. O catálogo abaixo indica o grau de reutilização disponível hoje dentro do monólito.

| Componente | Reuso atual ou potencial | Dependência que delimita o uso |
|---|---|---|
| `br.fairgo.documentos` | `FormatoDocumento`, `JpegSeguro`, `PdfSeguro` e `CifraDocumental` já processam bytes ou operam com contexto autenticado fornecido pelo chamador. | JDK, ImageIO, JCA, PDFBox e subprocesso para PDF; o JAR usa o lançador do Spring Boot. Tipos aceitos, AAD histórico e tradução HTTP pertencem a `Cofre`. |
| `TokenOpaco` | Geração aleatória com prefixo e digest SHA-256 já compartilhados por autenticação e recuperação. | Apenas JDK. Prefixos, prazo, vínculo e finalidade pertencem aos consumidores. |
| `ResultadoCadastro`, `AutoriaRegistro`, `Placa` | Resultado de criação e autoria são dados simples; a validação de placa é compartilhada entre Admin e Driver. | Placa pertence a veículos. A decisão de identidade vincula nome, CNH e foto ao cadastro do motorista. |
| `br.fairgo.seguranca.ProvaCodigo` | Gera códigos de oito dígitos com SecureRandom, calcula HMAC-SHA256 e confere a prova com MessageDigest.isEqual. Pode ser usado, por exemplo, na retirada de reservas de uma biblioteca. | Apenas JDK. Recebe uma chave de 32 bytes. O chamador fornece titular, canal, desafio, destino e código. |
| `VerificacaoContato` | Usa o componente de prova nos dois canais do Driver. | Lê FAIRGO_CONTACT_KEY, controla disponibilidade, expiração, reenvio e tentativas, bloqueia contas, atualiza os indicadores e persiste desafios na mesma transação JDBC. |
| `ControleTentativas` | Reserva e devolução por chave e janela já estão separadas dos limites decididos por `TentativasDriver` e `PoliticaLogin`. | Spring JDBC e PostgreSQL: tabela `limites_driver`, relógio do banco, upsert, `RETURNING` e limpeza em lotes com `SKIP LOCKED`. |
| `RecuperacaoDriver` | O padrão reutilizável é emitir um segredo de uso único vinculado ao titular e destino atual, com expiração. | Usa `PasswordEncoder`, transação, perfil MOTORISTA, e-mail verificado, `recuperacoes_driver` e revogação das sessões do titular. |
| `ContaDriver`, `SegurancaDriver`, `Seguranca` | Há operações de sessão e trechos de integração Spring Security candidatos a organização interna. | Conta e sessão Driver, credencial administrativa e decisão de acesso têm formatos, titulares, permissões e persistência próprios. |
| `ObservacaoLogin`, `MetricasLogin` | Contagens e medidas agregadas podem orientar outros fluxos se surgir demanda. | Filtro Servlet específico de POST de sessão Driver; preservar categorias agregadas e ausência de identificadores nos registros. |

### Fluxos, duplicações e limites transacionais

- **Cadastro.** Admin e Driver repetem a normalização de nome e e-mail e a inserção em `usuarios`. O Admin escolhe o perfil. O autocadastro Driver valida CPF e celular brasileiro, gera o hash da senha e grava `usuarios` e `contas_driver` atomicamente. A normalização de e-mail é um candidato pequeno. Compartilhar a criação integral poderia deixar usuário sem conta ou atribuir perfil incorreto.
- **Autenticação.** `ContaDriver.entrar` reúne HTTP, interpretação do identificador, busca por e-mail/UUID, verificação da senha, reservas e emissão de sessão. Uma conta conhecida usa a mesma chave de cota nas duas formas de login. A senha é conferida antes da transação e seu hash é reconferido sob bloqueio antes de emitir a sessão. Esse limite protege a troca de senha concorrente.
- **Sessões e autorização.** Os filtros repetem leitura do Bearer, consulta do digest e criação do principal Spring. No Admin, o token hexadecimal identifica uma credencial administrativa com expiração e revogação. No Driver, o prefixo `drv_` e a tabela `sessoes_driver` identificam uma sessão de oito horas. Logout elimina apenas a sessão do titular; redefinição elimina todas as sessões dele. Manter cadeias e políticas separadas. Uma função de parsing só se justifica diante de manutenção duplicada real.
- **Contato.** `ContatoDriver` usa o titular autenticado. A emissão interna vincula HMAC ao titular, canal, desafio, destino e código. PostgreSQL controla expiração, reenvio, orçamento e consumo. A confirmação decrementa a tentativa e, no sucesso, atualiza o contato e elimina o desafio na mesma transação. A solicitação pública continua em 503 enquanto a entrega estiver desabilitada.
- **Recuperação.** O pedido público responde 202 após o controle por origem. A emissão interna só produz um envelope para e-mail verificado. A confirmação exige o vínculo com o destino atual e consome o token na transação que troca a senha e revoga sessões. A resposta pública e a disponibilidade do provedor são contratos distintos. Os mecanismos de recuperação e de contato têm segredos, provas e efeitos diferentes.
- **Tentativas.** O orçamento por origem começa antes da verificação da conta. As reservas de rajada/par, conta e sistema são obtidas em conjunto; o sucesso devolve apenas as próprias reservas de rajada, par e conta na janela original. Uma falha ou um sucesso concorrente não deve limpar as demais tentativas. A política de origem, os limiares e as métricas continuam sob responsabilidade do Driver.

### Escopo proposto e critérios de aprovação

| Entrega independente | Benefício e risco | Critério de aceite |
|---|---|---|
| **A — prioridade alta. Isolar a geração e a prova do desafio de contato.** Extrair apenas código Java puro para gerar o código e calcular/conferir a prova. Manter o canal, destino atual, relógio SQL, orçamentos e decisão de atualizar flags no fluxo existente. | Reuso concreto entre canais e teste independente do banco. Alterar ordem dos campos, separador, capitalização do canal ou codificação invalidaria desafios em andamento. | Vetor fixo compatível com a versão anterior, inclusive código com zero inicial. Variar titular, canal, desafio, destino e código individualmente. Usar comparação de prova em tempo constante. Preservar chave ausente, 503/400/204 e consumo único sob concorrência. |
| **B — prioridade média. Compartilhar normalização de e-mail no cadastro.** Substituir apenas o comportamento idêntico de Admin e Driver, mantendo validação HTTP e gravações próprias. | Evita divergência no valor persistido. Normalizar antes da validação pode alterar as entradas aceitas. | Mesmas respostas para entradas válidas, inválidas e duplicadas antes/depois, mesmo valor gravado, conflito 409 e rollback integral do autocadastro. Comparar os dois caminhos com a mesma entrada. |
| **C — prioridade média. Separar o caso de uso de login do controlador Driver.** Mover a orquestração como uma unidade; manter conversão HTTP na borda e consultas JDBC internas. | Reduz concentração de responsabilidades no controlador. Mover a fronteira transacional ou as devoluções pode emitir sessão após redefinição ou apagar falhas concorrentes. | Preservar 200/401/429, token e expiração, hash fictício, cota compartilhada entre e-mail/UUID, ordenação de reservas e teste concorrente entre login e troca de senha. |

A ARQ-803.1 implementa a proposta **A**. O formato persistido usa os cinco campos na ordem titular, canal, desafio, destino e código, unidos por LF e codificados em UTF-8. UUIDs usam a representação textual e o canal usa o nome exato do enum. O código é texto de largura oito. Preservar esses bytes e a chave permite confirmar desafios anteriores. Para novos consumidores, usar campos canônicos sem LF. A aplicação mantém os segredos fora das respostas e dos logs. **B** e **C** continuam pendentes. Cada entrega pode partir da main vigente por si só. Recuperação, filtros de papéis, armazenamento de cotas e integração com provedores permanecem fora do primeiro recorte. Não há necessidade demonstrada de biblioteca externa, novo esquema ou serviço geral de identidade.

### Contratos e persistência de referência

As rotas abaixo usam o contrato vigente. Os códigos indicam os comportamentos centrais de cada fluxo. O contrato completo e os casos de autorização estão na [Etapa 5](etapa-5-driver-admin.md).

| Operação | Contrato observável | Tabelas, projeções e regressões existentes |
|---|---|---|
| POST `/api/admin/usuarios` e `/api/driver/contas` | 201 com `id` e `situacao`; duplicidade retorna 409. O perfil e os campos de entrada dependem da frente. | `usuarios`, `contas_driver`, cotas em `limites_driver`. Cobertura em `test_validacao.py`, `test_driver.py` e `test_driver_documentos.py`. |
| POST `/api/driver/sessoes`, GET `/api/driver/me` e DELETE `/api/driver/sessoes/atual` | Login por e-mail ou UUID retorna 200 com token e expiração; credenciais inválidas retornam 401 e cota excedida, 429. Consulta restrita ao titular e logout 204. | `usuarios`, `contas_driver`, `sessoes_driver`, `limites_driver` e visão `identidade_motorista_atual`. A autenticação do Admin usa `credenciais_admin`. Ver `test_driver.py`, `MetricasLoginTest` e testes administrativos de autorização. |
| POST em `/api/driver/me/verificacoes/{canal}/solicitacoes` e `/confirmacao` sob o mesmo prefixo | A solicitação autenticada responde 503 enquanto a entrega está desabilitada. Na confirmação, desafio e código válidos retornam 204; tentativa inválida retorna 400. Preservar também o comportamento sem chave configurada. | `usuarios`, `contas_driver`, `desafios_contato_driver`. Ver `VerificacaoContatoTest` e `test_driver.py`. |
| POST `/api/driver/recuperacao` e `/api/driver/recuperacao/confirmacao` | Pedido válido dentro da cota recebe 202 uniforme sem envio. Confirmação válida recebe 204; token inválido recebe 400. A resposta pública nunca contém o segredo. | `usuarios`, `contas_driver`, `recuperacoes_driver`, `sessoes_driver` e `limites_driver`. Ver `RecuperacaoDriverTest` e `test_driver.py`. |

Para documentos e identidade, preservar os vínculos em `documentos_cadastro`, `decisoes_cadastro` e `conferencias_identidade`, a visão usada por Admin e Driver e a compatibilidade de leitura. As referências são `CofreCompatibilidadeTest`, `DocumentosTest`, `test_driver_documentos.py`, `test_validacao.py` e `test_painel.py`.

**Decisões pendentes.** A escolha e a contratação de provedores, a ativação de envio, a recuperação por perda de contato e o efeito das verificações na aprovação dependem de decisão de produto (DA-02 e DA-03). Os limites vigentes são a referência de compatibilidade; eventuais calibrações pertencem a OS-06. Os recortes B e C seguem sujeitos a autorização individual. A preparação documental ARQ-803.0 não aprova essas mudanças.

### Regressão e pendências

Usar PostgreSQL descartável nos testes JUnit de contato e recuperação. Preservar as suítes HTTP para conta legada, login por dois identificadores, redes compartilhadas, concorrência, expiração, logout, isolamento de papéis, reenvio e mudança de destino. Incluir as respostas de recuperação 202/204/400, rejeição do segundo consumo, revogação das sessões do titular e sobrevivência das sessões de outra conta. Na CI, conferir Maven verify, as três rodadas Driver, Admin, Selenium e integração Driver/Admin. Esses são critérios para mudanças futuras; o diagnóstico é estático.

Continuam abertas **OS-01** (cotas, abuso e isolamento operacional do conversor), **OS-02** (legibilidade e tempo com amostras autorizadas), **OS-03** (retenção, acesso e backups), **OS-04** (rotação, recifragem e restauração), **OS-05** (avaliação de antivírus) e **DA-05** (autenticidade e assinaturas digitais). O subprocesso herda as permissões do backend. Para autenticação, **OS-06** acompanha proxy, redes compartilhadas, calibração e contenção da cota global. [Operação e segurança](pendencias/operacao-seguranca.md) · [Driver e Admin](pendencias/driver-admin.md)

### Aplicativo FairGo Driver

O cliente Android reside em `apps/driver`. A entrada apresenta a marca e as rotas Entrar e Criar conta. Em `lib/core` ficam o tema, as rotas e a configuração do prefixo REST `/api/driver/`. As telas ficam em `features` e o símbolo visual em `shared`. A navegação funciona offline. A aplicação não solicita sessão nem transmite dados nesta etapa. O backend compartilhado continua responsável pelas regras e pela autorização. [Execução e teste por USB](../apps/driver/README.md)
