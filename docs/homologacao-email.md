# Homologação Brevo por e-mail — INT-001.1

O responsável confirmou em 27/11/2026 o recebimento de um teste direto via IPv4. A homologação a seguir valida os fluxos do backend. Usar banco de homologação, caixa autorizada, conta fictícia e uma sessão Driver. Manter a chave no Bitwarden e injetar no processo por mecanismo local de segredos. Evitar histórico de comandos, capturas e logs HTTP detalhados.

1. Configurar FAIRGO_EMAIL_PROVIDER=brevo, FAIRGO_SMS_PROVIDER=disabled, FAIRGO_BREVO_EMAIL_API_KEY, FAIRGO_EMAIL_SENDER e FAIRGO_EMAIL_SENDER_NAME. Informar os endereços completos autorizados em FAIRGO_EMAIL_ALLOWED_RECIPIENTS, separados por vírgula. Manter FAIRGO_CONTACT_KEY configurada conforme o guia do backend. Ambos os canais iniciam desativados por padrão.
2. Com Bearer Driver, chamar POST /api/driver/me/verificacoes/EMAIL/solicitacoes. Esperar 202 com desafio, conferir o recebimento efetivo na caixa e confirmar o código em POST /api/driver/me/verificacoes/EMAIL/confirmacao. Conferir emailVerificado em /me e a independência do celular. Repetições imediatas durante o desafio devem receber 429.
3. Solicitar POST /api/driver/recuperacao com o endereço verificado. Verificar o mesmo 202 para endereço desconhecido. Repetir antes do consumo e verificar que chegou um único token. Usá-lo em /api/driver/recuperacao/confirmacao, confirmar o encerramento da sessão anterior e a rejeição de reutilização. Nunca anexar o conteúdo das mensagens ao relatório.
4. Conferir que endereço fora da lista, contato não verificado na recuperação e canal CELULAR ficam sem entrega. Desligar o provedor após o teste. Registrar somente horário, cenário e resultado.
5. Em falha de conectividade, comparar DNS A/AAAA e conexão TLS ao host api.brevo.com por IPv4 e IPv6 sem enviar chave. Corrigir a rota do ambiente com o operador. O adaptador usa DNS e TLS padrão da JVM.

## Limites e falhas

Um HTTP 201 indica aceitação do pedido pelo fornecedor. A chegada à caixa depende da entrega posterior. Recusas 4xx (exceto 408), incerteza de transporte, timeout e respostas inesperadas são diferenciados internamente. Falhas não disparam nova tentativa automática. Na verificação, a resposta é 503 em caso de falha; o titular pode solicitar outro desafio depois do intervalo normal.

Para recuperação, a API aplica o limite por origem e responde 202 independentemente do titular. Uma fila volátil de 16 pedidos e um trabalhador por instância isolam a latência externa. Reinício ou saturação pode descartar pedidos. O banco impede nova emissão pública enquanto houver token vigente do titular, inclusive após resultado indeterminado. FAIRGO_RECOVERY_TTL_SECONDS define a validade em homologação (padrão técnico 600, faixa 1–3600 segundos). A política pública e uma eventual fila durável permanecem em avaliação. O adaptador não mantém transação aberta durante o envio.

A integração real de SMS (INT-001.2) aguarda autorização posterior.
