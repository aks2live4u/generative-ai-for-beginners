import 'package:flutter/material.dart';

import '../models/language.dart';

/// "English ⇄ Telugu" header shown at the top of the conversation screen.
/// Purely informational — which language was spoken is auto-detected per
/// turn, so there's nothing to swap or configure here.
class LanguageBadge extends StatelessWidget {
  const LanguageBadge({
    super.key,
    required this.english,
    required this.nativeLanguage,
  });

  final AppLanguage english;
  final AppLanguage nativeLanguage;

  @override
  Widget build(BuildContext context) {
    final style = Theme.of(context).textTheme.titleLarge;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(english.name, style: style),
        const Padding(
          padding: EdgeInsets.symmetric(horizontal: 8),
          child: Icon(Icons.sync_alt_rounded, size: 20),
        ),
        Text(nativeLanguage.name, style: style),
      ],
    );
  }
}
