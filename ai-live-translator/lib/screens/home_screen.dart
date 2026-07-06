import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../services/conversation_controller.dart';
import '../services/secure_storage_service.dart';
import 'conversation_screen.dart';
import 'settings_screen.dart';

/// Deliberately minimal: a title, the language pair, and one button.
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
    final controller = context.watch<ConversationController>();
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            onPressed: () async {
              await Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const SettingsScreen()),
              );
              _checkApiKey();
            },
            icon: const Icon(Icons.settings_outlined),
            tooltip: 'Settings',
          ),
        ],
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('AI Translator', style: Theme.of(context).textTheme.headlineMedium),
              const SizedBox(height: 8),
              Text(
                '${controller.english.name} ↔ ${controller.nativeLanguage.name}',
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
                  // Each conversation is ephemeral: once you leave it, it's gone.
                  if (context.mounted) {
                    context.read<ConversationController>().clearConversation();
                  }
                  _checkApiKey();
                },
                child: const Text('Start Conversation'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
