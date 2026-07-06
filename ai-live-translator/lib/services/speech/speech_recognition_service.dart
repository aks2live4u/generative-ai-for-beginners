import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

import '../../models/transcription_result.dart';
import '../../utils/api_exception.dart';
import '../../utils/constants.dart';

/// Speech-to-text via OpenAI's transcription endpoint
/// (POST https://api.openai.com/v1/audio/transcriptions).
class SpeechRecognitionService {
  Future<TranscriptionResult> transcribe({
    required String audioFilePath,
    required String apiKey,
    String? nativeLanguageName,
  }) async {
    final file = File(audioFilePath);
    if (!await file.exists()) {
      throw ApiException('Could not hear clearly. Try speaking again.');
    }

    final request =
        http.MultipartRequest('POST', Uri.parse(ApiConstants.transcriptionEndpoint))
          ..headers['Authorization'] = 'Bearer $apiKey'
          ..fields['model'] = ApiConstants.transcriptionModel
          ..fields['response_format'] = 'json'
          ..files.add(await http.MultipartFile.fromPath('file', audioFilePath));

    // Biases the decoder toward the two languages actually in play, since
    // this is a closed English/native-language conversation, not open
    // vocabulary in any language.
    if (nativeLanguageName != null) {
      request.fields['prompt'] =
          'A conversation in English or $nativeLanguageName.';
    }

    http.StreamedResponse streamed;
    try {
      streamed = await request.send().timeout(const Duration(seconds: 20));
    } catch (_) {
      throw ApiException('AI unavailable. Retry in a few seconds.');
    }

    final response = await http.Response.fromStream(streamed);
    if (response.statusCode != 200) {
      throw ApiException(
        _friendlyMessageFor(response.statusCode),
        statusCode: response.statusCode,
      );
    }

    final data = jsonDecode(response.body) as Map<String, dynamic>;
    final text = (data['text'] as String? ?? '').trim();
    if (text.isEmpty) {
      throw ApiException('Could not hear clearly. Try speaking again.');
    }

    return TranscriptionResult(
      text: text,
      whisperLanguageHint: data['language'] as String?,
    );
  }

  String _friendlyMessageFor(int statusCode) {
    if (statusCode == 401) {
      return 'Your OpenAI API key was rejected. Check it in Settings.';
    }
    if (statusCode == 429) {
      return 'AI unavailable. Retry in a few seconds.';
    }
    return 'Could not hear clearly. Try speaking again.';
  }
}
