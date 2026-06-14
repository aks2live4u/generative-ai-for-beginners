# Brain Dump

A private, local-first thought journal for Android. Write quick "brain dumps",
attach photos and voice notes, track your mood, and optionally use Claude
(and Gemini, for voice transcription) to help you write — all running on your
own phone.

## What's unlocked

Every feature is fully available with no premium tier, subscriptions, ads, or
"watch ad to unlock" prompts: all 16 color themes, all 12 fonts, mood
tracking, AI rewriting/chat, voice notes, image attachments, PIN +
fingerprint lock, and data export/import.

## Architecture

The app is a thin Kotlin shell around a WebView UI:

- **UI** — HTML/CSS/JS in `app/src/main/assets/`, served from
  `https://appassets.androidplatform.net/assets/` via
  `androidx.webkit.WebViewAssetLoader`. Local media (photos, voice notes) is
  served from `/media/`.
- **Native bridge** — `app/src/main/java/com/braindump/app/bridge/AppBridge.kt`
  exposes `@JavascriptInterface` methods to the page as `AndroidBridge.*`.
  `assets/js/bridge.js` wraps these in Promises (`window.Bridge`).
- **Storage** — SQLite (`DatabaseHelper`/`ThoughtRepository`) for thoughts and
  chat history, regular `SharedPreferences` for app settings, and
  `EncryptedSharedPreferences` for the PIN and API keys
  (`security/SecureStore.kt`). All data stays on-device — there is no backend
  or cloud sync.
- **AI** — `ai/ClaudeClient.kt` calls the Claude Messages API for chat and
  journal rewriting (including image context). `ai/GeminiClient.kt` calls the
  Gemini `generateContent` API only for transcribing voice notes, since the
  Claude API doesn't accept raw audio.

## Setting up AI features

No API keys are bundled with the app. To enable AI features, open the app
drawer → **Settings** → **AI Assistant Setup** and paste in your own keys:

- **Claude API key** — enables the ✨ rewrite presets in the editor/composer
  and the "Chat with AI" screen.
- **Gemini API key** — enables automatic transcription of voice notes
  (optional; everything else works without it).

Keys are stored in `EncryptedSharedPreferences` and are never bundled,
logged, or sent anywhere except directly to Anthropic/Google's APIs over
HTTPS.

## Security

- Optional 4-digit PIN + fingerprint unlock (`security/SecureStore.kt`,
  `security/BiometricHelper.kt`). The PIN is stored as a salted
  PBKDF2-HMAC-SHA256 hash, never in plain text.
- API keys are stored in `EncryptedSharedPreferences`.
- The WebView only loads bundled local assets (`allowFileAccess` /
  `allowContentAccess` are disabled) and talks to the network only through
  the Kotlin AI clients.

## Building

Open the `BrainDump/` folder in Android Studio (or run
`./gradlew assembleDebug` from a shell with a JDK 17+ installed) and run the
app on a device or emulator running Android 8.0 (API 26) or newer.

> **Note:** building requires resolving the Android Gradle Plugin and
> AndroidX/Material dependencies from Google's Maven repository
> (`dl.google.com` / `maven.google.com`). If you're building in a sandboxed
> environment that blocks those hosts, the build will fail at dependency
> resolution — this is an environment/network restriction, not a code issue.
> It will build normally on a developer machine or CI with normal internet
> access.
