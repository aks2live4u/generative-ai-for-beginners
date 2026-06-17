import 'package:uuid/uuid.dart';

import '../core/db/database_helper.dart';
import '../models/content_item.dart';
import 'tag_repository.dart';

class ContentRepository {
  final _uuid = const Uuid();
  final _tagRepo = TagRepository();

  Future<List<String>> _tagsFor(String contentId) async {
    final db = await DatabaseHelper.instance.database;
    final rows = await db.rawQuery('''
      SELECT t.name FROM tags t
      INNER JOIN content_tags ct ON ct.tagId = t.id
      WHERE ct.contentId = ? ORDER BY t.name ASC
    ''', [contentId]);
    return rows.map((e) => e['name'] as String).toList();
  }

  /// Ids of items already tagged with [tagName] (case-insensitive), used to
  /// decide whether enough related items exist to justify auto-creating a
  /// collection for it.
  Future<List<String>> contentIdsWithTag(String tagName) async {
    final db = await DatabaseHelper.instance.database;
    final rows = await db.rawQuery('''
      SELECT ct.contentId FROM content_tags ct
      INNER JOIN tags t ON t.id = ct.tagId
      WHERE t.name = ?
    ''', [tagName.trim().toLowerCase()]);
    return rows.map((e) => e['contentId'] as String).toList();
  }

  Future<void> addToCollection(String contentId, String collectionId) async {
    final db = await DatabaseHelper.instance.database;
    final existing = await db.query(
      'content_collections',
      where: 'contentId = ? AND collectionId = ?',
      whereArgs: [contentId, collectionId],
    );
    if (existing.isEmpty) {
      await db.insert('content_collections', {'contentId': contentId, 'collectionId': collectionId});
    }
  }

  Future<List<String>> _collectionsFor(String contentId) async {
    final db = await DatabaseHelper.instance.database;
    final rows = await db.rawQuery('''
      SELECT collectionId FROM content_collections WHERE contentId = ?
    ''', [contentId]);
    return rows.map((e) => e['collectionId'] as String).toList();
  }

  Future<ContentItem> _hydrate(Map<String, dynamic> row) async {
    final id = row['id'] as String;
    final tags = await _tagsFor(id);
    final collections = await _collectionsFor(id);
    return ContentItem.fromMap(row, tags: tags, collectionIds: collections);
  }

  Future<ContentItem> create(ContentItem draft) async {
    final db = await DatabaseHelper.instance.database;
    final id = draft.id.isEmpty ? _uuid.v4() : draft.id;
    final item = draft.copyWith()._withId(id);
    await db.insert('content', item.toMap());
    await _applyTags(id, item.tags);
    await _applyCollections(id, item.collectionIds);
    await DatabaseHelper.instance.syncFts(id);
    return item;
  }

  Future<void> update(ContentItem item) async {
    final db = await DatabaseHelper.instance.database;
    await db.update('content', item.toMap(), where: 'id = ?', whereArgs: [item.id]);
    await _applyTags(item.id, item.tags);
    await _applyCollections(item.id, item.collectionIds);
    await DatabaseHelper.instance.syncFts(item.id);
  }

  Future<void> _applyTags(String contentId, List<String> tagNames) async {
    final db = await DatabaseHelper.instance.database;
    await db.delete('content_tags', where: 'contentId = ?', whereArgs: [contentId]);
    for (final name in tagNames) {
      final tagId = await _tagRepo.getOrCreateTagId(name);
      await db.insert('content_tags', {'contentId': contentId, 'tagId': tagId});
    }
  }

  Future<void> _applyCollections(String contentId, List<String> collectionIds) async {
    final db = await DatabaseHelper.instance.database;
    await db.delete('content_collections', where: 'contentId = ?', whereArgs: [contentId]);
    for (final cid in collectionIds) {
      await db.insert('content_collections', {'contentId': contentId, 'collectionId': cid});
    }
  }

  Future<void> delete(String id) async {
    final db = await DatabaseHelper.instance.database;
    await db.delete('content', where: 'id = ?', whereArgs: [id]);
    await DatabaseHelper.instance.syncFts(id);
  }

  Future<void> toggleFavorite(String id, bool favorite) async {
    final db = await DatabaseHelper.instance.database;
    await db.update('content', {'favorite': favorite ? 1 : 0}, where: 'id = ?', whereArgs: [id]);
  }

  Future<void> markOpened(String id) async {
    final db = await DatabaseHelper.instance.database;
    final rows = await db.query('content', columns: ['openCount'], where: 'id = ?', whereArgs: [id]);
    final current = rows.isEmpty ? 0 : (rows.first['openCount'] as int? ?? 0);
    await db.update(
      'content',
      {'openCount': current + 1, 'lastOpenedAt': DateTime.now().toIso8601String()},
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  Future<List<ContentItem>> all({
    bool includeArchived = false,
    bool includeInbox = true,
    List<ContentType>? types,
    String? collectionId,
    bool onlyFavorites = false,
    String orderBy = 'createdAt DESC',
  }) async {
    final db = await DatabaseHelper.instance.database;
    final where = <String>[];
    final args = <Object?>[];
    if (!includeArchived) where.add('archived = 0');
    if (!includeInbox) where.add('inInbox = 0');
    if (onlyFavorites) where.add('favorite = 1');
    if (types != null && types.isNotEmpty) {
      where.add('type IN (${types.map((_) => '?').join(',')})');
      args.addAll(types.map((t) => t.name));
    }

    List<Map<String, dynamic>> rows;
    if (collectionId != null) {
      rows = await db.rawQuery('''
        SELECT c.* FROM content c
        INNER JOIN content_collections cc ON cc.contentId = c.id
        WHERE cc.collectionId = ? ${where.isNotEmpty ? 'AND ${where.join(' AND ')}' : ''}
        ORDER BY $orderBy
      ''', [collectionId, ...args]);
    } else {
      rows = await db.query(
        'content',
        where: where.isEmpty ? null : where.join(' AND '),
        whereArgs: args.isEmpty ? null : args,
        orderBy: orderBy,
      );
    }
    return Future.wait(rows.map(_hydrate));
  }

  Future<ContentItem?> getById(String id) async {
    final db = await DatabaseHelper.instance.database;
    final rows = await db.query('content', where: 'id = ?', whereArgs: [id]);
    if (rows.isEmpty) return null;
    return _hydrate(rows.first);
  }

  Future<List<ContentItem>> search(String query) async {
    final ids = await DatabaseHelper.instance.searchContentIds(query);
    if (ids.isEmpty) return [];
    final db = await DatabaseHelper.instance.database;
    final rows = await db.query(
      'content',
      where: 'id IN (${ids.map((_) => '?').join(',')}) AND archived = 0',
      whereArgs: ids,
    );
    return Future.wait(rows.map(_hydrate));
  }
}

extension on ContentItem {
  ContentItem _withId(String newId) => ContentItem(
        id: newId,
        type: type,
        platform: platform,
        url: url,
        title: title,
        description: description,
        body: body,
        summary: summary,
        thumbnailUrl: thumbnailUrl,
        author: author,
        publishedDate: publishedDate,
        createdAt: createdAt,
        updatedAt: updatedAt,
        favorite: favorite,
        archived: archived,
        isDraft: isDraft,
        inInbox: inInbox,
        openCount: openCount,
        lastOpenedAt: lastOpenedAt,
        tags: tags,
        collectionIds: collectionIds,
      );
}
