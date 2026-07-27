/// Snapshot of the numbers achievements care about. Computed once from
/// session history and handed to every definition's [AchievementDef.isMet].
class AchievementStats {
  final int totalCompletedSessions;
  final int currentStreak;
  final int longestStreak;
  final int totalFocusMinutes;
  final bool metDailyGoalAtLeastOnce;

  const AchievementStats({
    required this.totalCompletedSessions,
    required this.currentStreak,
    required this.longestStreak,
    required this.totalFocusMinutes,
    required this.metDailyGoalAtLeastOnce,
  });
}

class AchievementDef {
  final String id;
  final String emoji;
  final String title;
  final String description;
  final bool Function(AchievementStats) isMet;

  const AchievementDef({
    required this.id,
    required this.emoji,
    required this.title,
    required this.description,
    required this.isMet,
  });
}

final List<AchievementDef> kAchievementDefs = [
  AchievementDef(
    id: 'first_session',
    emoji: '🌱',
    title: 'First Session',
    description: 'Complete your very first focus session.',
    isMet: (s) => s.totalCompletedSessions >= 1,
  ),
  AchievementDef(
    id: 'ten_sessions',
    emoji: '🌼',
    title: '10 Sessions',
    description: 'Complete 10 focus sessions.',
    isMet: (s) => s.totalCompletedSessions >= 10,
  ),
  AchievementDef(
    id: 'busy_bee',
    emoji: '🐝',
    title: 'Busy Bee',
    description: 'Hit your daily goal in a single day.',
    isMet: (s) => s.metDailyGoalAtLeastOnce,
  ),
  AchievementDef(
    id: 'consistency',
    emoji: '🐢',
    title: 'Consistency',
    description: 'Reach a 7 day streak.',
    isMet: (s) => s.currentStreak >= 7 || s.longestStreak >= 7,
  ),
  AchievementDef(
    id: 'focus_master',
    emoji: '🌈',
    title: 'Focus Master',
    description: 'Complete 100 focus sessions.',
    isMet: (s) => s.totalCompletedSessions >= 100,
  ),
  AchievementDef(
    id: 'hundred_hours',
    emoji: '⭐',
    title: '100 Hours',
    description: 'Rack up 100 hours of total focus time.',
    isMet: (s) => s.totalFocusMinutes >= 100 * 60,
  ),
];

/// Returns the ids of every achievement satisfied by [stats], regardless of
/// whether they were already unlocked before - callers diff against
/// previously-unlocked ids to detect newly-earned ones.
List<String> metAchievementIds(AchievementStats stats) =>
    kAchievementDefs.where((d) => d.isMet(stats)).map((d) => d.id).toList();
