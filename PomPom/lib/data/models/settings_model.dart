import '../../core/constants/app_constants.dart';

enum AppThemeMode { light, dark, system }

class SettingsModel {
  final int focusMinutes;
  final int breakMinutes;
  final int longBreakMinutes;
  final bool autoStartBreak;
  final bool autoStartNextFocus;
  final AmbientSound ambientSound;
  final double soundVolume; // 0.0 - 1.0
  final bool soundsEnabled;
  final AppThemeMode themeMode;
  final PastelPalette palette;
  final Mascot mascot;
  final AppBackgroundTheme backgroundTheme;
  final bool animationsEnabled;
  final bool notificationsEnabled;
  final bool keepScreenAwake;
  final int dailyGoalSessions;
  final RewardCurrency rewardCurrency;
  final int rewardBalance;
  final List<String> unlockedBackgrounds;
  final List<String> unlockedMascotHats;

  const SettingsModel({
    this.focusMinutes = 25,
    this.breakMinutes = 5,
    this.longBreakMinutes = 15,
    this.autoStartBreak = true,
    this.autoStartNextFocus = false,
    this.ambientSound = AmbientSound.bell,
    this.soundVolume = 0.6,
    this.soundsEnabled = true,
    this.themeMode = AppThemeMode.system,
    this.palette = PastelPalette.softPink,
    this.mascot = Mascot.cat,
    this.backgroundTheme = AppBackgroundTheme.morning,
    this.animationsEnabled = true,
    this.notificationsEnabled = true,
    this.keepScreenAwake = true,
    this.dailyGoalSessions = 8,
    this.rewardCurrency = RewardCurrency.coins,
    this.rewardBalance = 0,
    this.unlockedBackgrounds = const [],
    this.unlockedMascotHats = const [],
  });

  SettingsModel copyWith({
    int? focusMinutes,
    int? breakMinutes,
    int? longBreakMinutes,
    bool? autoStartBreak,
    bool? autoStartNextFocus,
    AmbientSound? ambientSound,
    double? soundVolume,
    bool? soundsEnabled,
    AppThemeMode? themeMode,
    PastelPalette? palette,
    Mascot? mascot,
    AppBackgroundTheme? backgroundTheme,
    bool? animationsEnabled,
    bool? notificationsEnabled,
    bool? keepScreenAwake,
    int? dailyGoalSessions,
    RewardCurrency? rewardCurrency,
    int? rewardBalance,
    List<String>? unlockedBackgrounds,
    List<String>? unlockedMascotHats,
  }) {
    return SettingsModel(
      focusMinutes: focusMinutes ?? this.focusMinutes,
      breakMinutes: breakMinutes ?? this.breakMinutes,
      longBreakMinutes: longBreakMinutes ?? this.longBreakMinutes,
      autoStartBreak: autoStartBreak ?? this.autoStartBreak,
      autoStartNextFocus: autoStartNextFocus ?? this.autoStartNextFocus,
      ambientSound: ambientSound ?? this.ambientSound,
      soundVolume: soundVolume ?? this.soundVolume,
      soundsEnabled: soundsEnabled ?? this.soundsEnabled,
      themeMode: themeMode ?? this.themeMode,
      palette: palette ?? this.palette,
      mascot: mascot ?? this.mascot,
      backgroundTheme: backgroundTheme ?? this.backgroundTheme,
      animationsEnabled: animationsEnabled ?? this.animationsEnabled,
      notificationsEnabled: notificationsEnabled ?? this.notificationsEnabled,
      keepScreenAwake: keepScreenAwake ?? this.keepScreenAwake,
      dailyGoalSessions: dailyGoalSessions ?? this.dailyGoalSessions,
      rewardCurrency: rewardCurrency ?? this.rewardCurrency,
      rewardBalance: rewardBalance ?? this.rewardBalance,
      unlockedBackgrounds: unlockedBackgrounds ?? this.unlockedBackgrounds,
      unlockedMascotHats: unlockedMascotHats ?? this.unlockedMascotHats,
    );
  }

  Map<String, dynamic> toMap() => {
    'focusMinutes': focusMinutes,
    'breakMinutes': breakMinutes,
    'longBreakMinutes': longBreakMinutes,
    'autoStartBreak': autoStartBreak,
    'autoStartNextFocus': autoStartNextFocus,
    'ambientSound': ambientSound.name,
    'soundVolume': soundVolume,
    'soundsEnabled': soundsEnabled,
    'themeMode': themeMode.name,
    'palette': palette.name,
    'mascot': mascot.name,
    'backgroundTheme': backgroundTheme.name,
    'animationsEnabled': animationsEnabled,
    'notificationsEnabled': notificationsEnabled,
    'keepScreenAwake': keepScreenAwake,
    'dailyGoalSessions': dailyGoalSessions,
    'rewardCurrency': rewardCurrency.name,
    'rewardBalance': rewardBalance,
    'unlockedBackgrounds': unlockedBackgrounds,
    'unlockedMascotHats': unlockedMascotHats,
  };

  factory SettingsModel.fromMap(Map<dynamic, dynamic> map) {
    const d = SettingsModel();
    return SettingsModel(
      focusMinutes: map['focusMinutes'] as int? ?? d.focusMinutes,
      breakMinutes: map['breakMinutes'] as int? ?? d.breakMinutes,
      longBreakMinutes: map['longBreakMinutes'] as int? ?? d.longBreakMinutes,
      autoStartBreak: map['autoStartBreak'] as bool? ?? d.autoStartBreak,
      autoStartNextFocus: map['autoStartNextFocus'] as bool? ?? d.autoStartNextFocus,
      ambientSound: AmbientSound.values.firstWhere((e) => e.name == map['ambientSound'], orElse: () => d.ambientSound),
      soundVolume: (map['soundVolume'] as num?)?.toDouble() ?? d.soundVolume,
      soundsEnabled: map['soundsEnabled'] as bool? ?? d.soundsEnabled,
      themeMode: AppThemeMode.values.firstWhere((e) => e.name == map['themeMode'], orElse: () => d.themeMode),
      palette: PastelPalette.values.firstWhere((e) => e.name == map['palette'], orElse: () => d.palette),
      mascot: Mascot.values.firstWhere((e) => e.name == map['mascot'], orElse: () => d.mascot),
      backgroundTheme: AppBackgroundTheme.values.firstWhere(
        (e) => e.name == map['backgroundTheme'],
        orElse: () => d.backgroundTheme,
      ),
      animationsEnabled: map['animationsEnabled'] as bool? ?? d.animationsEnabled,
      notificationsEnabled: map['notificationsEnabled'] as bool? ?? d.notificationsEnabled,
      keepScreenAwake: map['keepScreenAwake'] as bool? ?? d.keepScreenAwake,
      dailyGoalSessions: map['dailyGoalSessions'] as int? ?? d.dailyGoalSessions,
      rewardCurrency: RewardCurrency.values.firstWhere(
        (e) => e.name == map['rewardCurrency'],
        orElse: () => d.rewardCurrency,
      ),
      rewardBalance: map['rewardBalance'] as int? ?? d.rewardBalance,
      unlockedBackgrounds: (map['unlockedBackgrounds'] as List?)?.cast<String>() ?? const [],
      unlockedMascotHats: (map['unlockedMascotHats'] as List?)?.cast<String>() ?? const [],
    );
  }
}
