import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:uuid/uuid.dart';

import '../config/languages_config.dart';
import '../models/app_settings.dart';
import '../models/language.dart';
import '../models/translation_message.dart';
import '../utils/api_exception.dart';
import 'connectivity_service.dart';
import 'secure_storage_service.dart';
import 'speech/audio_recorder_service.dart';
import 'speech/speech_recognition_service.dart';
import 'speech/voice_activity_detector.dart';
import 'translation/translation_service.dart';
import 'tts/tts_service.dart';

enum ConversationMode { pushToTalk, auto }

enum ListeningStatus { idle, listening, processing, speaking, error }

/// Orchestrates the full pipeline described in the PRD:
/// record -> VAD/silence -> transcription -> GPT translation ->
/// OpenAI TTS -> playback, deleting every temp audio file along the way.
/// This is the one place that wires the individual services together;
/// screens only ever talk to this controller.
///
/// English is always one side of the conversation; [nativeLanguage] is the
/// other side, chosen in Settings.
class ConversationController extends ChangeNotifier {
  ConversationController({required this.settings}) {
    _watchConnectivity();
  }

  final AppSettings settings;

  final AudioRecorderService _recorder = AudioRecorderService();
  final SpeechRecognitionService _speech = SpeechRecognitionService();
  final TranslationService _translation = TranslationService();
  final TtsService _tts = TtsService();
  final VoiceActivityDetector _vad = VoiceActivityDetector();

  ConversationMode mode = ConversationMode.pushToTalk;
  ListeningStatus status = ListeningStatus.idle;
  final List<TranslationMessage> messages = [];
  TranslationMessage? lastMessage;
  String? errorMessage;
  bool muted = false;
  double amplitude = -160;
  bool isOnline = true;

  StreamSubscription<double>? _ampSub;
  StreamSubscription<bool>? _connectivitySub;
  bool _autoLoopActive = false;

  Future<void> _watchConnectivity() async {
    isOnline = await ConnectivityService.instance.isOnline();
    notifyListeners();
    _connectivitySub = ConnectivityService.instance.onStatusChange.listen((online) {
      isOnline = online;
      notifyListeners();
    });
  }

  AppLanguage get english => LanguagesConfig.english;
  AppLanguage get nativeLanguage => LanguagesConfig.byCode(settings.toLanguageCode);

  /// Clears the current conversation. Called when the user leaves the
  /// conversation screen, starts a new one, or changes the language pair —
  /// nothing here is ever persisted, so "clearing" just means forgetting it.
  void clearConversation() {
    messages.clear();
    lastMessage = null;
    errorMessage = null;
    notifyListeners();
  }

  /// Call after the partner language changes in Settings, so a stale
  /// conversation from a different language pair doesn't linger.
  void onLanguagePairChanged() => clearConversation();

  void toggleMute() {
    muted = !muted;
    notifyListeners();
  }

  void setMode(ConversationMode newMode) {
    if (mode == newMode) return;
    mode = newMode;
    if (newMode == ConversationMode.auto) {
      _startAutoLoop();
    } else {
      _stopAutoLoop();
    }
    notifyListeners();
  }

  // ---- Push-to-talk mode ----

  Future<void> startPushToTalk() async {
    if (status == ListeningStatus.listening) return;
    errorMessage = null;
    status = ListeningStatus.listening;
    notifyListeners();
    try {
      final stream = await _recorder.start();
      _ampSub = stream.listen((db) {
        amplitude = db;
        notifyListeners();
      });
    } catch (_) {
      errorMessage = 'Could not hear clearly. Try speaking again.';
      status = ListeningStatus.error;
      notifyListeners();
    }
  }

  Future<void> stopPushToTalkAndProcess() async {
    if (status != ListeningStatus.listening) return;
    await _ampSub?.cancel();
    final path = await _recorder.stop();
    if (path == null) {
      status = ListeningStatus.idle;
      notifyListeners();
      return;
    }
    await _processRecording(path);
  }

  // ---- Continuous "auto conversation" mode ----

  void _startAutoLoop() {
    _autoLoopActive = true;
    _armAutoListen();
  }

  Future<void> _stopAutoLoop() async {
    _autoLoopActive = false;
    if (status == ListeningStatus.listening) {
      await _ampSub?.cancel();
      await _recorder.cancel();
      status = ListeningStatus.idle;
      notifyListeners();
    }
  }

  Future<void> _armAutoListen() async {
    if (!_autoLoopActive) return;
    errorMessage = null;
    status = ListeningStatus.listening;
    _vad
      ..reset()
      // Higher sensitivity setting -> lower (more negative) threshold ->
      // picks up quieter speech. sensitivity 0 -> -20dB (loud only),
      // sensitivity 1 -> -50dB (quiet speech counts too).
      ..silenceThresholdDb = -20 - (settings.microphoneSensitivity * 30);
    notifyListeners();

    try {
      final stream = await _recorder.start();
      _ampSub = stream.listen((db) async {
        amplitude = db;
        notifyListeners();
        if (_vad.feed(db)) {
          await _ampSub?.cancel();
          final path = await _recorder.stop();
          if (path != null) {
            await _processRecording(path);
          } else if (_autoLoopActive) {
            await _armAutoListen();
          }
        }
      });
    } catch (_) {
      errorMessage = 'Could not hear clearly. Try speaking again.';
      status = ListeningStatus.error;
      notifyListeners();
    }
  }

  // ---- Shared pipeline ----

  Future<void> _processRecording(String path) async {
    status = ListeningStatus.processing;
    notifyListeners();

    final apiKey = await SecureStorageService.instance.getApiKey();
    if (apiKey == null || apiKey.isEmpty) {
      errorMessage = MissingApiKeyException().toString();
      status = ListeningStatus.error;
      notifyListeners();
      await _recorder.deleteFile(path);
      return _rearmIfAuto();
    }

    if (!await ConnectivityService.instance.isOnline()) {
      errorMessage =
          'No internet connection. Translation requires an online AI service.';
      status = ListeningStatus.error;
      notifyListeners();
      await _recorder.deleteFile(path);
      return _rearmIfAuto();
    }

    final native = nativeLanguage;

    try {
      final transcription = await _speech.transcribe(
        audioFilePath: path,
        apiKey: apiKey,
        nativeLanguageName: native.name,
      );
      final translation = await _translation.translate(
        transcript: transcription.text,
        nativeLanguage: native,
        apiKey: apiKey,
      );

      final message = TranslationMessage(
        id: const Uuid().v4(),
        spokenLanguageCode: translation.sourceLanguageCode,
        englishText: translation.englishText,
        nativeText: translation.nativeText,
        nativeLanguageCode: native.code,
        nativeTransliteration: translation.nativeTransliteration,
        timestamp: DateTime.now(),
      );
      messages.insert(0, message);
      lastMessage = message;
      status = ListeningStatus.speaking;
      notifyListeners();

      if (settings.autoPlayVoice && !muted) {
        await replayTranslation(message);
      }
      status = ListeningStatus.idle;
    } on ApiException catch (e) {
      errorMessage = e.message;
      status = ListeningStatus.error;
    } catch (_) {
      errorMessage = 'AI unavailable. Retry in a few seconds.';
      status = ListeningStatus.error;
    } finally {
      await _recorder.deleteFile(path);
      notifyListeners();
      await _rearmIfAuto();
    }
  }

  Future<void> _rearmIfAuto() async {
    if (mode != ConversationMode.auto || !_autoLoopActive) return;
    await Future.delayed(
      errorMessage != null
          ? const Duration(seconds: 2)
          : const Duration(milliseconds: 300),
    );
    if (mode == ConversationMode.auto && _autoLoopActive) {
      await _armAutoListen();
    }
  }

  Future<void> _speakSide(bool speakNative, String text) async {
    final apiKey = await SecureStorageService.instance.getApiKey();
    if (apiKey == null) return;

    final native = nativeLanguage;
    final languageName = speakNative ? native.name : english.name;
    final voice = settings.voiceGender == VoiceGender.male
        ? 'onyx'
        : (speakNative ? native.ttsVoice : english.ttsVoice);

    try {
      await _tts.speak(
        text: text,
        apiKey: apiKey,
        voice: voice,
        speed: settings.speechSpeed,
        languageName: languageName,
      );
    } on ApiException catch (e) {
      errorMessage = e.message;
      notifyListeners();
    }
  }

  /// Speaks the side of the message the *listener* actually needs — i.e.
  /// whichever language wasn't spoken. If English was spoken, the native
  /// speaker needs to hear the native-language translation, and vice versa.
  Future<void> replayTranslation(TranslationMessage message) {
    final speakNative = message.wasEnglishSpoken;
    final text = speakNative ? message.nativeText : message.englishText;
    return _speakSide(speakNative, text);
  }

  /// Speaks back the side that was actually said, in its own language —
  /// lets a speaker confirm what the app heard.
  Future<void> replayOriginal(TranslationMessage message) {
    final speakNative = !message.wasEnglishSpoken;
    final text = speakNative ? message.nativeText : message.englishText;
    return _speakSide(speakNative, text);
  }

  Future<void> replayLast() async {
    final msg = lastMessage;
    if (msg == null) return;
    await replayTranslation(msg);
  }

  @override
  void dispose() {
    _ampSub?.cancel();
    _connectivitySub?.cancel();
    _recorder.dispose();
    _tts.dispose();
    super.dispose();
  }
}
