import 'package:flutter/foundation.dart';
import 'package:hive_flutter/hive_flutter.dart';
import '../models/task_model.dart';
import 'hive_boxes.dart';

class TaskRepository {
  Box<Map> get _box => HiveBoxes.tasksBox;

  List<TaskModel> getAll() {
    final tasks = _box.values.map((m) => TaskModel.fromMap(m)).toList();
    tasks.sort((a, b) => a.createdAt.compareTo(b.createdAt));
    return tasks;
  }

  ValueListenable<Box<Map>> listenable() => _box.listenable();

  Future<void> add(TaskModel task) => _box.put(task.id, task.toMap());

  Future<void> update(TaskModel task) => _box.put(task.id, task.toMap());

  Future<void> delete(String id) => _box.delete(id);

  Future<void> clearCompleted() async {
    final completedIds = getAll().where((t) => t.completed).map((t) => t.id).toList();
    await _box.deleteAll(completedIds);
  }
}
