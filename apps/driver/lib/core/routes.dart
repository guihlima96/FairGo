import 'package:flutter/widgets.dart';

import '../features/access/access_page.dart';
import '../features/welcome/welcome_page.dart';

abstract final class DriverRoutes {
  static const home = '/';
  static const login = '/entrar';
  static const signup = '/criar-conta';

  static final pages = <String, WidgetBuilder>{
    home: (_) => const WelcomePage(),
    login: (_) => const AccessPage(
      title: 'Entrar',
      message: 'O acesso � conta estar� dispon�vel em uma pr�xima etapa.',
    ),
    signup: (_) => const AccessPage(
      title: 'Criar conta',
      message:
          'O cadastro de motoristas estar� dispon�vel em uma pr�xima etapa.',
    ),
  };
}
