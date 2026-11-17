# Cadastro e seguranca

## Estado das definicoes

O escopo informado confirma os módulos FairGo Passenger, FairGo Driver e FairGo Admin, mas não define campos de cadastro, verificações, autenticação, autorizações ou procedimentos de segurança. Este documento não introduz requisitos de cadastro como regras de negócio aprovadas.

## Regras confirmadas

- A plataforma está planejada para três módulos com os nomes Passenger, Driver e Admin.
- Os detalhes de cadastro e os acessos associados a cada módulo ainda não foram especificados.

## Pendencias para definicao

Devem ser decididos antes de implementar os fluxos correspondentes:

- Dados obrigatórios e opcionais para cada tipo de conta.
- Processo de criação, atualização, recuperação e encerramento de conta.
- Verificação de identidade, contato ou documentos, incluindo escopo e responsáveis.
- Métodos de autenticação e políticas de sessão e recuperação de acesso.
- Permissões de cada módulo e regras de autorização no backend.
- Tratamento de dados pessoais, consentimento, retenção, exclusão e atendimento a solicitações.
- Registro e resposta a eventos de segurança, incidentes e tentativas suspeitas.
- Requisitos específicos para acesso administrativo e auditoria.

Os itens acima são pendências de produto e engenharia, não controles já implementados nem regras definitivamente aprovadas. As decisões devem ser validadas e documentadas antes de orientar a implementação. A escolha de Java/Spring Boot, Flutter, PostgreSQL, REST e WebSocket, por si só, não determina como esses controles serão realizados.
