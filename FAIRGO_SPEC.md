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

A tarifa mínima do passageiro é R$ 8,00 por corrida concluída. O motorista recebe no mínimo R$ 6,50 líquidos por corrida concluída e, nas corridas longas, pelo menos R$ 1,20 por quilômetro remunerável. O deslocamento até o passageiro entra no cálculo. A comissão varia de 8% a 20%, preservados os mínimos do motorista. Na compensação por congestionamento imprevisível elegível, passageiro e FAIRGO pagam metade cada, sem comissão. Os parâmetros de tempo em avaliação constam nas [regras de tarifas](docs/regras-de-tarifas.md).

### 4.2 Cancelamentos

Antes do aceite, o passageiro que cancelar deve aguardar 1 minuto para solicitar outra corrida. Após o aceite e antes da chegada, a compensação é devida quando o motorista percorre mais de 50% do deslocamento previsto. Aplicam-se as cinco faixas aprovadas nas [regras de cancelamento](docs/regras-de-cancelamento.md) sobre os quilômetros efetivamente percorridos.

No embarque, o cancelamento pelo passageiro gera R$ 4,00 para deslocamentos previstos de até 5 km ou compensação por quilometragem acima de 5 km. Soma-se a espera excedente de R$ 0,20 a partir de 1min30s e a cada minuto completo adicional. A espera máxima por ausência é de 5 minutos. O ressarcimento automático de pedágio exige passageiro embarcado e deslocamento ao destino.

A plataforma acompanha aceitação e finalização. A redução de ofertas por cancelamentos do motorista após o aceite só se aplica acima de 30% de cancelamentos sem justificativa no mês vigente.

### 4.3 Cadastro e segurança

O passageiro anexa documento de identidade. O motorista vê o nome real do passageiro, compatível com o documento. Para o motorista, aplicam-se verificação de identidade, antecedentes criminais, exame toxicológico, CNH válida com EAR e CRLV. Os critérios de elegibilidade do veículo e as verificações constam em [cadastro e segurança](docs/cadastro-e-seguranca.md).

## 5. Decisões ainda necessárias

Antes de detalhar a arquitetura ou implementar os fluxos correspondentes, é necessário definir e aprovar:

- A composição dos aplicativos e a relação entre os módulos.
- A estrutura interna do backend e os contratos de comunicação.
- Os fluxos e permissões de cada módulo.
- O modelo de dados e as políticas aplicáveis aos dados armazenados.
- Os parâmetros de tarifa em avaliação e os pontos operacionais de cancelamento identificados nos documentos relacionados.
- Os demais campos de cadastro, a autenticação, a autorização e os requisitos de segurança e privacidade.
- A implantação, os ambientes, a observabilidade e os requisitos operacionais.

Os pontos acima são uma lista de pendências, não propostas adotadas.

## 6. Documentação relacionada

- [README do projeto](README.md)
- [Arquitetura](docs/arquitetura.md)
- [Regras de tarifas](docs/regras-de-tarifas.md)
- [Regras de cancelamento](docs/regras-de-cancelamento.md)
- [Cadastro e segurança](docs/cadastro-e-seguranca.md)
