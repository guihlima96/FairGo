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
      message: 'O acesso à conta estará disponível em uma próxima etapa.',
    ),
    signup: (_) => const AccessPage(
      title: 'Criar conta',
      message:
          'O cadastro de motoristas estará disponível em uma próxima etapa.',
    ),
  };
}
