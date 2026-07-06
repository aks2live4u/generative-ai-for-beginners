import '../models/language.dart';

/// Single source of truth for every language the app knows about.
///
/// To add a new Indian language: add one entry here and set [AppLanguage.available]
/// to true. Nothing in services/ or screens/ needs to change — they all read
/// from [LanguagesConfig.enabledLanguages].
class LanguagesConfig {
  LanguagesConfig._();

  static const List<AppLanguage> all = [
    AppLanguage(
      code: 'en',
      name: 'English',
      nativeName: 'English',
      ttsVoice: 'alloy',
      available: true,
    ),
    AppLanguage(
      code: 'te',
      name: 'Telugu',
      nativeName: 'తెలుగు',
      ttsVoice: 'shimmer',
      available: true,
    ),
    AppLanguage(
      code: 'hi',
      name: 'Hindi',
      nativeName: 'हिन्दी',
      ttsVoice: 'alloy',
      available: false,
    ),
    AppLanguage(
      code: 'ta',
      name: 'Tamil',
      nativeName: 'தமிழ்',
      ttsVoice: 'alloy',
      available: false,
    ),
    AppLanguage(
      code: 'kn',
      name: 'Kannada',
      nativeName: 'ಕನ್ನಡ',
      ttsVoice: 'alloy',
      available: false,
    ),
    AppLanguage(
      code: 'ml',
      name: 'Malayalam',
      nativeName: 'മലയാളം',
      ttsVoice: 'alloy',
      available: false,
    ),
    AppLanguage(
      code: 'gu',
      name: 'Gujarati',
      nativeName: 'ગુજરાતી',
      ttsVoice: 'alloy',
      available: false,
    ),
    AppLanguage(
      code: 'mr',
      name: 'Marathi',
      nativeName: 'मराठी',
      ttsVoice: 'alloy',
      available: false,
    ),
    AppLanguage(
      code: 'pa',
      name: 'Punjabi',
      nativeName: 'ਪੰਜਾਬੀ',
      ttsVoice: 'alloy',
      available: false,
    ),
    AppLanguage(
      code: 'bn',
      name: 'Bengali',
      nativeName: 'বাংলা',
      ttsVoice: 'alloy',
      available: false,
    ),
    AppLanguage(
      code: 'or',
      name: 'Odia',
      nativeName: 'ଓଡ଼ିଆ',
      ttsVoice: 'alloy',
      available: false,
    ),
    AppLanguage(
      code: 'as',
      name: 'Assamese',
      nativeName: 'অসমীয়া',
      ttsVoice: 'alloy',
      available: false,
    ),
    AppLanguage(
      code: 'ur',
      name: 'Urdu',
      nativeName: 'اردو',
      ttsVoice: 'alloy',
      available: false,
    ),
    AppLanguage(
      code: 'kok',
      name: 'Konkani',
      nativeName: 'कोंकणी',
      ttsVoice: 'alloy',
      available: false,
    ),
    AppLanguage(
      code: 'sa',
      name: 'Sanskrit',
      nativeName: 'संस्कृतम्',
      ttsVoice: 'alloy',
      available: false,
    ),
  ];

  /// Languages actually selectable in this build. MVP ships English + Telugu.
  static List<AppLanguage> get enabledLanguages =>
      all.where((l) => l.available).toList(growable: false);

  static AppLanguage byCode(String code) =>
      all.firstWhere((l) => l.code == code, orElse: () => all.first);

  /// Default MVP conversation pair: English <-> Telugu.
  static AppLanguage get defaultFrom => byCode('en');
  static AppLanguage get defaultTo => byCode('te');
}
