# NeuroLoop — Android App

A native Android app wrapping the **NeuroLoop** game in a full-screen immersive WebView, with native haptic feedback wired to each phase of the loop.

## Concept

A three-phase, two-minute game loop engineered to pull a player out of a low-arousal, unfocused state and into a calm, executive-functioning state:

| Phase | Length | Mechanic | Target neurochemical effect |
|-------|--------|----------|------------------------------|
| 1. Vigilance Shock | 30s | Fast reflex tapping on a randomly relocating target | Norepinephrine — instant alertness |
| 2. Earned Mastery | 60s | Tap a 3×3 grid in ascending numeric order, repeatedly | Dopamine — steady micro-reward from tracked progress |
| 3. Satiety Harvest | 30s | All input stops; a slow breathing pacer (4s in / 4s out) plays while a short reflection note can be logged | Serotonin — parasympathetic wind-down |

The loop ends on a summary screen showing the score breakdown and the player's note.

## Android-Specific Additions

- **Full immersive display** — hides status bar and navigation bar, edge-to-edge rendering
- **Native haptic feedback** — `AndroidBridge` JavaScript interface gives each phase a distinct vibration: a light tap for Phase 1 hits, a stronger buzz for correct Phase 2 taps, a sharp buzz for mistakes, and silence in Phase 3
- **Back navigation** — hardware back button navigates WebView history before exiting

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
NeuroLoop/
├── app/src/main/
│   ├── assets/
│   │   └── neuroloop.html            # Full game (HTML/CSS/JS)
│   ├── java/com/neuroloop/
│   │   └── MainActivity.kt           # WebView host + AndroidBridge
│   ├── res/
│   │   ├── layout/activity_main.xml  # FrameLayout with WebView
│   │   ├── values/themes.xml         # Dark NoActionBar theme
│   │   └── mipmap-*/                 # Adaptive launcher icons
│   └── AndroidManifest.xml
├── build.gradle
└── settings.gradle
```

## Opening in Android Studio

1. `File → Open` and select the `NeuroLoop/` folder (not the repo root).
2. Let Gradle sync. If prompted to regenerate the Gradle wrapper jar, allow it.
3. Run on a device/emulator (API 24+).
