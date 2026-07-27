import 'package:hive_flutter/hive_flutter.dart';
import 'package:home_widget/home_widget.dart';
import '../data/repositories/hive_boxes.dart';
import '../data/repositories/widget_command_repository.dart';
import '../domain/timer/pomodoro_phase.dart';

/// Handles taps on the widget's Pause/Resume button. Runs in a short-lived
/// headless isolate spawned just for this event (per home_widget's
/// interactivity mechanism), so it can't reach the running foreground task
/// isolate directly - it drops a command in Hive instead, which the task
/// handler's onRepeatEvent polls every second (see WidgetCommandRepository).
@pragma('vm:entry-point')
Future<void> pompomWidgetBackgroundCallback(Uri? uri) async {
  final action = uri?.host;
  if (action != 'pause' && action != 'resume' && action != 'skip') return;
  if (!Hive.isBoxOpen(HiveBoxes.widgetCommands)) {
    await Hive.initFlutter();
    await Hive.openBox<String>(HiveBoxes.widgetCommands);
  }
  await WidgetCommandRepository().pushCommand(action!);
}

/// Pushes live timer state to the Android home screen widget. Called from
/// both the main isolate (when the app is open) and the foreground task
/// isolate (while a session is running in the background) - home_widget
/// supports being driven from either.
class HomeWidgetService {
  static const String _androidReceiver = 'com.pompom.pompom.PomPomWidgetProvider';

  static Future<void> push({
    required PomodoroPhase phase,
    required int remainingSeconds,
    required int totalSeconds,
    required bool isRunning,
  }) async {
    final minutes = (remainingSeconds ~/ 60).toString().padLeft(2, '0');
    final seconds = (remainingSeconds % 60).toString().padLeft(2, '0');
    // Stored as an int percentage (not a double) so the Kotlin side can read
    // it with a plain getInt - home_widget encodes doubles as raw long bits,
    // which isn't worth the complexity here.
    final progressPercent = totalSeconds == 0 ? 0 : (100 - (remainingSeconds * 100 / totalSeconds)).round();

    await Future.wait([
      HomeWidget.saveWidgetData<String>('time_text', '$minutes:$seconds'),
      HomeWidget.saveWidgetData<String>('phase_label', _label(phase)),
      HomeWidget.saveWidgetData<int>('progress_percent', progressPercent),
      HomeWidget.saveWidgetData<bool>('is_running', isRunning),
      HomeWidget.saveWidgetData<bool>('is_active', phase != PomodoroPhase.idle),
    ]);
    await HomeWidget.updateWidget(qualifiedAndroidName: _androidReceiver);
  }

  static Future<void> clear() async {
    await Future.wait([
      HomeWidget.saveWidgetData<String>('time_text', '--:--'),
      HomeWidget.saveWidgetData<String>('phase_label', 'PomPom'),
      HomeWidget.saveWidgetData<int>('progress_percent', 0),
      HomeWidget.saveWidgetData<bool>('is_running', false),
      HomeWidget.saveWidgetData<bool>('is_active', false),
    ]);
    await HomeWidget.updateWidget(qualifiedAndroidName: _androidReceiver);
  }

  static String _label(PomodoroPhase phase) => switch (phase) {
    PomodoroPhase.idle => 'Ready?',
    PomodoroPhase.focus => 'Focus Time',
    PomodoroPhase.shortBreak => 'Break',
    PomodoroPhase.longBreak => 'Long Break',
  };
}
