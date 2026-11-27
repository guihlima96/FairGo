import 'dart:async';

import 'package:fairgo_driver/core/api_config.dart';
import 'package:fairgo_driver/core/driver_api.dart';
import 'package:fairgo_driver/core/session_store.dart';
import 'package:fairgo_driver/features/access/login_page.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

class MemoryStore extends SessionStore {
  MemoryStore() : super('test');
  Grant? value;
  @override
  Future<Grant?> read() async => value;
  @override
  Future<void> clear() async {
    value = null;
  }

  @override
  Future<void> save(Grant grant) async {
    value = grant;
  }
}

class FakeApi extends DriverApi {
  FakeApi() : super(DriverApiConfig.parse('', development: true));
  final pending = Completer<Grant>();
  int calls = 0;
  String? revoked;
  @override
  Future<Grant> login(String id, String password) {
    calls++;
    return pending.future;
  }

  @override
  Future<String> name(String token) async => 'Motorista de teste';
  @override
  Future<void> logout(String token) async {
    revoked = token;
  }
}

void main() {
  testWidgets('protege senha, envia uma vez, identifica e encerra sess�o', (
    tester,
  ) async {
    final api = FakeApi();
    final store = MemoryStore();
    await tester.pumpWidget(
      MaterialApp(
        home: LoginPage(api: api, store: store),
      ),
    );
    await tester.pumpAndSettle();
    await tester.enterText(
      find.byType(TextField).first,
      'pessoa@example.invalid',
    );
    final field = find.byType(TextField).last;
    expect(tester.widget<TextField>(field).obscureText, isTrue);
    await tester.tap(find.byTooltip('Mostrar senha'));
    await tester.pump();
    expect(tester.widget<TextField>(field).obscureText, isFalse);
    await tester.enterText(field, 'senha fict�cia');
    final button = find.widgetWithText(FilledButton, 'Entrar');
    await tester.ensureVisible(button);
    await tester.tap(button);
    await tester.tap(button);
    expect(api.calls, 1);
    api.pending.complete(
      Grant('ficticio', DateTime.now().add(const Duration(hours: 1))),
    );
    await tester.pumpAndSettle();
    expect(find.text('Motorista de teste'), findsOneWidget);
    expect(store.value?.token, 'ficticio');
    await tester.tap(find.text('Sair'));
    await tester.pumpAndSettle();
    expect(api.revoked, 'ficticio');
    expect(store.value, isNull);
    expect(find.byType(TextField), findsNWidgets(2));
  });
  testWidgets('descarta sess�o expirada ao reabrir', (tester) async {
    final store = MemoryStore()..value = Grant('antigo', DateTime.utc(2020));
    await tester.pumpWidget(
      MaterialApp(
        home: LoginPage(api: FakeApi(), store: store),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.textContaining('Sess�o expirada'), findsOneWidget);
    expect(store.value, isNull);
  });
}
