import 'package:fairgo_driver/main.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('mostra FairGo Driver e permite abrir e voltar das duas rotas', (
    tester,
  ) async {
    await tester.pumpWidget(const FairGoDriverApp());
    expect(find.text('FairGo Driver'), findsOneWidget);
    for (final label in ['Entrar', 'Criar conta']) {
      await tester.tap(find.text(label));
      await tester.pumpAndSettle();
      expect(find.widgetWithText(AppBar, label), findsOneWidget);
      expect(find.textContaining('pr�xima etapa'), findsOneWidget);
      await tester.tap(find.byTooltip('Voltar'));
      await tester.pumpAndSettle();
      expect(find.text('FairGo Driver'), findsOneWidget);
    }
  });
}
