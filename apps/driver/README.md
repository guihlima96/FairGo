# FairGo Driver

Cliente Android do motorista. A tela inicial, o tema e a navegação funcionam offline. Entrar autentica a conta Driver. Criar conta abre o cadastro inicial em quatro etapas.

## VS Code e Android físico

Use o Flutter **3.50.1 Stable** com Dart **3.14.0**, Java 17, SDK Android 36 e NDK 28.2.13676358. O projeto fixa o Flutter no pubspec. No Windows com 6 GB de RAM, use o aparelho conectado por USB e mantenha o Android Studio fechado. O projeto limita o Gradle a dois workers e heap de 1536 MiB.

Na raiz do repositório, com o Galaxy S10e desbloqueado, depuração USB habilitada e computador autorizado:

```powershell
cd apps/driver
flutter --version
flutter pub get --enforce-lockfile
adb devices -l
flutter devices
code .
```

Selecione o SM-G970F na barra inferior do VS Code e pressione F5. Para passar a origem local pela linha de comando, substitua SERIAL pelo identificador listado no ADB:

```powershell
adb -s SERIAL reverse tcp:8080 tcp:8080
adb -s SERIAL reverse --list
flutter run -d SERIAL --debug --dart-define=FAIRGO_API_ORIGIN=http://127.0.0.1:8080
```

No VS Code, a configuração pessoal de execução pode incluir `"toolArgs": ["--dart-define=FAIRGO_API_ORIGIN=http://127.0.0.1:8080"]`. Se `adb` estiver fora do PATH, use `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"` no PowerShell. Ao encerrar, remova o encaminhamento com `adb -s SERIAL reverse --remove tcp:8080`.

## Configuração REST

`FAIRGO_API_ORIGIN` aceita uma origem HTTPS sem caminho, credenciais ou parâmetros. A configuração constrói exclusivamente o prefixo `/api/driver/`. A origem pode ficar ausente para usar a navegação offline. O HTTP local é aceito apenas em debug para `127.0.0.1:8080`. A política Android de debug permite cleartext apenas para esse host. O manifesto principal desabilita cleartext.

Iniciar o backend no computador conforme o [guia do servidor](../../backend/README.md), mantendo o bind em 127.0.0.1. O ADB encaminha a porta HTTP do servidor inteiro por USB. A autorização das rotas administrativas continua no servidor e o app conhece apenas recursos Driver. Não encaminhar a porta do PostgreSQL. Nunca incluir tokens, chaves ou dados pessoais em `--dart-define`.

## Validação

```powershell
dart format --output=none --set-exit-if-changed lib test
flutter analyze --no-pub
flutter test --no-pub
flutter build apk --debug --no-pub --target-platform android-arm64 --dart-define=FAIRGO_API_ORIGIN=http://127.0.0.1:8080
```

O workflow Flutter testa as configurações de origem, a tela inicial, as duas rotas e o retorno. Compila o APK e verifica no pacote a biblioteca ARM64 e a API mínima 29. O workflow existente mantém a regressão do backend e painel.

**Roteiro no Galaxy S10e (Android 10).** Instalar o build desta branch com `flutter run`, conferir o título, as cores, a legibilidade e as ações em retrato e paisagem. Abrir as duas opções e voltar pelo app e pelo botão do sistema. Repetir sem backend e conferir o túnel com `adb reverse --list`. Com uma conta fictícia no banco de homologação, testar login por e-mail e por UUID, senha incorreta, botão de visibilidade, reabertura da área pelo botão Entrar e logout. Desconectar o USB durante uma tentativa e verificar a mensagem. Reconectar antes de validar a revogação no servidor. Manter o aparelho sem emulador.

## Homologação interna do cadastro no Galaxy S10e

Com o backend de homologação em loopback, configure o túnel e execute o app com os comandos USB acima. Toque em Criar conta. Use celular, CPF válido de teste, nome, e-mail e senha fictícios em base isolada. Volte uma etapa e confira os dados. Após confirmar a senha, toque uma vez em Criar minha conta. Confira o número retornado, siga para o login e autentique pelo número. Repita os dados para conferir o conflito genérico. Em uma nova tentativa, interrompa o túnel e confira a mensagem de resultado incerto. Verifique a conta antes de repetir.

### Homologação da PR #30 no Galaxy S10e

Com o backend e o APK desta revisão, use dados fictícios e o túnel USB descrito acima.

1. Digite um CPF válido, cole com e sem pontuação e tente um 12º dígito. Corrija um dígito no meio e apague junto a um ponto. Confira o cursor, a apresentação XXX.XXX.XXX-XX e a rejeição de um verificador inválido.
2. No cadastro, tente senhas de 7, 8, 32 e 33 caracteres e uma confirmação divergente. Confira os olhos dos dois campos e teste uma senha com espaços nas extremidades. Volte da etapa de senha e confira que ambos os campos estão vazios e ocultos ao retornar.
3. Entre com uma conta legada cuja senha tenha mais de 32 caracteres. No fluxo interno de recuperação por e-mail, cole o token integral, alterne os três olhos individualmente e redefina a senha. Confira que a sessão anterior é revogada, a nova senha permite login e o logout encerra a sessão.
4. Confira a verificação de e-mail e a legibilidade em retrato e paisagem. Não inclua CPF, senha, token ou conteúdo do e-mail em capturas ou registros de homologação.
