import 'package:flutter/material.dart';

import '../models/app_settings.dart';
import '../models/translation_message.dart';

/// Renders one conversation turn as three sections, matching the PRD:
///
/// - English spoken -> Original (English) / Translation (native script) /
///   Pronunciation (of the native-script translation).
/// - Native language spoken -> Original (native script) / Pronunciation
///   (of it) / Meaning (English).
///
/// Either way, the transliteration always belongs to the native-script
/// text specifically — never to English, which needs no sounding-out help.
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
    final spokeEnglish = message.wasEnglishSpoken;

    final originalText = spokeEnglish ? message.englishText : message.nativeText;
    final finalLabel = spokeEnglish ? 'Translation' : 'Meaning';
    final finalText = spokeEnglish ? message.nativeText : message.englishText;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (settings.showOriginalScript) ...[
              _Label('Original', scheme),
              const SizedBox(height: 4),
              Text(originalText, style: textTheme.headlineMedium),
              const SizedBox(height: 14),
            ],
            // Pronunciation always describes the native-script text, so it
            // only appears right after whichever section is in that script.
            if (!spokeEnglish &&
                settings.showTransliteration &&
                message.nativeTransliteration.isNotEmpty) ...[
              _Label('Pronunciation', scheme),
              const SizedBox(height: 4),
              Text(
                message.nativeTransliteration,
                style: textTheme.bodyLarge?.copyWith(
                  fontStyle: FontStyle.italic,
                  color: scheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 14),
            ],
            if (settings.showTranslation) ...[
              _Label(finalLabel, scheme),
              const SizedBox(height: 4),
              Row(
                children: [
                  Expanded(
                    child: Text(finalText, style: textTheme.titleLarge),
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
            if (spokeEnglish &&
                settings.showTransliteration &&
                message.nativeTransliteration.isNotEmpty) ...[
              const SizedBox(height: 14),
              _Label('Pronunciation', scheme),
              const SizedBox(height: 4),
              Text(
                message.nativeTransliteration,
                style: textTheme.bodyLarge?.copyWith(
                  fontStyle: FontStyle.italic,
                  color: scheme.onSurfaceVariant,
                ),
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
