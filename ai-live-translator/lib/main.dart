import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'models/app_settings.dart';
import 'screens/home_screen.dart';
import 'services/conversation_controller.dart';
import 'services/settings_controller.dart';
import 'themes/app_theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final settingsController = SettingsController();
  await settingsController.load();
  runApp(AiLiveTranslatorApp(settingsController: settingsController));
}

class AiLiveTranslatorApp extends StatelessWidget {
  const AiLiveTranslatorApp({super.key, required this.settingsController});

  final SettingsController settingsController;

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider<SettingsController>.value(value: settingsController),
        ChangeNotifierProvider<ConversationController>(
          create: (_) => ConversationController(settings: settingsController.settings),
        ),
      ],
      child: Consumer<SettingsController>(
        builder: (context, settings, _) {
          final mode = switch (settings.settings.themeMode) {
            AppThemeMode.light => ThemeMode.light,
            AppThemeMode.dark => ThemeMode.dark,
            AppThemeMode.auto => ThemeMode.system,
          };
          return MaterialApp(
            title: 'Anyspeak',
            debugShowCheckedModeBanner: false,
            theme: AppTheme.light,
            darkTheme: AppTheme.dark,
            themeMode: mode,
            home: const HomeScreen(),
          );
        },
      ),
    );
  }
}
