# Especificação do FairGo

> **Documento mestre do projeto.** Esta é a referência central para o escopo e as decisões documentadas do FairGo.

## 1. Propósito e status

O FairGo é uma plataforma de mobilidade urbana em planejamento. Este documento consolida o escopo e as decisões que foram informados até agora, distinguindo-os de assuntos que ainda dependem de definição e aprovação.

Esta especificação não afirma que qualquer módulo ou serviço já esteja implementado. Itens identificados como pendentes não devem ser tratados como requisitos ou regras aprovadas.

### Autoridade e relação com os demais documentos

Este arquivo é a fonte de referência do projeto neste repositório. O README apresenta o projeto, e os documentos em `docs/` detalham assuntos específicos. Esses materiais complementares devem permanecer coerentes com esta especificação.

Se houver divergência, a especificação mestre prevalece até que a decisão seja confirmada e os documentos relacionados sejam atualizados. Uma pendência só passa a ser decisão do projeto após aprovação explícita e registro nesta especificação; propostas e listas de questões em aberto não constituem aprovação.

## 2. Escopo confirmado

A plataforma está planejada em torno de três módulos:

- **FairGo Passenger** — módulo voltado a passageiros.
- **FairGo Driver** — módulo voltado a motoristas.
- **FairGo Admin** — módulo administrativo.

As funcionalidades, permissões e fluxos detalhados desses módulos ainda não foram especificados.

## 3. Tecnologias planejadas

- **Backend:** Java e Spring Boot.
- **Aplicativos:** Flutter.
- **Banco de dados:** PostgreSQL.
- **Comunicação:** API REST e WebSocket.

Essas tecnologias são escolhas planejadas. Este documento não define versões, hospedagem, topologia, divisão de serviços, contratos de API ou casos de uso específicos para REST e WebSocket.

## 4. Regras de negócio

### 4.1 Tarifas

Não há política de tarifas confirmada. Fórmulas, componentes de preço, taxas, descontos, moeda, estimativas e ajustes permanecem pendentes de decisão e aprovação.

### 4.2 Cancelamentos

Não há política de cancelamento confirmada. Responsáveis, etapas permitidas, prazos, motivos, cobranças, reembolsos, notificações e tratamento de exceções permanecem pendentes.

### 4.3 Cadastro e segurança

Não foram definidos campos de cadastro, verificações, autenticação, recuperação de conta, permissões, tratamento de dados pessoais ou controles administrativos. A existência dos módulos Passenger, Driver e Admin não determina, por si só, esses detalhes.

## 5. Decisões ainda necessárias

Antes de detalhar a arquitetura ou implementar os fluxos correspondentes, é necessário definir e aprovar:

- A composição dos aplicativos e a relação entre os módulos.
- A estrutura interna do backend e os contratos de comunicação.
- Os fluxos e permissões de cada módulo.
- O modelo de dados e as políticas aplicáveis aos dados armazenados.
- As regras de tarifas e cancelamentos.
- O cadastro, a autenticação, a autorização e os requisitos de segurança e privacidade.
- A implantação, os ambientes, a observabilidade e os requisitos operacionais.

Os pontos acima são uma lista de pendências, não propostas adotadas.

## 6. Documentação relacionada

- [README do projeto](README.md)
- [Arquitetura](docs/arquitetura.md)
- [Regras de tarifas](docs/regras-de-tarifas.md)
- [Regras de cancelamento](docs/regras-de-cancelamento.md)
- [Cadastro e segurança](docs/cadastro-e-seguranca.md)
