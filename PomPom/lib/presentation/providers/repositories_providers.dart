import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/achievement_repository.dart';
import '../../data/repositories/session_repository.dart';
import '../../data/repositories/settings_repository.dart';
import '../../data/repositories/task_repository.dart';
import '../../data/repositories/widget_command_repository.dart';

final taskRepositoryProvider = Provider<TaskRepository>((ref) => TaskRepository());
final sessionRepositoryProvider = Provider<SessionRepository>((ref) => SessionRepository());
final settingsRepositoryProvider = Provider<SettingsRepository>((ref) => SettingsRepository());
final achievementRepositoryProvider = Provider<AchievementRepository>((ref) => AchievementRepository());
final widgetCommandRepositoryProvider = Provider<WidgetCommandRepository>((ref) => WidgetCommandRepository());

/// Bumped whenever the app resumes and Hive boxes are force-reloaded from
/// disk, so screens reading session/achievement data know to rebuild even
/// though those boxes aren't watched through Riverpod directly.
final refreshTickProvider = StateProvider<int>((ref) => 0);
