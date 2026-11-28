# REG-001.0 — Propostas e encaminhamentos

As regras aprovadas constam da [especificação mestre](../../FAIRGO_SPEC.md). As hipóteses abaixo estão em avaliação. Qualquer implementação requer autorização expressa do responsável pelo FAIRGO.

## Tarifa e remuneração

Estudar R$ 10 de tarifa mínima na categoria econômica, R$ 8 líquidos de mínimo do motorista e comissão variável de 10% a 22%. Preservar prioritariamente os mínimos e, até revisão específica, os critérios de quilometragem, deslocamento até o passageiro, complementos e ressarcimentos. Comissão e recolhimento viário têm bases e finalidades distintas. Comissão variável refere-se à intermediação. Se o preço ao passageiro variar dinamicamente, a transparência exige o aviso na solicitação e a exibição do valor final previsto, conforme [SJC-13](matriz-sao-jose-dos-campos.md).

Nos exemplos, a comissão efetiva é limitada pelo mínimo aplicável. Assume-se que o uso viário incide sobre a tarifa cheia apenas para simulação. O saldo é parcial e exclui ISSQN, tributos próprios, pagamentos e demais custos.

| Cenário | Tarifa original / paga | Comissão pretendida | Motorista | Comissão efetiva | Uso viário a 1% / 2% | Saldo a 1% / 2% |
|---|---|---|---|---|---|---|
| Corrida mínima | R$ 10 / R$ 10 | 22% | R$ 8,00 | R$ 2,00 (20%) | R$ 0,10 / R$ 0,20 | R$ 1,90 / R$ 1,80 |
| Corrida mínima, comissão de entrada | R$ 10 / R$ 10 | 10% | R$ 9,00 | R$ 1,00 (10%) | R$ 0,10 / R$ 0,20 | R$ 0,90 / R$ 0,80 |
| Corrida longa, 20 km remuneráveis e piso de R$ 24 | R$ 28 / R$ 28 | 22% | R$ 24,00 | R$ 4,00 (14,29%) | R$ 0,28 / R$ 0,56 | R$ 3,72 / R$ 3,44 |
| Corrida mínima com cupom FAIRGO de R$ 2 | R$ 10 / R$ 8 | 22% | R$ 8,00 | R$ 2,00 (20%) | R$ 0,10 / R$ 0,20 | −R$ 0,10 / −R$ 0,20 |

Se a base municipal do último exemplo for o valor pago, a incidência simulada seria R$ 0,08 ou R$ 0,16. A base em caso de subsídio depende de posição da Prefeitura. Em um cenário sem desconto, o ponto de equilíbrio do centro físico para o recolhimento isolado é quando 1% do total elegível mensal iguala o custo incremental do centro. Dimensionar o atendimento e validar os critérios de enquadramento antes de usar essa conta.

## Categorias veiculares

| Segmento em estudo | Nome provisório | Aspectos para classificação |
|---|---|---|
| Econômico | FairGo Essential | Modelo, carroceria e capacidade homologada, com foco em entrada. |
| Intermediário | FairGo Comfort | Espaço interno, capacidade de bagagem e conservação. |
| Premium | FairGo Executive | Espaço, conservação e qualidade do serviço. |

Avaliar idade, modelos, carrocerias como hatch e sedã, espaço, bagagem, conservação e qualidade em todas as categorias, respeitada a elegibilidade municipal. Um veículo poderá atender a múltiplas categorias, conforme critérios futuros. Nomes, preços, listas e cortes de elegibilidade estão em avaliação.

## Cupons e campanhas

Proposta inicial para a categoria econômica. O FAIRGO financiaria o subsídio, preservando integralmente a remuneração da corrida original para o motorista. Definir validade, orçamento, teto de desconto, limites por usuário, critérios de elegibilidade e estorno. Prever controle administrativo com trilha de alterações, conciliação por campanha, prevenção de abuso e revisão de decisões automatizadas com coleta proporcional. Medir primeira corrida e retorno, custo por passageiro adquirido, retenção e resultado líquido por campanha. Avaliar reutilização do componente em FairGo Recharge somente em escopo próprio.

## Conciliação e arquitetura

Separar na memória de cálculo e na auditoria financeira tarifa original, desconto promocional e financiador, valor efetivamente pago, remuneração do motorista, comissão, complementos, ressarcimentos, recolhimento viário, tributos próprios, tributos retidos e custos operacionais. Conciliar cada viagem com recibo, liquidação, estorno, competência e município de incidência. Trilhas devem identificar alteração, instante, responsável e regras aplicadas.

Manter [monólito modular e backend compartilhado](../arquitetura.md). Prever componentes internos de conformidade, elegibilidade, motor tarifário, campanhas, contabilidade operacional e relatórios. Identificar cada regra por município, versão, intervalo de vigência, fonte normativa ou decisão aprovada e autoria. Registrar a versão usada na decisão e na apuração para reproduzir eventos anteriores.

## Pendências por responsável

| Responsável | Decisões ou confirmações |
|---|---|
| Jurídico | Analisar legalidade, proporcionalidade e eventual contestação administrativa/judicial dos R$ 50 mil à luz do Tema 967 e do regime municipal, inclusive efeitos e estratégia. Revisar DPVAT/SPVAT, biometria, compartilhamento, retenção e direitos. |
| Contador e Fazenda municipal | Validar constituição, CNAEs, regime, inscrição, ISSQN próprio e retido, base, alíquotas, prazo, guias e NFS-e, além da separação contábil e reflexos da transição tributária. |
| SEMOB | Confirmar checklist, conteúdo do plano, meio de protocolo, valor exigível e renovação, centro físico, percentuais e apuração territorial/temporal incluindo cupons e estornos. Confirmar idade por ano de fabricação, identificação visual, porte, curso online e seguro, além de esquema e periodicidade de relatórios. |
| Produto, Engenharia e Privacidade | Verificar configuração efetiva por município, evidenciar lacunas de cadastro, dimensionar controles de Passenger e estabelecer aceite de dados antes de construir os fluxos. |

## Sequência proposta de PRs futuras

1. Critérios municipais e rastreabilidade de evidências no Driver/Admin, após validar idade, curso, seguro, INSS e identificação com a SEMOB.
2. Privacidade e jornada Passenger, após aprovar bases, necessidade da biometria, exposição mínima, retenção, avaliação de impacto e resposta a incidentes.
3. Cotação, transparência, recibo e conciliação, após definir contratos e tratamento regulatório e fiscal.
4. Relatórios municipais, credenciamento e vigência, após receber padrões da SEMOB e validar escopo de dados.
5. Motor tarifário, categorias e campanhas, após aprovação comercial e validação de cenários financeiros.

A auditoria geral pelo Claude Code permanece adiada.
