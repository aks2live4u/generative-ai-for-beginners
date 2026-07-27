import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/theme/app_theme.dart';
import 'data/models/settings_model.dart';
import 'presentation/providers/settings_provider.dart';
import 'presentation/screens/splash_screen.dart';

class PomPomApp extends ConsumerWidget {
  const PomPomApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings = ref.watch(settingsControllerProvider);
    final themeMode = switch (settings.themeMode) {
      AppThemeMode.light => ThemeMode.light,
      AppThemeMode.dark => ThemeMode.dark,
      AppThemeMode.system => ThemeMode.system,
    };

    return MaterialApp(
      title: 'PomPom',
      debugShowCheckedModeBanner: false,
      themeMode: themeMode,
      theme: AppTheme.light(settings.palette),
      darkTheme: AppTheme.dark(settings.palette),
      home: const SplashScreen(),
    );
  }
}
