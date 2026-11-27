import 'dart:convert';

import 'package:flutter/services.dart';

import 'driver_api.dart';

class SessionStore {
  SessionStore(this.origin);
  final String origin;
  static const channel = MethodChannel('br.fairgo.driver/session');
  Future<void> save(Grant grant) => channel.invokeMethod<void>(
    'save',
    jsonEncode({'origin': origin, ...grant.toJson()}),
  );
  Future<Grant?> read() async {
    final value = await channel.invokeMethod<String>('read');
    if (value == null) return null;
    final data = jsonDecode(value) as Map<String, dynamic>;
    if (data['origin'] != origin) return null;
    return Grant.fromJson(data);
  }

  Future<void> clear() => channel.invokeMethod<void>('clear');
}
