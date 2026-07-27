import 'package:flutter/material.dart';
import '../constants/app_constants.dart';
import 'app_colors.dart';

class AppTheme {
  static ThemeData light(PastelPalette palette) => _build(palette, Brightness.light);

  static ThemeData dark(PastelPalette palette) => _build(palette, Brightness.dark);

  static ThemeData _build(PastelPalette palette, Brightness brightness) {
    final preset = kPalettePresets[palette]!;
    final isDark = brightness == Brightness.dark;
    final primary = isDark ? preset.primaryDark : preset.primary;
    final background = isDark ? preset.backgroundDark : preset.background;
    final surface = isDark ? preset.surfaceDark : preset.surface;
    final onBackground = isDark ? const Color(0xFFF3EFEF) : const Color(0xFF3A3238);

    final colorScheme = ColorScheme(
      brightness: brightness,
      primary: primary,
      onPrimary: isDark ? Colors.black : const Color(0xFF3A3238),
      secondary: preset.secondary,
      onSecondary: const Color(0xFF3A3238),
      error: const Color(0xFFE08585),
      onError: Colors.white,
      surface: surface,
      onSurface: onBackground,
    );

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: background,
      fontFamily: 'Quicksand',
      textTheme: _textTheme(onBackground),
      appBarTheme: AppBarTheme(
        backgroundColor: background,
        foregroundColor: onBackground,
        elevation: 0,
        centerTitle: true,
        titleTextStyle: TextStyle(fontFamily: 'Baloo2', fontWeight: FontWeight.w700, fontSize: 22, color: onBackground),
      ),
      cardTheme: CardThemeData(
        color: surface,
        elevation: 0,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        margin: EdgeInsets.zero,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: primary,
          foregroundColor: isDark ? Colors.black : const Color(0xFF3A3238),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
          padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16),
          textStyle: const TextStyle(fontFamily: 'Baloo2', fontWeight: FontWeight.w600, fontSize: 16),
          elevation: 0,
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: onBackground,
          side: BorderSide(color: primary, width: 2),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
          padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 14),
          textStyle: const TextStyle(fontFamily: 'Baloo2', fontWeight: FontWeight.w600, fontSize: 15),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: onBackground.withValues(alpha: 0.7),
          textStyle: const TextStyle(fontFamily: 'Quicksand', fontWeight: FontWeight.w600),
        ),
      ),
      chipTheme: ChipThemeData(
        backgroundColor: preset.secondary,
        selectedColor: primary,
        labelStyle: const TextStyle(fontFamily: 'Quicksand', fontWeight: FontWeight.w600),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        side: BorderSide.none,
      ),
      switchTheme: SwitchThemeData(
        thumbColor: WidgetStateProperty.resolveWith((s) => s.contains(WidgetState.selected) ? primary : null),
        trackColor: WidgetStateProperty.resolveWith(
          (s) => s.contains(WidgetState.selected) ? primary.withValues(alpha: 0.5) : null,
        ),
      ),
      sliderTheme: SliderThemeData(activeTrackColor: primary, thumbColor: primary),
      bottomNavigationBarTheme: BottomNavigationBarThemeData(
        backgroundColor: surface,
        selectedItemColor: isDark ? primary : preset.primaryDark,
        unselectedItemColor: onBackground.withValues(alpha: 0.4),
        type: BottomNavigationBarType.fixed,
        selectedLabelStyle: const TextStyle(fontFamily: 'Quicksand', fontWeight: FontWeight.w700, fontSize: 12),
        unselectedLabelStyle: const TextStyle(fontFamily: 'Quicksand', fontSize: 12),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      ),
      progressIndicatorTheme: ProgressIndicatorThemeData(color: primary),
    );
  }

  static TextTheme _textTheme(Color color) {
    return TextTheme(
      displayLarge: TextStyle(fontFamily: 'Baloo2', fontWeight: FontWeight.w700, fontSize: 64, color: color),
      displayMedium: TextStyle(fontFamily: 'Baloo2', fontWeight: FontWeight.w700, fontSize: 40, color: color),
      headlineMedium: TextStyle(fontFamily: 'Baloo2', fontWeight: FontWeight.w700, fontSize: 26, color: color),
      titleLarge: TextStyle(fontFamily: 'Baloo2', fontWeight: FontWeight.w600, fontSize: 20, color: color),
      titleMedium: TextStyle(fontFamily: 'Quicksand', fontWeight: FontWeight.w600, fontSize: 16, color: color),
      bodyLarge: TextStyle(fontFamily: 'Quicksand', fontWeight: FontWeight.w500, fontSize: 16, color: color),
      bodyMedium: TextStyle(fontFamily: 'Quicksand', fontWeight: FontWeight.w500, fontSize: 14, color: color),
      labelLarge: TextStyle(fontFamily: 'Quicksand', fontWeight: FontWeight.w600, fontSize: 14, color: color),
    );
  }
}
