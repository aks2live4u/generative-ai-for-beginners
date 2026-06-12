# Pulse — Interval Timer Android App

A native Android app wrapping the **PULSE** interval-timer web experience, with full-screen
immersive display and native Android capabilities bridged in for the things that don't
work reliably from a `file://` WebView page.

## Features

All of the original web app's functionality:

| Feature | Description |
|---------|-------------|
| ⏱️ Prepare / Work / Rest phases | Configurable hours:minutes:seconds for each phase |
| 🔁 Rounds & Sets | Counters for rounds per set and number of sets, with rest breaks between sets |
| 🎯 Animated progress ring | SVG ring that drains in sync with the current phase |
| 🔔 Audio cues | Distinct Web Audio tones for prepare/work/rest start, 3·2·1 countdown and completion fanfare |
| 🗣️ Voice countdown | Spoken "Get ready" / "Go" / "Rest" / "Workout complete" cues |
| 📊 Live stats | Round, set, elapsed time and total session time remaining |
| ⏸️ Pause / Skip / Stop controls | Full session control while running |

## Android-Specific Additions

- **Full immersive display** — hides the status bar and navigation bar, edge-to-edge rendering
- **Native voice cues** — `AndroidBridge.speak()` routes phase/countdown announcements through
  the device's `TextToSpeech` engine instead of the Web Speech API (which is unreliable from a
  `file://` page)
- **Native "keep screen awake"** — `AndroidBridge.keepScreenOn()` toggles `FLAG_KEEP_SCREEN_ON`
  on the window, replacing the Screen Wake Lock Web API (which requires a secure origin and
  isn't available from local assets)
- **Haptic countdown** — a short vibration accompanies each of the 3·2·1 countdown ticks
- **Back navigation** — hardware back button navigates WebView history before exiting
- **Display cutout support** — extends into notch/punch-hole areas on API 28+
- **Portrait lock** — the layout is tuned for a single-column phone session

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- minSdk 24 (Android 7.0+)
- Kotlin 1.9.x

## Building

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing config)
./gradlew assembleRelease
```

The debug APK will be at:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```
PulseTimer/
├── app/src/main/
│   ├── assets/
│   │   └── pulsetimer.html           # Full web app (timer engine, UI, audio cues)
│   ├── java/com/pulsetimer/
│   │   └── MainActivity.kt           # WebView host + AndroidBridge (TTS, wake lock, haptics)
│   ├── res/
│   │   ├── layout/activity_main.xml  # FrameLayout with WebView
│   │   ├── values/themes.xml         # Dark NoActionBar theme
│   │   └── mipmap-*/                 # Adaptive launcher icons
│   └── AndroidManifest.xml
├── build.gradle
└── settings.gradle
```
