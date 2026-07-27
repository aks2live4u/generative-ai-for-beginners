import 'dart:math';

final Random _rand = Random();

/// Generates a short, locally-unique id. We never sync across devices,
/// so a timestamp + random suffix is more than enough to avoid collisions.
String generateId() {
  final ts = DateTime.now().microsecondsSinceEpoch;
  final suffix = _rand.nextInt(0xFFFFFF).toRadixString(16).padLeft(6, '0');
  return '$ts$suffix';
}
