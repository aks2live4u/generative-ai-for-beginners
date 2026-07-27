import 'package:flutter_test/flutter_test.dart';
import 'package:pompom/domain/achievements/achievement_engine.dart';

void main() {
  test('no achievements met with zero activity', () {
    const stats = AchievementStats(
      totalCompletedSessions: 0,
      currentStreak: 0,
      longestStreak: 0,
      totalFocusMinutes: 0,
      metDailyGoalAtLeastOnce: false,
    );
    expect(metAchievementIds(stats), isEmpty);
  });

  test('first session unlocks only the first-session achievement', () {
    const stats = AchievementStats(
      totalCompletedSessions: 1,
      currentStreak: 1,
      longestStreak: 1,
      totalFocusMinutes: 25,
      metDailyGoalAtLeastOnce: false,
    );
    expect(metAchievementIds(stats), ['first_session']);
  });

  test('ten sessions unlocks both first-session and ten-session achievements', () {
    const stats = AchievementStats(
      totalCompletedSessions: 10,
      currentStreak: 1,
      longestStreak: 1,
      totalFocusMinutes: 250,
      metDailyGoalAtLeastOnce: false,
    );
    final ids = metAchievementIds(stats);
    expect(ids, containsAll(['first_session', 'ten_sessions']));
  });

  test('busy bee requires hitting the daily goal at least once', () {
    const stats = AchievementStats(
      totalCompletedSessions: 3,
      currentStreak: 1,
      longestStreak: 1,
      totalFocusMinutes: 75,
      metDailyGoalAtLeastOnce: true,
    );
    expect(metAchievementIds(stats), contains('busy_bee'));
  });

  test('consistency requires a 7 day streak (current or longest)', () {
    const stats = AchievementStats(
      totalCompletedSessions: 7,
      currentStreak: 2,
      longestStreak: 7,
      totalFocusMinutes: 175,
      metDailyGoalAtLeastOnce: false,
    );
    expect(metAchievementIds(stats), contains('consistency'));
  });

  test('focus master requires 100 completed sessions', () {
    const stats = AchievementStats(
      totalCompletedSessions: 99,
      currentStreak: 0,
      longestStreak: 0,
      totalFocusMinutes: 0,
      metDailyGoalAtLeastOnce: false,
    );
    expect(metAchievementIds(stats), isNot(contains('focus_master')));

    const metStats = AchievementStats(
      totalCompletedSessions: 100,
      currentStreak: 0,
      longestStreak: 0,
      totalFocusMinutes: 0,
      metDailyGoalAtLeastOnce: false,
    );
    expect(metAchievementIds(metStats), contains('focus_master'));
  });

  test('100 hours requires 6000 total focus minutes', () {
    const stats = AchievementStats(
      totalCompletedSessions: 0,
      currentStreak: 0,
      longestStreak: 0,
      totalFocusMinutes: 6000,
      metDailyGoalAtLeastOnce: false,
    );
    expect(metAchievementIds(stats), contains('hundred_hours'));
  });

  test('every definition has a unique id', () {
    final ids = kAchievementDefs.map((d) => d.id).toSet();
    expect(ids.length, kAchievementDefs.length);
  });
}
