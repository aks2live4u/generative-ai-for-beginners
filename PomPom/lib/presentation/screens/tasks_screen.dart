import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';

import '../../core/utils/id_gen.dart';
import '../../data/models/task_model.dart';
import '../providers/repositories_providers.dart';

class TasksScreen extends ConsumerStatefulWidget {
  const TasksScreen({super.key});

  @override
  ConsumerState<TasksScreen> createState() => _TasksScreenState();
}

class _TasksScreenState extends ConsumerState<TasksScreen> {
  final _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _addTask() {
    final title = _controller.text.trim();
    if (title.isEmpty) return;
    final repo = ref.read(taskRepositoryProvider);
    repo.add(TaskModel(id: generateId(), title: title, createdAt: DateTime.now()));
    _controller.clear();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final repo = ref.read(taskRepositoryProvider);

    return Scaffold(
      appBar: AppBar(title: const Text("Today's Tasks")),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 12, 20, 8),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _controller,
                    decoration: const InputDecoration(hintText: 'Add a little task…', border: OutlineInputBorder()),
                    onSubmitted: (_) => _addTask(),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton.filled(onPressed: _addTask, icon: const Icon(Icons.add_rounded)),
              ],
            ),
          ),
          Expanded(
            child: ValueListenableBuilder<Box<Map>>(
              valueListenable: repo.listenable(),
              builder: (context, box, _) {
                final tasks = repo.getAll();
                if (tasks.isEmpty) {
                  return Center(child: Text('No tasks yet — add something small 🌱', style: theme.textTheme.bodyLarge));
                }
                return ListView.builder(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  itemCount: tasks.length,
                  itemBuilder: (context, index) {
                    final task = tasks[index];
                    return Dismissible(
                      key: ValueKey(task.id),
                      background: _swipeBackground(alignStart: true, color: Colors.green, icon: Icons.check_rounded),
                      secondaryBackground: _swipeBackground(
                        alignStart: false,
                        color: Colors.redAccent,
                        icon: Icons.delete_rounded,
                      ),
                      confirmDismiss: (direction) async {
                        if (direction == DismissDirection.startToEnd) {
                          repo.update(task.copyWith(completed: !task.completed, completedAt: DateTime.now()));
                          return false;
                        }
                        return true;
                      },
                      onDismissed: (_) => repo.delete(task.id),
                      child: Card(
                        margin: const EdgeInsets.symmetric(vertical: 6),
                        child: ListTile(
                          leading: Icon(
                            task.completed ? Icons.check_circle_rounded : Icons.circle_outlined,
                            color: task.completed
                                ? theme.colorScheme.primary
                                : theme.colorScheme.onSurface.withValues(alpha: 0.4),
                          ),
                          title: Text(
                            task.title,
                            style: theme.textTheme.bodyLarge?.copyWith(
                              decoration: task.completed ? TextDecoration.lineThrough : null,
                              color: task.completed ? theme.colorScheme.onSurface.withValues(alpha: 0.5) : null,
                            ),
                          ),
                        ),
                      ),
                    );
                  },
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _swipeBackground({required bool alignStart, required Color color, required IconData icon}) {
    return Container(
      alignment: alignStart ? Alignment.centerLeft : Alignment.centerRight,
      padding: const EdgeInsets.symmetric(horizontal: 24),
      decoration: BoxDecoration(color: color.withValues(alpha: 0.85), borderRadius: BorderRadius.circular(20)),
      child: Icon(icon, color: Colors.white),
    );
  }
}
