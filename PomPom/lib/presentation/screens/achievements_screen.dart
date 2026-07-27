import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';

import '../../domain/achievements/achievement_engine.dart';
import '../providers/repositories_providers.dart';

class AchievementsScreen extends ConsumerWidget {
  const AchievementsScreen({super.key});

  static void push(BuildContext context) {
    Navigator.of(context).push(MaterialPageRoute(builder: (_) => const AchievementsScreen()));
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final repo = ref.read(achievementRepositoryProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Achievements')),
      body: ValueListenableBuilder<Box<String>>(
        valueListenable: repo.listenable(),
        builder: (context, box, _) {
          final unlocked = repo.getUnlocked();
          return ListView.separated(
            padding: const EdgeInsets.all(20),
            itemCount: kAchievementDefs.length,
            separatorBuilder: (_, _) => const SizedBox(height: 12),
            itemBuilder: (context, index) {
              final def = kAchievementDefs[index];
              final isUnlocked = unlocked.containsKey(def.id);
              return Card(
                color: isUnlocked ? null : theme.colorScheme.surface.withValues(alpha: 0.5),
                child: ListTile(
                  leading: Opacity(
                    opacity: isUnlocked ? 1 : 0.3,
                    child: Text(def.emoji, style: const TextStyle(fontSize: 30)),
                  ),
                  title: Text(
                    def.title,
                    style: theme.textTheme.titleMedium?.copyWith(
                      color: isUnlocked ? null : theme.colorScheme.onSurface.withValues(alpha: 0.4),
                    ),
                  ),
                  subtitle: Text(
                    def.description,
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: isUnlocked ? null : theme.colorScheme.onSurface.withValues(alpha: 0.4),
                    ),
                  ),
                  trailing: isUnlocked ? const Icon(Icons.check_circle_rounded, color: Colors.green) : null,
                ),
              );
            },
          );
        },
      ),
    );
  }
}
