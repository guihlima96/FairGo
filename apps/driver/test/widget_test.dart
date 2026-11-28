import 'package:fairgo_driver/main.dart';
import 'package:fairgo_driver/core/session_store.dart';
import 'package:material_ui/material_ui.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('mostra FairGo Driver e permite abrir e voltar das duas rotas', (
    tester,
  ) async {
    final messenger =
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
    messenger.setMockMethodCallHandler(
      SessionStore.channel,
      (call) async => null,
    );
    addTearDown(
      () => messenger.setMockMethodCallHandler(SessionStore.channel, null),
    );
    await tester.pumpWidget(const FairGoDriverApp());
    expect(find.text('FairGo Driver'), findsOneWidget);
    final theme = Theme.of(tester.element(find.byType(Scaffold)));
    expect(theme.brightness, Brightness.dark);
    for (final label in ['Entrar', 'Criar conta']) {
      await tester.tap(find.text(label));
      await tester.pumpAndSettle();
      expect(find.widgetWithText(AppBar, label), findsOneWidget);
      if (label == 'Entrar') {
        expect(find.byType(TextField), findsNWidgets(2));
      } else {
        expect(find.text('Celular com DDD'), findsOneWidget);
      }
      await tester.tap(find.byTooltip('Voltar'));
      await tester.pumpAndSettle();
      expect(find.text('FairGo Driver'), findsOneWidget);
    }
  });
}
