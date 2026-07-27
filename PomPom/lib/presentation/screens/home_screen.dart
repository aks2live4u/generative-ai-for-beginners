import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:wakelock_plus/wakelock_plus.dart';

import '../../core/constants/app_constants.dart';
import '../../core/theme/app_colors.dart';
import '../../data/models/settings_model.dart';
import '../../domain/stats/stats_calculator.dart';
import '../../domain/timer/pomodoro_phase.dart';
import '../../services/sound_service.dart';
import '../providers/pomodoro_controller.dart';
import '../providers/repositories_providers.dart';
import '../providers/settings_provider.dart';
import '../widgets/background/animated_background.dart';
import '../widgets/common/goal_progress_bar.dart';
import '../widgets/common/streak_badge.dart';
import '../widgets/mascot/mascot_widget.dart';
import '../widgets/timer_ring/circular_timer.dart';
import 'achievements_screen.dart';
import 'celebration_screen.dart';
import 'daily_summary_dialog.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  int? _lastHandledEventId;

  @override
  Widget build(BuildContext context) {
    final uiState = ref.watch(pomodoroControllerProvider);
    final settings = ref.watch(settingsControllerProvider);
    ref.watch(refreshTickProvider);
    final theme = Theme.of(context);

    ref.listen<PomodoroUiState>(pomodoroControllerProvider, (previous, next) {
      _syncWakelock(next, settings);
      if (next.completionEventId != _lastHandledEventId) {
        _lastHandledEventId = next.completionEventId;
        if (next.completionEventId > 0 && next.justCompletedPhase != null) {
          _onPhaseCompleted(next.justCompletedPhase!, next);
        }
      }
    });

    final sessions = ref.read(sessionRepositoryProvider).getAll();
    final today = StatsCalculator.today(sessions);
    final streaks = StatsCalculator.streaks(sessions);

    final isBreak = uiState.phase.isBreak;
    final ringColor = isBreak ? kSuccessGreen : theme.colorScheme.primary;
    final mascotMood = _moodFor(uiState);

    return Scaffold(
      body: AnimatedBackground(
        theme: settings.backgroundTheme,
        animate: settings.animationsEnabled,
        child: SafeArea(
          child: Column(
            children: [
              _Header(onAchievements: () => AchievementsScreen.push(context)),
              Expanded(
                child: Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(_phaseTitle(uiState.phase), style: theme.textTheme.headlineMedium),
                      const SizedBox(height: 24),
                      CircularTimer(
                        progress: uiState.progress,
                        size: 260,
                        ringColor: ringColor,
                        trackColor: ringColor.withValues(alpha: 0.15),
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            MascotWidget(
                              mascot: settings.mascot,
                              mood: mascotMood,
                              animate: settings.animationsEnabled,
                            ),
                            const SizedBox(height: 8),
                            Text(_formatTime(uiState.remainingSeconds), style: theme.textTheme.displayLarge),
                          ],
                        ),
                      ),
                      const SizedBox(height: 28),
                      if (isBreak) _BreakSuggestions(theme: theme),
                      _Controls(uiState: uiState),
                    ],
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
                child: Row(
                  children: [
                    Expanded(
                      child: GoalProgressBar(
                        completed: today.completedSessions,
                        goal: settings.dailyGoalSessions,
                        color: theme.colorScheme.primary,
                      ),
                    ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
                child: StreakBadge(current: streaks.current, longest: streaks.longest),
              ),
            ],
          ),
        ),
      ),
    );
  }

  MascotMood _moodFor(PomodoroUiState state) {
    if (state.phase == PomodoroPhase.idle) return MascotMood.idle;
    if (state.phase.isBreak) return MascotMood.happy;
    return state.isRunning ? MascotMood.thinking : MascotMood.sleeping;
  }

  String _phaseTitle(PomodoroPhase phase) => switch (phase) {
    PomodoroPhase.idle => 'Ready to focus?',
    PomodoroPhase.focus => 'Focus Time',
    PomodoroPhase.shortBreak => 'Break Time 🌿',
    PomodoroPhase.longBreak => 'Long Break 🌈',
  };

  String _formatTime(int seconds) {
    final m = (seconds ~/ 60).toString().padLeft(2, '0');
    final s = (seconds % 60).toString().padLeft(2, '0');
    return '$m:$s';
  }

  void _syncWakelock(PomodoroUiState state, SettingsModel settings) {
    final shouldKeepAwake = settings.keepScreenAwake && state.isRunning;
    if (shouldKeepAwake) {
      WakelockPlus.enable();
    } else {
      WakelockPlus.disable();
    }
  }

  Future<void> _onPhaseCompleted(PomodoroPhase completedPhase, PomodoroUiState state) async {
    final settings = ref.read(settingsControllerProvider);
    if (settings.soundsEnabled) {
      await SoundService.instance.playEffect(settings.ambientSound, volume: settings.soundVolume);
    }

    if (completedPhase == PomodoroPhase.focus) {
      if (!mounted) return;
      await Navigator.of(context).push(
        PageRouteBuilder(
          opaque: false,
          barrierColor: Colors.black45,
          transitionDuration: const Duration(milliseconds: 350),
          pageBuilder: (_, _, _) => CelebrationScreen(rewardAmount: kRewardPerSession),
          transitionsBuilder: (_, anim, _, child) => FadeTransition(opacity: anim, child: child),
        ),
      );
      if (state.phase == PomodoroPhase.longBreak && mounted) {
        await showDailySummaryDialog(context, ref);
      }
    }
  }
}

class _Header extends StatelessWidget {
  final VoidCallback onAchievements;
  const _Header({required this.onAchievements});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 12, 12, 0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text('PomPom', style: theme.textTheme.titleLarge),
          IconButton(
            onPressed: onAchievements,
            icon: const Text('🏅', style: TextStyle(fontSize: 22)),
            tooltip: 'Achievements',
          ),
        ],
      ),
    );
  }
}

class _BreakSuggestions extends StatelessWidget {
  final ThemeData theme;
  const _BreakSuggestions({required this.theme});

  @override
  Widget build(BuildContext context) {
    const suggestions = [('🧘', 'Stretch'), ('💧', 'Drink Water'), ('🚶', 'Walk')];
    return Padding(
      padding: const EdgeInsets.only(bottom: 20),
      child: Wrap(
        spacing: 10,
        alignment: WrapAlignment.center,
        children: suggestions
            .map(
              (s) => Chip(
                avatar: Text(s.$1, style: const TextStyle(fontSize: 16)),
                label: Text(s.$2),
              ),
            )
            .toList(),
      ),
    );
  }
}

class _Controls extends ConsumerWidget {
  final PomodoroUiState uiState;
  const _Controls({required this.uiState});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final controller = ref.read(pomodoroControllerProvider.notifier);

    if (uiState.phase == PomodoroPhase.idle) {
      return ElevatedButton(
        onPressed: () => controller.startFocus(),
        child: const Padding(padding: EdgeInsets.symmetric(horizontal: 12), child: Text('Start')),
      );
    }

    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        OutlinedButton(onPressed: () => controller.skip(), child: const Text('Skip')),
        const SizedBox(width: 14),
        ElevatedButton(
          onPressed: () => uiState.isRunning ? controller.pause() : controller.resume(),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Text(uiState.isRunning ? 'Pause' : 'Resume'),
          ),
        ),
        const SizedBox(width: 14),
        OutlinedButton(onPressed: () => controller.stopAndReset(), child: const Text('Reset')),
      ],
    );
  }
}
