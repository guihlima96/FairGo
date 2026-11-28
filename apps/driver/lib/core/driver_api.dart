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
    int? expected,
    bool readJson = false,
    String? token,
    Map<String, String>? body,
  }) async {
    final base = config.base;
    if (base == null) {
      throw const DriverFailure('Configure a conexÃ£o com o FairGo.');
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
      if (expected != null) {
        if (response.statusCode == expected) {
          if (!readJson) return {};
          final value = jsonDecode(await utf8.decoder.bind(response).join());
          if (value is Map<String, dynamic>) return value;
          throw const FormatException();
        }
        final status = response.statusCode;
        throw DriverFailure(switch (status) {
          400 => 'Dados inválidos, vencidos ou já utilizados.',
          401 => 'Sessão encerrada. Entre novamente.',
          409 => 'E-mail já verificado.',
          429 => 'Limite atingido. Aguarde antes de tentar novamente.',
          503 => 'Serviço de verificação indisponível.',
          _ => 'Não foi possível confirmar o resultado.',
        }, status);
      }
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
              'HÃ¡ um conflito com um cadastro existente.',
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
                ? 'Identificador ou senha invÃ¡lidos.'
                : 'SessÃ£o encerrada. Entre novamente.',
            response.statusCode,
          );
        case 429:
          throw const DriverFailure(
            'Limite de tentativas atingido. Aguarde antes de tentar novamente.',
            429,
          );
        default:
          throw const DriverFailure('Servidor indisponÃ­vel. Tente novamente.');
      }
    }

    try {
      return await send().timeout(timeout);
    } on FormatException {
      if (expected == null) rethrow;
      throw const DriverFailure('Resposta inesperada do servidor.');
    } on IOException {
      throw const DriverFailure(
        'Sem conexÃ£o com o servidor. Confira a rede ou o cabo USB.',
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
      // A conexÃ£o pode cair apÃ³s o servidor gravar a conta.
    } on FormatException {
      // Uma resposta incompleta nÃ£o confirma a criaÃ§Ã£o.
    }
    throw const DriverFailure(
      'NÃ£o foi possÃ­vel confirmar o resultado. Confira a conexÃ£o e tente entrar antes de enviar outro cadastro.',
    );
  }

  Future<Grant> login(String id, String password) async => Grant.fromJson(
    await _request(
      'POST',
      'sessoes',
      body: {'identificador': id, 'senha': password},
    ),
  );
  Future<({String nome, bool verificado})> perfil(String token) async {
    final data = await _request(
      'GET',
      'me',
      token: token,
      expected: 200,
      readJson: true,
    );
    if (data['nome'] is! String || data['emailVerificado'] is! bool) {
      throw const DriverFailure('Perfil indisponível.');
    }
    return (
      nome: data['nome'] as String,
      verificado: data['emailVerificado'] as bool,
    );
  }

  static const verificacoes = 'me/verificacoes/EMAIL';
  Future<String> solicitarCodigo(String token) async {
    final data = await _request(
      'POST',
      '$verificacoes/solicitacoes',
      token: token,
      expected: 202,
      readJson: true,
    );
    final id = data['desafio'];
    if (id is! String ||
        !RegExp(r'^[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$')
            .hasMatch(id)) {
      throw const DriverFailure('Desafio não confirmado.');
    }
    return id;
  }

  Future<void> confirmarCodigo(
    String token,
    String desafio,
    String codigo,
  ) async {
    await _request(
      'POST',
      '$verificacoes/confirmacao',
      token: token,
      expected: 204,
      body: {'desafio': desafio, 'codigo': codigo},
    );
  }

  Future<void> recuperar(String email) async {
    await _request(
      'POST',
      'recuperacao',
      expected: 202,
      body: {'email': email},
    );
  }

  Future<void> redefinir(String token, String senha) async {
    await _request(
      'POST',
      'recuperacao/confirmacao',
      expected: 204,
      body: {'token': token, 'novaSenha': senha},
    );
  }

  Future<String> name(String token) async =>
      (await _request('GET', 'me', token: token))['nome'] as String;
  Future<void> logout(String token) async {
    await _request('DELETE', 'sessoes/atual', token: token);
  }
}
