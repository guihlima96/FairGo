# Etapa 5 — FairGo Driver e FairGo Admin

**Proposta para revisão.** A primeira entrega é documental. As próximas entregas começam pelo backend compartilhado. A interface Flutter fica para uma etapa posterior. As regras vigentes estão na [especificação mestre](../FAIRGO_SPEC.md) e em [cadastro e segurança](cadastro-e-seguranca.md).

## Sequência proposta de implementação

| Entrega | Comportamento proposto | Critério de aceite futuro |
|---|---|---|
| 1. Cadastro próprio | Coletar nome completo, celular ativo, e-mail e CPF, com verificação do nome e dos contatos. Criar conta e perfil MOTORISTA de forma atômica. O servidor atribui o perfil. | Reenvios e pedidos simultâneos preservam a unicidade. Um cadastro administrativo anterior exige comprovação de titularidade antes de qualquer vínculo. |
| 2. Acesso e recuperação | Autenticar na área Driver e associar a sessão ao motorista. | A sessão permite completar o cadastro pendente. Uma credencial Driver não acessa a fila, imagens administrativas nem decisões. |
| 3. Veículo | Receber placa e dados técnicos e vincular o veículo ao motorista autenticado. | O servidor deriva o proprietário da sessão e verifica esse vínculo a cada leitura ou alteração. |
| 4. Documentos | Receber foto de perfil, comprovante de residência, CNH definitiva com EAR, antecedentes e toxicológico no cadastro do motorista e CRLV no veículo. | Validar titularidade antes da gravação. Usar as mesmas validações, cofre e revisões do fluxo administrativo. |
| 5. Análise | Disponibilizar os cadastros na fila existente da Etapa 4. | O administrador abre a revisão, informa motivo, confirma e decide pelo painel atual. |
| 6. Situação | Consultar o estado do próprio motorista e de cada veículo vinculado. | Exibir PENDENTE, APROVADO ou REJEITADO por cadastro. Uma decisão sobre o veículo não altera a do motorista. |

## Conta, autenticação e recuperação

Como opção para avaliação, usar e-mail verificado e senha armazenada com hash adaptativo. Manter credenciais e sessões do motorista separadas dos tokens individuais do Admin. A API Driver deve identificar o titular no servidor, inclusive quando o cliente informar um UUID. Uma conta autenticada pode acompanhar a análise sem que o login conceda aprovação cadastral.

Para recuperação, propõe-se enviar ao contato previamente verificado um token aleatório, de uso único e com prazo. Guardar somente seu resumo criptográfico, responder de modo uniforme para contas existentes e inexistentes e limitar solicitações e tentativas. Ao definir a nova senha, consumir o token atomicamente e revogar as sessões anteriores. Testar expiração, reutilização e duas confirmações simultâneas. Excluir tokens, senhas e conteúdo documental dos logs.

**Decisões para revisão antes de implementar:** identificador de login, canal e provedor de verificação e recuperação, política de senha, tipo e duração de sessão, prazos e limites de tentativas, procedimento para perda do contato e comprovação para vincular cadastros legados. Confirmar também como apresentar a justificativa ao motorista e como registrar a verificação de identidade. A existência de uma conta com o mesmo e-mail não comprova titularidade.

## Integração com o fluxo da Etapa 4

A proposta é oferecer operações próprias ao Driver e reutilizar a gravação transacional do backend. O aplicativo não deve receber um token administrativo. Motorista e veículo conservam seus UUIDs e estados no mesmo banco. A fila atual lista pendências e a decisão continua exclusiva do Admin. Não introduzir uma segunda fila ou um segundo histórico.

- **Envio.** Preservar o contrato atual de imagem JPEG de até 4 MiB e 8 milhões de pixels, a recodificação e a cifragem. Para CNH, registrar EAR e validade. Para o veículo, manter portas, ar-condicionado, ano de fabricação e município. Aplicar os critérios de aprovação existentes.
- **Ampliação prevista.** Planejar o recebimento da foto de perfil e do comprovante de residência, com conferência pelo Admin e registro da revisão. Incluir nos testes PPD, foto reaproveitada da CNH e comprovante em nome de terceiro. O fluxo atual aceita JPEG. O PDF do CRLV exportado da Carteira Digital de Trânsito é uma opção a avaliar para recebimento e visualização protegida.
- **Revisão.** Vincular cada versão ao alvo e tipo corretos. A conferência administrativa usa as versões atuais. Preservar o bloqueio de aprovação incompleta, a rejeição de revisão substituída e o registro da justificativa, data e identidade administrativa.
- **Correção.** Nova evidência após decisão reabre o cadastro afetado conforme a Etapa 4. Em envio simultâneo a uma decisão, preservar a serialização atual. A consulta Driver lê a situação resultante. O conteúdo do histórico interno e os dados de outros titulares ficam restritos.
- **Evolução de dados.** Se a conta exigir estruturas adicionais, propor mudanças aditivas em JDBC com plano de compatibilidade para cadastros existentes. Submeter o esquema a revisão antes de executar alterações.

## Validação para as futuras entregas

Manter o workflow e as suítes atuais do Admin como regressão. Acrescentar cenários Driver com banco exclusivo e identidades fictícias. Percorrer cadastro, autenticação, vínculo do veículo e envio; verificar a presença no painel, rejeitar um documento ilegível, corrigir, conferir a nova revisão, aprovar e consultar os dois estados pelo Driver. Conferir o histórico após cada decisão.

Testar também acesso cruzado entre dois motoristas, tentativa de autoaprovação, cadastro duplicado e recuperação de acesso. Confirmar que cadastros criados antes da Etapa 5 continuam consultáveis no painel. Contratos de API, esquema de credenciais e política de retenção permanecem sujeitos a revisão.
