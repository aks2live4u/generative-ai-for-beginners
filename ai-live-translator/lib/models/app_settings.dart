enum AppThemeMode { light, dark, auto }

enum VoiceGender { male, female }

/// User-configurable preferences. Held in memory + a small local prefs store
/// (not the API key, which lives only in secure storage).
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
    this.fromLanguageCode = 'en',
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
  String fromLanguageCode;
  String toLanguageCode;

  AppSettings copyWith({
    AppThemeMode? themeMode,
    VoiceGender? voiceGender,
    double? speechSpeed,
    bool? autoPlayVoice,
    double? microphoneSensitivity,
    bool? showTransliteration,
    bool? showOriginalScript,
    bool? showTranslation,
    String? fromLanguageCode,
    String? toLanguageCode,
  }) {
    return AppSettings(
      themeMode: themeMode ?? this.themeMode,
      voiceGender: voiceGender ?? this.voiceGender,
      speechSpeed: speechSpeed ?? this.speechSpeed,
      autoPlayVoice: autoPlayVoice ?? this.autoPlayVoice,
      microphoneSensitivity:
          microphoneSensitivity ?? this.microphoneSensitivity,
      showTransliteration: showTransliteration ?? this.showTransliteration,
      showOriginalScript: showOriginalScript ?? this.showOriginalScript,
      showTranslation: showTranslation ?? this.showTranslation,
      fromLanguageCode: fromLanguageCode ?? this.fromLanguageCode,
      toLanguageCode: toLanguageCode ?? this.toLanguageCode,
    );
  }
}
