/// Central place for every tunable constant and enum-like option list used
/// across the app. Keeping these here means the UI, settings screen and
/// domain logic all agree on the same set of choices.
library;

/// One entry per selectable focus/break duration in the settings screen.
class DurationOption {
  final String label;
  final int minutes;
  const DurationOption(this.label, this.minutes);
}

const List<DurationOption> kFocusDurationOptions = [
  DurationOption('15 min', 15),
  DurationOption('20 min', 20),
  DurationOption('25 min', 25),
  DurationOption('30 min', 30),
  DurationOption('45 min', 45),
];

const List<DurationOption> kBreakDurationOptions = [
  DurationOption('5 min', 5),
  DurationOption('10 min', 10),
  DurationOption('15 min', 15),
];

const List<DurationOption> kLongBreakDurationOptions = [
  DurationOption('15 min', 15),
  DurationOption('20 min', 20),
  DurationOption('25 min', 25),
  DurationOption('30 min', 30),
];

enum Mascot { cat, bear, bunny, panda, frog, penguin }

extension MascotX on Mascot {
  String get label => switch (this) {
    Mascot.cat => 'Cat',
    Mascot.bear => 'Bear',
    Mascot.bunny => 'Bunny',
    Mascot.panda => 'Panda',
    Mascot.frog => 'Frog',
    Mascot.penguin => 'Penguin',
  };

  /// Base emoji shown in idle state.
  String get idleEmoji => switch (this) {
    Mascot.cat => '🐱',
    Mascot.bear => '🐻',
    Mascot.bunny => '🐰',
    Mascot.panda => '🐼',
    Mascot.frog => '🐸',
    Mascot.penguin => '🐧',
  };

  String get happyEmoji => switch (this) {
    Mascot.cat => '😻',
    Mascot.bear => '🐻',
    Mascot.bunny => '🐰',
    Mascot.panda => '🐼',
    Mascot.frog => '🐸',
    Mascot.penguin => '🐧',
  };

  String get sleepingEmoji => '💤';

  String get thinkingEmoji => '🤔';

  String get celebratingEmoji => '🎉';
}

enum MascotMood { idle, happy, sleeping, thinking, celebrating }

enum AppBackgroundTheme { morning, night, cafe, library, forest, rain, galaxy, japaneseRoom }

extension AppBackgroundThemeX on AppBackgroundTheme {
  String get label => switch (this) {
    AppBackgroundTheme.morning => 'Morning',
    AppBackgroundTheme.night => 'Night',
    AppBackgroundTheme.cafe => 'Cafe',
    AppBackgroundTheme.library => 'Library',
    AppBackgroundTheme.forest => 'Forest',
    AppBackgroundTheme.rain => 'Rain',
    AppBackgroundTheme.galaxy => 'Galaxy',
    AppBackgroundTheme.japaneseRoom => 'Japanese Room',
  };
}

enum AmbientSound { none, tick, pageFlip, bubble, pop, bell, windChime, birdChirp, rain, forest, coffeeShop }

extension AmbientSoundX on AmbientSound {
  String get label => switch (this) {
    AmbientSound.none => 'None',
    AmbientSound.tick => 'Tick',
    AmbientSound.pageFlip => 'Page Flip',
    AmbientSound.bubble => 'Bubble',
    AmbientSound.pop => 'Pop',
    AmbientSound.bell => 'Bell',
    AmbientSound.windChime => 'Wind Chime',
    AmbientSound.birdChirp => 'Bird Chirp',
    AmbientSound.rain => 'Rain',
    AmbientSound.forest => 'Forest',
    AmbientSound.coffeeShop => 'Coffee Shop',
  };

  /// Underlying synthesized asset file. Several cute labels share one
  /// synthesized sample since we only ship a handful of base tones.
  String? get assetFile => switch (this) {
    AmbientSound.none => null,
    AmbientSound.tick => 'tick.wav',
    AmbientSound.pageFlip => 'pop.wav',
    AmbientSound.bubble => 'bubble.wav',
    AmbientSound.pop => 'pop.wav',
    AmbientSound.bell => 'bell.wav',
    AmbientSound.windChime => 'chime.wav',
    AmbientSound.birdChirp => 'chirp.wav',
    AmbientSound.rain => 'rain.wav',
    AmbientSound.forest => 'forest.wav',
    AmbientSound.coffeeShop => 'cafe.wav',
  };

  bool get isLooping => this == AmbientSound.rain || this == AmbientSound.forest || this == AmbientSound.coffeeShop;
}

enum RewardCurrency { coins, stars, leaves, cookies }

extension RewardCurrencyX on RewardCurrency {
  String get label => switch (this) {
    RewardCurrency.coins => 'Coins',
    RewardCurrency.stars => 'Stars',
    RewardCurrency.leaves => 'Leaves',
    RewardCurrency.cookies => 'Cookies',
  };

  String get emoji => switch (this) {
    RewardCurrency.coins => '🪙',
    RewardCurrency.stars => '⭐',
    RewardCurrency.leaves => '🍃',
    RewardCurrency.cookies => '🍪',
  };
}

enum PastelPalette { softPink, mintGreen, lavender, skyBlue, peach, cream }

extension PastelPaletteX on PastelPalette {
  String get label => switch (this) {
    PastelPalette.softPink => 'Soft Pink',
    PastelPalette.mintGreen => 'Mint Green',
    PastelPalette.lavender => 'Lavender',
    PastelPalette.skyBlue => 'Sky Blue',
    PastelPalette.peach => 'Peach',
    PastelPalette.cream => 'Cream',
  };
}

/// Number of completed focus sessions before a long break is triggered.
const int kSessionsBeforeLongBreak = 4;

/// Coins/stars/etc. awarded per completed focus session.
const int kRewardPerSession = 5;

const String kNotifChannelId = 'pompom_alerts';
const String kNotifChannelName = 'PomPom Alerts';

const String kForegroundNotifChannelId = 'pompom_timer_service';
const String kForegroundNotifChannelName = 'PomPom Timer';
