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
    expect(find.text('529.982.247-25'), findsOneWidget);
    await tap('Continuar');
    await enter(0, 'invalido');
    await tap('Continuar');
    expect(find.text('Confira o e-mail.'), findsOneWidget);
    await enter(0, 'teste@example.invalid');
    await tap('Continuar');
    await enter(0, 'curta');
    await tap('Criar minha conta');
    expect(find.textContaining('Use uma senha'), findsOneWidget);
    await enter(0, ' teste1 ');
    await tap('Criar minha conta');
    expect(find.textContaining('precisam coincidir'), findsOneWidget);
    await enter(1, ' teste1 ');
    final senha = find.byType(TextField).first;
    final repetir = find.byType(TextField).last;
    expect(t.widget<TextField>(senha).obscureText, isTrue);
    expect(t.widget<TextField>(repetir).obscureText, isTrue);
    for (final (label, campo, outro) in [
      ('nova senha', senha, repetir),
      ('repetir a senha', repetir, senha),
    ]) {
      await t.tap(find.byTooltip('Mostrar $label'));
      await t.pump();
      expect(t.widget<TextField>(campo).obscureText, isFalse);
      expect(t.widget<TextField>(outro).obscureText, isTrue);
      expect(t.widget<TextField>(campo).controller!.text, ' teste1 ');
      await t.tap(find.byTooltip('Ocultar $label'));
      await t.pump();
    }
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
    expect(api.calls.single['senha'], ' teste1 ');
    expect(api.calls.single['cpf'], '52998224725');
    expect(api.calls.single['celular'], '+5511912345678');
    expect(find.text('Conta criada'), findsNothing);
    api.result.complete('aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee');
    await t.pumpAndSettle();
    expect(find.text('aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee'), findsOneWidget);
    expect(find.textContaining('pendente de verificações'), findsOneWidget);
  });
}
