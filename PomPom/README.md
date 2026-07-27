# 🍓 PomPom — Cute Pomodoro Timer

A tiny, cute, fully-offline Pomodoro timer for Android, built with Flutter.
No accounts, no ads, no internet required — just a little companion to help
you focus.

## Features

- Large animated circular timer with a pastel, cozy design
- Pick a mascot (cat, bear, bunny, panda, frog, penguin) with idle / happy /
  sleeping / thinking / celebrating animations
- Configurable focus / break / long-break durations, with a long break every
  4 pomodoros
- Reliable background timer via an Android foreground service — survives
  screen-off and app minimization
- Cute celebration screen with confetti when a session completes
- Simple task list with swipe-to-complete / swipe-to-delete
- Statistics (today / week / month) with a weekly bar chart, a time-split
  pie chart, and a month calendar heatmap
- Daily goal tracker, streaks, coin/star/leaf/cookie rewards, and 6
  achievements
- 8 animated pastel background themes (morning, night, cafe, library,
  forest, rain, galaxy, Japanese room)
- Soft synthesized sound effects (tick, pop, bubble, bell, chime, chirp,
  rain, forest, cafe) — no external audio files, generated locally
- Light/dark mode across 6 pastel color palettes
- Home screen widget (remaining time + Start/Pause/Resume)
- Quick Settings tile to start a focus session instantly
- Export data to a local JSON backup, reset everything from Settings
- 100% local storage (Hive) — nothing ever leaves the device

## Tech stack

- Flutter + Dart, Riverpod for state management
- Hive for local storage (no backend, no accounts)
- `flutter_foreground_task` for a reliable background timer service
- `flutter_local_notifications` for one-shot reminders
- `fl_chart` for statistics charts
- `home_widget` + a native Kotlin `AppWidgetProvider` for the home screen
  widget, and a native `TileService` for the Quick Settings tile
- Minimum Android version: **Android 8.0 (API 26)**

## Opening the project in Android Studio

1. Install [Android Studio](https://developer.android.com/studio) (which
   bundles the Android SDK) and the
   [Flutter SDK](https://docs.flutter.dev/get-started/install) if you don't
   already have them, then run `flutter doctor` to confirm everything is
   set up.
2. Open Android Studio → **Open** → select the `PomPom` folder (the one
   containing `pubspec.yaml`).
3. Let Android Studio index the project and prompt you to install any
   missing Flutter/Dart plugin — accept it.
4. Open a terminal in the project root and run:
   ```
   flutter pub get
   ```
5. Plug in your Android phone via USB with **USB debugging** enabled
   (Settings → About phone → tap "Build number" 7 times → Developer
   options → USB debugging), or start an emulator.
6. Click the green ▶ **Run** button (or `flutter run` in the terminal) to
   install and launch PomPom on your device.

To build an installable APK directly:
```
flutter build apk --release
```
The APK will be at `build/app/outputs/flutter-apk/app-release.apk` — copy
it to your phone and open it to install (you may need to allow "install
from unknown sources" the first time).

## Notes on this build

- This project was written and verified (`flutter analyze` — no issues,
  `flutter test` — all unit tests passing) in a sandboxed environment
  without access to the Android SDK's build servers, so a compiled APK
  could not be produced there. The project itself is complete — building it
  yourself in Android Studio (steps above) is all that's needed.
- Mascots, background particles, and app icon are all drawn procedurally in
  code (emoji + custom painters) rather than bundled art files, and sound
  effects are short synthesized tones — this keeps the app fully offline
  with no external asset licensing to worry about.
