import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/stats/stats_calculator.dart';
import '../providers/repositories_providers.dart';
import '../providers/settings_provider.dart';
import '../widgets/mascot/mascot_widget.dart';
import '../../core/constants/app_constants.dart';

Future<void> showDailySummaryDialog(BuildContext context, WidgetRef ref) async {
  final sessions = ref.read(sessionRepositoryProvider).getAll();
  final today = StatsCalculator.today(sessions);
  final streaks = StatsCalculator.streaks(sessions);
  final settings = ref.read(settingsControllerProvider);

  if (!context.mounted) return;

  await showDialog<void>(
    context: context,
    builder: (context) {
      final theme = Theme.of(context);
      return AlertDialog(
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            MascotWidget(mascot: settings.mascot, mood: MascotMood.happy, size: 72),
            const SizedBox(height: 16),
            Text('🌈 Long Break Time!', style: theme.textTheme.headlineMedium, textAlign: TextAlign.center),
            const SizedBox(height: 16),
            _Row(label: 'Sessions today', value: '${today.completedSessions}'),
            _Row(label: 'Focus time today', value: _formatDuration(today.totalFocusTime)),
            _Row(label: 'Current streak', value: '${streaks.current} days'),
            const SizedBox(height: 20),
            Text(
              'You\'ve earned a longer break. Enjoy it! 💤',
              style: theme.textTheme.bodyMedium,
              textAlign: TextAlign.center,
            ),
          ],
        ),
        actions: [TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Thanks!'))],
      );
    },
  );
}

String _formatDuration(Duration d) {
  final h = d.inHours;
  final m = d.inMinutes % 60;
  if (h > 0) return '${h}h ${m}m';
  return '${m}m';
}

class _Row extends StatelessWidget {
  final String label;
  final String value;
  const _Row({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: theme.textTheme.bodyMedium),
          Text(value, style: theme.textTheme.titleMedium),
        ],
      ),
    );
  }
}
