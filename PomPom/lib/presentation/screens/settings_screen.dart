import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path_provider/path_provider.dart';

import '../../core/constants/app_constants.dart';
import '../../data/models/settings_model.dart';
import '../../data/repositories/hive_boxes.dart';
import '../providers/repositories_providers.dart';
import '../providers/settings_provider.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings = ref.watch(settingsControllerProvider);
    final controller = ref.read(settingsControllerProvider.notifier);
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _Section(
            title: 'Timer',
            children: [
              _DurationPicker(
                label: 'Focus duration',
                options: kFocusDurationOptions,
                value: settings.focusMinutes,
                onChanged: (v) => controller.update((s) => s.copyWith(focusMinutes: v)),
              ),
              _DurationPicker(
                label: 'Break duration',
                options: kBreakDurationOptions,
                value: settings.breakMinutes,
                onChanged: (v) => controller.update((s) => s.copyWith(breakMinutes: v)),
              ),
              _DurationPicker(
                label: 'Long break duration',
                options: kLongBreakDurationOptions,
                value: settings.longBreakMinutes,
                onChanged: (v) => controller.update((s) => s.copyWith(longBreakMinutes: v)),
              ),
              SwitchListTile(
                title: const Text('Auto-start break'),
                value: settings.autoStartBreak,
                onChanged: (v) => controller.update((s) => s.copyWith(autoStartBreak: v)),
              ),
              SwitchListTile(
                title: const Text('Auto-start next focus'),
                value: settings.autoStartNextFocus,
                onChanged: (v) => controller.update((s) => s.copyWith(autoStartNextFocus: v)),
              ),
              ListTile(
                title: const Text('Daily goal (sessions)'),
                trailing: SizedBox(
                  width: 120,
                  child: Slider(
                    min: 1,
                    max: 16,
                    divisions: 15,
                    value: settings.dailyGoalSessions.toDouble(),
                    label: '${settings.dailyGoalSessions}',
                    onChanged: (v) => controller.update((s) => s.copyWith(dailyGoalSessions: v.round())),
                  ),
                ),
              ),
            ],
          ),
          _Section(
            title: 'Sounds',
            children: [
              SwitchListTile(
                title: const Text('Sounds enabled'),
                value: settings.soundsEnabled,
                onChanged: (v) => controller.update((s) => s.copyWith(soundsEnabled: v)),
              ),
              ListTile(
                title: const Text('Volume'),
                subtitle: Slider(
                  value: settings.soundVolume,
                  onChanged: (v) => controller.update((s) => s.copyWith(soundVolume: v)),
                ),
              ),
              _EnumWrap<AmbientSound>(
                label: 'Sound effect',
                values: AmbientSound.values,
                current: settings.ambientSound,
                labelOf: (e) => e.label,
                onSelected: (v) => controller.update((s) => s.copyWith(ambientSound: v)),
              ),
            ],
          ),
          _Section(
            title: 'Appearance',
            children: [
              _EnumWrap<AppThemeMode>(
                label: 'Theme',
                values: AppThemeMode.values,
                current: settings.themeMode,
                labelOf: (e) => switch (e) {
                  AppThemeMode.light => 'Light',
                  AppThemeMode.dark => 'Dark',
                  AppThemeMode.system => 'System',
                },
                onSelected: (v) => controller.update((s) => s.copyWith(themeMode: v)),
              ),
              _EnumWrap<PastelPalette>(
                label: 'Color palette',
                values: PastelPalette.values,
                current: settings.palette,
                labelOf: (e) => e.label,
                onSelected: (v) => controller.update((s) => s.copyWith(palette: v)),
              ),
              _EnumWrap<Mascot>(
                label: 'Mascot',
                values: Mascot.values,
                current: settings.mascot,
                labelOf: (e) => '${e.idleEmoji} ${e.label}',
                onSelected: (v) => controller.update((s) => s.copyWith(mascot: v)),
              ),
              _EnumWrap<AppBackgroundTheme>(
                label: 'Background theme',
                values: AppBackgroundTheme.values,
                current: settings.backgroundTheme,
                labelOf: (e) => e.label,
                onSelected: (v) => controller.update((s) => s.copyWith(backgroundTheme: v)),
              ),
              SwitchListTile(
                title: const Text('Animations'),
                value: settings.animationsEnabled,
                onChanged: (v) => controller.update((s) => s.copyWith(animationsEnabled: v)),
              ),
            ],
          ),
          _Section(
            title: 'Rewards',
            children: [
              _EnumWrap<RewardCurrency>(
                label: 'Reward currency',
                values: RewardCurrency.values,
                current: settings.rewardCurrency,
                labelOf: (e) => '${e.emoji} ${e.label}',
                onSelected: (v) => controller.update((s) => s.copyWith(rewardCurrency: v)),
              ),
              ListTile(
                title: const Text('Balance'),
                trailing: Text(
                  '${settings.rewardBalance} ${settings.rewardCurrency.emoji}',
                  style: theme.textTheme.titleMedium,
                ),
              ),
            ],
          ),
          _Section(
            title: 'Focus & Notifications',
            children: [
              SwitchListTile(
                title: const Text('Keep screen awake while focusing'),
                value: settings.keepScreenAwake,
                onChanged: (v) => controller.update((s) => s.copyWith(keepScreenAwake: v)),
              ),
              SwitchListTile(
                title: const Text('Notifications'),
                value: settings.notificationsEnabled,
                onChanged: (v) => controller.update((s) => s.copyWith(notificationsEnabled: v)),
              ),
              ListTile(
                title: const Text('Battery optimization'),
                subtitle: const Text('Allow PomPom to run reliably in the background'),
                trailing: const Icon(Icons.chevron_right_rounded),
                onTap: () => FlutterForegroundTask.requestIgnoreBatteryOptimization(),
              ),
            ],
          ),
          _Section(
            title: 'Data',
            children: [
              ListTile(
                title: const Text('Export data'),
                subtitle: const Text('Save a JSON backup of tasks, sessions & settings'),
                trailing: const Icon(Icons.download_rounded),
                onTap: () => _exportData(context, ref),
              ),
              ListTile(
                title: const Text('Reset all data'),
                subtitle: const Text('Clears tasks, sessions, streaks and settings'),
                trailing: const Icon(Icons.delete_outline_rounded, color: Colors.redAccent),
                onTap: () => _confirmReset(context, ref),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Future<void> _exportData(BuildContext context, WidgetRef ref) async {
    final tasks = ref.read(taskRepositoryProvider).getAll().map((t) => t.toMap()).toList();
    final sessions = ref.read(sessionRepositoryProvider).getAll().map((s) => s.toMap()).toList();
    final settings = ref.read(settingsControllerProvider).toMap();
    final payload = jsonEncode({'tasks': tasks, 'sessions': sessions, 'settings': settings});

    try {
      final dir = await getApplicationDocumentsDirectory();
      final file = File('${dir.path}/pompom_backup.json');
      await file.writeAsString(payload);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Saved to ${file.path}')));
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Export failed: $e')));
      }
    }
  }

  Future<void> _confirmReset(BuildContext context, WidgetRef ref) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Reset everything?'),
        content: const Text(
          'This deletes all tasks, sessions, streaks, achievements and settings. This cannot be undone.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.of(context).pop(true), child: const Text('Reset')),
        ],
      ),
    );
    if (confirmed != true) return;

    await HiveBoxes.tasksBox.clear();
    await HiveBoxes.sessionsBox.clear();
    await HiveBoxes.achievementsBox.clear();
    await ref.read(settingsControllerProvider.notifier).resetAllData();
    ref.read(refreshTickProvider.notifier).state++;
  }
}

class _Section extends StatelessWidget {
  final String title;
  final List<Widget> children;
  const _Section({required this.title, required this.children});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(left: 4, bottom: 8),
            child: Text(title, style: theme.textTheme.titleLarge),
          ),
          Card(child: Column(children: children)),
        ],
      ),
    );
  }
}

class _DurationPicker extends StatelessWidget {
  final String label;
  final List<DurationOption> options;
  final int value;
  final ValueChanged<int> onChanged;

  const _DurationPicker({required this.label, required this.options, required this.value, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return ListTile(
      title: Text(label),
      subtitle: Wrap(
        spacing: 8,
        children: options
            .map(
              (o) => ChoiceChip(
                label: Text(o.label),
                selected: value == o.minutes,
                onSelected: (_) => onChanged(o.minutes),
              ),
            )
            .toList(),
      ),
    );
  }
}

class _EnumWrap<T> extends StatelessWidget {
  final String label;
  final List<T> values;
  final T current;
  final String Function(T) labelOf;
  final ValueChanged<T> onSelected;

  const _EnumWrap({
    required this.label,
    required this.values,
    required this.current,
    required this.labelOf,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      title: Text(label),
      subtitle: Wrap(
        spacing: 8,
        runSpacing: 8,
        children: values
            .map((v) => ChoiceChip(label: Text(labelOf(v)), selected: current == v, onSelected: (_) => onSelected(v)))
            .toList(),
      ),
    );
  }
}
