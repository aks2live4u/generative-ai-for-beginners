import 'package:flutter_test/flutter_test.dart';
import 'package:pompom/domain/timer/pomodoro_engine.dart';
import 'package:pompom/domain/timer/pomodoro_phase.dart';

void main() {
  const config = PomodoroConfig(
    focusMinutes: 25,
    breakMinutes: 5,
    longBreakMinutes: 15,
    sessionsBeforeLongBreak: 4,
    autoStartBreak: true,
    autoStartNextFocus: false,
  );
  const engine = PomodoroEngine(config);

  test('initial state is idle with focus duration preloaded', () {
    final state = PomodoroState.initial(config.focusMinutes);
    expect(state.phase, PomodoroPhase.idle);
    expect(state.remainingSeconds, 25 * 60);
    expect(state.isRunning, false);
  });

  test('start begins a running focus phase', () {
    final state = engine.start(PomodoroState.initial(config.focusMinutes));
    expect(state.phase, PomodoroPhase.focus);
    expect(state.isRunning, true);
    expect(state.remainingSeconds, 25 * 60);
  });

  test('tick decrements remaining seconds while running', () {
    var state = engine.start(PomodoroState.initial(config.focusMinutes));
    final result = engine.tick(state);
    expect(result.completedPhase, isNull);
    expect(result.state.remainingSeconds, 25 * 60 - 1);
  });

  test('tick does nothing while paused', () {
    var state = engine.start(PomodoroState.initial(config.focusMinutes));
    state = engine.pause(state);
    final result = engine.tick(state);
    expect(result.state.remainingSeconds, state.remainingSeconds);
  });

  test('focus phase completing transitions to short break and auto-starts it', () {
    var state = engine.start(PomodoroState.initial(config.focusMinutes));
    state = state.copyWith(remainingSeconds: 1);
    final result = engine.tick(state);
    expect(result.completedPhase, PomodoroPhase.focus);
    expect(result.state.phase, PomodoroPhase.shortBreak);
    expect(result.state.remainingSeconds, config.breakMinutes * 60);
    expect(result.state.isRunning, true); // autoStartBreak
    expect(result.state.focusSessionsInCycle, 1);
    expect(result.state.totalFocusSessionsCompleted, 1);
  });

  test('break completing transitions back to focus respecting autoStartNextFocus', () {
    var state = engine.start(PomodoroState.initial(config.focusMinutes));
    state = state.copyWith(
      phase: PomodoroPhase.shortBreak,
      remainingSeconds: 1,
      totalSeconds: config.breakMinutes * 60,
    );
    final result = engine.tick(state);
    expect(result.completedPhase, PomodoroPhase.shortBreak);
    expect(result.state.phase, PomodoroPhase.focus);
    expect(result.state.isRunning, false); // autoStartNextFocus is false
  });

  test('every 4th completed focus session triggers a long break and resets the cycle', () {
    var state = engine.start(PomodoroState.initial(config.focusMinutes));
    for (var i = 0; i < 4; i++) {
      // finish the focus phase
      state = state.copyWith(
        phase: PomodoroPhase.focus,
        remainingSeconds: 1,
        totalSeconds: config.focusMinutes * 60,
        isRunning: true,
      );
      final focusResult = engine.tick(state);
      state = focusResult.state;
      if (i < 3) {
        expect(state.phase, PomodoroPhase.shortBreak, reason: 'cycle $i should be a short break');
        // finish the short break to get back to focus for the next loop
        state = state.copyWith(remainingSeconds: 1, isRunning: true);
        state = engine.tick(state).state;
      } else {
        expect(state.phase, PomodoroPhase.longBreak, reason: 'the 4th session should trigger a long break');
        expect(state.focusSessionsInCycle, 0);
      }
    }
  });

  test('skipping a focus session moves to break without counting toward stats/cycle', () {
    var state = engine.start(PomodoroState.initial(config.focusMinutes));
    final skipped = engine.skip(state);
    expect(skipped.phase, PomodoroPhase.shortBreak);
    expect(skipped.focusSessionsInCycle, 0);
    expect(skipped.totalFocusSessionsCompleted, 0);
  });

  test('skipping a break jumps straight back to focus', () {
    var state = engine.start(PomodoroState.initial(config.focusMinutes));
    state = state.copyWith(phase: PomodoroPhase.shortBreak);
    final skipped = engine.skip(state);
    expect(skipped.phase, PomodoroPhase.focus);
    expect(skipped.remainingSeconds, config.focusMinutes * 60);
  });

  test('reset returns to a fresh idle state', () {
    var state = engine.start(PomodoroState.initial(config.focusMinutes));
    state = state.copyWith(remainingSeconds: 5, focusSessionsInCycle: 2);
    final reset = engine.reset(state);
    expect(reset.phase, PomodoroPhase.idle);
    expect(reset.focusSessionsInCycle, 0);
    expect(reset.remainingSeconds, config.focusMinutes * 60);
  });
}
