# Cadastro e segurança

## Identificação do passageiro

O passageiro deve anexar um documento de identidade. O nome real exibido ao motorista deve corresponder ao nome do documento apresentado.

## Pendências para definição

Devem ser decididos antes de implementar os fluxos correspondentes:

- Demais dados obrigatórios e opcionais para cada tipo de conta.
- Processo de criação, atualização, recuperação e encerramento de conta.
- Verificação de identidade, contato ou documentos, incluindo escopo e responsáveis.
- Métodos de autenticação e políticas de sessão e recuperação de acesso.
- Permissões de cada módulo e regras de autorização no backend.
- Tratamento de dados pessoais, consentimento, retenção, exclusão e atendimento a solicitações.
- Registro e resposta a eventos de segurança, incidentes e tentativas suspeitas.
- Requisitos específicos para acesso administrativo e auditoria.

Os itens acima são pendências de produto e engenharia, não controles já implementados nem regras definitivamente aprovadas. As decisões devem ser validadas e documentadas antes de orientar a implementação. A escolha de Java/Spring Boot, Flutter, PostgreSQL, REST e WebSocket, por si só, não determina como esses controles serão realizados.

## Requisitos de habilitação e veículos — regras aprovadas

### Dados e documentos do motorista

- Informar nome completo, número de celular ativo, e-mail e CPF. Verificar nome e contatos durante o cadastro. Os métodos de verificação estão em aberto.
- Tirar a foto de perfil pela câmera durante o cadastro. A foto deve ser frontal, nítida, sem óculos escuros e distinta da foto da CNH. O fluxo do motorista deve oferecer captura e nova tentativa pela câmera, sem seleção da galeria ou de arquivos existentes.
- Apresentar comprovante de residência em nome do motorista.
- O motorista deverá apresentar CNH definitiva e válida (PPD não aceita) com a observação EAR (Exerce Atividade Remunerada).
- O motorista deverá anexar o CRLV atualizado do veículo.
- A documentação deverá ser verificada antes da aprovação do cadastro.
- A verificação de identidade e a de antecedentes criminais são obrigatórias. O exame toxicológico é facultativo nesta etapa. Sua ausência permite concluir a análise e aprovar o cadastro quando os requisitos obrigatórios forem atendidos.
- Aceitar JPEG ou PDF para cada um destes documentos: CNH digital ou física, CRLV e comprovante de residência. O comprovante de antecedentes criminais também poderá ser enviado em PDF.

### Requisitos mínimos do veículo

- Possuir no mínimo quatro portas.
- Possuir ar-condicionado.
- Ter até 10 anos de uso, calculados pelo ano de fabricação.
- Será permitido um ano adicional de operação, totalizando até 11 anos de uso.
- Veículos com 12 anos ou mais não poderão realizar corridas.

### Cálculo da idade do veículo

Idade do veículo = ano atual - ano de fabricação.

Exemplo para 2026:

- Fabricado em 2016: 10 anos, permitido.
- Fabricado em 2015: 11 anos, permitido no ano adicional.
- Fabricado em 2014: 12 anos, não permitido.

A elegibilidade será recalculada anualmente.

Caso a legislação municipal estabeleça limites mais restritivos, deverão prevalecer as exigências legais aplicáveis.

### Validação pelo sistema

O FAIRGO deverá:

1. Verificar a validade da CNH e a presença da observação EAR.
2. Validar o CRLV e identificar o ano de fabricação.
3. Verificar os requisitos mínimos do veículo.
4. Calcular automaticamente a idade do veículo.
5. Impedir a ativação de veículos que ultrapassem o limite permitido.
6. Notificar o motorista antes do encerramento do período de elegibilidade.

**Status:** requisitos aprovados para o FAIRGO, sujeitos à conformidade com a legislação aplicável.
