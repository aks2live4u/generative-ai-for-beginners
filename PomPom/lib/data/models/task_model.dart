class TaskModel {
  final String id;
  final String title;
  final bool completed;
  final DateTime createdAt;
  final DateTime? completedAt;

  const TaskModel({
    required this.id,
    required this.title,
    this.completed = false,
    required this.createdAt,
    this.completedAt,
  });

  TaskModel copyWith({String? title, bool? completed, DateTime? completedAt, bool clearCompletedAt = false}) {
    return TaskModel(
      id: id,
      title: title ?? this.title,
      completed: completed ?? this.completed,
      createdAt: createdAt,
      completedAt: clearCompletedAt ? null : (completedAt ?? this.completedAt),
    );
  }

  Map<String, dynamic> toMap() => {
    'id': id,
    'title': title,
    'completed': completed,
    'createdAt': createdAt.toIso8601String(),
    'completedAt': completedAt?.toIso8601String(),
  };

  factory TaskModel.fromMap(Map<dynamic, dynamic> map) => TaskModel(
    id: map['id'] as String,
    title: map['title'] as String,
    completed: map['completed'] as bool? ?? false,
    createdAt: DateTime.parse(map['createdAt'] as String),
    completedAt: map['completedAt'] != null ? DateTime.parse(map['completedAt'] as String) : null,
  );
}
