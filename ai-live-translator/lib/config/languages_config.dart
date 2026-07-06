import '../models/language.dart';

/// Single source of truth for every language the app knows about.
///
/// To add a language: add one entry here. Nothing in services/ or screens/
/// needs to change — they all read from [LanguagesConfig.enabledLanguages].
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
      available: true,
    ),
    AppLanguage(
      code: 'ta',
      name: 'Tamil',
      nativeName: 'தமிழ்',
      ttsVoice: 'alloy',
      available: true,
    ),
    AppLanguage(
      code: 'kn',
      name: 'Kannada',
      nativeName: 'ಕನ್ನಡ',
      ttsVoice: 'alloy',
      available: true,
    ),
    AppLanguage(
      code: 'ml',
      name: 'Malayalam',
      nativeName: 'മലയാളം',
      ttsVoice: 'alloy',
      available: true,
    ),
    AppLanguage(
      code: 'gu',
      name: 'Gujarati',
      nativeName: 'ગુજરાતી',
      ttsVoice: 'alloy',
      available: true,
    ),
    AppLanguage(
      code: 'mr',
      name: 'Marathi',
      nativeName: 'मराठी',
      ttsVoice: 'alloy',
      available: true,
    ),
    AppLanguage(
      code: 'pa',
      name: 'Punjabi',
      nativeName: 'ਪੰਜਾਬੀ',
      ttsVoice: 'alloy',
      available: true,
    ),
    AppLanguage(
      code: 'bn',
      name: 'Bengali',
      nativeName: 'বাংলা',
      ttsVoice: 'alloy',
      available: true,
    ),
    AppLanguage(
      code: 'or',
      name: 'Odia',
      nativeName: 'ଓଡ଼ିଆ',
      ttsVoice: 'alloy',
      available: true,
    ),
    AppLanguage(
      code: 'as',
      name: 'Assamese',
      nativeName: 'অসমীয়া',
      ttsVoice: 'alloy',
      available: true,
    ),
    AppLanguage(
      code: 'ur',
      name: 'Urdu',
      nativeName: 'اردو',
      ttsVoice: 'alloy',
      available: true,
    ),
    AppLanguage(
      code: 'kok',
      name: 'Konkani',
      nativeName: 'कोंकणी',
      ttsVoice: 'alloy',
      available: true,
    ),
    AppLanguage(
      code: 'sa',
      name: 'Sanskrit',
      nativeName: 'संस्कृतम्',
      ttsVoice: 'alloy',
      available: true,
    ),
  ];

  /// Languages actually selectable in this build.
  static List<AppLanguage> get enabledLanguages =>
      all.where((l) => l.available).toList(growable: false);

  /// English is always one side of the conversation; these are the
  /// available "other" languages a user can pair it with in Settings.
  static List<AppLanguage> get partnerLanguages =>
      enabledLanguages.where((l) => l.code != english.code).toList(growable: false);

  static AppLanguage byCode(String code) =>
      all.firstWhere((l) => l.code == code, orElse: () => all.first);

  static AppLanguage get english => byCode('en');

  /// Default partner language: Telugu.
  static AppLanguage get defaultPartner => byCode('te');
}
