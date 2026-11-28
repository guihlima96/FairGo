import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/driver_api.dart';
import '../../shared/sensitive_field.dart';
import 'driver_password.dart';

class DigitMask extends TextInputFormatter {
  DigitMask(this.pattern);
  final String pattern;
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    if (oldValue.text == newValue.text) return newValue;
    final separators = RegExp(r'\D');
    var raw = newValue.text;
    var selection = newValue.selection;
    // Backspace/Delete on punctuation must also remove the adjacent digit.
    if (oldValue.selection.isValid &&
        oldValue.selection.isCollapsed &&
        selection.isCollapsed &&
        selection.isValid &&
        raw.length + 1 == oldValue.text.length &&
        raw.replaceAll(separators, '') ==
            oldValue.text.replaceAll(separators, '')) {
      final caret = selection.extentOffset;
      final backward = oldValue.selection.extentOffset > caret;
      var index = backward ? caret - 1 : caret;
      while (index >= 0 &&
          index < raw.length &&
          separators.hasMatch(raw[index])) {
        index += backward ? -1 : 1;
      }
      if (index >= 0 && index < raw.length) {
        raw = raw.replaceRange(index, index + 1, '');
        selection = TextSelection.collapsed(offset: backward ? index : caret);
      }
    }
    final digits = raw.replaceAll(separators, '');
    final out = StringBuffer();
    final offsets = [0];
    var i = 0;
    for (final char in pattern.split('')) {
      if (i == digits.length) break;
      if (char == '#') {
        out.write(digits[i++]);
        offsets.add(out.length);
      } else {
        out.write(char);
      }
    }
    int offset(int position) {
      if (position < 0) return out.length;
      final left = raw
          .substring(0, position.clamp(0, raw.length))
          .replaceAll(separators, '')
          .length;
      return offsets[left.clamp(0, i)];
    }

    return TextEditingValue(
      text: out.toString(),
      selection: TextSelection(
        baseOffset: offset(selection.baseOffset),
        extentOffset: offset(selection.extentOffset),
        affinity: selection.affinity,
        isDirectional: selection.isDirectional,
      ),
    );
  }
}

bool validCpf(String value) {
  final s = value.replaceAll(RegExp(r'\D'), '');
  if (s.length != 11 || s.split('').toSet().length == 1) return false;
  for (final n in [9, 10]) {
    var sum = 0;
    for (var i = 0; i < n; i++) {
      sum += int.parse(s[i]) * (n + 1 - i);
    }
    if ((sum * 10 % 11) % 10 != int.parse(s[n])) return false;
  }
  return true;
}

class SignupPage extends StatefulWidget {
  const SignupPage({super.key, required this.api});
  final DriverApi api;
  @override
  State<SignupPage> createState() => _SignupState();
}

class _SignupState extends State<SignupPage> {
  final phone = TextEditingController(),
      name = TextEditingController(),
      cpf = TextEditingController(),
      email = TextEditingController(),
      password = TextEditingController(),
      confirmation = TextEditingController();
  int step = 0;
  bool busy = false;
  String? error, id;
  Iterable<TextEditingController> get fields => [
    phone,
    name,
    cpf,
    email,
    password,
    confirmation,
  ];
  @override
  void dispose() {
    for (final field in fields) {
      field.dispose();
    }
    super.dispose();
  }

  String? validate() {
    switch (step) {
      case 0:
        if (!RegExp(r'^[1-9]{2}9\d{8}$')
            .hasMatch(phone.text.replaceAll(RegExp(r'\D'), ''))) {
          return 'Informe o celular com DDD e nove dígitos.';
        }
      case 1:
        if (name.text.trim().isEmpty || name.text.trim().length > 160) {
          return 'Informe o nome completo.';
        }
        if (!validCpf(cpf.text)) return 'Confira o CPF.';
      case 2:
        if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$')
                .hasMatch(email.text.trim()) ||
            email.text.trim().length > 254) {
          return 'Confira o e-mail.';
        }
      case 3:
        final passwordError = newDriverPasswordError(password.text);
        if (passwordError != null) {
          return passwordError;
        }
        if (password.text != confirmation.text) {
          return 'As senhas precisam coincidir.';
        }
    }
    return null;
  }

  void back() {
    if (busy) return;
    if (step == 0 || id != null) {
      Navigator.pop(context);
    } else {
      setState(() {
        if (step == 3) {
          password.clear();
          confirmation.clear();
        }
        step--;
        error = null;
      });
    }
  }

  Future<void> next() async {
    if (busy || id != null) return;
    final issue = validate();
    setState(() => error = issue);
    if (issue != null) return;
    FocusScope.of(context).unfocus();
    if (step < 3) {
      setState(() => step++);
      return;
    }
    setState(() => busy = true);
    try {
      final result = await widget.api.cadastrar({
        'nome': name.text.trim(),
        'cpf': cpf.text.replaceAll(RegExp(r'\D'), ''),
        'celular': '+55${phone.text.replaceAll(RegExp(r'\D'), '')}',
        'email': email.text.trim(),
        'senha': password.text,
      });
      if (!mounted) return;
      for (final field in fields) {
        field.clear();
      }
      id = result;
    } on DriverFailure catch (e) {
      error = e.message;
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Widget field(
    String label,
    TextEditingController controller, {
    String? mask,
    TextInputType? type,
  }) => TextField(
    key: ValueKey(label),
    controller: controller,
    enabled: !busy,
    autocorrect: false,
    enableSuggestions: false,
    keyboardType: type,
    inputFormatters: mask == null ? null : [DigitMask(mask)],
    decoration: InputDecoration(
      labelText: label,
      prefixText: controller == phone ? '+55 ' : null,
    ),
  );
  @override
  Widget build(BuildContext context) => PopScope(
    canPop: !busy && (step == 0 || id != null),
    onPopInvokedWithResult: (popped, result) {
      if (!popped && !busy) back();
    },
    child: Scaffold(
      appBar: AppBar(
        title: const Text('Criar conta'),
        leading: IconButton(
          tooltip: 'Voltar',
          icon: const Icon(Icons.arrow_back),
          onPressed: busy ? null : back,
        ),
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            Text(
              id == null ? 'Etapa ${step + 1} de 4' : 'Conta criada',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 20),
            if (id != null) ...[
              const Text('Número de cadastro'),
              SelectableText(id!),
              const Text(
                'Seu cadastro está pendente de verificações, documentos e análise administrativa.',
              ),
              FilledButton(
                onPressed: () =>
                    Navigator.pushReplacementNamed(context, '/entrar'),
                child: const Text('Ir para o login'),
              ),
            ] else ...[
              if (step == 0)
                field(
                  'Celular com DDD',
                  phone,
                  mask: '(##) #####-####',
                  type: TextInputType.phone,
                ),
              if (step == 1) ...[
                field('Nome completo', name),
                field(
                  'CPF',
                  cpf,
                  mask: '###.###.###-##',
                  type: TextInputType.phone,
                ),
              ],
              if (step == 2)
                field('E-mail', email, type: TextInputType.emailAddress),
              if (step == 3) ...[
                SensitiveField(
                  label: 'Nova senha',
                  controller: password,
                  enabled: !busy,
                  helperText: driverPasswordHint,
                ),
                SensitiveField(
                  label: 'Repetir a senha',
                  controller: confirmation,
                  enabled: !busy,
                ),
              ],
              if (error != null)
                Semantics(liveRegion: true, child: Text(error!)),
              const SizedBox(height: 24),
              FilledButton(
                onPressed: busy ? null : next,
                child: Text(
                  busy
                      ? 'Enviando…'
                      : step == 3
                      ? 'Criar minha conta'
                      : 'Continuar',
                ),
              ),
            ],
          ],
        ),
      ),
    ),
  );
}
