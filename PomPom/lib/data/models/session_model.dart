enum SessionType { focus, shortBreak, longBreak }

/// One completed (or abandoned) timer session, used to compute statistics,
/// streaks and achievements. Only [SessionType.focus] sessions with
/// [completed] true count toward focus-hours / pomodoro counts.
class SessionModel {
  final String id;
  final SessionType type;
  final DateTime startedAt;
  final DateTime endedAt;
  final int plannedMinutes;
  final bool completed;

  const SessionModel({
    required this.id,
    required this.type,
    required this.startedAt,
    required this.endedAt,
    required this.plannedMinutes,
    required this.completed,
  });

  Duration get actualDuration => endedAt.difference(startedAt);

  Map<String, dynamic> toMap() => {
    'id': id,
    'type': type.name,
    'startedAt': startedAt.toIso8601String(),
    'endedAt': endedAt.toIso8601String(),
    'plannedMinutes': plannedMinutes,
    'completed': completed,
  };

  factory SessionModel.fromMap(Map<dynamic, dynamic> map) => SessionModel(
    id: map['id'] as String,
    type: SessionType.values.firstWhere((t) => t.name == map['type'], orElse: () => SessionType.focus),
    startedAt: DateTime.parse(map['startedAt'] as String),
    endedAt: DateTime.parse(map['endedAt'] as String),
    plannedMinutes: map['plannedMinutes'] as int,
    completed: map['completed'] as bool? ?? false,
  );
}
