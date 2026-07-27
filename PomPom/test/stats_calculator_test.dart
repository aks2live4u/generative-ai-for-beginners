import 'package:flutter_test/flutter_test.dart';
import 'package:pompom/data/models/session_model.dart';
import 'package:pompom/domain/stats/stats_calculator.dart';

SessionModel _focusSession(DateTime day, {int minutes = 25, bool completed = true}) {
  return SessionModel(
    id: '${day.toIso8601String()}-$minutes',
    type: SessionType.focus,
    startedAt: day,
    endedAt: day.add(Duration(minutes: minutes)),
    plannedMinutes: minutes,
    completed: completed,
  );
}

void main() {
  final now = DateTime(2026, 7, 27, 10); // a Monday

  test('today() only counts sessions started today', () {
    final sessions = [
      _focusSession(DateTime(2026, 7, 27, 8), minutes: 25),
      _focusSession(DateTime(2026, 7, 26, 8), minutes: 25),
    ];
    final stats = StatsCalculator.today(sessions, now: now);
    expect(stats.completedSessions, 1);
    expect(stats.totalFocusTime, const Duration(minutes: 25));
  });

  test('incomplete sessions are excluded from stats', () {
    final sessions = [_focusSession(now, completed: false)];
    final stats = StatsCalculator.today(sessions, now: now);
    expect(stats.completedSessions, 0);
  });

  test('break sessions are excluded from focus stats', () {
    final sessions = [
      SessionModel(
        id: '1',
        type: SessionType.shortBreak,
        startedAt: now,
        endedAt: now.add(const Duration(minutes: 5)),
        plannedMinutes: 5,
        completed: true,
      ),
    ];
    final stats = StatsCalculator.today(sessions, now: now);
    expect(stats.completedSessions, 0);
  });

  test('streaks: consecutive days counted, gap breaks the streak', () {
    final sessions = [
      _focusSession(now),
      _focusSession(now.subtract(const Duration(days: 1))),
      _focusSession(now.subtract(const Duration(days: 2))),
      // gap at day 3
      _focusSession(now.subtract(const Duration(days: 4))),
    ];
    final streaks = StatsCalculator.streaks(sessions, now: now);
    expect(streaks.current, 3);
    expect(streaks.longest, 3);
  });

  test('streak counts from yesterday if today has no sessions yet', () {
    final sessions = [
      _focusSession(now.subtract(const Duration(days: 1))),
      _focusSession(now.subtract(const Duration(days: 2))),
    ];
    final streaks = StatsCalculator.streaks(sessions, now: now);
    expect(streaks.current, 2);
  });

  test('longest streak can exceed the current streak', () {
    final sessions = [
      _focusSession(now), // current streak of 1
      _focusSession(now.subtract(const Duration(days: 10))),
      _focusSession(now.subtract(const Duration(days: 11))),
      _focusSession(now.subtract(const Duration(days: 12))),
      _focusSession(now.subtract(const Duration(days: 13))),
      _focusSession(now.subtract(const Duration(days: 14))),
    ];
    final streaks = StatsCalculator.streaks(sessions, now: now);
    expect(streaks.current, 1);
    expect(streaks.longest, 5);
  });

  test('no sessions means zero streaks', () {
    final streaks = StatsCalculator.streaks([], now: now);
    expect(streaks.current, 0);
    expect(streaks.longest, 0);
  });

  test('minutesByType groups completed sessions by type within range', () {
    final sessions = [
      _focusSession(now, minutes: 25),
      SessionModel(
        id: 'b1',
        type: SessionType.shortBreak,
        startedAt: now,
        endedAt: now.add(const Duration(minutes: 5)),
        plannedMinutes: 5,
        completed: true,
      ),
    ];
    final byType = StatsCalculator.minutesByType(
      sessions,
      now.subtract(const Duration(days: 1)),
      now.add(const Duration(days: 1)),
    );
    expect(byType[SessionType.focus], 25);
    expect(byType[SessionType.shortBreak], 5);
  });

  test('totalFocusMinutesAllTime sums every completed focus session regardless of date', () {
    final sessions = [
      _focusSession(now, minutes: 25),
      _focusSession(now.subtract(const Duration(days: 200)), minutes: 45),
    ];
    expect(StatsCalculator.totalFocusMinutesAllTime(sessions), 70);
  });
}
