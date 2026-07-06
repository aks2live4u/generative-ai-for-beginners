import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../models/language.dart';
import '../../utils/api_exception.dart';
import '../../utils/constants.dart';

/// Structured result of one translation pass: exactly the three sections
/// the PRD requires — Original, Transliteration, Translation — plus which
/// of the two configured languages the speaker was actually using.
class TranslationResult {
  TranslationResult({
    required this.sourceLanguageCode,
    required this.originalText,
    required this.transliteration,
    required this.translatedText,
  });

  final String sourceLanguageCode;
  final String originalText;
  final String transliteration;
  final String translatedText;
}

/// Translation + transliteration via an OpenAI chat completion. This is
/// where the PRD's translation prompt lives: translate naturally, preserve
/// emotion and politeness, never explain or summarize, and always return
/// the original, its transliteration, and the translation — nothing else.
/// The model is asked to return that as strict JSON so the UI can render
/// the three sections without brittle text parsing.
class TranslationService {
  Future<TranslationResult> translate({
    required String transcript,
    required AppLanguage languageA,
    required AppLanguage languageB,
    required String apiKey,
  }) async {
    final systemPrompt = '''
You are a real-time interpreter for a live spoken conversation between a
${languageA.name} speaker and a ${languageB.name} speaker.

Rules:
- Translate naturally. Preserve emotion, tone, and politeness.
- Do not explain. Do not summarize. Do not add commentary or notes.
- Decide which of these two languages the input is written in: "${languageA.code}" (${languageA.name}) or "${languageB.code}" (${languageB.name}).
- Translate it into the OTHER of those two languages.
- Provide a romanized, English-letters transliteration showing how the
  ORIGINAL text is pronounced, readable by someone who cannot read that script.
- If the original is already in English (or another Latin-script language),
  the transliteration is simply the original text.

Respond with ONLY strict JSON, no markdown fences, no extra text, in exactly
this shape:
{"source_language":"<language code>","original_text":"<cleaned original text>","transliteration":"<romanized pronunciation of the original>","translated_text":"<natural translation in the other language>"}
''';

    final body = jsonEncode({
      'model': ApiConstants.translationModel,
      'temperature': 0.3,
      'messages': [
        {'role': 'system', 'content': systemPrompt},
        {'role': 'user', 'content': transcript},
      ],
      'response_format': {'type': 'json_object'},
    });

    http.Response response;
    try {
      response = await http
          .post(
            Uri.parse(ApiConstants.chatCompletionsEndpoint),
            headers: {
              'Authorization': 'Bearer $apiKey',
              'Content-Type': 'application/json',
            },
            body: body,
          )
          .timeout(const Duration(seconds: 20));
    } catch (_) {
      throw ApiException('AI unavailable. Retry in a few seconds.');
    }

    if (response.statusCode != 200) {
      throw ApiException(_friendlyMessageFor(response.statusCode));
    }

    final data = jsonDecode(response.body) as Map<String, dynamic>;
    final content =
        data['choices'][0]['message']['content'] as String? ?? '{}';

    late Map<String, dynamic> parsed;
    try {
      parsed = jsonDecode(content) as Map<String, dynamic>;
    } catch (_) {
      throw ApiException('Language not recognised. Try again.');
    }

    final sourceCode = (parsed['source_language'] as String? ?? '').trim();
    final original = (parsed['original_text'] as String? ?? transcript).trim();
    final transliteration = (parsed['transliteration'] as String? ?? '').trim();
    final translated = (parsed['translated_text'] as String? ?? '').trim();

    if (translated.isEmpty) {
      throw ApiException('Language not recognised. Try again.');
    }

    return TranslationResult(
      sourceLanguageCode:
          sourceCode == languageA.code || sourceCode == languageB.code
              ? sourceCode
              : languageA.code,
      originalText: original,
      transliteration: transliteration,
      translatedText: translated,
    );
  }

  String _friendlyMessageFor(int statusCode) {
    if (statusCode == 401) {
      return 'Your OpenAI API key was rejected. Check it in Settings.';
    }
    if (statusCode == 429) {
      return 'AI unavailable. Retry in a few seconds.';
    }
    return 'AI unavailable. Retry in a few seconds.';
  }
}
