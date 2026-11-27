import 'package:flutter/widgets.dart';

import 'api_config.dart';
import 'driver_api.dart';
import 'session_store.dart';
import '../features/access/login_page.dart';

import '../features/access/access_page.dart';
import '../features/welcome/welcome_page.dart';

abstract final class DriverRoutes {
  static const home = '/';
  static const login = '/entrar';
  static const signup = '/criar-conta';

  static final pages = <String, WidgetBuilder>{
    home: (_) => const WelcomePage(),
    login: (_) {
      final config = DriverApiConfig.fromEnvironment();
      return LoginPage(
        api: DriverApi(config),
        store: SessionStore(config.base?.toString() ?? ''),
      );
    },
    signup: (_) => const AccessPage(
      title: 'Criar conta',
      message:
          'O cadastro de motoristas estar� dispon�vel em uma pr�xima etapa.',
    ),
  };
}
