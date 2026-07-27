import '../../data/models/session_model.dart';

enum PomodoroPhase { idle, focus, shortBreak, longBreak }

extension PomodoroPhaseX on PomodoroPhase {
  SessionType? get sessionType => switch (this) {
    PomodoroPhase.idle => null,
    PomodoroPhase.focus => SessionType.focus,
    PomodoroPhase.shortBreak => SessionType.shortBreak,
    PomodoroPhase.longBreak => SessionType.longBreak,
  };

  bool get isBreak => this == PomodoroPhase.shortBreak || this == PomodoroPhase.longBreak;
}
