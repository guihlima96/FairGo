import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'api_config.dart';

class DriverFailure implements Exception {
  const DriverFailure(this.message, [this.status]);
  final String message;
  final int? status;
}

class Grant {
  Grant(this.token, this.expires);
  final String token;
  final DateTime expires;

  factory Grant.fromJson(Map<String, dynamic> json) => Grant(
    json['token'] as String,
    DateTime.parse(json['expiraEm'] as String),
  );

  Map<String, String> toJson() => {
    'token': token,
    'expiraEm': expires.toUtc().toIso8601String(),
  };
}

class DriverApi {
  DriverApi(this.config);
  final DriverApiConfig config;

  Future<Map<String, dynamic>> _request(
    String method,
    String path, {
    String? token,
    Map<String, String>? body,
  }) async {
    final base = config.base;
    if (base == null)
      throw const DriverFailure('Configure a conex�o com o FairGo.');
    final client = HttpClient();
    Future<Map<String, dynamic>> send() async {
      final request = await client.openUrl(method, base.resolve(path));
      request.followRedirects = false;
      if (token != null)
        request.headers.set(HttpHeaders.authorizationHeader, 'Bearer $token');
      if (body != null) {
        request.headers.contentType = ContentType.json;
        request.add(utf8.encode(jsonEncode(body)));
      }
      final response = await request.close();
      switch (response.statusCode) {
        case 204:
          return {};
        case 200:
          return jsonDecode(await utf8.decoder.bind(response).join())
              as Map<String, dynamic>;
        case 400:
        case 401:
          throw DriverFailure(
            token == null
                ? 'Identificador ou senha inv�lidos.'
                : 'Sess�o encerrada. Entre novamente.',
            response.statusCode,
          );
        case 429:
          throw const DriverFailure(
            'Limite de tentativas atingido. Aguarde antes de tentar novamente.',
            429,
          );
        default:
          throw const DriverFailure('Servidor indispon�vel. Tente novamente.');
      }
    }

    try {
      return await send().timeout(const Duration(seconds: 20));
    } on SocketException {
      throw const DriverFailure(
        'Sem conex�o com o servidor. Confira a rede ou o cabo USB.',
      );
    } on TimeoutException {
      throw const DriverFailure(
        'O servidor demorou para responder. Tente novamente.',
      );
    } finally {
      client.close(force: true);
    }
  }

  Future<Grant> login(String id, String password) async => Grant.fromJson(
    await _request(
      'POST',
      'sessoes',
      body: {'identificador': id, 'senha': password},
    ),
  );
  Future<String> name(String token) async =>
      (await _request('GET', 'me', token: token))['nome'] as String;
  Future<void> logout(String token) async {
    await _request('DELETE', 'sessoes/atual', token: token);
  }
}
