import 'package:flutter/material.dart';

import '../services/secure_storage_service.dart';
import 'conversation_screen.dart';
import 'settings_screen.dart';

/// Deliberately minimal: a title, the language pair, and two buttons.
/// No login, no account, no history — one tap to start.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  bool _checkingKey = true;
  bool _hasApiKey = false;

  @override
  void initState() {
    super.initState();
    _checkApiKey();
  }

  Future<void> _checkApiKey() async {
    final has = await SecureStorageService.instance.hasApiKey();
    if (!mounted) return;
    setState(() {
      _hasApiKey = has;
      _checkingKey = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.translate_rounded,
                  size: 72, color: Theme.of(context).colorScheme.primary),
              const SizedBox(height: 16),
              Text('AI Translator', style: Theme.of(context).textTheme.headlineMedium),
              const SizedBox(height: 8),
              Text(
                'English ↔ Telugu',
                style: Theme.of(context)
                    .textTheme
                    .bodyLarge
                    ?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant),
              ),
              const SizedBox(height: 48),
              if (!_checkingKey && !_hasApiKey)
                Padding(
                  padding: const EdgeInsets.only(bottom: 20),
                  child: Card(
                    color: Theme.of(context).colorScheme.errorContainer,
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Text(
                        'Add your OpenAI API key in Settings before you start.',
                        style: TextStyle(
                          color: Theme.of(context).colorScheme.onErrorContainer,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ),
                ),
              ElevatedButton(
                onPressed: () async {
                  await Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => const ConversationScreen()),
                  );
                  _checkApiKey();
                },
                child: const Text('Start Conversation'),
              ),
              const SizedBox(height: 16),
              OutlinedButton(
                onPressed: () async {
                  await Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => const SettingsScreen()),
                  );
                  _checkApiKey();
                },
                style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(56)),
                child: const Text('Settings'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
