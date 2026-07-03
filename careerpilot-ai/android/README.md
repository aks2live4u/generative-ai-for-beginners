# CareerPilot AI — Android app

A native Android shell (Kotlin + WebView) around the CareerPilot AI web app.
It bundles the production frontend build as local assets, so the UI loads
instantly with no network dependency — only API calls (jobs, resumes, match
scores, etc.) go over the network to your FastAPI backend.

## Before you open this in Android Studio

**You need the CareerPilot AI backend running somewhere your phone can
reach it.** This app is a client; it has no server logic of its own.
Options, easiest first:

1. **Same Wi-Fi as your computer**: run the backend on your machine
   (`uvicorn app.main:app --host 0.0.0.0 --port 8000` from the `backend/`
   folder in the main project), find your machine's LAN IP
   (`ipconfig`/`ifconfig`), and use `http://<that-ip>:8000` as the backend
   URL in the app.
2. **Android emulator only**: use `http://10.0.2.2:8000` — the emulator's
   special alias for your host machine's `localhost`.
3. **Deployed backend**: if you've deployed the FastAPI backend to a cloud
   host, use its public URL (ideally HTTPS).

## Opening and running

1. Unzip this folder and open it in Android Studio (**File → Open**, select
   the `android` folder — the one containing `settings.gradle.kts`).
2. Let Gradle sync. Android Studio will download the Android SDK platform
   (API 34) and build tools automatically if you don't already have them.
3. Run on an emulator or a physical device (USB debugging enabled) via the
   ▶ Run button.
4. On first launch, the app asks for your **Backend API URL** — enter one of
   the options above. You can change it later from the ⋮ menu →
   **Backend settings**.

## Building an installable APK yourself

From a terminal in this folder (or via Android Studio's **Build → Build
Bundle(s)/APK(s) → Build APK(s)**):

```bash
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```

Copy that APK to your phone and install it (you'll need to allow "install
from unknown sources" for a debug build not signed by Google Play). For a
release build you'll want to sign it — Android Studio's **Build → Generate
Signed Bundle / APK** wizard walks you through creating a keystore.

## Updating the bundled web app after you change the frontend

The web app is pre-built and copied into `app/src/main/assets/www/`. If you
change anything in `../frontend/src`, rebuild and re-copy it:

```bash
cd ../frontend
npm run build:android
rm -rf ../android/app/src/main/assets/www
cp -r dist-android ../android/app/src/main/assets/www
```

`build:android` (not the regular `build`) matters here — it emits
relative asset paths (`./assets/...`) instead of absolute ones
(`/assets/...`), which is required for the bundle to load correctly from
`file:///android_asset/`, and it skips the service worker registration
(irrelevant when the app is already bundled locally).

## What's native vs. web here

- **Native (Kotlin)**: the WebView host activity, the backend-URL settings
  dialog, and the SharedPreferences bridge (`BackendConfig`,
  `WebAppInterface`) that lets the web app read the configured backend URL
  via `window.AndroidConfig.getApiBaseUrl()`.
- **Web (bundled)**: everything else — every screen, all the agent-driven
  features (resume generation, match scoring, Kanban tracker, etc.) are the
  same React app used in the browser PWA, unchanged.

This keeps one codebase for all the actual product logic; the Android
project only adds the thin native chrome needed to install and launch it
like a normal app.
