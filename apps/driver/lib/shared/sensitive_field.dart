import 'package:material_ui/material_ui.dart';

class SensitiveField extends StatefulWidget {
  const SensitiveField({
    super.key,
    required this.label,
    required this.controller,
    required this.enabled,
    this.onSubmitted,
    this.helperText,
    this.border,
  });

  final String label;
  final TextEditingController controller;
  final bool enabled;
  final ValueChanged<String>? onSubmitted;
  final String? helperText;
  final InputBorder? border;

  @override
  State<SensitiveField> createState() => _SensitiveFieldState();
}

class _SensitiveFieldState extends State<SensitiveField> {
  bool hidden = true;

  @override
  Widget build(BuildContext context) => TextField(
    controller: widget.controller,
    enabled: widget.enabled,
    obscureText: hidden,
    keyboardType: TextInputType.visiblePassword,
    autocorrect: false,
    enableSuggestions: false,
    enableIMEPersonalizedLearning: false,
    onSubmitted: widget.onSubmitted,
    decoration: InputDecoration(
      labelText: widget.label,
      helperText: widget.helperText,
      border: widget.border,
      suffixIcon: IconButton(
        tooltip:
            '${hidden ? 'Mostrar' : 'Ocultar'} ${widget.label.toLowerCase()}',
        icon: Icon(hidden ? Icons.visibility : Icons.visibility_off),
        onPressed: widget.enabled
            ? () => setState(() => hidden = !hidden)
            : null,
      ),
    ),
  );
}
