import 'package:fairgo_driver/core/api_config.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('origem opcional e escopo exclusivo do Driver', () {
    expect(DriverApiConfig.parse('', development: false).base, isNull);
    expect(
      DriverApiConfig.parse(
        'https://example.invalid',
        development: false,
      ).perfil.toString(),
      'https://example.invalid/api/driver/me',
    );
    expect(
      DriverApiConfig.parse(
        'http://127.0.0.1:8080',
        development: true,
      ).base.toString(),
      'http://127.0.0.1:8080/api/driver/',
    );
  });
  test('rejeita HTTP fora do USB e origem com credencial ou caminho', () {
    for (final origem in [
      'http://127.0.0.1:8080',
      'https://example.invalid/api/admin',
      'https://teste@example.invalid',
      'postgresql://example.invalid',
      'https://example.invalid?x=1',
    ]) {
      expect(
        () => DriverApiConfig.parse(origem, development: false),
        throwsFormatException,
      );
    }
    expect(
      () => DriverApiConfig.parse('http://example.invalid', development: true),
      throwsFormatException,
    );
  });
}
