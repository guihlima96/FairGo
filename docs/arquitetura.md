# Arquitetura do FairGo

## Objetivo

Registrar o escopo arquitetural conhecido do FairGo e separar as escolhas informadas dos detalhes que ainda precisam ser decididos. Este documento descreve uma arquitetura planejada, não uma implementação existente.

## Decisoes informadas

- A plataforma sera organizada em tres modulos: **FairGo Passenger**, **FairGo Driver** e **FairGo Admin**.
- O backend planejado utiliza **Java e Spring Boot**.
- Os aplicativos planejados utilizam **Flutter**.
- O banco de dados planejado é **PostgreSQL**.
- As formas de comunicação planejadas são **API REST** e **WebSocket**.

## Visao conceitual

```text
FairGo Passenger ─┐
FairGo Driver ────┼── comunicacao planejada: REST API e WebSocket ── Backend Java/Spring Boot ── PostgreSQL
FairGo Admin ─────┘
```

O diagrama representa apenas as tecnologias e os módulos previstos. Não define topologia de implantação, limites de serviços, fluxos de dados ou quais operações usarão cada protocolo.

## Pendencias de arquitetura

As decisões abaixo não foram especificadas e permanecem abertas:

- Se Passenger, Driver e Admin serao aplicativos separados, areas de uma aplicacao ou outra composicao.
- Se o backend sera um monolito modular, um conjunto de servicos ou outra estrutura.
- Quais modulos consumirao cada endpoint ou canal WebSocket e para quais casos de uso.
- Contratos, versionamento, autenticação e autorização da API.
- Modelo de dados, estratégia de migrações e políticas de retenção no PostgreSQL.
- Ambientes, hospedagem, implantação, observabilidade, disponibilidade e escalabilidade.
- Integrações externas e requisitos de privacidade e conformidade aplicáveis.

Esses pontos devem ser definidos antes de serem tratados como decisões do projeto.
