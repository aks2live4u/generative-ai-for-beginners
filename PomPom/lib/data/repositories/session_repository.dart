import 'package:flutter/foundation.dart';
import 'package:hive_flutter/hive_flutter.dart';
import '../models/session_model.dart';
import 'hive_boxes.dart';

class SessionRepository {
  Box<Map> get _box => HiveBoxes.sessionsBox;

  List<SessionModel> getAll() {
    final sessions = _box.values.map((m) => SessionModel.fromMap(m)).toList();
    sessions.sort((a, b) => a.startedAt.compareTo(b.startedAt));
    return sessions;
  }

  ValueListenable<Box<Map>> listenable() => _box.listenable();

  Future<void> add(SessionModel session) => _box.put(session.id, session.toMap());
}
