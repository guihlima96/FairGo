import 'dart:async';

import 'package:fairgo_driver/core/api_config.dart';
import 'package:fairgo_driver/core/driver_api.dart';
import 'package:fairgo_driver/features/access/signup_page.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

class FakeSignup extends DriverApi {
  FakeSignup() : super(DriverApiConfig.parse('', development: true));
  final result = Completer<String>();
  final calls = <Map<String, String>>[];
  @override
  Future<String> cadastrar(Map<String, String> dados) {
    calls.add(dados);
    return result.future;
  }
}

void main() {
  testWidgets('valida etapas, preserva dados e aguarda uma única criação', (
    t,
  ) async {
    final api = FakeSignup();
    await t.pumpWidget(MaterialApp(home: SignupPage(api: api)));
    Future<void> tap(String label) async {
      final f = find.text(label);
      await t.ensureVisible(f);
      await t.tap(f);
      await t.pumpAndSettle();
    }

    Future<void> enter(int index, String text) async {
      await t.enterText(find.byType(TextField).at(index), text);
    }

    await tap('Continuar');
    expect(find.textContaining('nove dígitos'), findsOneWidget);
    await enter(0, '11912345678');
    await tap('Continuar');
    await t.tap(find.byTooltip('Voltar'));
    await t.pumpAndSettle();
    expect(find.text('(11) 91234-5678'), findsOneWidget);
    await tap('Continuar');
    await enter(0, 'Teste Interno');
    await enter(1, '00000000000');
    await tap('Continuar');
    expect(find.text('Confira o CPF.'), findsOneWidget);
    await enter(1, '52998224725');
    await tap('Continuar');
    await enter(0, 'invalido');
    await tap('Continuar');
    expect(find.text('Confira o e-mail.'), findsOneWidget);
    await enter(0, 'teste@example.invalid');
    await tap('Continuar');
    await enter(0, 'curta');
    await tap('Criar minha conta');
    expect(find.textContaining('Use uma senha'), findsOneWidget);
    await enter(0, 'Senha fictícia de teste');
    await tap('Criar minha conta');
    expect(find.textContaining('precisam coincidir'), findsOneWidget);
    await enter(1, 'Senha fictícia de teste');
    final button = t.widget<FilledButton>(find.byType(FilledButton));
    button.onPressed!();
    button.onPressed!();
    await t.pump();
    expect(api.calls.single.keys.toSet(), {
      'celular',
      'nome',
      'cpf',
      'email',
      'senha',
    });
    expect(api.calls.single['celular'], '+5511912345678');
    expect(find.text('Conta criada'), findsNothing);
    api.result.complete('aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee');
    await t.pumpAndSettle();
    expect(find.text('aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee'), findsOneWidget);
    expect(find.textContaining('pendente de verificações'), findsOneWidget);
  });
}
