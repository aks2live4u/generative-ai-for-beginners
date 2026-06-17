import 'package:uuid/uuid.dart';

import '../core/db/database_helper.dart';
import '../models/collection.dart';

class CollectionRepository {
  final _uuid = const Uuid();

  Future<List<Collection>> all() async {
    final db = await DatabaseHelper.instance.database;
    final rows = await db.query('collections', orderBy: 'pinned DESC, name ASC');
    final result = <Collection>[];
    for (final row in rows) {
      final countRows = await db.rawQuery(
        'SELECT COUNT(*) as c FROM content_collections WHERE collectionId = ?',
        [row['id']],
      );
      final count = countRows.first['c'] as int;
      result.add(Collection.fromMap(row, itemCount: count));
    }
    return result;
  }

  Future<Collection> create(String name, {String? description}) async {
    final db = await DatabaseHelper.instance.database;
    final collection = Collection(
      id: _uuid.v4(),
      name: name,
      description: description,
      createdAt: DateTime.now(),
    );
    await db.insert('collections', collection.toMap());
    return collection;
  }

  Future<void> rename(String id, String newName) async {
    final db = await DatabaseHelper.instance.database;
    await db.update('collections', {'name': newName}, where: 'id = ?', whereArgs: [id]);
  }

  Future<void> delete(String id) async {
    final db = await DatabaseHelper.instance.database;
    await db.delete('collections', where: 'id = ?', whereArgs: [id]);
  }

  Future<void> setPinned(String id, bool pinned) async {
    final db = await DatabaseHelper.instance.database;
    await db.update('collections', {'pinned': pinned ? 1 : 0}, where: 'id = ?', whereArgs: [id]);
  }

  Future<void> setFavorite(String id, bool favorite) async {
    final db = await DatabaseHelper.instance.database;
    await db.update('collections', {'favorite': favorite ? 1 : 0}, where: 'id = ?', whereArgs: [id]);
  }

  Future<void> setCoverImage(String id, String? coverImage) async {
    final db = await DatabaseHelper.instance.database;
    await db.update('collections', {'coverImage': coverImage}, where: 'id = ?', whereArgs: [id]);
  }

  /// Merges [sourceId] into [targetId]: moves all items, then deletes the source collection.
  Future<void> merge(String sourceId, String targetId) async {
    final db = await DatabaseHelper.instance.database;
    final items = await db.query('content_collections', where: 'collectionId = ?', whereArgs: [sourceId]);
    for (final item in items) {
      final contentId = item['contentId'] as String;
      final existing = await db.query(
        'content_collections',
        where: 'contentId = ? AND collectionId = ?',
        whereArgs: [contentId, targetId],
      );
      if (existing.isEmpty) {
        await db.insert('content_collections', {'contentId': contentId, 'collectionId': targetId});
      }
    }
    await delete(sourceId);
  }
}
