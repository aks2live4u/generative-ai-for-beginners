import 'pomodoro_phase.dart';

class PomodoroConfig {
  final int focusMinutes;
  final int breakMinutes;
  final int longBreakMinutes;
  final int sessionsBeforeLongBreak;
  final bool autoStartBreak;
  final bool autoStartNextFocus;

  const PomodoroConfig({
    required this.focusMinutes,
    required this.breakMinutes,
    required this.longBreakMinutes,
    this.sessionsBeforeLongBreak = 4,
    this.autoStartBreak = true,
    this.autoStartNextFocus = false,
  });
}

class PomodoroState {
  final PomodoroPhase phase;
  final int remainingSeconds;
  final int totalSeconds;
  final bool isRunning;

  /// Focus sessions completed since the last long break (0..sessionsBeforeLongBreak).
  final int focusSessionsInCycle;

  /// Lifetime count of fully completed focus sessions this app run (persisted
  /// stats live in SessionRepository; this only drives "session N of 4" UI).
  final int totalFocusSessionsCompleted;

  const PomodoroState({
    required this.phase,
    required this.remainingSeconds,
    required this.totalSeconds,
    required this.isRunning,
    required this.focusSessionsInCycle,
    required this.totalFocusSessionsCompleted,
  });

  factory PomodoroState.initial(int focusMinutes) => PomodoroState(
    phase: PomodoroPhase.idle,
    remainingSeconds: focusMinutes * 60,
    totalSeconds: focusMinutes * 60,
    isRunning: false,
    focusSessionsInCycle: 0,
    totalFocusSessionsCompleted: 0,
  );

  double get progress => totalSeconds == 0 ? 0 : 1 - (remainingSeconds / totalSeconds);

  PomodoroState copyWith({
    PomodoroPhase? phase,
    int? remainingSeconds,
    int? totalSeconds,
    bool? isRunning,
    int? focusSessionsInCycle,
    int? totalFocusSessionsCompleted,
  }) {
    return PomodoroState(
      phase: phase ?? this.phase,
      remainingSeconds: remainingSeconds ?? this.remainingSeconds,
      totalSeconds: totalSeconds ?? this.totalSeconds,
      isRunning: isRunning ?? this.isRunning,
      focusSessionsInCycle: focusSessionsInCycle ?? this.focusSessionsInCycle,
      totalFocusSessionsCompleted: totalFocusSessionsCompleted ?? this.totalFocusSessionsCompleted,
    );
  }
}

/// Result of advancing the clock by one second. [completedPhase] is non-null
/// only on the tick where a phase naturally ran out of time, so the caller
/// can record a finished session / show a celebration exactly once.
class TickResult {
  final PomodoroState state;
  final PomodoroPhase? completedPhase;
  const TickResult(this.state, {this.completedPhase});
}

/// Pure state machine for the Pomodoro cycle: focus -> short break, repeating,
/// with a long break every [PomodoroConfig.sessionsBeforeLongBreak] focus
/// sessions. Contains no Flutter/platform code so it's cheap to unit test.
class PomodoroEngine {
  final PomodoroConfig config;
  const PomodoroEngine(this.config);

  PomodoroState start(PomodoroState state) {
    if (state.phase == PomodoroPhase.idle) {
      final seconds = config.focusMinutes * 60;
      return state.copyWith(
        phase: PomodoroPhase.focus,
        remainingSeconds: seconds,
        totalSeconds: seconds,
        isRunning: true,
      );
    }
    return state.copyWith(isRunning: true);
  }

  PomodoroState pause(PomodoroState state) => state.copyWith(isRunning: false);

  PomodoroState resume(PomodoroState state) => state.copyWith(isRunning: true);

  PomodoroState reset(PomodoroState state) => PomodoroState.initial(config.focusMinutes);

  TickResult tick(PomodoroState state) {
    if (!state.isRunning || state.phase == PomodoroPhase.idle) {
      return TickResult(state);
    }
    if (state.remainingSeconds > 1) {
      return TickResult(state.copyWith(remainingSeconds: state.remainingSeconds - 1));
    }
    final completedPhase = state.phase;
    return TickResult(_advance(state), completedPhase: completedPhase);
  }

  /// Jumps straight to the next phase without waiting for the clock to run
  /// out. A skipped focus session never counts toward the long-break cycle
  /// or stats - only phases that finish naturally do.
  PomodoroState skip(PomodoroState state) {
    if (state.phase == PomodoroPhase.idle) return state;
    if (state.phase == PomodoroPhase.focus) {
      final seconds = config.breakMinutes * 60;
      return state.copyWith(
        phase: PomodoroPhase.shortBreak,
        remainingSeconds: seconds,
        totalSeconds: seconds,
        isRunning: config.autoStartBreak,
      );
    }
    final seconds = config.focusMinutes * 60;
    return state.copyWith(
      phase: PomodoroPhase.focus,
      remainingSeconds: seconds,
      totalSeconds: seconds,
      isRunning: config.autoStartNextFocus,
    );
  }

  PomodoroState _advance(PomodoroState state) {
    switch (state.phase) {
      case PomodoroPhase.focus:
        final newCycleCount = state.focusSessionsInCycle + 1;
        final longBreakNext = newCycleCount >= config.sessionsBeforeLongBreak;
        final nextSeconds = (longBreakNext ? config.longBreakMinutes : config.breakMinutes) * 60;
        return state.copyWith(
          phase: longBreakNext ? PomodoroPhase.longBreak : PomodoroPhase.shortBreak,
          remainingSeconds: nextSeconds,
          totalSeconds: nextSeconds,
          isRunning: config.autoStartBreak,
          focusSessionsInCycle: longBreakNext ? 0 : newCycleCount,
          totalFocusSessionsCompleted: state.totalFocusSessionsCompleted + 1,
        );
      case PomodoroPhase.shortBreak:
      case PomodoroPhase.longBreak:
        final nextSeconds = config.focusMinutes * 60;
        return state.copyWith(
          phase: PomodoroPhase.focus,
          remainingSeconds: nextSeconds,
          totalSeconds: nextSeconds,
          isRunning: config.autoStartNextFocus,
        );
      case PomodoroPhase.idle:
        return state;
    }
  }
}
