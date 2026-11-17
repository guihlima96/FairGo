# FairGo

FairGo é uma plataforma de mobilidade urbana em planejamento. O projeto está sendo organizado em três módulos: FairGo Passenger, FairGo Driver e FairGo Admin.

## Modulos planejados

- **FairGo Passenger**: modulo voltado a passageiros.
- **FairGo Driver**: modulo voltado a motoristas.
- **FairGo Admin**: modulo administrativo da plataforma.

As funcionalidades, permissões e fluxos detalhados de cada módulo ainda precisam ser especificados e aprovados.

## Tecnologias planejadas

- **Backend**: Java com Spring Boot.
- **Aplicativos**: Flutter.
- **Banco de dados**: PostgreSQL.
- **Comunicação**: API REST e WebSocket.

Esta é a tecnologia planejada; a presença de implementações ou de uma arquitetura implantada ainda não está documentada neste repositório.

## Documentacao

- [Especificação mestre do FairGo](FAIRGO_SPEC.md) — referência central para escopo, decisões confirmadas e pendências.
- [Arquitetura](docs/arquitetura.md)
- [Regras de tarifas](docs/regras-de-tarifas.md)
- [Regras de cancelamento](docs/regras-de-cancelamento.md)
- [Cadastro e segurança](docs/cadastro-e-seguranca.md)

Os documentos em `docs/` complementam a especificação mestre. Regras de negócio ainda não especificadas estão identificadas como pendências e não devem ser tratadas como aprovadas até que haja uma decisão explícita registrada na especificação.

## Estado atual

Este repositório está no início da documentação do FairGo. Os documentos descrevem o escopo e as tecnologias planejadas, distinguindo as decisões confirmadas dos pontos que ainda precisam de definição. Não há aqui uma declaração de que os módulos ou serviços já estejam implementados.
