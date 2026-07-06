import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../models/language.dart';
import '../../utils/api_exception.dart';
import '../../utils/constants.dart';

/// Structured result of one translation pass. English is always one side
/// of the conversation, so the result always carries both the English
/// text and the native-script text, plus which one was actually spoken.
class TranslationResult {
  TranslationResult({
    required this.sourceLanguageCode,
    required this.englishText,
    required this.nativeText,
    required this.nativeTransliteration,
  });

  final String sourceLanguageCode;
  final String englishText;
  final String nativeText;
  final String nativeTransliteration;
}

/// Translation + transliteration via an OpenAI chat completion. This is
/// where the PRD's translation prompt lives: translate naturally, preserve
/// emotion and politeness, never explain or summarize. The transliteration
/// is always of the native-script text (whichever side it's on), since
/// that's the side an English-only reader actually needs help sounding out.
class TranslationService {
  Future<TranslationResult> translate({
    required String transcript,
    required AppLanguage nativeLanguage,
    required String apiKey,
  }) async {
    final systemPrompt = '''
You are a real-time interpreter for a live spoken conversation between an
English speaker and a ${nativeLanguage.name} speaker.

The transcript you receive is ALWAYS either English or ${nativeLanguage.name} —
never any other language. Speech-to-text can be noisy or misheard; if the
transcript looks garbled or ambiguous, interpret it as whichever of these
two languages is most plausible. Never respond in, or transliterate into,
any language or script other than English and ${nativeLanguage.name}.

Rules:
- Translate naturally, the way people actually talk day to day — not
  formal, textbook, or literary ${nativeLanguage.name}. Use the everyday
  conversational register a native speaker would use with friends or
  family, not a stiff word-for-word rendering. Grammar should still be
  correct, just relaxed and natural rather than overly formal.
- Preserve emotion, tone, and politeness level from the original.
- Do not explain. Do not summarize. Do not add commentary or notes.
- Determine whether the transcript is English or ${nativeLanguage.name}.
- Produce BOTH: the English-language version of this message, and the
  ${nativeLanguage.name}-script version of this message. One of the two is
  the original (fix minor speech-recognition errors but keep its meaning),
  the other is your natural translation of it.
- Provide a romanized, English-letters transliteration of the
  ${nativeLanguage.name}-script text specifically, showing how to pronounce
  it aloud. Do not transliterate the English text — it's already Latin
  script.

Respond with ONLY strict JSON, no markdown fences, no extra text, in
exactly this shape:
{"source_language":"en or ${nativeLanguage.code}","english_text":"<the English-language version>","native_text":"<the ${nativeLanguage.name}-script version>","native_transliteration":"<romanized pronunciation of native_text>"}
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
    final englishText = (parsed['english_text'] as String? ?? '').trim();
    final nativeText = (parsed['native_text'] as String? ?? '').trim();
    final nativeTransliteration =
        (parsed['native_transliteration'] as String? ?? '').trim();

    if (englishText.isEmpty || nativeText.isEmpty) {
      throw ApiException('Language not recognised. Try again.');
    }

    return TranslationResult(
      sourceLanguageCode: sourceCode == 'en' || sourceCode == nativeLanguage.code
          ? sourceCode
          : 'en',
      englishText: englishText,
      nativeText: nativeText,
      nativeTransliteration: nativeTransliteration,
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
