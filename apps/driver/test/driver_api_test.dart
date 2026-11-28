import 'dart:convert';
import 'dart:io';

import 'package:fairgo_driver/core/api_config.dart';
import 'package:fairgo_driver/core/driver_api.dart';
import 'package:flutter_test/flutter_test.dart';

// Só este servidor de teste usa uma porta efêmera de loopback.
class TestConfig implements DriverApiConfig {
  TestConfig(this.base);
  @override
  final Uri base;
  @override
  Uri get perfil => base.resolve('me');
}

void main() {
  late HttpServer server;
  late DriverApi api;
  setUp(() async {
    server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    api = DriverApi(
      TestConfig(Uri.parse('http://127.0.0.1:${server.port}/api/driver/')),
    );
  });
  tearDown(() async {
    await server.close(force: true);
  });

  test('contratos de verificação e recuperação', () async {
    const id = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee';
    final paths = <String>[];
    server.listen((req) async {
      paths.add(req.uri.path);
      final protected = req.uri.path.contains('verificacoes');
      expect(
        req.headers.value('Authorization'),
        protected ? 'Bearer fixture' : isNull,
      );
      final body = await utf8.decoder.bind(req).join();
      if (req.uri.path.endsWith('solicitacoes')) {
        req.response.statusCode = 202;
        req.response.write(jsonEncode({'desafio': id}));
      } else if (req.uri.path.endsWith('recuperacao')) {
        expect(jsonDecode(body), {'email': 'teste@example.invalid'});
        req.response.statusCode = 202;
      } else {
        expect(
          jsonDecode(body),
          protected
              ? {'desafio': id, 'codigo': '01234567'}
              : {'token': 'fixture', 'novaSenha': 'senha de teste fictícia'},
        );
        req.response.statusCode = 204;
      }
      await req.response.close();
    });
    expect(await api.solicitarCodigo('fixture'), id);
    await api.confirmarCodigo('fixture', id, '01234567');
    await api.recuperar('teste@example.invalid');
    await api.redefinir('fixture', 'senha de teste fictícia');
    expect(paths.length, 4);
  });
  for (final status in [400, 401, 409, 429, 503, 200]) {
    test('confirmação com HTTP $status preserva falha', () async {
      var calls = 0;
      server.listen((req) async {
        calls++;
        req.response.statusCode = status;
        await req.response.close();
      });
      await expectLater(
        api.confirmarCodigo('fixture', 'desafio', '12345678'),
        throwsA(isA<DriverFailure>().having((e) => e.status, 'status', status)),
      );
      expect(calls, 1);
    });
  }
  for (final status in [201, 400, 409, 429, 200]) {
    test('cadastro HTTP $status', () async {
      server.listen((req) async {
        expect(req.method, 'POST');
        expect(req.uri.path, '/api/driver/contas');
        await req.drain<void>();
        req.response.statusCode = status;
        req.response.write(
          '{"id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","situacao":"PENDENTE"}',
        );
        await req.response.close();
      });
      final result = api.cadastrar({'nome': 'Teste'});
      if (status == 201) {
        expect(await result, 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee');
      } else {
        await expectLater(
          result,
          throwsA(
            isA<DriverFailure>().having(
              (e) => e.status,
              'status',
              status == 200 ? null : status,
            ),
          ),
        );
      }
    });
  }
  test('timeout ou desconexão mantém o resultado incerto', () async {
    var calls = 0;
    server.listen((req) {
      calls++;
    });
    final short = DriverApi(
      api.config,
      timeout: const Duration(milliseconds: 200),
    );
    final unknown = throwsA(
      isA<DriverFailure>().having(
        (e) => e.message,
        'mensagem',
        contains('confirmar o resultado'),
      ),
    );
    await expectLater(short.cadastrar({}), unknown);
    expect(calls, 1);
    await server.close(force: true);
    await expectLater(short.cadastrar({}), unknown);
  });
  test('contrato HTTP do login, perfil e logout', () async {
    final requests = <String>[];
    server.listen((request) async {
      requests.add('${request.method} ${request.uri.path}');
      final response = request.response;
      if (request.method == 'POST') {
        expect(request.headers.value('Authorization'), isNull);
        expect(jsonDecode(await utf8.decoder.bind(request).join()), {
          'identificador': 'teste',
          'senha': 'fictícia',
        });
        response.write(
          jsonEncode({'token': 'ficticio', 'expiraEm': '2030-01-01T12:00:00Z'}),
        );
      } else {
        expect(request.headers.value('Authorization'), 'Bearer ficticio');
        if (request.method == 'DELETE') {
          response.statusCode = 204;
        } else {
          response.write('{"nome":"Teste"}');
        }
      }
      await response.close();
    });
    final grant = await api.login('teste', 'fictícia');
    expect(await api.name(grant.token), 'Teste');
    await api.logout(grant.token);
    expect(requests, [
      'POST /api/driver/sessoes',
      'GET /api/driver/me',
      'DELETE /api/driver/sessoes/atual',
    ]);
  });

  for (final status in [401, 429, 503, 307]) {
    test(
      'trata HTTP $status sem expor resposta nem seguir redirecionamento',
      () async {
        var calls = 0;
        server.listen((request) async {
          calls++;
          request.response.statusCode = status;
          if (status == 307) request.response.headers.set('Location', '/outro');
          request.response.write('informação interna');
          await request.response.close();
        });
        await expectLater(
          api.login('teste', 'fictícia'),
          throwsA(
            isA<DriverFailure>()
                .having(
                  (e) => e.message,
                  'mensagem',
                  isNot(contains('interna')),
                )
                .having(
                  (e) => e.status,
                  'status',
                  status == 401 || status == 429 ? status : null,
                ),
          ),
        );
        expect(calls, 1);
      },
    );
  }
  test('servidor desconectado', () async {
    await server.close(force: true);
    await expectLater(
      api.login('teste', 'fictícia'),
      throwsA(
        isA<DriverFailure>().having(
          (e) => e.message,
          'mensagem',
          contains('Sem conexão'),
        ),
      ),
    );
  });
}
