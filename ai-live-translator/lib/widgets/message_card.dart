import 'package:flutter/material.dart';

import '../models/app_settings.dart';
import '../models/translation_message.dart';

/// Renders one conversation turn as the three sections the PRD requires:
/// Original, Pronunciation (transliteration), and Meaning (translation).
class MessageCard extends StatelessWidget {
  const MessageCard({
    super.key,
    required this.message,
    required this.settings,
    this.onReplay,
  });

  final TranslationMessage message;
  final AppSettings settings;
  final VoidCallback? onReplay;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final scheme = Theme.of(context).colorScheme;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (settings.showOriginalScript) ...[
              _Label('Original', scheme),
              const SizedBox(height: 4),
              Text(message.originalText, style: textTheme.headlineMedium),
              const SizedBox(height: 14),
            ],
            if (settings.showTransliteration &&
                message.transliteration.isNotEmpty) ...[
              _Label('Pronunciation', scheme),
              const SizedBox(height: 4),
              Text(
                message.transliteration,
                style: textTheme.bodyLarge?.copyWith(
                  fontStyle: FontStyle.italic,
                  color: scheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 14),
            ],
            if (settings.showTranslation) ...[
              _Label('Meaning', scheme),
              const SizedBox(height: 4),
              Row(
                children: [
                  Expanded(
                    child: Text(message.translatedText, style: textTheme.titleLarge),
                  ),
                  if (onReplay != null)
                    IconButton(
                      onPressed: onReplay,
                      icon: const Icon(Icons.volume_up_rounded),
                      tooltip: 'Replay voice',
                    ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _Label extends StatelessWidget {
  const _Label(this.text, this.scheme);
  final String text;
  final ColorScheme scheme;

  @override
  Widget build(BuildContext context) {
    return Text(
      text.toUpperCase(),
      style: TextStyle(
        fontSize: 12,
        fontWeight: FontWeight.w700,
        letterSpacing: 1.1,
        color: scheme.primary,
      ),
    );
  }
}
