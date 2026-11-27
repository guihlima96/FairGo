import 'package:material_ui/material_ui.dart';

const navy = Color(0xff14233e);
const background = Color(0xff101a2c);
const electricBlue = Color(0xff1854df);

final driverTheme = ThemeData(
  useMaterial3: true,
  scaffoldBackgroundColor: background,
  colorScheme: const ColorScheme.dark(
    primary: electricBlue,
    onPrimary: Colors.white,
    surface: background,
    onSurface: Colors.white,
  ),
  fontFamily: 'Roboto',
  textTheme: const TextTheme(
    displaySmall: TextStyle(
      color: Colors.white,
      fontSize: 40,
      fontWeight: FontWeight.w700,
      letterSpacing: -1.6,
    ),
    titleLarge: TextStyle(color: Colors.white, fontWeight: FontWeight.w600),
  ),
  appBarTheme: const AppBarTheme(
    backgroundColor: background,
    foregroundColor: Colors.white,
  ),
  filledButtonTheme: FilledButtonThemeData(
    style: FilledButton.styleFrom(
      minimumSize: const Size.fromHeight(54),
      textStyle: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
    ),
  ),
  outlinedButtonTheme: OutlinedButtonThemeData(
    style: OutlinedButton.styleFrom(
      minimumSize: const Size.fromHeight(54),
      foregroundColor: Colors.white,
      side: const BorderSide(color: Color(0xffb9c6da)),
      textStyle: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
    ),
  ),
);
