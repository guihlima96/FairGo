import 'dart:async';

import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/driver_api.dart';
import '../../core/session_store.dart';
import '../../shared/driver_mark.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key, required this.api, required this.store});
  final DriverApi api;
  final SessionStore store;
  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> with WidgetsBindingObserver {
  final id = TextEditingController();
  final password = TextEditingController();
  bool busy = true, hidden = true;
  String? notice, name;
  Grant? grant;
  Timer? timer;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    run(restore);
  }

  Future<void> run(Future<void> Function() action) async {
    setState(() {
      busy = true;
      notice = null;
    });
    try {
      await action();
    } on DriverFailure catch (error) {
      notice = error.message;
    } on PlatformException {
      notice = 'Não foi possível acessar o armazenamento protegido.';
    } catch (_) {
      notice = 'Não foi possível concluir o acesso. Tente novamente.';
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> forget() async {
    timer?.cancel();
    name = null;
    grant = null;
    await widget.store.clear();
  }

  Future<void> restore() async {
    final saved = await widget.store.read();
    if (saved != null && mounted) await identify(saved);
  }

  Future<void> identify(Grant saved) async {
    if (saved.expires.isAfter(DateTime.now())) {
      try {
        final who = await widget.api.name(saved.token);
        if (!mounted) return;
        final remaining = saved.expires.difference(DateTime.now());
        if (!remaining.isNegative && remaining != Duration.zero) {
          grant = saved;
          name = who;
          timer = Timer(remaining, expire);
          return;
        }
      } on DriverFailure catch (error) {
        if (error.status == 401) await forget();
        rethrow;
      }
    }
    await forget();
    throw const DriverFailure('Sessão expirada. Entre novamente.');
  }

  void expire() {
    if (mounted && !busy) {
      run(() async {
        await forget();
        notice = 'Sessão expirada. Entre novamente.';
      });
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed &&
        grant != null &&
        !grant!.expires.isAfter(DateTime.now())) {
      expire();
    }
  }

  void login() {
    if (busy) return;
    if (id.text.trim().isEmpty || password.text.isEmpty) {
      setState(() => notice = 'Informe seu identificador e sua senha.');
      return;
    }
    run(() async {
      final saved = await widget.api.login(id.text.trim(), password.text);
      if (!mounted) return;
      password.clear();
      hidden = true;
      await widget.store.save(saved);
      if (mounted) await identify(saved);
    });
  }

  void logout() {
    if (busy) return;
    final current = grant!;
    run(() async {
      await forget();
      id.clear();
      try {
        await widget.api.logout(current.token);
      } on DriverFailure catch (error) {
        if (error.status != 401) notice = 'Você saiu deste aparelho. A revogação no servidor não pôde ser confirmada.';
      }
    });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    timer?.cancel();
    id.dispose();
    password.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => PopScope(
    canPop: !busy,
    child: Scaffold(
      appBar: AppBar(
        title: Text(name == null ? 'Entrar' : 'Minha conta'),
        leading: IconButton(
          tooltip: 'Voltar',
          icon: const Icon(Icons.arrow_back),
          onPressed: busy ? null : () => Navigator.pop(context),
        ),
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            const Center(child: DriverMark()),
            const SizedBox(height: 16),
            Text(
              'FairGo Driver',
              style: Theme.of(context).textTheme.displaySmall,
            ),
            const SizedBox(height: 24),
            if (name == null) ...[
              TextField(
                controller: id,
                enabled: !busy,
                autocorrect: false,
                enableSuggestions: false,
                decoration: const InputDecoration(
                  labelText: 'Número de cadastro ou e-mail',
                ),
              ),
              TextField(
                controller: password,
                enabled: !busy,
                obscureText: hidden,
                autocorrect: false,
                enableSuggestions: false,
                onSubmitted: (_) => login(),
                decoration: InputDecoration(
                  labelText: 'Senha',
                  suffixIcon: IconButton(
                    tooltip: hidden ? 'Mostrar senha' : 'Ocultar senha',
                    onPressed: () => setState(() => hidden = !hidden),
                    icon: Icon(
                      hidden ? Icons.visibility : Icons.visibility_off,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 24),
              FilledButton(
                onPressed: busy ? null : login,
                child: const Text('Entrar'),
              ),
            ] else ...[
              Text(name!, style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 24),
              OutlinedButton(
                onPressed: busy ? null : logout,
                child: const Text('Sair'),
              ),
            ],
            if (busy) const LinearProgressIndicator(),
            if (notice != null)
              Semantics(liveRegion: true, child: Text(notice!)),
          ],
        ),
      ),
    ),
  );
}
