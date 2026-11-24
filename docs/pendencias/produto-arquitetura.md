# Produto e arquitetura

Estes itens vêm da documentação de produto e complementam o levantamento das PRs. Parâmetros em avaliação dependem de decisão registrada na [especificação mestre](../../FAIRGO_SPEC.md).

| ID | Estado | Pendência e resultado esperado | Origem |
|---|---|---|---|
| PA-01 | Decidir | Detalhar os fluxos e permissões ainda ausentes de Passenger, Driver e Admin. Definir os contratos e quando usar REST ou WebSocket. | [Especificação](../../FAIRGO_SPEC.md), [Arquitetura](../arquitetura.md) |
| PA-02 | Decidir | Detalhar ambientes, implantação, observabilidade e requisitos operacionais dos módulos. | [Arquitetura](../arquitetura.md), [Especificação](../../FAIRGO_SPEC.md) |
| PA-03 | Avaliar | Definir classificação final de corridas curtas, médias e longas e avaliar garantia por tempo, tolerância de congestionamento, compensação por minuto excedente e prevenção de dupla compensação. | [Tarifas](../regras-de-tarifas.md) |
| PA-04 | Decidir | Definir a base de cálculo mensal dos cancelamentos do motorista, critérios de justificativa, intensidade e duração da redução de ofertas. | [Cancelamentos](../regras-de-cancelamento.md) |
| PA-05 | Decidir | Definir registro de ausência do passageiro, encerramento da solicitação, falhas de GPS ou conexão, emergências, divergências de identidade, isenções, reembolso e contestação. | [Cancelamentos](../regras-de-cancelamento.md) |
| PA-06 | Decidir | Definir destinação da taxa de cancelamento, eventual comissão e critérios de comprovação e prevenção de duplicidade no ressarcimento de pedágio. | [Cancelamentos](../regras-de-cancelamento.md) |
| PA-07 | Implementar em etapa futura | Planejar campanhas de descontos e cupons conforme a condição de capital de giro documentada. | [Tarifas](../regras-de-tarifas.md) |
