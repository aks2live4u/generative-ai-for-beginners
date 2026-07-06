import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../models/app_settings.dart';
import '../models/translation_message.dart';
import '../themes/app_theme.dart';

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
    this.onReplayOriginal,
    this.onReplayTranslation,
  });

  final TranslationMessage message;
  final AppSettings settings;
  final VoidCallback? onReplayOriginal;
  final VoidCallback? onReplayTranslation;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final accents = AppAccents.of(context);
    final spokeEnglish = message.wasEnglishSpoken;

    final originalText = spokeEnglish ? message.englishText : message.nativeText;
    final finalLabel = spokeEnglish ? 'Translation' : 'Meaning';
    final finalText = spokeEnglish ? message.nativeText : message.englishText;

    final pronunciation = Padding(
      padding: const EdgeInsets.only(bottom: 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _Label('Pronunciation', accents.pronunciation),
          const SizedBox(height: 6),
          Row(
            children: [
              Expanded(
                child: Text(
                  message.nativeTransliteration,
                  style: textTheme.bodyLarge?.copyWith(
                    fontStyle: FontStyle.italic,
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
              ),
              _AccentIconButton(
                icon: Icons.copy_rounded,
                color: accents.pronunciation,
                tooltip: 'Copy pronunciation',
                onPressed: () => _copyToClipboard(context, message.nativeTransliteration),
              ),
            ],
          ),
        ],
      ),
    );
    final showPronunciation =
        settings.showTransliteration && message.nativeTransliteration.isNotEmpty;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (settings.showOriginalScript) ...[
              _Label('Original', accents.original),
              const SizedBox(height: 6),
              Row(
                children: [
                  Expanded(
                    child: Text(originalText, style: textTheme.headlineMedium),
                  ),
                  if (onReplayOriginal != null)
                    _AccentIconButton(
                      icon: Icons.volume_up_rounded,
                      color: accents.original,
                      tooltip: 'Replay original',
                      onPressed: onReplayOriginal!,
                    ),
                ],
              ),
              const SizedBox(height: 14),
            ],
            // Pronunciation always describes the native-script text, so it
            // only appears right after whichever section is in that script.
            if (!spokeEnglish && showPronunciation) pronunciation,
            if (settings.showTranslation) ...[
              _Label(finalLabel, accents.meaning),
              const SizedBox(height: 6),
              Row(
                children: [
                  Expanded(
                    child: Text(finalText, style: textTheme.titleLarge),
                  ),
                  if (onReplayTranslation != null)
                    _AccentIconButton(
                      icon: Icons.volume_up_rounded,
                      color: accents.meaning,
                      tooltip: 'Replay translation',
                      onPressed: onReplayTranslation!,
                    ),
                ],
              ),
            ],
            if (spokeEnglish && showPronunciation) ...[
              const SizedBox(height: 14),
              pronunciation,
            ],
          ],
        ),
      ),
    );
  }

  void _copyToClipboard(BuildContext context, String text) {
    Clipboard.setData(ClipboardData(text: text));
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Pronunciation copied'), duration: Duration(seconds: 1)),
    );
  }
}

class _Label extends StatelessWidget {
  const _Label(this.text, this.color);
  final String text;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Text(
      text.toUpperCase(),
      style: TextStyle(
        fontSize: 12,
        fontWeight: FontWeight.w700,
        letterSpacing: 1.1,
        color: color,
      ),
    );
  }
}

/// A tappable icon with a soft tinted background in the section's accent
/// color — bigger and easier to hit/notice than a plain small line icon.
class _AccentIconButton extends StatelessWidget {
  const _AccentIconButton({
    required this.icon,
    required this.color,
    required this.tooltip,
    required this.onPressed,
  });

  final IconData icon;
  final Color color;
  final String tooltip;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return IconButton(
      onPressed: onPressed,
      tooltip: tooltip,
      icon: Icon(icon),
      iconSize: 24,
      style: IconButton.styleFrom(
        backgroundColor: color.withValues(alpha: 0.16),
        foregroundColor: color,
        minimumSize: const Size(44, 44),
      ),
    );
  }
}
