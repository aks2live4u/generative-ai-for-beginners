import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:sqflite/sqflite.dart';

class DatabaseHelper {
  DatabaseHelper._();
  static final DatabaseHelper instance = DatabaseHelper._();

  Database? _db;

  Future<Database> get database async {
    _db ??= await _open();
    return _db!;
  }

  /// Closes the current connection so a new one is opened on next access.
  /// Used after restoring a backup file in place of the live database.
  Future<void> reset() async {
    await _db?.close();
    _db = null;
  }

  Future<Database> _open() async {
    final dir = await getApplicationDocumentsDirectory();
    final path = p.join(dir.path, 'stash.db');
    return openDatabase(
      path,
      version: 1,
      onConfigure: (db) async => db.execute('PRAGMA foreign_keys = ON'),
      onCreate: _onCreate,
    );
  }

  Future<void> _onCreate(Database db, int version) async {
    await db.execute('''
      CREATE TABLE content (
        id TEXT PRIMARY KEY,
        type TEXT NOT NULL,
        platform TEXT NOT NULL,
        url TEXT,
        title TEXT NOT NULL,
        description TEXT,
        body TEXT,
        summary TEXT,
        thumbnailUrl TEXT,
        author TEXT,
        publishedDate TEXT,
        createdAt TEXT NOT NULL,
        updatedAt TEXT NOT NULL,
        favorite INTEGER NOT NULL DEFAULT 0,
        archived INTEGER NOT NULL DEFAULT 0,
        isDraft INTEGER NOT NULL DEFAULT 0,
        inInbox INTEGER NOT NULL DEFAULT 0,
        openCount INTEGER NOT NULL DEFAULT 0,
        lastOpenedAt TEXT
      )
    ''');

    await db.execute('''
      CREATE TABLE collections (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        description TEXT,
        coverImage TEXT,
        createdAt TEXT NOT NULL,
        pinned INTEGER NOT NULL DEFAULT 0,
        favorite INTEGER NOT NULL DEFAULT 0
      )
    ''');

    await db.execute('''
      CREATE TABLE tags (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL UNIQUE
      )
    ''');

    await db.execute('''
      CREATE TABLE content_tags (
        contentId TEXT NOT NULL,
        tagId TEXT NOT NULL,
        PRIMARY KEY (contentId, tagId),
        FOREIGN KEY (contentId) REFERENCES content(id) ON DELETE CASCADE,
        FOREIGN KEY (tagId) REFERENCES tags(id) ON DELETE CASCADE
      )
    ''');

    await db.execute('''
      CREATE TABLE content_collections (
        contentId TEXT NOT NULL,
        collectionId TEXT NOT NULL,
        PRIMARY KEY (contentId, collectionId),
        FOREIGN KEY (contentId) REFERENCES content(id) ON DELETE CASCADE,
        FOREIGN KEY (collectionId) REFERENCES collections(id) ON DELETE CASCADE
      )
    ''');

    // FTS5 virtual table for global search across the searchable fields.
    await db.execute('''
      CREATE VIRTUAL TABLE content_fts USING fts5(
        id UNINDEXED,
        title,
        description,
        summary,
        body,
        tagsText
      )
    ''');
  }

  /// Rebuilds the FTS row for a given content id from its current fields + tags.
  Future<void> syncFts(String contentId) async {
    final db = await database;
    final rows = await db.query('content', where: 'id = ?', whereArgs: [contentId]);
    if (rows.isEmpty) {
      await db.delete('content_fts', where: 'id = ?', whereArgs: [contentId]);
      return;
    }
    final row = rows.first;
    final tagRows = await db.rawQuery('''
      SELECT t.name FROM tags t
      INNER JOIN content_tags ct ON ct.tagId = t.id
      WHERE ct.contentId = ?
    ''', [contentId]);
    final tagsText = tagRows.map((e) => e['name'] as String).join(' ');

    await db.delete('content_fts', where: 'id = ?', whereArgs: [contentId]);
    await db.insert('content_fts', {
      'id': contentId,
      'title': row['title'],
      'description': row['description'] ?? '',
      'summary': row['summary'] ?? '',
      'body': row['body'] ?? '',
      'tagsText': tagsText,
    });
  }

  Future<List<String>> searchContentIds(String query, {int limit = 100}) async {
    final db = await database;
    final sanitized = query.trim().replaceAll('"', '');
    if (sanitized.isEmpty) return [];
    final match = '"$sanitized"*';
    final rows = await db.rawQuery('''
      SELECT id FROM content_fts WHERE content_fts MATCH ? ORDER BY rank LIMIT ?
    ''', [match, limit]);
    return rows.map((e) => e['id'] as String).toList();
  }
}
