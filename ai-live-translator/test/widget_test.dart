import 'package:flutter_test/flutter_test.dart';

import 'package:ai_live_translator/config/languages_config.dart';
import 'package:ai_live_translator/services/speech/voice_activity_detector.dart';

void main() {
  group('LanguagesConfig', () {
    test('English and Telugu are enabled', () {
      final codes = LanguagesConfig.enabledLanguages.map((l) => l.code).toSet();
      expect(codes, containsAll(['en', 'te']));
    });

    test('every official Indian language is enabled as a partner option', () {
      final partnerCodes = LanguagesConfig.partnerLanguages.map((l) => l.code);
      expect(partnerCodes, contains('hi'));
      expect(partnerCodes, contains('ta'));
      expect(partnerCodes, isNot(contains('en')));
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
