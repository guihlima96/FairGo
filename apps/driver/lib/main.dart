import 'package:material_ui/material_ui.dart';

import 'core/api_config.dart';
import 'core/routes.dart';
import 'core/theme.dart';

void main() {
  DriverApiConfig.fromEnvironment();
  runApp(const FairGoDriverApp());
}

class FairGoDriverApp extends StatelessWidget {
  const FairGoDriverApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'FairGo Driver',
    debugShowCheckedModeBanner: false,
    theme: driverTheme,
    routes: DriverRoutes.pages,
  );
}
