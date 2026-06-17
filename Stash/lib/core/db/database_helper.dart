import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:sqflite/sqflite.dart';

class DatabaseHelper {
  DatabaseHelper._();
  static final DatabaseHelper instance = DatabaseHelper._();

  Database? _db;
  bool _ftsAvailable = false;

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
      onOpen: (db) async => _ftsAvailable = await _ensureFts(db),
    );
  }

  /// Tries to create/verify the FTS5 virtual table. Some OEM Android builds
  /// ship a system SQLite without the fts5 module compiled in, so this must
  /// degrade gracefully instead of crashing every screen that opens the db.
  Future<bool> _ensureFts(Database db) async {
    try {
      await db.execute('''
        CREATE VIRTUAL TABLE IF NOT EXISTS content_fts USING fts5(
          id UNINDEXED,
          title,
          description,
          summary,
          body,
          tagsText
        )
      ''');
      return true;
    } catch (_) {
      return false;
    }
  }

  Future<void> _onCreate(Database db, int version) async {
    await db.execute('''
      CREATE TABLE IF NOT EXISTS content (
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
      CREATE TABLE IF NOT EXISTS collections (
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
      CREATE TABLE IF NOT EXISTS tags (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL UNIQUE
      )
    ''');

    await db.execute('''
      CREATE TABLE IF NOT EXISTS content_tags (
        contentId TEXT NOT NULL,
        tagId TEXT NOT NULL,
        PRIMARY KEY (contentId, tagId),
        FOREIGN KEY (contentId) REFERENCES content(id) ON DELETE CASCADE,
        FOREIGN KEY (tagId) REFERENCES tags(id) ON DELETE CASCADE
      )
    ''');

    await db.execute('''
      CREATE TABLE IF NOT EXISTS content_collections (
        contentId TEXT NOT NULL,
        collectionId TEXT NOT NULL,
        PRIMARY KEY (contentId, collectionId),
        FOREIGN KEY (contentId) REFERENCES content(id) ON DELETE CASCADE,
        FOREIGN KEY (collectionId) REFERENCES collections(id) ON DELETE CASCADE
      )
    ''');

    // The FTS5 virtual table is created separately in _ensureFts, since
    // some devices' system SQLite doesn't have the fts5 module compiled in
    // and search must fall back gracefully instead of crashing here.
  }

  /// Rebuilds the FTS row for a given content id from its current fields + tags.
  /// No-op when FTS5 isn't available on this device.
  Future<void> syncFts(String contentId) async {
    if (!_ftsAvailable) return;
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

    if (_ftsAvailable) {
      final match = '"$sanitized"*';
      final rows = await db.rawQuery('''
        SELECT id FROM content_fts WHERE content_fts MATCH ? ORDER BY rank LIMIT ?
      ''', [match, limit]);
      return rows.map((e) => e['id'] as String).toList();
    }

    // Fallback for devices without the fts5 module: plain LIKE search.
    final like = '%$sanitized%';
    final rows = await db.rawQuery('''
      SELECT DISTINCT c.id FROM content c
      LEFT JOIN content_tags ct ON ct.contentId = c.id
      LEFT JOIN tags t ON t.id = ct.tagId
      WHERE c.title LIKE ? OR c.description LIKE ? OR c.summary LIKE ? OR c.body LIKE ? OR t.name LIKE ?
      LIMIT ?
    ''', [like, like, like, like, like, limit]);
    return rows.map((e) => e['id'] as String).toList();
  }
}
