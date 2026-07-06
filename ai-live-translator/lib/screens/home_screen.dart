import 'dart:ui' show ImageFilter;

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../services/conversation_controller.dart';
import '../services/secure_storage_service.dart';
import '../themes/app_theme.dart';
import '../widgets/glass.dart';
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
    final scheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: GlassAppBar(
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
              Container(
                width: 112,
                height: 112,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(28),
                  boxShadow: [
                    BoxShadow(
                      color: AppTheme.heroGradient.first.withValues(alpha: 0.4),
                      blurRadius: 24,
                      offset: const Offset(0, 10),
                    ),
                  ],
                ),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(28),
                  child: Image.asset('assets/icon/icon.png'),
                ),
              ),
              const SizedBox(height: 20),
              ShaderMask(
                shaderCallback: (bounds) => LinearGradient(
                  colors: AppTheme.heroGradient,
                ).createShader(bounds),
                child: Text(
                  'AI Live Translator',
                  textAlign: TextAlign.center,
                  style: Theme.of(context)
                      .textTheme
                      .headlineMedium
                      ?.copyWith(color: Colors.white),
                ),
              ),
              const SizedBox(height: 12),
              GlassContainer(
                borderRadius: 999,
                blurSigma: 12,
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: Text(
                  '${controller.english.name} ⇄ ${controller.nativeLanguage.name}',
                  style: Theme.of(context)
                      .textTheme
                      .bodyLarge
                      ?.copyWith(color: scheme.onSurfaceVariant),
                ),
              ),
              const SizedBox(height: 48),
              if (!_checkingKey && !_hasApiKey)
                Padding(
                  padding: const EdgeInsets.only(bottom: 20),
                  child: GlassContainer(
                    tint: scheme.errorContainer,
                    opacity: 0.55,
                    padding: const EdgeInsets.all(16),
                    child: Text(
                      'Add your OpenAI API key in Settings before you start.',
                      style: TextStyle(color: scheme.onErrorContainer),
                      textAlign: TextAlign.center,
                    ),
                  ),
                ),
              _StartButton(
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
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _StartButton extends StatelessWidget {
  const _StartButton({required this.onPressed});
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(16),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 18, sigmaY: 18),
        child: Ink(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: AppTheme.heroGradient
                  .map((c) => c.withValues(alpha: 0.75))
                  .toList(),
            ),
            border: Border.all(color: Colors.white.withValues(alpha: 0.25)),
          ),
          child: InkWell(
            onTap: onPressed,
            child: const SizedBox(
              height: 56,
              width: double.infinity,
              child: Center(
                child: Text(
                  'Start Conversation',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
