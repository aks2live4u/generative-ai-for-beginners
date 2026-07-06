import 'dart:async';
import 'dart:io';

import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:record/record.dart';
import 'package:uuid/uuid.dart';

/// Records short voice clips to the app's ephemeral cache directory and
/// exposes a live amplitude stream (used for the wave animation and the
/// [VoiceActivityDetector]). Every recorded file is deleted as soon as it
/// has been sent to the AI pipeline — see [deleteFile] — matching the
/// "no permanent storage" requirement.
class AudioRecorderService {
  final AudioRecorder _recorder = AudioRecorder();
  StreamSubscription<Amplitude>? _ampSub;
  String? _currentPath;

  Future<bool> hasPermission() async {
    final status = await Permission.microphone.request();
    return status.isGranted;
  }

  bool get isRecording => _currentPath != null;

  /// Starts recording and returns a stream of amplitude readings (dBFS).
  Future<Stream<double>> start() async {
    if (!await hasPermission()) {
      throw StateError('Microphone permission was not granted.');
    }

    final dir = await getTemporaryDirectory();
    final path = '${dir.path}/${const Uuid().v4()}.m4a';
    _currentPath = path;

    await _recorder.start(
      const RecordConfig(
        encoder: AudioEncoder.aacLc,
        sampleRate: 16000,
        numChannels: 1,
      ),
      path: path,
    );

    final controller = StreamController<double>();
    _ampSub = _recorder
        .onAmplitudeChanged(const Duration(milliseconds: 120))
        .listen((amp) => controller.add(amp.current));
    controller.onCancel = () => _ampSub?.cancel();
    return controller.stream;
  }

  /// Stops recording and returns the path to the recorded file, or null if
  /// nothing was captured.
  Future<String?> stop() async {
    final path = await _recorder.stop();
    await _ampSub?.cancel();
    _ampSub = null;
    _currentPath = null;
    return path ?? _currentPath;
  }

  Future<void> cancel() async {
    await _recorder.cancel();
    await _ampSub?.cancel();
    _ampSub = null;
    _currentPath = null;
  }

  /// Deletes a temporary audio file immediately after it has been
  /// transcribed — the app never keeps recordings around.
  Future<void> deleteFile(String path) async {
    try {
      final file = File(path);
      if (await file.exists()) {
        await file.delete();
      }
    } catch (_) {
      // Best-effort cleanup; a stray temp file in the cache dir is harmless
      // and will be cleared by the OS regardless.
    }
  }

  void dispose() {
    _ampSub?.cancel();
    _recorder.dispose();
  }
}
