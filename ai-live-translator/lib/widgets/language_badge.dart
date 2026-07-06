import 'package:flutter/material.dart';

import '../models/language.dart';

/// "English ⇄ Telugu" header shown at the top of the conversation screen.
class LanguageBadge extends StatelessWidget {
  const LanguageBadge({
    super.key,
    required this.languageA,
    required this.languageB,
    required this.onSwap,
  });

  final AppLanguage languageA;
  final AppLanguage languageB;
  final VoidCallback onSwap;

  @override
  Widget build(BuildContext context) {
    final style = Theme.of(context).textTheme.titleLarge;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(languageA.name, style: style),
        IconButton(
          onPressed: onSwap,
          icon: const Icon(Icons.swap_horiz_rounded),
          tooltip: 'Swap languages',
        ),
        Text(languageB.name, style: style),
      ],
    );
  }
}
