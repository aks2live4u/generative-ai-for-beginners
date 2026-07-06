import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/app_settings.dart';

/// Holds the shared [AppSettings] instance and persists only non-sensitive
/// UI preferences locally (theme, voice, toggles). The API key is never
/// stored here — see [SecureStorageService] — and no conversation content
/// is ever written to disk.
class SettingsController extends ChangeNotifier {
  final AppSettings settings = AppSettings();

  static const _kTheme = 'theme_mode';
  static const _kVoiceGender = 'voice_gender';
  static const _kSpeed = 'speech_speed';
  static const _kAutoPlay = 'auto_play_voice';
  static const _kSensitivity = 'mic_sensitivity';
  static const _kShowTransliteration = 'show_transliteration';
  static const _kShowOriginal = 'show_original';
  static const _kShowTranslation = 'show_translation';

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    settings.themeMode = AppThemeMode.values[prefs.getInt(_kTheme) ?? AppThemeMode.auto.index];
    settings.voiceGender = VoiceGender.values[prefs.getInt(_kVoiceGender) ?? VoiceGender.female.index];
    settings.speechSpeed = prefs.getDouble(_kSpeed) ?? 1.0;
    settings.autoPlayVoice = prefs.getBool(_kAutoPlay) ?? true;
    settings.microphoneSensitivity = prefs.getDouble(_kSensitivity) ?? 0.5;
    settings.showTransliteration = prefs.getBool(_kShowTransliteration) ?? true;
    settings.showOriginalScript = prefs.getBool(_kShowOriginal) ?? true;
    settings.showTranslation = prefs.getBool(_kShowTranslation) ?? true;
    notifyListeners();
  }

  Future<void> _persist() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt(_kTheme, settings.themeMode.index);
    await prefs.setInt(_kVoiceGender, settings.voiceGender.index);
    await prefs.setDouble(_kSpeed, settings.speechSpeed);
    await prefs.setBool(_kAutoPlay, settings.autoPlayVoice);
    await prefs.setDouble(_kSensitivity, settings.microphoneSensitivity);
    await prefs.setBool(_kShowTransliteration, settings.showTransliteration);
    await prefs.setBool(_kShowOriginal, settings.showOriginalScript);
    await prefs.setBool(_kShowTranslation, settings.showTranslation);
  }

  void setThemeMode(AppThemeMode mode) {
    settings.themeMode = mode;
    notifyListeners();
    _persist();
  }

  void setVoiceGender(VoiceGender gender) {
    settings.voiceGender = gender;
    notifyListeners();
    _persist();
  }

  void setSpeechSpeed(double speed) {
    settings.speechSpeed = speed;
    notifyListeners();
    _persist();
  }

  void setAutoPlayVoice(bool value) {
    settings.autoPlayVoice = value;
    notifyListeners();
    _persist();
  }

  void setMicrophoneSensitivity(double value) {
    settings.microphoneSensitivity = value;
    notifyListeners();
    _persist();
  }

  void setShowTransliteration(bool value) {
    settings.showTransliteration = value;
    notifyListeners();
    _persist();
  }

  void setShowOriginalScript(bool value) {
    settings.showOriginalScript = value;
    notifyListeners();
    _persist();
  }

  void setShowTranslation(bool value) {
    settings.showTranslation = value;
    notifyListeners();
    _persist();
  }
}
