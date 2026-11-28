const driverPasswordHint = 'Use uma senha de 8 a 32 caracteres.';

// @Size conta unidades UTF-16. O @NotBlank do backend usa String.trim()
// apenas para verificar se há conteúdo; a senha enviada é a original.
final _onlyWhitespace = RegExp(r'^[\x00-\x20]*$');

bool validNewDriverPassword(String value) =>
    value.length >= 8 && value.length <= 32 && !_onlyWhitespace.hasMatch(value);
