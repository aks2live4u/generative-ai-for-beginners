/// Lightweight energy-based voice activity detector.
///
/// Feed it a stream of amplitude readings (in dBFS — roughly -160 for
/// silence up to 0 for max volume) and it tells you once the speaker has
/// said something and then gone quiet, so "Auto Conversation" mode knows
/// when to stop listening and start translating without the user tapping
/// anything. This is intentionally simple (threshold + timers) rather than
/// a trained model — it runs instantly, on-device, and needs no extra API.
class VoiceActivityDetector {
  VoiceActivityDetector({
    this.silenceThresholdDb = -35.0,
    this.silenceDuration = const Duration(milliseconds: 1100),
    this.minSpeechDuration = const Duration(milliseconds: 300),
  });

  /// Louder than this (in dBFS) counts as speech. Raised/lowered by the
  /// "Microphone Sensitivity" setting.
  double silenceThresholdDb;

  /// How long the mic must stay quiet after speech before we consider the
  /// speaker done.
  final Duration silenceDuration;

  /// Minimum sustained loud audio before we trust it's actually speech
  /// (filters out short clicks/pops).
  final Duration minSpeechDuration;

  DateTime? _speechStartedAt;
  DateTime? _lastLoudAt;
  bool _hasSpeech = false;

  /// Feed the latest amplitude reading. Returns true the moment sustained
  /// silence follows detected speech — the caller's cue to stop recording.
  bool feed(double amplitudeDb) {
    final now = DateTime.now();
    final isLoud = amplitudeDb > silenceThresholdDb;

    if (isLoud) {
      _speechStartedAt ??= now;
      _lastLoudAt = now;
      if (!_hasSpeech &&
          now.difference(_speechStartedAt!) >= minSpeechDuration) {
        _hasSpeech = true;
      }
      return false;
    }

    if (_hasSpeech && _lastLoudAt != null) {
      return now.difference(_lastLoudAt!) >= silenceDuration;
    }
    return false;
  }

  bool get hasDetectedSpeech => _hasSpeech;

  void reset() {
    _speechStartedAt = null;
    _lastLoudAt = null;
    _hasSpeech = false;
  }
}
