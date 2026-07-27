import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/constants/app_constants.dart';
import '../../data/repositories/achievement_repository.dart';
import '../../data/repositories/hive_boxes.dart';
import '../../data/repositories/session_repository.dart';
import '../../data/repositories/settings_repository.dart';
import '../../domain/achievements/achievement_engine.dart';
import '../../domain/stats/stats_calculator.dart';
import '../../domain/timer/pomodoro_phase.dart';
import '../../services/foreground_task_handler.dart';
import 'repositories_providers.dart';
import 'settings_provider.dart';

class PomodoroUiState {
  final PomodoroPhase phase;
  final int remainingSeconds;
  final int totalSeconds;
  final bool isRunning;
  final bool isServiceActive;
  final int focusSessionsInCycle;
  final int totalFocusSessionsCompleted;
  final int completionEventId;
  final PomodoroPhase? justCompletedPhase;
  final List<String> newlyUnlockedAchievementIds;

  const PomodoroUiState({
    required this.phase,
    required this.remainingSeconds,
    required this.totalSeconds,
    required this.isRunning,
    required this.isServiceActive,
    required this.focusSessionsInCycle,
    required this.totalFocusSessionsCompleted,
    required this.completionEventId,
    required this.justCompletedPhase,
    required this.newlyUnlockedAchievementIds,
  });

  factory PomodoroUiState.idle(int focusMinutes) => PomodoroUiState(
    phase: PomodoroPhase.idle,
    remainingSeconds: focusMinutes * 60,
    totalSeconds: focusMinutes * 60,
    isRunning: false,
    isServiceActive: false,
    focusSessionsInCycle: 0,
    totalFocusSessionsCompleted: 0,
    completionEventId: 0,
    justCompletedPhase: null,
    newlyUnlockedAchievementIds: const [],
  );

  double get progress => totalSeconds == 0 ? 0 : 1 - (remainingSeconds / totalSeconds);

  PomodoroUiState copyWith({
    PomodoroPhase? phase,
    int? remainingSeconds,
    int? totalSeconds,
    bool? isRunning,
    bool? isServiceActive,
    int? focusSessionsInCycle,
    int? totalFocusSessionsCompleted,
    int? completionEventId,
    PomodoroPhase? justCompletedPhase,
    List<String>? newlyUnlockedAchievementIds,
  }) {
    return PomodoroUiState(
      phase: phase ?? this.phase,
      remainingSeconds: remainingSeconds ?? this.remainingSeconds,
      totalSeconds: totalSeconds ?? this.totalSeconds,
      isRunning: isRunning ?? this.isRunning,
      isServiceActive: isServiceActive ?? this.isServiceActive,
      focusSessionsInCycle: focusSessionsInCycle ?? this.focusSessionsInCycle,
      totalFocusSessionsCompleted: totalFocusSessionsCompleted ?? this.totalFocusSessionsCompleted,
      completionEventId: completionEventId ?? this.completionEventId,
      justCompletedPhase: justCompletedPhase ?? this.justCompletedPhase,
      newlyUnlockedAchievementIds: newlyUnlockedAchievementIds ?? this.newlyUnlockedAchievementIds,
    );
  }
}

/// Thin mirror of the timer running in the foreground task isolate. It never
/// ticks the clock itself - every number shown here comes from a broadcast
/// sent by [PompomTaskHandler], so the UI stays correct even if this
/// controller was just created (e.g. app reopened mid-session).
class PomodoroController extends StateNotifier<PomodoroUiState> {
  final Ref ref;

  PomodoroController(this.ref) : super(PomodoroUiState.idle(ref.read(settingsControllerProvider).focusMinutes)) {
    FlutterForegroundTask.addTaskDataCallback(_onTaskData);
    _syncWithRunningService();
  }

  SettingsRepository get _settingsRepo => ref.read(settingsRepositoryProvider);
  SessionRepository get _sessionRepo => ref.read(sessionRepositoryProvider);
  AchievementRepository get _achievementRepo => ref.read(achievementRepositoryProvider);

  Future<void> _syncWithRunningService() async {
    if (await FlutterForegroundTask.isRunningService) {
      state = state.copyWith(isServiceActive: true);
      FlutterForegroundTask.sendDataToTask('request_state');
    }
  }

  Future<bool> startFocus() async {
    final permission = await FlutterForegroundTask.checkNotificationPermission();
    if (permission != NotificationPermission.granted) {
      await FlutterForegroundTask.requestNotificationPermission();
    }

    final settings = _settingsRepo.get();
    final result = await FlutterForegroundTask.startService(
      serviceId: 100,
      notificationTitle: '🍓 Focus Time',
      notificationText: '${settings.focusMinutes.toString().padLeft(2, '0')}:00 remaining',
      notificationButtons: const [
        NotificationButton(id: 'pause', text: 'Pause'),
        NotificationButton(id: 'skip', text: 'Skip'),
      ],
      callback: pompomTaskHandlerStartCallback,
    );

    if (result is ServiceRequestSuccess) {
      state = state.copyWith(
        isServiceActive: true,
        phase: PomodoroPhase.focus,
        isRunning: true,
        remainingSeconds: settings.focusMinutes * 60,
        totalSeconds: settings.focusMinutes * 60,
      );
      return true;
    }
    return false;
  }

  void pause() => FlutterForegroundTask.sendDataToTask('pause');

  void resume() => FlutterForegroundTask.sendDataToTask('resume');

  void skip() => FlutterForegroundTask.sendDataToTask('skip');

  Future<void> stopAndReset() async {
    if (await FlutterForegroundTask.isRunningService) {
      await FlutterForegroundTask.stopService();
    }
    state = PomodoroUiState.idle(_settingsRepo.get().focusMinutes);
  }

  void clearNewlyUnlocked() => state = state.copyWith(newlyUnlockedAchievementIds: const []);

  void _onTaskData(Object data) {
    if (data is! Map) return;
    final map = Map<String, dynamic>.from(data);
    final phase = PomodoroPhase.values.firstWhere((p) => p.name == map['phase'], orElse: () => PomodoroPhase.idle);
    final isPhaseCompleted = map['type'] == 'phase_completed';
    final completedPhaseName = map['completedPhase'] as String?;
    final completedPhase = completedPhaseName == null
        ? null
        : PomodoroPhase.values.firstWhere((p) => p.name == completedPhaseName, orElse: () => PomodoroPhase.idle);

    state = state.copyWith(
      phase: phase,
      remainingSeconds: map['remainingSeconds'] as int,
      totalSeconds: map['totalSeconds'] as int,
      isRunning: map['isRunning'] as bool,
      isServiceActive: true,
      focusSessionsInCycle: map['focusSessionsInCycle'] as int,
      totalFocusSessionsCompleted: map['totalFocusSessionsCompleted'] as int,
      completionEventId: isPhaseCompleted ? state.completionEventId + 1 : state.completionEventId,
      justCompletedPhase: completedPhase ?? state.justCompletedPhase,
    );

    if (isPhaseCompleted && completedPhase != null) {
      _handlePhaseCompleted(completedPhase);
    }
  }

  Future<void> _handlePhaseCompleted(PomodoroPhase completedPhase) async {
    await HiveBoxes.refreshAll();
    ref.read(refreshTickProvider.notifier).state++;

    if (completedPhase == PomodoroPhase.focus) {
      ref.read(settingsControllerProvider.notifier).addReward(kRewardPerSession);
    }

    final sessions = _sessionRepo.getAll();
    final streaks = StatsCalculator.streaks(sessions);
    final todayStats = StatsCalculator.today(sessions);
    final settings = _settingsRepo.get();

    final stats = AchievementStats(
      totalCompletedSessions: StatsCalculator.completedFocusSessions(sessions).length,
      currentStreak: streaks.current,
      longestStreak: streaks.longest,
      totalFocusMinutes: StatsCalculator.totalFocusMinutesAllTime(sessions),
      metDailyGoalAtLeastOnce: todayStats.completedSessions >= settings.dailyGoalSessions,
    );

    final metIds = metAchievementIds(stats).toSet();
    final alreadyUnlocked = _achievementRepo.getUnlocked().keys.toSet();
    final newlyUnlocked = metIds.difference(alreadyUnlocked);

    for (final id in newlyUnlocked) {
      await _achievementRepo.unlock(id);
    }
    if (newlyUnlocked.isNotEmpty) {
      state = state.copyWith(newlyUnlockedAchievementIds: newlyUnlocked.toList());
    }
  }

  @override
  void dispose() {
    FlutterForegroundTask.removeTaskDataCallback(_onTaskData);
    super.dispose();
  }
}

final pomodoroControllerProvider = StateNotifierProvider<PomodoroController, PomodoroUiState>(
  (ref) => PomodoroController(ref),
);
