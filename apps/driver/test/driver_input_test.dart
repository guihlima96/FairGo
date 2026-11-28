import 'package:fairgo_driver/features/access/driver_password.dart';
import 'package:fairgo_driver/features/access/signup_page.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  final cpf = DigitMask('###.###.###-##');
  TextEditingValue edit(String text, [int? caret]) => TextEditingValue(
    text: text,
    selection: TextSelection.collapsed(offset: caret ?? text.length),
  );
  test('CPF progressivo, colagem e limite', () {
    const digits = '52998224725';
    const formatted = '529.982.247-25';
    var state = TextEditingValue.empty;
    for (var i = 0; i < digits.length; i++) {
      state = cpf.formatEditUpdate(state, edit(state.text + digits[i]));
      expect(state.text, formatted.substring(0, state.text.length));
      expect(state.selection.extentOffset, state.text.length);
    }
    for (final paste in [digits, formatted, '${digits}999']) {
      expect(
        cpf.formatEditUpdate(TextEditingValue.empty, edit(paste)).text,
        formatted,
      );
    }
    expect(validCpf(formatted), isTrue);
    expect(validCpf('529.982.247-24'), isFalse);
  });

  test('CPF mantém o cursor em correções e apaga junto aos separadores', () {
    const full = '529.982.247-25';
    final deletion = cpf.formatEditUpdate(
      edit(full, 6),
      edit('529.92.247-25', 5),
    );
    expect(deletion.text, '529.922.472-5');
    expect(deletion.selection.extentOffset, 5);
    final insert = cpf.formatEditUpdate(deletion, edit('529.9822.472-5', 6));
    expect(insert.text, full);
    expect(insert.selection.extentOffset, 6);
    expect(
      cpf.formatEditUpdate(edit(full, 4), edit('529982.247-25', 3)).text,
      '529.822.472-5',
    );
    expect(
      cpf.formatEditUpdate(edit(full, 3), edit('529982.247-25', 3)).text,
      '529.822.472-5',
    );
  });

  test('nova senha preserva limites e rejeita espaços Unicode', () {
    for (final n in [8, 32]) {
      expect(newDriverPasswordError('a' * n), isNull);
    }
    for (final n in [7, 33]) {
      expect(newDriverPasswordError('a' * n), driverPasswordHint);
    }
    for (final s in [
      ' abcdefg',
      'abc defg',
      'abcdefg ',
      ' ' * 8,
      'abc\tdefg',
      'abc\ndefg',
      'abc\u0085defg',
      'abc\u00a0defg',
      'abc\u2009defg',
      'abc\u2028defg',
      'abc\u3000defg',
      'abc\ufeffdefg',
    ]) {
      expect(newDriverPasswordError(s), driverPasswordWhitespaceError);
    }
  });
}
