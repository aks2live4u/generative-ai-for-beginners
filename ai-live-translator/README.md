# AI Live Translator

A privacy-first, live speech translator for English ↔ Telugu conversations,
built with Flutter. No login, no accounts, no saved conversations — audio is
deleted from the device immediately after each translation.

This implements the pipeline from the PRD:

```
Record → Voice Activity Detection → Whisper (speech-to-text) →
GPT (translation + transliteration) → OpenAI TTS (voice) → Playback
```

---

## 1. Which AI API this app uses (and why)

The app is built around **OpenAI's API**, not Wispr:

| Pipeline step | OpenAI API used | Model (see `lib/utils/constants.dart`) |
|---|---|---|
| Speech-to-text | `POST /v1/audio/transcriptions` | `whisper-1` |
| Translation + transliteration | `POST /v1/chat/completions` | `gpt-4o-mini` |
| Text-to-speech | `POST /v1/audio/speech` | `tts-1` |

**Why OpenAI and not Wispr:** Wispr Flow is a dictation app (voice → typed
text on your keyboard) — it doesn't publish a general developer API for
speech recognition, translation, and text-to-speech that an app like this
can call. OpenAI does, and one API key covers all three steps this app
needs, which is why the PRD's tech stack points at OpenAI.

**One API key is all you need.** The same key is used for all three calls
above.

### Where to get your OpenAI API key

1. Go to **https://platform.openai.com/** and sign in (or create an account).
2. Add a payment method at **https://platform.openai.com/settings/organization/billing** —
   API usage is billed pay-as-you-go and is separate from a ChatGPT Plus
   subscription; the API will not work without billing enabled.
3. Go to **https://platform.openai.com/api-keys** and click **"Create new
   secret key"**.
4. Copy the key (starts with `sk-...`). OpenAI only shows it once.
5. Open the app → **Settings** → paste it into **OpenAI API Key** → **Save Key**.

The key is stored using `flutter_secure_storage`, which on Android is backed
by `EncryptedSharedPreferences` / the Android Keystore. It is never hardcoded,
never bundled into the app, never logged, and never leaves the device except
as the `Authorization` header on direct HTTPS calls to `api.openai.com`.

---

## 2. Opening this project in Android Studio

**Prerequisites (one-time setup on your machine):**

1. Install **Flutter SDK**: https://docs.flutter.dev/get-started/install
2. Install **Android Studio**: https://developer.android.com/studio
3. In Android Studio, go to **Settings → Plugins**, install the **Flutter**
   plugin (this pulls in the Dart plugin too), then restart.
4. Run `flutter doctor` in a terminal and resolve anything it flags (Android
   SDK licenses, etc.).

**Opening the project:**

1. Unzip the project you received.
2. Android Studio → **File → Open** → select the `ai-live-translator` folder
   (the one containing `pubspec.yaml`).
3. Let Android Studio index the project and run `pub get` automatically (or
   run it yourself: `flutter pub get`).
4. Connect an Android phone (USB debugging on) or start an emulator —
   **you'll need a real microphone**, so a physical device gives the truest
   test of the live conversation experience.
5. Click **Run** (▶) with `main.dart` as the entry point.
6. First launch will prompt for microphone permission — allow it.
7. Go to **Settings** in the app and add your OpenAI API key (see above)
   before starting a conversation.

**Building a release APK** (from a terminal in the project folder):

```
flutter build apk --release
```

The APK is written to `build/app/outputs/flutter-apk/app-release.apk`.

---

## 3. How the app works

- **Home screen** — "Start Conversation" and "Settings". Nothing else.
- **Conversation screen** —
  - **Push-to-talk (default):** hold the mic button, speak, release. The app
    transcribes, detects which of the two languages was spoken, translates,
    and (unless muted) speaks the translation aloud.
  - **Auto Conversation mode:** tap the "Auto" button. The app listens
    continuously and uses a simple energy-based voice activity detector
    (`lib/services/speech/voice_activity_detector.dart`) to notice when a
    speaker has finished a sentence, so no one has to tap anything —
    useful while traveling.
  - Every message shows **Original**, **Pronunciation** (romanized
    transliteration), and **Meaning** (translation), matching the PRD's
    three-part message layout.
  - **Swap**, **Replay**, and **Mute** are available at the bottom.
- **Settings screen** — API key entry, theme (light/dark/auto), voice
  gender, speech speed, auto-play, microphone sensitivity, and toggles for
  which of the three message sections to show.

---

## 4. Privacy & security, matching the PRD

- No login, no accounts, no analytics, no ads.
- No conversation history is ever written to disk — messages live only in
  memory for the current session and disappear when the app closes.
- Recorded audio is written to a temp file only long enough to send it to
  Whisper, then deleted (`AudioRecorderService.deleteFile`). Synthesized
  speech audio is deleted the same way after playback.
- The only thing persisted locally is non-sensitive UI preferences (theme,
  voice, toggles) via `shared_preferences`, and the API key via
  `flutter_secure_storage`. Neither is a conversation database.
- All network calls are HTTPS, directly to `api.openai.com`, with the key
  sent only as a bearer token.

---

## 5. Adding a new language later

Everything language-specific lives in one file:
`lib/config/languages_config.dart`. Flip an `AppLanguage`'s `available` flag
to `true` (and give it a real `ttsVoice`) to enable it — no changes needed
in the pipeline, screens, or services. The PRD's full future list (Hindi,
Tamil, Kannada, Malayalam, Gujarati, Marathi, Punjabi, Bengali, Odia,
Assamese, Urdu, Konkani, Sanskrit, ...) is already stubbed out there,
disabled, ready to switch on.

---

## 6. Project structure

```
lib/
  main.dart                 App entry point, theming, providers
  config/                   languages_config.dart — the language registry
  models/                   Plain data classes (Language, TranslationMessage, AppSettings, ...)
  services/
    conversation_controller.dart   Orchestrates the whole pipeline
    secure_storage_service.dart    API key storage
    connectivity_service.dart      Online/offline detection
    settings_controller.dart       User preferences (non-sensitive)
    speech/                        Recording, VAD, Whisper transcription
    translation/                   GPT translation + transliteration
    tts/                           OpenAI text-to-speech + playback
  screens/                  home_screen, conversation_screen, settings_screen
  widgets/                  mic_button, wave_animation, message_card, language_badge
  themes/                   app_theme.dart
  utils/                    constants.dart (model names/endpoints), api_exception.dart
android/                    Standard Flutter Android project (open this repo root in Android Studio)
```
