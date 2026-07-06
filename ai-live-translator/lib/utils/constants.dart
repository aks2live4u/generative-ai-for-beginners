/// Central place for the OpenAI endpoints/models the pipeline calls.
/// Changing a model name (e.g. to a newer TTS or transcription model)
/// only requires editing this file.
class ApiConstants {
  ApiConstants._();

  static const String baseUrl = 'https://api.openai.com/v1';
  static const String transcriptionEndpoint = '$baseUrl/audio/transcriptions';
  static const String chatCompletionsEndpoint = '$baseUrl/chat/completions';
  static const String speechEndpoint = '$baseUrl/audio/speech';

  /// Speech-to-text model. "gpt-4o-transcribe" is OpenAI's newer
  /// transcription model, generally more accurate on non-English speech
  /// than the older "whisper-1" — swap back to whisper-1 if you need
  /// verbose_json/word timestamps, which gpt-4o-transcribe doesn't support.
  static const String transcriptionModel = 'gpt-4o-transcribe';

  /// Chat model used for translation + transliteration.
  static const String translationModel = 'gpt-4o-mini';

  /// Text-to-speech model. "gpt-4o-mini-tts" supports an `instructions`
  /// field to steer delivery (pace, clarity) which plain "tts-1" doesn't.
  static const String ttsModel = 'gpt-4o-mini-tts';
}
