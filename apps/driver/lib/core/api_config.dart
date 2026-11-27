import 'package:flutter/foundation.dart';

/// Origem do backend. O catálogo de recursos deste cliente é exclusivo do Driver.
class DriverApiConfig {
  const DriverApiConfig._(this.base);
  final Uri? base;

  factory DriverApiConfig.fromEnvironment() => DriverApiConfig.parse(
    const String.fromEnvironment('FAIRGO_API_ORIGIN'),
    development: kDebugMode,
  );

  factory DriverApiConfig.parse(String value, {required bool development}) {
    if (value.isEmpty) return const DriverApiConfig._(null);
    final uri = Uri.tryParse(value);
    if (uri == null ||
        !uri.hasAuthority ||
        uri.host.isEmpty ||
        uri.authority.contains('@') ||
        uri.hasQuery ||
        uri.hasFragment ||
        (uri.path.isNotEmpty && uri.path != '/')) {
      throw const FormatException('Informe somente a origem da API Driver.');
    }
    final usb =
        development &&
        uri.scheme == 'http' &&
        uri.host == '127.0.0.1' &&
        uri.port == 8080;
    if (uri.scheme != 'https' && !usb) {
      throw const FormatException(
        'A API exige HTTPS fora do desenvolvimento por USB.',
      );
    }
    return DriverApiConfig._(uri.resolve('/api/driver/'));
  }

  Uri? get perfil => base?.resolve('me');
}
