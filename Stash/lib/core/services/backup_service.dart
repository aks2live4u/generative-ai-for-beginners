import 'dart:convert';
import 'dart:io';

import 'package:archive/archive_io.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

import '../../data/collection_repository.dart';
import '../../data/content_repository.dart';
import '../db/database_helper.dart';

/// Exports/imports the whole vault as a local ZIP containing a JSON dump and
/// the raw SQLite file, so users can back up and restore without any cloud
/// account.
class BackupService {
  final _contentRepo = ContentRepository();
  final _collectionRepo = CollectionRepository();

  Future<File> exportZip() async {
    final content = await _contentRepo.all(includeArchived: true, includeInbox: true);
    final collections = await _collectionRepo.all();

    final jsonPayload = {
      'exportedAt': DateTime.now().toIso8601String(),
      'version': 1,
      'collections': collections
          .map((c) => {
                'id': c.id,
                'name': c.name,
                'description': c.description,
                'pinned': c.pinned,
                'favorite': c.favorite,
                'createdAt': c.createdAt.toIso8601String(),
              })
          .toList(),
      'content': content
          .map((item) => {
                ...item.toMap(),
                'tags': item.tags,
                'collectionIds': item.collectionIds,
              })
          .toList(),
    };

    final db = await DatabaseHelper.instance.database;
    final dbPath = db.path;

    final jsonBytes = utf8.encode(jsonEncode(jsonPayload));
    final archive = Archive();
    archive.addFile(ArchiveFile('stash_export.json', jsonBytes.length, jsonBytes));
    final dbBytes = await File(dbPath).readAsBytes();
    archive.addFile(ArchiveFile('stash.db', dbBytes.length, dbBytes));

    final zipData = ZipEncoder().encode(archive);
    final outDir = await getApplicationDocumentsDirectory();
    final timestamp = DateTime.now().millisecondsSinceEpoch;
    final outFile = File(p.join(outDir.path, 'stash_backup_$timestamp.zip'));
    await outFile.writeAsBytes(zipData!);
    return outFile;
  }

  /// Restores the database file from a previously exported ZIP backup.
  /// This replaces the current vault entirely — callers should confirm with
  /// the user before invoking this.
  Future<void> importZip(File zipFile) async {
    final bytes = await zipFile.readAsBytes();
    final archive = ZipDecoder().decodeBytes(bytes);
    final dbEntry = archive.findFile('stash.db');
    if (dbEntry == null) {
      throw Exception('Invalid backup file: missing stash.db');
    }

    final dir = await getApplicationDocumentsDirectory();
    final dbPath = p.join(dir.path, 'stash.db');

    await DatabaseHelper.instance.reset();
    await File(dbPath).writeAsBytes(dbEntry.content as List<int>);
  }
}
