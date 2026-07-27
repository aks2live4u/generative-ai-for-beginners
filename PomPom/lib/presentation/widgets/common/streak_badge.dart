import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';

class StreakBadge extends StatelessWidget {
  final int current;
  final int longest;

  const StreakBadge({super.key, required this.current, required this.longest});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
      decoration: BoxDecoration(color: theme.colorScheme.surface, borderRadius: BorderRadius.circular(20)),
      child: Row(
        children: [
          const Text('🔥', style: TextStyle(fontSize: 28)),
          const SizedBox(width: 12),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '$current Day${current == 1 ? '' : 's'}',
                style: theme.textTheme.titleLarge?.copyWith(color: kStreakOrange),
              ),
              Text('Current streak · Longest $longest', style: theme.textTheme.bodyMedium),
            ],
          ),
        ],
      ),
    );
  }
}
