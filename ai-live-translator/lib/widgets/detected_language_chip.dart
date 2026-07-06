import 'package:flutter/material.dart';

import '../models/language.dart';
import 'glass.dart';

/// Small pill showing which language the last turn was detected as —
/// a quick visual confirmation that auto-detection picked up the right side.
class DetectedLanguageChip extends StatelessWidget {
  const DetectedLanguageChip({super.key, required this.language});

  final AppLanguage language;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return GlassContainer(
      borderRadius: 999,
      blurSigma: 12,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.translate_rounded, size: 16, color: scheme.secondary),
          const SizedBox(width: 8),
          Text(
            'Detected language: ${language.name}',
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: scheme.onSurfaceVariant,
                  fontWeight: FontWeight.w600,
                ),
          ),
        ],
      ),
    );
  }
}
