import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import 'app_colors.dart';

enum StashThemeMode { light, dark, amoled, system }

class AppTheme {
  AppTheme._();

  static ThemeData get light => _build(
        brightness: Brightness.light,
        background: AppColors.surfaceLight,
        surface: AppColors.cardLight,
      );

  static ThemeData get dark => _build(
        brightness: Brightness.dark,
        background: AppColors.charcoal,
        surface: AppColors.charcoalLight,
      );

  static ThemeData get amoled => _build(
        brightness: Brightness.dark,
        background: Colors.black,
        surface: AppColors.deepBlack,
      );

  static ThemeData _build({
    required Brightness brightness,
    required Color background,
    required Color surface,
  }) {
    final isDark = brightness == Brightness.dark;
    final colorScheme = ColorScheme(
      brightness: brightness,
      primary: AppColors.purpleAccent,
      onPrimary: Colors.white,
      secondary: AppColors.orangeAccent,
      onSecondary: Colors.white,
      error: const Color(0xFFEF4444),
      onError: Colors.white,
      surface: surface,
      onSurface: isDark ? Colors.white : AppColors.deepBlack,
    );

    final textTheme = GoogleFonts.interTextTheme(
      isDark ? ThemeData.dark().textTheme : ThemeData.light().textTheme,
    );

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: background,
      textTheme: textTheme,
      cardTheme: CardThemeData(
        color: surface,
        // A flat elevation: 0 card is indistinguishable from the page
        // background when the two colors are this close, which is what
        // made every section look flat. A real (if subtle) drop shadow
        // gives cards the "lifted" look instead.
        elevation: isDark ? 6 : 3,
        shadowColor: Colors.black.withValues(alpha: isDark ? 0.6 : 0.18),
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        clipBehavior: Clip.antiAlias,
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: background,
        foregroundColor: isDark ? Colors.white : AppColors.deepBlack,
        elevation: 0,
        centerTitle: false,
        titleTextStyle: textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w700),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: surface,
        indicatorColor: AppColors.purpleAccent.withValues(alpha: 0.18),
        elevation: isDark ? 6 : 3,
        shadowColor: Colors.black.withValues(alpha: isDark ? 0.6 : 0.18),
        surfaceTintColor: Colors.transparent,
      ),
      floatingActionButtonTheme: const FloatingActionButtonThemeData(
        backgroundColor: AppColors.purpleAccent,
        foregroundColor: Colors.white,
      ),
      chipTheme: ChipThemeData(
        backgroundColor: surface,
        labelStyle: textTheme.labelMedium,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
          side: BorderSide(color: (isDark ? Colors.white : Colors.black).withValues(alpha: 0.08)),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: surface,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide.none,
        ),
      ),
    );
  }
}
