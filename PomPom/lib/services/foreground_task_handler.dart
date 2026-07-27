import 'dart:async';

import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:hive_flutter/hive_flutter.dart';

import '../core/utils/id_gen.dart';
import '../data/models/session_model.dart';
import '../data/repositories/hive_boxes.dart';
import '../data/repositories/session_repository.dart';
import '../data/repositories/settings_repository.dart';
import '../data/repositories/widget_command_repository.dart';
import '../domain/timer/pomodoro_engine.dart';
import '../domain/timer/pomodoro_phase.dart';
import 'home_widget_service.dart';
import 'notification_service.dart';

/// Entry point the OS spawns a fresh headless isolate for when the
/// foreground service (re)starts. Must stay top-level + `vm:entry-point`,
/// per flutter_foreground_task's requirements.
@pragma('vm:entry-point')
void pompomTaskHandlerStartCallback() {
  FlutterForegroundTask.setTaskHandler(PompomTaskHandler());
}

/// Owns the live Pomodoro countdown while the app is backgrounded or its
/// Activity has been destroyed. This isolate is the durable source of truth
/// for session completions - the main UI isolate only mirrors what this
/// handler broadcasts, since it may not even be alive when a phase ends.
class PompomTaskHandler extends TaskHandler {
  PomodoroEngine? _engine;
  PomodoroState _state = PomodoroState.initial(25);
  DateTime? _phaseStartedAt;

  final SettingsRepository _settingsRepository = SettingsRepository();
  final SessionRepository _sessionRepository = SessionRepository();
  final WidgetCommandRepository _widgetCommandRepository = WidgetCommandRepository();

  @override
  Future<void> onStart(DateTime timestamp, TaskStarter starter) async {
    if (!Hive.isBoxOpen(HiveBoxes.settings)) {
      await HiveBoxes.initAll();
    }
    await NotificationService.instance.init();

    final settings = _settingsRepository.get();
    _engine = PomodoroEngine(
      PomodoroConfig(
        focusMinutes: settings.focusMinutes,
        breakMinutes: settings.breakMinutes,
        longBreakMinutes: settings.longBreakMinutes,
        autoStartBreak: settings.autoStartBreak,
        autoStartNextFocus: settings.autoStartNextFocus,
      ),
    );
    _state = _engine!.start(PomodoroState.initial(settings.focusMinutes));
    _phaseStartedAt = DateTime.now();

    await _refreshNotification();
    await _pushWidget();
    _broadcastState();
  }

  @override
  void onRepeatEvent(DateTime timestamp) {
    final pendingCommand = _widgetCommandRepository.takeCommand();
    if (pendingCommand != null) {
      _applyCommand(pendingCommand);
      return;
    }

    final engine = _engine;
    if (engine == null) return;

    final result = engine.tick(_state);
    final completedPhase = result.completedPhase;
    _state = result.state;

    if (completedPhase != null) {
      unawaited(_recordCompletedSession(completedPhase));
      _phaseStartedAt = DateTime.now();
      unawaited(_notifyPhaseTransition(completedPhase, _state.phase));
    }

    unawaited(_refreshNotification());
    unawaited(_pushWidget());
    _broadcastState(completedPhase: completedPhase);
  }

  @override
  void onReceiveData(Object data) {
    if (data is String) _applyCommand(data);
  }

  @override
  void onNotificationButtonPressed(String id) => _applyCommand(id);

  @override
  Future<void> onDestroy(DateTime timestamp, bool isTimeout) async {
    await HomeWidgetService.clear();
  }

  void _applyCommand(String command) {
    final engine = _engine;
    if (engine == null) return;
    switch (command) {
      case 'pause':
        _state = engine.pause(_state);
      case 'resume':
        _state = engine.resume(_state);
      case 'skip':
        _state = engine.skip(_state);
        _phaseStartedAt = DateTime.now();
      case 'stop':
        FlutterForegroundTask.stopService();
        return;
      case 'request_state':
        break;
    }
    unawaited(_refreshNotification());
    unawaited(_pushWidget());
    _broadcastState();
  }

  Future<SessionModel> _recordCompletedSession(PomodoroPhase completedPhase) async {
    final type = completedPhase.sessionType!;
    final plannedMinutes = _plannedMinutesFor(completedPhase);
    final session = SessionModel(
      id: generateId(),
      type: type,
      startedAt: _phaseStartedAt ?? DateTime.now().subtract(Duration(minutes: plannedMinutes)),
      endedAt: DateTime.now(),
      plannedMinutes: plannedMinutes,
      completed: true,
    );
    await _sessionRepository.add(session);
    return session;
  }

  int _plannedMinutesFor(PomodoroPhase completedPhase) {
    final settings = _settingsRepository.get();
    return switch (completedPhase) {
      PomodoroPhase.focus => settings.focusMinutes,
      PomodoroPhase.shortBreak => settings.breakMinutes,
      PomodoroPhase.longBreak => settings.longBreakMinutes,
      PomodoroPhase.idle => 0,
    };
  }

  Future<void> _notifyPhaseTransition(PomodoroPhase completedPhase, PomodoroPhase nextPhase) async {
    if (completedPhase == PomodoroPhase.focus) {
      await NotificationService.instance.sessionCompleted();
      if (nextPhase == PomodoroPhase.longBreak) {
        await NotificationService.instance.timeToRelax();
      } else {
        await NotificationService.instance.breakStarted();
      }
    } else {
      await NotificationService.instance.readyToFocus();
    }
  }

  Future<void> _refreshNotification() async {
    final phaseLabel = switch (_state.phase) {
      PomodoroPhase.focus => '🍓 Focus Time',
      PomodoroPhase.shortBreak => '🌿 Break Time',
      PomodoroPhase.longBreak => '🌈 Long Break',
      PomodoroPhase.idle => 'PomPom',
    };
    final minutes = (_state.remainingSeconds ~/ 60).toString().padLeft(2, '0');
    final seconds = (_state.remainingSeconds % 60).toString().padLeft(2, '0');
    final statusText = _state.isRunning ? '$minutes:$seconds remaining' : '$minutes:$seconds · Paused';

    await FlutterForegroundTask.updateService(
      notificationTitle: phaseLabel,
      notificationText: statusText,
      notificationButtons: [
        NotificationButton(id: _state.isRunning ? 'pause' : 'resume', text: _state.isRunning ? 'Pause' : 'Resume'),
        const NotificationButton(id: 'skip', text: 'Skip'),
      ],
    );
  }

  Future<void> _pushWidget() => HomeWidgetService.push(
    phase: _state.phase,
    remainingSeconds: _state.remainingSeconds,
    totalSeconds: _state.totalSeconds,
    isRunning: _state.isRunning,
  );

  void _broadcastState({PomodoroPhase? completedPhase}) {
    FlutterForegroundTask.sendDataToMain({
      'type': completedPhase != null ? 'phase_completed' : 'state',
      'phase': _state.phase.name,
      'remainingSeconds': _state.remainingSeconds,
      'totalSeconds': _state.totalSeconds,
      'isRunning': _state.isRunning,
      'focusSessionsInCycle': _state.focusSessionsInCycle,
      'totalFocusSessionsCompleted': _state.totalFocusSessionsCompleted,
      if (completedPhase != null) 'completedPhase': completedPhase.name,
    });
  }
}
