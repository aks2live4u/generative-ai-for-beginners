import 'package:hive_flutter/hive_flutter.dart';

/// Box names, kept in one place so nothing can typo a string literal and
/// silently open a second, empty box.
class HiveBoxes {
  static const String tasks = 'pompom_tasks';
  static const String sessions = 'pompom_sessions';
  static const String settings = 'pompom_settings';
  static const String achievements = 'pompom_achievements';
  static const String widgetCommands = 'pompom_widget_commands';

  static const String settingsKey = 'settings';
  static const String widgetCommandKey = 'command';

  static Future<void> initAll() async {
    await Hive.initFlutter();
    await Future.wait([
      Hive.openBox<Map>(tasks),
      Hive.openBox<Map>(sessions),
      Hive.openBox<Map>(settings),
      Hive.openBox<String>(achievements),
      Hive.openBox<String>(widgetCommands),
    ]);
  }

  static Box<Map> get tasksBox => Hive.box<Map>(tasks);
  static Box<Map> get sessionsBox => Hive.box<Map>(sessions);
  static Box<Map> get settingsBox => Hive.box<Map>(settings);
  static Box<String> get achievementsBox => Hive.box<String>(achievements);
  static Box<String> get widgetCommandsBox => Hive.box<String>(widgetCommands);

  /// The foreground-service isolate persists sessions directly (it may run
  /// for a long time with no UI isolate alive to relay data to). Each Hive
  /// box instance only reflects writes made through *that* isolate's copy,
  /// so the UI isolate force-reloads from disk whenever the app resumes.
  static Future<void> refreshAll() async {
    await _reopenMap(tasks);
    await _reopenMap(sessions);
    await _reopenMap(settings);
    await _reopenString(achievements);
  }

  static Future<void> _reopenMap(String name) async {
    if (Hive.isBoxOpen(name)) await Hive.box<Map>(name).close();
    await Hive.openBox<Map>(name);
  }

  static Future<void> _reopenString(String name) async {
    if (Hive.isBoxOpen(name)) await Hive.box<String>(name).close();
    await Hive.openBox<String>(name);
  }
}
