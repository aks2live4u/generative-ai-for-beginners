# BodyLog — Android App

A native Android app wrapping the **BodyLog v2** measurements tracker web app in a full-screen WebView, ready to build and install via Android Studio.

## What's inside

- `app/src/main/assets/bodylog.html` — the BodyLog web app. Besides pointing the Chart.js `<script>` and Google Fonts `<link>` at files bundled inside the app (offline use), it adds:
  - A **Goals** tab — record your starting measurements and target goals (weight, body fat, waist, chest, hips, arms, thighs, calves), in either metric (kg/cm) or imperial (lb/in), plus a "Progress to Goal" summary against your latest log entry.
  - Stored notes/dates are HTML-escaped when rendered in History to prevent injected markup from running.
  - The Import feature was removed.
- `app/src/main/assets/chart.umd.js` — Chart.js 4.4.1 (bundled locally).
- `app/src/main/assets/fonts/` + `fonts.css` — DM Serif Display & DM Mono, the fonts used by the original design (bundled locally).
- `app/src/main/java/com/bodylog/MainActivity.kt` — hosts the WebView and adds the native bits a WebView needs:
  - **Export** — the page base64-encodes the backup JSON and hands it directly to `AndroidBridge.exportFile`, which is saved through the system "Save As" dialog (Storage Access Framework).
  - Navigation is restricted to the bundled app (`file:///android_asset/`) only.
  - Hardware back button navigates within the app before exiting.
- App data (all your logged entries, plus your Goals profile) is stored via `localStorage`, which persists in the WebView's local storage across app launches as long as the app isn't uninstalled or its data cleared.
- Adaptive app icon (`res/drawable/ic_launcher_*.xml`, `res/mipmap-anydpi*`) — a body-silhouette/measuring-tape mark in the app's lime-green accent on its dark background, sized to stay within the adaptive icon "safe zone" so it isn't cropped by any launcher mask shape.

No special permissions are required — everything runs offline and file export goes through the system file picker.

## Building

Open the `BodyLog/` folder in Android Studio (Hedgehog or newer) and let it sync, or from the command line:

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires your own signing config)
./gradlew assembleRelease
```

The debug APK will be at:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- minSdk 24 (Android 7.0+)
- Kotlin 1.9.x

## Project Structure

```
BodyLog/
├── app/src/main/
│   ├── assets/
│   │   ├── bodylog.html        # The web app (unchanged except local asset paths)
│   │   ├── chart.umd.js         # Chart.js, bundled for offline use
│   │   ├── fonts.css            # @font-face rules for bundled fonts
│   │   └── fonts/*.woff2        # DM Serif Display & DM Mono
│   ├── java/com/bodylog/
│   │   └── MainActivity.kt      # WebView host + export bridge
│   ├── res/
│   │   ├── layout/activity_main.xml
│   │   ├── values/themes.xml    # Dark NoActionBar theme matching the app
│   │   ├── drawable/ic_launcher_*.xml  # Adaptive icon background/foreground
│   │   └── mipmap-anydpi*/       # Adaptive launcher icon references
│   └── AndroidManifest.xml
├── build.gradle
└── settings.gradle
```
