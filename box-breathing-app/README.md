# Box Breathing

A minimal, self-contained box-breathing (4-phase paced breathing) timer app.

## Features

- Four phase timers — **Breathe In**, **Hold**, **Breathe Out**, **Hold** — each adjustable from 1 to 60 seconds.
- Two ways to size a session, switchable at any time:
  - **Set rounds**: pick the number of rounds and the total session time is calculated and shown automatically.
  - **Set duration**: pick a total session length in whole-minute increments, and the number of rounds that fit is calculated automatically.
- Animated breathing circle that grows/shrinks in sync with each phase's duration.
- Optional background video, picked from your device and played on loop behind the session.
- Optional background music, picked from your device and looped during the session.
- Pause/resume and stop controls, plus a session-complete summary.

All media files are read locally in the browser (via `URL.createObjectURL`) and are never uploaded anywhere.

## Development

```bash
npm install
npm run dev
```

## Build (web)

```bash
npm run build
```

## Install via Android Studio

This app is wrapped with [Capacitor](https://capacitorjs.com/), so the same React code ships as a
real Android app. The `android/` folder is a standard Gradle/Android Studio project.

1. Install dependencies once: `npm install`
2. Whenever you change the web app, rebuild and sync the native project:
   ```bash
   npm run android:sync
   ```
3. Open the project in Android Studio:
   ```bash
   npm run android:open
   ```
   (or open the `android/` folder directly from Android Studio's "Open" dialog)
4. Let Android Studio sync Gradle and download the SDK/build tools if prompted, then press **Run** to
   install the app on an emulator or a connected device — or use **Build > Generate Signed Bundle / APK**
   to produce an installable APK.

No native plugins are required: the video/music file pickers use the standard HTML file input, which
Android's WebView already surfaces as the native file/media picker.
