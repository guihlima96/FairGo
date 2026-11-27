import 'package:material_ui/material_ui.dart';

class AccessPage extends StatelessWidget {
  const AccessPage({super.key, required this.title, required this.message});
  final String title;
  final String message;

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      leading: IconButton(
        tooltip: 'Voltar',
        icon: const Icon(Icons.arrow_back),
        onPressed: () => Navigator.pop(context),
      ),
      title: Text(title),
    ),
    body: SafeArea(
      child: Padding(padding: const EdgeInsets.all(28), child: Text(message)),
    ),
  );
}
