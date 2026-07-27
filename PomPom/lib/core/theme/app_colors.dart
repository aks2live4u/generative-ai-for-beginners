import 'package:flutter/material.dart';
import '../constants/app_constants.dart';

/// A resolved set of colors for one pastel palette choice, in both
/// light and dark flavors.
class PalettePreset {
  final Color primary;
  final Color primaryDark;
  final Color secondary;
  final Color background;
  final Color backgroundDark;
  final Color surface;
  final Color surfaceDark;

  const PalettePreset({
    required this.primary,
    required this.primaryDark,
    required this.secondary,
    required this.background,
    required this.backgroundDark,
    required this.surface,
    required this.surfaceDark,
  });
}

const Map<PastelPalette, PalettePreset> kPalettePresets = {
  PastelPalette.softPink: PalettePreset(
    primary: Color(0xFFF6A6C1),
    primaryDark: Color(0xFFD98BAA),
    secondary: Color(0xFFFFE1EC),
    background: Color(0xFFFFF5F8),
    backgroundDark: Color(0xFF241A1E),
    surface: Color(0xFFFFFFFF),
    surfaceDark: Color(0xFF32262B),
  ),
  PastelPalette.mintGreen: PalettePreset(
    primary: Color(0xFF9BE0C3),
    primaryDark: Color(0xFF6FBF9E),
    secondary: Color(0xFFDFF7EA),
    background: Color(0xFFF3FBF7),
    backgroundDark: Color(0xFF19231F),
    surface: Color(0xFFFFFFFF),
    surfaceDark: Color(0xFF23302A),
  ),
  PastelPalette.lavender: PalettePreset(
    primary: Color(0xFFC5B3F0),
    primaryDark: Color(0xFF9A82D4),
    secondary: Color(0xFFEAE1FB),
    background: Color(0xFFF8F5FE),
    backgroundDark: Color(0xFF211D2C),
    surface: Color(0xFFFFFFFF),
    surfaceDark: Color(0xFF2C2739),
  ),
  PastelPalette.skyBlue: PalettePreset(
    primary: Color(0xFFA8D8F0),
    primaryDark: Color(0xFF6FB3D9),
    secondary: Color(0xFFE1F3FB),
    background: Color(0xFFF3FAFE),
    backgroundDark: Color(0xFF19222B),
    surface: Color(0xFFFFFFFF),
    surfaceDark: Color(0xFF232F3A),
  ),
  PastelPalette.peach: PalettePreset(
    primary: Color(0xFFF7C4A0),
    primaryDark: Color(0xFFE0996A),
    secondary: Color(0xFFFCE7D6),
    background: Color(0xFFFFF8F2),
    backgroundDark: Color(0xFF261F19),
    surface: Color(0xFFFFFFFF),
    surfaceDark: Color(0xFF332921),
  ),
  PastelPalette.cream: PalettePreset(
    primary: Color(0xFFEBDCB8),
    primaryDark: Color(0xFFCBB784),
    secondary: Color(0xFFF7F0DD),
    background: Color(0xFFFFFCF3),
    backgroundDark: Color(0xFF231F17),
    surface: Color(0xFFFFFFFF),
    surfaceDark: Color(0xFF302B20),
  ),
};

const Color kSuccessGreen = Color(0xFF7FC9A0);
const Color kStreakOrange = Color(0xFFF6A66B);
