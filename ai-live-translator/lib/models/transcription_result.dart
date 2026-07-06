/// Raw output of the speech-to-text step, before translation.
class TranscriptionResult {
  TranscriptionResult({required this.text, this.whisperLanguageHint});

  final String text;

  /// Whisper's own best-guess language name (e.g. "english", "telugu").
  /// Treated as a hint only — the translation step makes the final call
  /// on which of the two configured languages was actually spoken.
  final String? whisperLanguageHint;
}
