import 'package:flutter/material.dart';

class GoalProgressBar extends StatelessWidget {
  final int completed;
  final int goal;
  final Color color;

  const GoalProgressBar({super.key, required this.completed, required this.goal, required this.color});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final ratio = goal == 0 ? 0.0 : (completed / goal).clamp(0.0, 1.0);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('Daily Goal', style: theme.textTheme.titleMedium),
            Text('$completed / $goal', style: theme.textTheme.bodyMedium),
          ],
        ),
        const SizedBox(height: 10),
        ClipRRect(
          borderRadius: BorderRadius.circular(20),
          child: TweenAnimationBuilder<double>(
            tween: Tween(begin: 0, end: ratio),
            duration: const Duration(milliseconds: 600),
            curve: Curves.easeOutCubic,
            builder: (context, value, _) => LinearProgressIndicator(
              value: value,
              minHeight: 14,
              backgroundColor: color.withValues(alpha: 0.18),
              valueColor: AlwaysStoppedAnimation(color),
            ),
          ),
        ),
      ],
    );
  }
}
