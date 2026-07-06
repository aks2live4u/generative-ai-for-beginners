/// One turn of the conversation: what was said, how to sound it out, and
/// what it means. Held only in memory — never written to disk or a database.
class TranslationMessage {
  TranslationMessage({
    required this.id,
    required this.spokenLanguageCode,
    required this.originalText,
    required this.transliteration,
    required this.translatedText,
    required this.translatedLanguageCode,
    required this.timestamp,
  });

  final String id;

  /// Language code of the original speech (as detected).
  final String spokenLanguageCode;

  /// What the speaker said, in its native script.
  final String originalText;

  /// Romanized pronunciation of the original text (e.g. "Nenu baagunnanu").
  final String transliteration;

  /// English (or target-language) meaning.
  final String translatedText;

  /// Language code the translation was produced in.
  final String translatedLanguageCode;

  final DateTime timestamp;
}
