/// One turn of the conversation. Held only in memory — never written to
/// disk or a database.
///
/// English is always one side of the conversation, so instead of generic
/// "original/translated" fields (which get confusing once you factor in
/// who spoke first), this stores the English text and the native-script
/// text directly. [MessageCard] decides the Original/Translation/Meaning
/// labels from [spokenLanguageCode].
class TranslationMessage {
  TranslationMessage({
    required this.id,
    required this.spokenLanguageCode,
    required this.englishText,
    required this.nativeText,
    required this.nativeLanguageCode,
    required this.nativeTransliteration,
    required this.timestamp,
  });

  final String id;

  /// Language code of whichever side was actually spoken: 'en' or
  /// [nativeLanguageCode].
  final String spokenLanguageCode;

  /// The English-language text (whether it was the original or the
  /// translation).
  final String englishText;

  /// The native-script text in the non-English partner language (whether
  /// it was the original or the translation).
  final String nativeText;

  /// Language code of the non-English partner language (e.g. 'te').
  final String nativeLanguageCode;

  /// Romanized, English-letters pronunciation of [nativeText] — always
  /// paired with the native-script text, regardless of which side spoke.
  final String nativeTransliteration;

  final DateTime timestamp;

  bool get wasEnglishSpoken => spokenLanguageCode == 'en';
}
