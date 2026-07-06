/// Central place for the OpenAI endpoints/models the pipeline calls.
/// Changing a model name (e.g. to a newer TTS or transcription model)
/// only requires editing this file.
class ApiConstants {
  ApiConstants._();

  static const String baseUrl = 'https://api.openai.com/v1';
  static const String transcriptionEndpoint = '$baseUrl/audio/transcriptions';
  static const String chatCompletionsEndpoint = '$baseUrl/chat/completions';
  static const String speechEndpoint = '$baseUrl/audio/speech';

  /// Speech-to-text model. "whisper-1" is the classic Whisper endpoint;
  /// "gpt-4o-mini-transcribe" is a newer, faster alternative — either works.
  static const String transcriptionModel = 'whisper-1';

  /// Chat model used for translation + transliteration.
  static const String translationModel = 'gpt-4o-mini';

  /// Text-to-speech model.
  static const String ttsModel = 'tts-1';
}
