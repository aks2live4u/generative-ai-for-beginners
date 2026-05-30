# Tiny Decisions — Android App

A native Android app wrapping the **Tiny Decisions** web experience with full-screen immersive display and native haptic feedback.

## Features

All 9 decision tools from the original web app:

| Tool | Description |
|------|-------------|
| 🎲 Yes No Dice | 3D CSS-animated die with 6 faces |
| 🪙 Coin Flip | Photorealistic gold coin flip |
| 🎱 Magic 8 Ball | SVG ball with 20 classic responses |
| 🎡 Spin the Wheel | Canvas-based spinning wheel with custom options |
| 🔢 Number Picker | Random number within custom range |
| ✊ Rock Paper Scissors | Challenge the CPU |
| 👆 Eenie Meenie | Classic rhyme picks from a list |
| ⏱️ Decision Timer | Countdown beat-the-clock picker |
| 🎰 Custom Picker | Add your own options, pick randomly |

## Android-Specific Additions

- **Full immersive display** — hides status bar and navigation bar, uses edge-to-edge rendering
- **Native haptic feedback** — `AndroidBridge` JavaScript interface wires dice rolls, coin flips, and button taps to device vibration motor
- **Back navigation** — hardware back button navigates WebView history before exiting
- **Display cutout support** — extends into notch/punch-hole areas on API 28+

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
TinyDecisions/
├── app/src/main/
│   ├── assets/
│   │   └── tinydecisions.html        # Full web app
│   ├── java/com/tinydecisions/
│   │   └── MainActivity.kt           # WebView host + AndroidBridge
│   ├── res/
│   │   ├── layout/activity_main.xml  # FrameLayout with WebView
│   │   ├── values/themes.xml         # Dark NoActionBar theme
│   │   └── mipmap-*/                 # Adaptive launcher icons
│   └── AndroidManifest.xml
├── build.gradle
└── settings.gradle
```
