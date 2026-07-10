# AI Chief of Staff — Android App

A personal, offline-first, voice-first executive assistant that runs entirely on-device. No account, no cloud database, no ads, no tracking. The only network calls are outbound requests to OpenAI, made solely when you use an AI feature.

This repository implements **Phase 1** of the four-phase roadmap below: the app foundation, local database, voice input, AI chat, tasks, projects, and search. Later phases (documents/OCR, meeting assistant, reports, dashboard, incident mode, widgets, offline AI, etc.) are intentionally not built yet — see [Roadmap](#roadmap).

## What's included (Phase 1)

- **Local-only storage** — Room database on-device; nothing leaves the phone except explicit AI requests.
- **Universal voice button** — tap the mic FAB from anywhere. On-device speech recognition (`SpeechRecognizer`) transcribes your voice; audio is never written to disk.
- **Lightweight intent routing** — phrases like "remind me to…" or "I have an idea" are handled locally (no network call). Anything else is sent to the AI Chief of Staff conversation.
- **AI Chief of Staff chat** — OpenAI-backed conversation screen, with full history stored locally and searchable.
- **Tasks** — priority, due date, notes, status, project association.
- **Projects** — group tasks and conversations, add tasks directly from a project.
- **Inbox** — quick capture for typed or voice-classified thoughts; convert any item into a task.
- **Search** — across tasks, projects, AI conversations, and inbox items.
- **Settings** — light/dark/system theme, OpenAI API key (encrypted via Android Keystore / `EncryptedSharedPreferences`), and a one-tap "delete all data" wipe.
- **Quick Share** — the app registers as a share target for text, images, and PDFs (wiring for Phase 2's document/OCR pipeline).

## Visual design

- Consistent rounded corner system (`Shapes.kt`, 10–28dp) and a shared `DepthCard` component (soft shadow + tonal elevation) used for every list row, so the UI reads as one layered surface instead of flat rows.
- Gradient hero header on Home (`GradientHero`/`GlassStatTile`) with a drop shadow tinted to the brand color, glass-morphism stat tiles.
- Priority color-coding via a left accent bar (Home) and a colored pill (`PriorityPill`) on task rows.
- Elevated FAB (10dp default / 4dp pressed) and tonal top app bar for depth against content.
- Chat bubbles colored by role (primary for user, surfaceVariant for assistant) with card elevation, not flat text rows.
- Full light/dark color schemes with distinct `surfaceVariant`/`outline` tones so elevation is visible in both themes.

## Security & privacy

- No `allowBackup`, no analytics, no third-party SDKs.
- The OpenAI API key is stored using `androidx.security.crypto.EncryptedSharedPreferences` backed by the Android Keystore, and is only ever attached to outbound HTTPS requests to `api.openai.com`.
- Network logging is capped at `HttpLoggingInterceptor.Level.BASIC` (request/response lines only) so prompts, replies, and the API key never hit logcat.

## Requirements

- Android Studio (Iguana/2023.2.1 or newer recommended)
- Android SDK 34, minSdk 26 (Android 8.0+)
- Kotlin 1.9.x
- An OpenAI API key (entered in Settings — the app has no key baked in)

## Building

```bash
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Verification status

The authoring environment's network policy blocks `dl.google.com` (confirmed via direct request and via Maven Central mirror probes — AGP, AndroidX, and Compose artifacts are not mirrored there), so a real Gradle/AGP build of this module could not be run in that environment. What was actually verified there:

- **Pure business logic compiled and unit-tested for real** — `IntentClassifier` (voice → task/note/assistant routing) and the task due-date preset math were extracted into a throwaway plain-Kotlin Gradle module (no Android/AndroidX dependency) and run under JUnit 5. All 10 tests passed (`gradle test` → `BUILD SUCCESSFUL`).
- **Every other file was manually audited**, not just written once: every `Modifier` extension (`weight`, `size`, `height`, `fillMaxWidth`, `fillMaxSize`, `background`, `clip`, `shadow`) was grep-checked against its required import across the whole source tree, every `Card`/`FloatingActionButton`/`TopAppBar`/`ModalBottomSheet`/DAO signature was checked against its actual Material3/Room API shape, and one real bug (a Room DAO method with a Kotlin default parameter, which the generated implementation would not have honored) was found and fixed this way.
- Still **not** verified: full AGP/Compose compilation, Room schema generation, on-device behavior. Build and smoke-test in Android Studio before relying on this.

## Project structure

```
AIChiefOfStaff/
├── app/src/main/java/com/aichiefofstaff/
│   ├── AiChiefApp.kt / AppContainer.kt   # Application class + manual DI container
│   ├── MainActivity.kt
│   ├── data/
│   │   ├── db/            # Room entities, DAOs, AppDatabase
│   │   ├── network/       # OpenAI Retrofit client + models
│   │   ├── prefs/         # Encrypted API key storage, DataStore settings
│   │   └── repository/    # Task/Project/Chat/Inbox repositories
│   ├── voice/              # SpeechRecognizer wrapper + on-device intent classifier
│   └── ui/
│       ├── theme/ nav/ components/
│       └── home/ inbox/ tasks/ projects/ assistant/ search/ settings/
└── build.gradle / settings.gradle
```

## Roadmap

- **Phase 1 (this app)** — foundation, local database, voice input, AI chat, tasks, projects, search.
- **Phase 2** — Document Center (PDF/Word/Excel), ML Kit OCR + document scanner, PDF reader, Meeting Assistant, Reports.
- **Phase 3** — Executive Dashboard, Morning Brief, Incident Mode, Decision Support frameworks, Knowledge Base.
- **Phase 4** — Widgets, encrypted backup/export/import, performance tuning, offline on-device AI model support.
