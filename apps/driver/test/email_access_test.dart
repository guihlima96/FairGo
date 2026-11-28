import 'dart:async';

import 'package:fairgo_driver/core/driver_api.dart';

import 'package:fairgo_driver/features/access/email_access.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

import 'login_test.dart' show FakeApi;

class EmailApi extends FakeApi {
  final pendingCode = Completer<String>();
  int requests = 0, resets = 0, confirmations = 0;
  @override
  Future<String> solicitarCodigo(String token) {
    requests++;
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
    resets++;
  }
}

void main() {
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
    await t.enterText(find.byType(TextField).first, 'rec_${'a' * 64}');
    await t.enterText(find.byType(TextField).at(1), 'senha de teste fictícia');
    await submit();
    expect(find.text('As senhas precisam coincidir.'), findsOneWidget);
    expect(api.resets, 0);
  });
}
