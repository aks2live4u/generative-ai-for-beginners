import 'package:flutter_test/flutter_test.dart';

import 'package:ai_live_translator/config/languages_config.dart';
import 'package:ai_live_translator/services/speech/voice_activity_detector.dart';

void main() {
  group('LanguagesConfig', () {
    test('MVP ships exactly English and Telugu enabled', () {
      final codes = LanguagesConfig.enabledLanguages.map((l) => l.code).toSet();
      expect(codes, {'en', 'te'});
    });

    test('every future language is present but disabled until launched', () {
      final future = LanguagesConfig.all.where((l) => !l.available);
      expect(future.map((l) => l.code), contains('hi'));
      expect(future.map((l) => l.code), contains('ta'));
    });

    test('byCode resolves a known language', () {
      expect(LanguagesConfig.byCode('te').name, 'Telugu');
    });
  });

  group('VoiceActivityDetector', () {
    test('does not fire on brief noise below the speech threshold', () {
      final vad = VoiceActivityDetector(
        silenceDuration: const Duration(milliseconds: 50),
        minSpeechDuration: const Duration(milliseconds: 20),
      );
      expect(vad.feed(-60), isFalse);
      expect(vad.hasDetectedSpeech, isFalse);
    });

    test('detects speech once loud audio is sustained', () async {
      final vad = VoiceActivityDetector(
        silenceDuration: const Duration(milliseconds: 30),
        minSpeechDuration: const Duration(milliseconds: 20),
      );
      vad.feed(-10);
      await Future.delayed(const Duration(milliseconds: 30));
      vad.feed(-10);
      expect(vad.hasDetectedSpeech, isTrue);
    });

    test('reports silence after speech goes quiet long enough', () async {
      final vad = VoiceActivityDetector(
        silenceDuration: const Duration(milliseconds: 30),
        minSpeechDuration: const Duration(milliseconds: 10),
      );
      vad.feed(-10);
      await Future.delayed(const Duration(milliseconds: 15));
      vad.feed(-10); // now past minSpeechDuration -> hasDetectedSpeech
      await Future.delayed(const Duration(milliseconds: 40));
      expect(vad.feed(-60), isTrue);
    });
  });
}
