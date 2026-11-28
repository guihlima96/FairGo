const driverPasswordHint =
    'Utilize uma senha de 8 a 32 caracteres, sem espaços.';
const driverPasswordWhitespaceError = 'A senha não pode conter espaços.';

// Unicode White_Space e o espaço de largura zero FEFF (BOM).
final _whitespace = RegExp(
  r'[\u0009-\u000D\u0020\u0085\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000\uFEFF]',
);
final _blank = RegExp(r'^[\x00-\x20]*$');

String? newDriverPasswordError(String value) {
  if (_whitespace.hasMatch(value)) return driverPasswordWhitespaceError;
  if (value.length < 8 || value.length > 32 || _blank.hasMatch(value)) {
    return driverPasswordHint;
  }
  return null;
}
