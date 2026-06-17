import 'package:flutter/material.dart';

class AppColors {
  AppColors._();

  static const deepBlack = Color(0xFF08080B);
  static const charcoal = Color(0xFF161618);
  static const charcoalLight = Color(0xFF222226);
  static const purpleAccent = Color(0xFF8B5CF6);
  static const orangeAccent = Color(0xFFF97316);
  static const surfaceLight = Color(0xFFF6F5F8);
  static const cardLight = Color(0xFFFFFFFF);

  static const accentGradient = LinearGradient(
    colors: [purpleAccent, orangeAccent],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );
}
