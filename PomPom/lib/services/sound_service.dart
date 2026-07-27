import 'package:audioplayers/audioplayers.dart';
import '../core/constants/app_constants.dart';

/// Plays the tiny synthesized sound effects bundled in assets/sounds. Kept
/// deliberately simple: one player for short one-shot cues, one for the
/// optional looping ambient background sound during a focus session.
class SoundService {
  static final SoundService instance = SoundService._();
  SoundService._();

  final AudioPlayer _effectPlayer = AudioPlayer();
  final AudioPlayer _loopPlayer = AudioPlayer();
  bool _initialized = false;

  Future<void> init() async {
    if (_initialized) return;
    await _loopPlayer.setReleaseMode(ReleaseMode.loop);
    _initialized = true;
  }

  Future<void> playEffect(AmbientSound sound, {double volume = 0.6}) async {
    final asset = sound.assetFile;
    if (asset == null) return;
    await init();
    await _effectPlayer.stop();
    await _effectPlayer.play(AssetSource('sounds/$asset'), volume: volume.clamp(0.0, 1.0));
  }

  Future<void> startAmbientLoop(AmbientSound sound, {double volume = 0.5}) async {
    final asset = sound.assetFile;
    if (asset == null || !sound.isLooping) return;
    await init();
    await _loopPlayer.stop();
    await _loopPlayer.play(AssetSource('sounds/$asset'), volume: volume.clamp(0.0, 1.0));
  }

  Future<void> stopAmbientLoop() => _loopPlayer.stop();

  Future<void> setLoopVolume(double volume) => _loopPlayer.setVolume(volume.clamp(0.0, 1.0));

  void dispose() {
    _effectPlayer.dispose();
    _loopPlayer.dispose();
  }
}
