import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/driver_api.dart';

class DigitMask extends TextInputFormatter {
  DigitMask(this.pattern);
  final String pattern;
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final digits = newValue.text.replaceAll(RegExp(r'\D'), '');
    final before = newValue.selection.end.clamp(0, newValue.text.length);
    final left = newValue.text
        .substring(0, before)
        .replaceAll(RegExp(r'\D'), '')
        .length;
    final out = StringBuffer();
    var i = 0, cursor = 0;
    for (final char in pattern.split('')) {
      if (i == digits.length) break;
      if (char == '#') {
        out.write(digits[i++]);
      } else {
        out.write(char);
      }
      if (i <= left) cursor = out.length;
    }
    return TextEditingValue(
      text: out.toString(),
      selection: TextSelection.collapsed(offset: cursor),
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
        if (password.text.trim().isEmpty ||
            password.text.length < 15 ||
            password.text.length > 128) {
          return 'Use uma senha de 15 a 128 caracteres.';
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
    bool secret = false,
  }) => TextField(
    key: ValueKey(label),
    controller: controller,
    enabled: !busy,
    obscureText: secret,
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
                  type: TextInputType.number,
                ),
              ],
              if (step == 2)
                field('E-mail', email, type: TextInputType.emailAddress),
              if (step == 3) ...[
                field('Senha (15 a 128 caracteres)', password, secret: true),
                field('Confirmar senha', confirmation, secret: true),
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
