/// Describes a single language the app can listen for, translate to/from,
/// and speak aloud. New languages are added here — never in pipeline logic.
class AppLanguage {
  const AppLanguage({
    required this.code,
    required this.name,
    required this.nativeName,
    required this.ttsVoice,
    required this.available,
  });

  /// BCP-47-ish code used in prompts and Whisper hints (e.g. "en", "te").
  final String code;

  /// English display name (e.g. "Telugu").
  final String name;

  /// Name written in its own script (e.g. "తెలుగు").
  final String nativeName;

  /// OpenAI TTS voice used when speaking this language aloud.
  final String ttsVoice;

  /// Whether this language is enabled in the current build (MVP = false for
  /// most, true for English/Telugu). Flip this on as languages are verified.
  final bool available;

  @override
  String toString() => name;
}
