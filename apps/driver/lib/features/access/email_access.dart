import 'dart:async';

import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/driver_api.dart';
import '../../shared/driver_mark.dart';

class VerificationPanel extends StatefulWidget {
  const VerificationPanel({
    super.key,
    required this.api,
    required this.token,
    required this.refresh,
    required this.expired,
  });
  final DriverApi api;
  final String token;
  final Future<void> Function() refresh, expired;
  @override
  State<VerificationPanel> createState() => _VerificationState();
}

class _VerificationState extends State<VerificationPanel> {
  final code = TextEditingController();
  String? challenge, notice;
  bool busy = false, needsRefresh = false;
  DateTime? issued;
  Timer? ticker;
  int get seconds => issued == null
      ? 0
      : (60 - DateTime.now().difference(issued!).inSeconds).clamp(0, 60);
  Future<void> refresh() async {
    needsRefresh = true;
    try {
      await widget.refresh();
      needsRefresh = false;
    } on DriverFailure catch (e) {
      if (e.status == 401) {
        await widget.expired();
      } else {
        notice = 'Consulte novamente a situação do e-mail.';
      }
    }
  }

  Future<void> act({bool confirm = false}) async {
    if (busy) return;
    setState(() {
      busy = true;
      notice = null;
    });
    try {
      if (needsRefresh) {
        await refresh();
        return;
      }
      if (confirm) {
        if (!RegExp(r'^\d{8}$').hasMatch(code.text)) {
          notice = 'Informe os oito dígitos.';
          return;
        }
        await widget.api.confirmarCodigo(widget.token, challenge!, code.text);
        if (!mounted) return;
        code.clear();
        await refresh();
      } else {
        if (seconds > 0) return;
        issued = DateTime.now();
        code.clear();
        challenge = null;
        ticker?.cancel();
        ticker = Timer.periodic(const Duration(seconds: 1), (timer) {
          if (mounted) setState(() {});
          if (seconds == 0) timer.cancel();
        });
        final id = await widget.api.solicitarCodigo(widget.token);
        if (mounted) challenge = id;
      }
    } on DriverFailure catch (e) {
      if (!mounted) return;
      notice = e.message;
      if (e.status == 401) {
        await widget.expired();
      } else if (e.status != 400 && e.status != 429) {
        await refresh();
      }
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  void dispose() {
    ticker?.cancel();
    code.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      const Text('E-mail pendente de verificação'),
      if (challenge != null) ...[
        TextField(
          controller: code,
          enabled: !busy,
          keyboardType: TextInputType.number,
          inputFormatters: [
            FilteringTextInputFormatter.digitsOnly,
            LengthLimitingTextInputFormatter(8),
          ],
          decoration: const InputDecoration(
            labelText: 'Código recebido por e-mail',
            helperText: '8 dígitos · válido por 10 minutos',
            border: OutlineInputBorder(),
          ),
        ),
        FilledButton(
          onPressed: busy || needsRefresh ? null : () => act(confirm: true),
          child: const Text('Confirmar código'),
        ),
      ],
      TextButton(
        onPressed: busy || (!needsRefresh && seconds > 0) ? null : () => act(),
        child: Text(
          needsRefresh
              ? 'Consultar situação'
              : seconds > 0
              ? 'Reenviar em ${seconds}s'
              : challenge == null
              ? 'Verificar e-mail'
              : 'Reenviar código',
        ),
      ),
      if (busy) const LinearProgressIndicator(),
      if (notice != null) Semantics(liveRegion: true, child: Text(notice!)),
    ],
  );
}

class RecoveryPage extends StatefulWidget {
  const RecoveryPage({super.key, required this.api});
  final DriverApi api;
  @override
  State<RecoveryPage> createState() => _RecoveryState();
}

class _RecoveryState extends State<RecoveryPage> {
  final email = TextEditingController(),
      token = TextEditingController(),
      password = TextEditingController(),
      confirmation = TextEditingController();
  bool reset = false, busy = false;
  String? notice;
  Future<void> submit() async {
    if (busy) return;
    setState(() {
      busy = true;
      notice = null;
    });
    try {
      if (!reset) {
        if (!RegExp(r'^[^@\s]+@[^@\s]+$').hasMatch(email.text.trim())) {
          notice = 'Confira o e-mail.';
          return;
        }
        await widget.api.recuperar(email.text.trim());
        if (!mounted) return;
        reset = true;
        email.clear();
      } else {
        if (!RegExp(r'^rec_[0-9a-f]{64}$').hasMatch(token.text.trim())) {
          notice = 'Confira o token recebido por e-mail.';
          return;
        }
        if (password.text.trim().isEmpty ||
            password.text.length < 15 ||
            password.text.length > 128) {
          notice = 'Use uma senha de 15 a 128 caracteres.';
          return;
        }
        if (password.text != confirmation.text) {
          notice = 'As senhas precisam coincidir.';
          return;
        }
        await widget.api.redefinir(token.text.trim(), password.text);
        if (mounted) Navigator.pop(context, true);
      }
    } on DriverFailure catch (e) {
      notice = e.status == 400 || e.status == 429
          ? e.message
          : reset
          ? 'Resultado não confirmado. Tente entrar com a nova senha antes de solicitar outra recuperação.'
          : 'Resultado não confirmado. Confira sua caixa de e-mail antes de solicitar novamente.';
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  void dispose() {
    for (final c in [email, token, password, confirmation]) {
      c.dispose();
    }
    super.dispose();
  }

  Widget field(String label, TextEditingController c, {bool secret = false}) =>
      TextField(
        key: ValueKey(label),
        controller: c,
        enabled: !busy,
        obscureText: secret,
        autocorrect: false,
        enableSuggestions: false,
        keyboardType: c == email ? TextInputType.emailAddress : null,
        decoration: InputDecoration(
          labelText: label,
          border: const OutlineInputBorder(),
        ),
      );
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('FairGO Driver')),
    body: SafeArea(
      child: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          const Center(child: DriverMark()),
          const SizedBox(height: 24),
          Text(
            'Recuperar acesso',
            style: Theme.of(context).textTheme.titleLarge,
          ),
          const SizedBox(height: 16),
          if (!reset)
            field('E-mail cadastrado', email)
          else ...[
            const Text(
              'Se o e-mail estiver elegível, você receberá instruções para recuperar o acesso.',
            ),
            field('Token recebido por e-mail', token, secret: true),
            field('Nova senha', password, secret: true),
            field('Confirmar nova senha', confirmation, secret: true),
          ],
          const SizedBox(height: 24),
          FilledButton(
            onPressed: busy ? null : submit,
            child: Text(reset ? 'Redefinir senha' : 'Solicitar instruções'),
          ),
          if (!reset)
            TextButton(
              onPressed: busy
                  ? null
                  : () => setState(() {
                      reset = true;
                      notice = null;
                      email.clear();
                    }),
              child: const Text('Já tenho o token'),
            ),
          if (busy) const LinearProgressIndicator(),
          if (notice != null) Semantics(liveRegion: true, child: Text(notice!)),
        ],
      ),
    ),
  );
}
