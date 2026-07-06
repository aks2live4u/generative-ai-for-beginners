enum AppThemeMode { light, dark, auto }

enum VoiceGender { male, female }

/// User-configurable preferences. Held in memory + a small local prefs store
/// (not the API key, which lives only in secure storage).
///
/// English is always one side of the conversation; [toLanguageCode] is the
/// partner language chosen in Settings.
class AppSettings {
  AppSettings({
    this.themeMode = AppThemeMode.auto,
    this.voiceGender = VoiceGender.female,
    this.speechSpeed = 1.0,
    this.autoPlayVoice = true,
    this.microphoneSensitivity = 0.5,
    this.showTransliteration = true,
    this.showOriginalScript = true,
    this.showTranslation = true,
    this.toLanguageCode = 'te',
  });

  AppThemeMode themeMode;
  VoiceGender voiceGender;
  double speechSpeed;
  bool autoPlayVoice;
  double microphoneSensitivity;
  bool showTransliteration;
  bool showOriginalScript;
  bool showTranslation;
  String toLanguageCode;
}
