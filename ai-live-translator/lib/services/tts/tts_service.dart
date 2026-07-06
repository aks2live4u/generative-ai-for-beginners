import 'dart:convert';
import 'dart:io';

import 'package:audioplayers/audioplayers.dart';
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import 'package:uuid/uuid.dart';

import '../../utils/api_exception.dart';
import '../../utils/constants.dart';

/// Text-to-speech via OpenAI's speech endpoint
/// (POST https://api.openai.com/v1/audio/speech), plus playback.
///
/// The synthesized MP3 is written to a temp file just long enough to play
/// it, then deleted — matching the "delete temporary audio immediately"
/// requirement.
class TtsService {
  final AudioPlayer _player = AudioPlayer();
  String? _lastAudioPath;

  Future<void> speak({
    required String text,
    required String apiKey,
    required String voice,
    double speed = 1.0,
  }) async {
    if (text.trim().isEmpty) return;

    http.Response response;
    try {
      response = await http
          .post(
            Uri.parse(ApiConstants.speechEndpoint),
            headers: {
              'Authorization': 'Bearer $apiKey',
              'Content-Type': 'application/json',
            },
            body: jsonEncode({
              'model': ApiConstants.ttsModel,
              'input': text,
              'voice': voice,
              'speed': speed,
              'response_format': 'mp3',
            }),
          )
          .timeout(const Duration(seconds: 20));
    } catch (_) {
      throw ApiException('AI unavailable. Retry in a few seconds.');
    }

    if (response.statusCode != 200) {
      throw ApiException('AI unavailable. Retry in a few seconds.');
    }

    final dir = await getTemporaryDirectory();
    final path = '${dir.path}/${const Uuid().v4()}.mp3';
    final file = File(path);
    await file.writeAsBytes(response.bodyBytes);

    await _deleteLastAudio();
    _lastAudioPath = path;

    await _player.play(DeviceFileSource(path));
  }

  Future<void> stop() => _player.stop();

  Future<void> _deleteLastAudio() async {
    final path = _lastAudioPath;
    if (path == null) return;
    try {
      final file = File(path);
      if (await file.exists()) await file.delete();
    } catch (_) {
      // Best-effort cleanup only.
    }
    _lastAudioPath = null;
  }

  void dispose() {
    _deleteLastAudio();
    _player.dispose();
  }
}
