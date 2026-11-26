import 'package:material_ui/material_ui.dart';

const navy = Color(0xff14233e);
const electricBlue = Color(0xff1854df);

final driverTheme = ThemeData(
  useMaterial3: true,
  scaffoldBackgroundColor: Colors.white,
  colorScheme: ColorScheme.fromSeed(seedColor: electricBlue).copyWith(
    primary: electricBlue,
    onPrimary: Colors.white,
    surface: Colors.white,
    onSurface: navy,
  ),
  fontFamily: 'Roboto',
  textTheme: const TextTheme(
    displaySmall: TextStyle(
      color: navy,
      fontSize: 40,
      fontWeight: FontWeight.w700,
      letterSpacing: -1.6,
    ),
    titleLarge: TextStyle(color: navy, fontWeight: FontWeight.w600),
  ),
  appBarTheme: const AppBarTheme(
    backgroundColor: Colors.white,
    foregroundColor: navy,
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
      foregroundColor: navy,
      side: const BorderSide(color: Color(0xffb9c6da)),
      textStyle: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
    ),
  ),
);
