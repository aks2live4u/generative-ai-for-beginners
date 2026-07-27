import 'package:flutter/foundation.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'hive_boxes.dart';

class AchievementRepository {
  Box<String> get _box => HiveBoxes.achievementsBox;

  /// Map of achievement id -> when it was unlocked.
  Map<String, DateTime> getUnlocked() {
    final result = <String, DateTime>{};
    for (final key in _box.keys) {
      final iso = _box.get(key);
      if (iso != null) result[key as String] = DateTime.parse(iso);
    }
    return result;
  }

  ValueListenable<Box<String>> listenable() => _box.listenable();

  bool isUnlocked(String id) => _box.containsKey(id);

  Future<void> unlock(String id, {DateTime? at}) => _box.put(id, (at ?? DateTime.now()).toIso8601String());
}
