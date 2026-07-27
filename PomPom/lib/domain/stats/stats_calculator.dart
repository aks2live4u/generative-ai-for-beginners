import '../../core/utils/date_utils.dart';
import '../../data/models/session_model.dart';

class PeriodStats {
  final Duration totalFocusTime;
  final int completedSessions;
  final double averageFocusMinutes;
  final Map<DateTime, int> sessionsPerDay;
  final Map<DateTime, int> focusMinutesPerDay;

  const PeriodStats({
    required this.totalFocusTime,
    required this.completedSessions,
    required this.averageFocusMinutes,
    required this.sessionsPerDay,
    required this.focusMinutesPerDay,
  });

  static const empty = PeriodStats(
    totalFocusTime: Duration.zero,
    completedSessions: 0,
    averageFocusMinutes: 0,
    sessionsPerDay: {},
    focusMinutesPerDay: {},
  );
}

class StreakInfo {
  final int current;
  final int longest;
  const StreakInfo({required this.current, required this.longest});
}

/// Pure aggregation functions over raw session history. No Flutter/platform
/// dependencies, so streak/stat math can be unit tested directly.
class StatsCalculator {
  static List<SessionModel> completedFocusSessions(List<SessionModel> sessions) =>
      sessions.where((s) => s.type == SessionType.focus && s.completed).toList();

  static PeriodStats forRange(List<SessionModel> sessions, DateTime start, DateTime endExclusive) {
    final focus = completedFocusSessions(
      sessions,
    ).where((s) => !s.startedAt.isBefore(start) && s.startedAt.isBefore(endExclusive)).toList();
    if (focus.isEmpty) return PeriodStats.empty;

    final totalMinutes = focus.fold<int>(0, (sum, s) => sum + s.plannedMinutes);
    final sessionsPerDay = <DateTime, int>{};
    final minutesPerDay = <DateTime, int>{};
    for (final s in focus) {
      final key = dayKey(s.startedAt);
      sessionsPerDay[key] = (sessionsPerDay[key] ?? 0) + 1;
      minutesPerDay[key] = (minutesPerDay[key] ?? 0) + s.plannedMinutes;
    }

    return PeriodStats(
      totalFocusTime: Duration(minutes: totalMinutes),
      completedSessions: focus.length,
      averageFocusMinutes: totalMinutes / focus.length,
      sessionsPerDay: sessionsPerDay,
      focusMinutesPerDay: minutesPerDay,
    );
  }

  static PeriodStats today(List<SessionModel> sessions, {DateTime? now}) {
    final d = dayKey(now ?? DateTime.now());
    return forRange(sessions, d, d.add(const Duration(days: 1)));
  }

  static PeriodStats thisWeek(List<SessionModel> sessions, {DateTime? now}) {
    final start = startOfWeek(now ?? DateTime.now());
    return forRange(sessions, start, start.add(const Duration(days: 7)));
  }

  static PeriodStats thisMonth(List<SessionModel> sessions, {DateTime? now}) {
    final n = now ?? DateTime.now();
    final start = startOfMonth(n);
    final nextMonth = start.month == 12 ? DateTime(start.year + 1, 1, 1) : DateTime(start.year, start.month + 1, 1);
    return forRange(sessions, start, nextMonth);
  }

  /// Current streak counts consecutive days with >=1 completed focus session,
  /// walking backward from today. If today has none yet, we start checking
  /// from yesterday so a streak isn't broken before the day is even over.
  static StreakInfo streaks(List<SessionModel> sessions, {DateTime? now}) {
    final today = dayKey(now ?? DateTime.now());
    final focusDays = completedFocusSessions(sessions).map((s) => dayKey(s.startedAt)).toSet();
    if (focusDays.isEmpty) return const StreakInfo(current: 0, longest: 0);

    int current = 0;
    DateTime cursor = focusDays.contains(today) ? today : today.subtract(const Duration(days: 1));
    while (focusDays.contains(cursor)) {
      current++;
      cursor = cursor.subtract(const Duration(days: 1));
    }

    final sortedDays = focusDays.toList()..sort();
    int longest = 0;
    int run = 0;
    DateTime? prev;
    for (final d in sortedDays) {
      run = (prev != null && d.difference(prev).inDays == 1) ? run + 1 : 1;
      if (run > longest) longest = run;
      prev = d;
    }

    return StreakInfo(current: current, longest: longest);
  }

  static int totalFocusMinutesAllTime(List<SessionModel> sessions) =>
      completedFocusSessions(sessions).fold<int>(0, (sum, s) => sum + s.plannedMinutes);

  /// Completed minutes grouped by session type within [start, endExclusive) -
  /// used for the "how did my time split up" pie chart.
  static Map<SessionType, int> minutesByType(List<SessionModel> sessions, DateTime start, DateTime endExclusive) {
    final result = <SessionType, int>{};
    for (final s in sessions) {
      if (!s.completed) continue;
      if (s.startedAt.isBefore(start) || !s.startedAt.isBefore(endExclusive)) continue;
      result[s.type] = (result[s.type] ?? 0) + s.plannedMinutes;
    }
    return result;
  }
}
