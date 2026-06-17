import 'package:uuid/uuid.dart';

import '../core/db/database_helper.dart';

class TagRepository {
  final _uuid = const Uuid();

  Future<String> getOrCreateTagId(String name) async {
    final db = await DatabaseHelper.instance.database;
    final normalized = name.trim().toLowerCase();
    final existing = await db.query('tags', where: 'name = ?', whereArgs: [normalized]);
    if (existing.isNotEmpty) return existing.first['id'] as String;
    final id = _uuid.v4();
    await db.insert('tags', {'id': id, 'name': normalized});
    return id;
  }

  Future<List<String>> allTagNames() async {
    final db = await DatabaseHelper.instance.database;
    final rows = await db.query('tags', orderBy: 'name ASC');
    return rows.map((e) => e['name'] as String).toList();
  }
}
