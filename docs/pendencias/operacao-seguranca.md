# Operação e segurança

| ID | Estado | Pendência e resultado esperado | Origem |
|---|---|---|---|
| OS-01 | Validar antes da exposição pública | Revisar cotas de upload por conta, controle de abuso, concorrência e isolamento do conversor de PDFs no ambiente de destino. O processo separado herda as permissões do backend. | [#7](https://github.com/guihlima96/FairGo/pull/7), [#9](https://github.com/guihlima96/FairGo/pull/9), [Backend](../../backend/README.md) |
| OS-02 | Validar antes da exposição pública | Medir legibilidade e tempo de conversão com CNH digital e documentos digitalizados reais de uso autorizado no ambiente de destino. Incluir documentos de várias páginas e avaliar se o limite vigente atende aos casos legítimos. | [#7](https://github.com/guihlima96/FairGo/pull/7), [#9](https://github.com/guihlima96/FairGo/pull/9) |
| OS-03 | Decidir | Definir períodos de retenção e exclusão, acesso aos dados pessoais e atendimento a solicitações. Incluir documentos, versões, histórico e backups na decisão. | [#9](https://github.com/guihlima96/FairGo/pull/9), [Cadastro e segurança](../cadastro-e-seguranca.md) |
| OS-04 | Implementar | Planejar rotação automatizada da chave do cofre com recifragem dos documentos e teste de restauração. | [#3](https://github.com/guihlima96/FairGo/pull/3), [Backend](../../backend/README.md) |
| OS-05 | Avaliar | Avaliar integração com antivírus externo para o fluxo de documentos. | [Backend](../../backend/README.md) |
| OS-06 | Validar antes da exposição pública | Conferir o endereço de origem atrás de proxy confiável e os limites de tentativas do Driver no ambiente de implantação. | [#8](https://github.com/guihlima96/FairGo/pull/8), [Etapa 5](../etapa-5-driver-admin.md) |
| OS-07 | Avaliar | Avaliar ferramenta de migrações versionadas, como Flyway, quando a evolução do esquema justificar. A atualização de restrições que rodava a cada reinício já foi corrigida. | [#7](https://github.com/guihlima96/FairGo/pull/7), revisão do projeto |
