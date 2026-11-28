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
  DriverApi(this.config, {this.timeout = const Duration(seconds: 20)});
  final Duration timeout;
  final DriverApiConfig config;

  Future<Map<String, dynamic>> _request(
    String method,
    String path, {
    String? token,
    Map<String, String>? body,
  }) async {
    final base = config.base;
    if (base == null) {
      throw const DriverFailure('Configure a conexão com o FairGo.');
    }
    final client = HttpClient();
    Future<Map<String, dynamic>> send() async {
      final request = await client.openUrl(method, base.resolve(path));
      request.followRedirects = false;
      if (token != null) {
        request.headers.set(HttpHeaders.authorizationHeader, 'Bearer $token');
      }
      if (body != null) {
        request.headers.contentType = ContentType.json;
        request.add(utf8.encode(jsonEncode(body)));
      }
      final response = await request.close();
      if (path == 'contas') {
        switch (response.statusCode) {
          case 201:
            final data = jsonDecode(await utf8.decoder.bind(response).join());
            if (data is Map<String, dynamic> &&
                data['situacao'] == 'PENDENTE' &&
                data['id'] is String &&
                RegExp(
                  r'^[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$',
                ).hasMatch(data['id'] as String)) {
              return data;
            }
            throw const FormatException();
          case 400:
            throw const DriverFailure('Revise os dados informados.', 400);
          case 409:
            throw const DriverFailure(
              'Há um conflito com um cadastro existente.',
              409,
            );
          case 429:
            break;
          default:
            throw const FormatException();
        }
      }
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
                ? 'Identificador ou senha inválidos.'
                : 'Sessão encerrada. Entre novamente.',
            response.statusCode,
          );
        case 429:
          throw const DriverFailure(
            'Limite de tentativas atingido. Aguarde antes de tentar novamente.',
            429,
          );
        default:
          throw const DriverFailure('Servidor indisponível. Tente novamente.');
      }
    }

    try {
      return await send().timeout(timeout);
    } on SocketException {
      throw const DriverFailure(
        'Sem conexão com o servidor. Confira a rede ou o cabo USB.',
      );
    } on TimeoutException {
      throw const DriverFailure(
        'O servidor demorou para responder. Tente novamente.',
      );
    } finally {
      client.close(force: true);
    }
  }

  Future<String> cadastrar(Map<String, String> dados) async {
    try {
      return (await _request('POST', 'contas', body: dados))['id'] as String;
    } on DriverFailure catch (error) {
      if (error.status != null || config.base == null) rethrow;
    } on IOException {
      // A conexão pode cair após o servidor gravar a conta.
    } on FormatException {
      // Uma resposta incompleta não confirma a criação.
    }
    throw const DriverFailure(
      'Não foi possível confirmar o resultado. Confira a conexão e tente entrar antes de enviar outro cadastro.',
    );
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
