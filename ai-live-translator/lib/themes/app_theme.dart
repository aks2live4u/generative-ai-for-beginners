import 'package:flutter/material.dart';

/// Deep navy/violet dark theme (matching the app's icon and marketing
/// design) with a light counterpart for users who prefer it. Large text
/// and high contrast throughout, per the PRD's accessibility goals.
class AppTheme {
  AppTheme._();

  /// Gradient used behind the mic button and primary CTAs.
  static const List<Color> heroGradient = [Color(0xFF8B5CF6), Color(0xFF2563EB)];

  static ThemeData get light => _build(_lightScheme);
  static ThemeData get dark => _build(_darkScheme);

  static const _darkScheme = ColorScheme(
    brightness: Brightness.dark,
    primary: Color(0xFF8B5CF6),
    onPrimary: Colors.white,
    secondary: Color(0xFF22D3EE),
    onSecondary: Color(0xFF042229),
    tertiary: Color(0xFF34D399),
    onTertiary: Color(0xFF04241A),
    error: Color(0xFFEF4444),
    onError: Colors.white,
    errorContainer: Color(0xFF3B1220),
    onErrorContainer: Color(0xFFFFD9D9),
    surface: Color(0xFF0A0E27),
    onSurface: Color(0xFFE7E9F5),
    surfaceContainerHigh: Color(0xFF171C3F),
    surfaceContainerHighest: Color(0xFF1E2348),
    onSurfaceVariant: Color(0xFF9AA1C4),
    outline: Color(0xFF3A3F63),
    outlineVariant: Color(0xFF272C50),
  );

  static const _lightScheme = ColorScheme(
    brightness: Brightness.light,
    primary: Color(0xFF6D28D9),
    onPrimary: Colors.white,
    secondary: Color(0xFF0E7490),
    onSecondary: Colors.white,
    tertiary: Color(0xFF059669),
    onTertiary: Colors.white,
    error: Color(0xFFDC2626),
    onError: Colors.white,
    errorContainer: Color(0xFFFEE2E2),
    onErrorContainer: Color(0xFF7F1D1D),
    surface: Color(0xFFF7F7FC),
    onSurface: Color(0xFF14172E),
    surfaceContainerHigh: Colors.white,
    surfaceContainerHighest: Color(0xFFEDEEF9),
    onSurfaceVariant: Color(0xFF5B5F81),
    outline: Color(0xFFD8DAEE),
    outlineVariant: Color(0xFFE7E8F5),
  );

  static ThemeData _build(ColorScheme scheme) {
    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: scheme.surface,
      textTheme: const TextTheme(
        headlineMedium: TextStyle(fontWeight: FontWeight.w800, fontSize: 28),
        titleLarge: TextStyle(fontWeight: FontWeight.w600, fontSize: 22),
        bodyLarge: TextStyle(fontSize: 18),
        bodyMedium: TextStyle(fontSize: 16),
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: scheme.surface,
        foregroundColor: scheme.onSurface,
        elevation: 0,
        centerTitle: true,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          minimumSize: const Size.fromHeight(56),
          textStyle: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
        ),
      ),
      cardTheme: CardThemeData(
        elevation: 0,
        color: scheme.surfaceContainerHigh,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
          side: BorderSide(color: scheme.outlineVariant),
        ),
      ),
    );
  }
}

/// Per-section accent colors for [MessageCard] — Original (native-language
/// cyan/teal), Pronunciation (violet), Meaning/Translation (green). Kept
/// separate from [ColorScheme] because Material only exposes one primary/
/// secondary/tertiary triplet and these three need to stay visually
/// distinct and consistent regardless of the app's primary color.
class AppAccents {
  const AppAccents({
    required this.original,
    required this.pronunciation,
    required this.meaning,
  });

  final Color original;
  final Color pronunciation;
  final Color meaning;

  static const dark = AppAccents(
    original: Color(0xFF38BDF8),
    pronunciation: Color(0xFFC4B5FD),
    meaning: Color(0xFF4ADE80),
  );

  static const light = AppAccents(
    original: Color(0xFF0369A1),
    pronunciation: Color(0xFF7C3AED),
    meaning: Color(0xFF15803D),
  );

  static AppAccents of(BuildContext context) =>
      Theme.of(context).brightness == Brightness.dark ? dark : light;
}
