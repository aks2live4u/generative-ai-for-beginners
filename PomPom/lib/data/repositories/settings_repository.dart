import 'package:flutter/foundation.dart';
import 'package:hive_flutter/hive_flutter.dart';
import '../models/settings_model.dart';
import 'hive_boxes.dart';

class SettingsRepository {
  Box<Map> get _box => HiveBoxes.settingsBox;

  SettingsModel get() {
    final map = _box.get(HiveBoxes.settingsKey);
    if (map == null) return const SettingsModel();
    return SettingsModel.fromMap(map);
  }

  ValueListenable<Box<Map>> listenable() => _box.listenable();

  Future<void> save(SettingsModel settings) => _box.put(HiveBoxes.settingsKey, settings.toMap());
}
