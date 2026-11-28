import 'dart:async';

import 'package:fairgo_driver/core/driver_api.dart';

import 'package:fairgo_driver/features/access/email_access.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

import 'login_test.dart' show FakeApi;

class EmailApi extends FakeApi {
  final pendingCode = Completer<String>();
  int requests = 0, resets = 0, confirmations = 0;
  String? receivedToken, receivedPassword;
  DriverFailure? failure;
  @override
  Future<String> solicitarCodigo(String token) {
    requests++;
    if (failure != null) return Future.error(failure!);
    return pendingCode.future;
  }

  @override
  Future<void> confirmarCodigo(
    String token,
    String desafio,
    String codigo,
  ) async {
    confirmations++;
    if (codigo != '01234567') throw const DriverFailure('Código inválido', 400);
  }

  @override
  Future<void> recuperar(String email) async {}
  @override
  Future<void> redefinir(String token, String senha) async {
    receivedToken = token;
    receivedPassword = senha;
    resets++;
  }
}

void main() {
  for (final status in [401, 409, 503]) {
    testWidgets('solicitação HTTP $status consulta perfil ou encerra sessão', (
      t,
    ) async {
      final api = EmailApi()
        ..failure = DriverFailure('Falha controlada', status);
      var reads = 0, expired = 0;
      await t.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: VerificationPanel(
              api: api,
              token: 'fixture',
              refresh: () async {
                reads++;
              },
              expired: () async {
                expired++;
              },
            ),
          ),
        ),
      );
      await t.tap(find.text('Verificar e-mail'));
      await t.pump();
      expect(reads, status == 401 ? 0 : 1);
      expect(expired, status == 401 ? 1 : 0);
      expect(api.requests, 1);
      await t.pumpWidget(const SizedBox());
    });
  }
  testWidgets('um envio, contador, confirmação seguida de consulta', (t) async {
    final api = EmailApi();
    int reads = 0;
    await t.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: VerificationPanel(
            api: api,
            token: 'fixture',
            refresh: () async {
              reads++;
            },
            expired: () async {},
          ),
        ),
      ),
    );
    final send = t.widget<TextButton>(find.byType(TextButton)).onPressed!;
    send();
    send();
    expect(api.requests, 1);
    api.pendingCode.complete('fixture');
    await t.pump();
    expect(find.textContaining('Reenviar em'), findsOneWidget);
    await t.enterText(find.byType(TextField), '12345678');
    await t.tap(find.text('Confirmar código'));
    await t.pump();
    expect(find.text('Código inválido'), findsOneWidget);
    await t.enterText(find.byType(TextField), '01234567');
    await t.tap(find.text('Confirmar código'));
    await t.pump();
    expect(reads, 1);
    expect(api.confirmations, 2);
    await t.pumpWidget(const SizedBox());
  });
  testWidgets('pedido uniforme, valida token e senhas antes da redefinição', (
    t,
  ) async {
    final api = EmailApi();
    await t.pumpWidget(MaterialApp(home: RecoveryPage(api: api)));
    await t.enterText(find.byType(TextField), 'teste@example.invalid');
    await t.tap(find.text('Solicitar instruções'));
    await t.pumpAndSettle();
    expect(
      find.text(
        'Se o e-mail estiver elegível, você receberá instruções para recuperar o acesso.',
      ),
      findsOneWidget,
    );
    Future<void> submit() async {
      final b = find.text('Redefinir senha');
      await t.ensureVisible(b);
      await t.tap(b);
      await t.pumpAndSettle();
    }

    await submit();
    expect(find.textContaining('Confira o token'), findsOneWidget);
    final fullToken = 'rec_${'a' * 64}';
    var clipboard = fullToken;
    final messenger =
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
    messenger.setMockMethodCallHandler(SystemChannels.platform, (call) async {
      if (call.method == 'Clipboard.getData') return {'text': clipboard};
      return null;
    });
    addTearDown(
      () => messenger.setMockMethodCallHandler(SystemChannels.platform, null),
    );
    await t.tap(find.byType(TextField).first);
    await t.pump();
    await t
        .state<EditableTextState>(find.byType(EditableText).first)
        .pasteText(SelectionChangedCause.toolbar);
    await t.pump();
    clipboard = ' teste1 ';
    await t.tap(find.byType(TextField).at(1));
    await t.pump();
    await t
        .state<EditableTextState>(find.byType(EditableText).at(1))
        .pasteText(SelectionChangedCause.toolbar);
    await submit();
    expect(find.text('A senha não pode conter espaços.'), findsOneWidget);
    expect(
      t.widget<TextField>(find.byType(TextField).at(1)).controller!.text,
      clipboard,
    );
    await t.enterText(find.byType(TextField).at(1), 'senha123');
    await submit();
    expect(find.text('As senhas precisam coincidir.'), findsOneWidget);
    expect(api.resets, 0);
    const senha = 'senha888';
    await t.enterText(find.byType(TextField).at(1), senha);
    await t.enterText(find.byType(TextField).at(2), senha);
    final labels = [
      'token recebido por e-mail',
      'nova senha',
      'repetir a senha',
    ];
    for (var i = 0; i < labels.length; i++) {
      final show = find.byTooltip('Mostrar ${labels[i]}');
      await t.ensureVisible(show);
      await t.tap(show);
      await t.pump();
      for (var j = 0; j < labels.length; j++) {
        final field = t.widget<TextField>(find.byType(TextField).at(j));
        expect(field.obscureText, j != i);
        expect(field.controller!.text, j == 0 ? fullToken : senha);
      }
      await t.tap(find.byTooltip('Ocultar ${labels[i]}'));
      await t.pump();
    }
    await submit();
    expect(api.receivedToken, fullToken);
    expect(api.receivedPassword, senha);
  });
}
