# FocusFlow AI — Android App

> "Your second brain for getting things done."

An AI-powered executive-function assistant for people with ADHD — not another
to-do list. FocusFlow AI actively breaks tasks down, prioritizes what matters,
rescues you when you're stuck, and reduces the decision fatigue of "what
should I do next?".

Built as a native Android app: a lightweight Kotlin/WebView shell (same
pattern as this repo's `TinyDecisions` app) wrapping a full HTML/CSS/JS
experience, wired directly to the **Gemini API**.

## Why WebView instead of Flutter?

The original concept doc proposed Flutter. This build uses the same
native-WebView pattern already established by `TinyDecisions` in this repo
instead: a single Kotlin `MainActivity` hosts a full-screen `WebView`
loading a bundled web app from `assets/`. It ships as a real installable
Android APK, keeps the codebase approachable (plain JS, no build pipeline),
and still gets native capabilities — haptics and on-device speech-to-text —
through a small `AndroidBridge` JS interface.

## AI Philosophy

Every AI feature exists to answer one of these questions *for* the user,
instead of making them do it:

- What should I do next?
- Is this task too big?
- Am I overwhelmed?
- Can this be simplified?

## Features implemented

| Area | What it does |
|---|---|
| 🏠 **Home** | Greeting, Today's Mission card, energy/time/streak stats, AI Coach tip, one big "Start Working" button |
| ✨ **AI Inbox** | Capture anything (voice or text) → AI triages it into Create Project / Create Task / Reminder / Research Later / Archive / Delegate |
| 🌊 **AI Brain Dump** | Speak or type a messy stream of consciousness → Gemini turns it into a named project with concrete, bucketed tasks |
| 📁 **Smart Projects** | Auto-grouped task sections, progress bars, manual add |
| 🧩 **AI Task Breakdown** | Any task → 4-7 tiny sequential steps, engineered to defeat task-initiation paralysis |
| 🎯 **Focus Mode** | Big countdown timer, single task on screen, Done / Need Help / Skip / Pause / Distracted controls |
| ✨ **AI Rescue ("I'm Stuck")** | Pick why you're stuck (bored, too hard, tired, anxious, perfectionism…) → AI gives one warm, concrete 2-minute next step |
| ⚡ **AI Prioritization** | "Plan My Day" ranks open tasks by urgency/effort/energy into Do Now / Today / Tomorrow / This Week / Later, and picks one Mission |
| 🔋 **Energy-based planning** | Tasks and prioritization respect your current Low/Medium/High energy level |
| ⏳ **Waiting List** | Track what you're waiting on from other people, with one-tap resolve |
| 🧠 **AI Assistant Chat** | Conversational coach with full context of your tasks/energy/time — "I'm overwhelmed", "I only have 30 minutes", etc. |
| 👀 **Hyperfocus detection** | After 2 continuous focus hours, a gentle break reminder (water, stretch, bathroom, medicine) |
| 📊 **AI Weekly Review** | Summarizes completed/delayed tasks and focus time into wins, patterns, and next-week suggestions — no guilt |
| 🏆 **Dopamine rewards** | XP, coins, and levels on task completion |
| 🎙️ **Native voice capture** | Android's on-device `SpeechRecognizer` for Brain Dump and quick capture |
| 🔒 **Offline-first / local-first** | All data lives in the WebView's local storage on-device. The only network calls are to the Gemini API, and only when you trigger an AI action |

## Not yet implemented (roadmap)

The original concept is large — meeting transcription, email inbox
integration, native calendar sync, OCR, home-screen widgets, and predictive
burnout/deadline detection are intentionally out of scope for this first
build. They're natural next steps once the core loop (capture → plan →
focus → review) is validated.

## Setup

FocusFlow AI needs a **Gemini API key** to power its AI features (everything
else — capture, projects, focus timer, XP — works fully offline without one).

1. Get a free key at [aistudio.google.com/apikey](https://aistudio.google.com/apikey)
2. Install the app, go to **More → Gemini API**, and paste your key
3. The key is stored only in the app's local WebView storage on your device — it is never sent anywhere except directly to Google's Gemini endpoint

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- minSdk 26 (Android 8.0+) — needed for notification channels and modern `SpeechRecognizer`/vibration APIs
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
FocusFlow/
├── app/src/main/
│   ├── assets/
│   │   ├── focusflow.html            # App shell
│   │   ├── css/styles.css            # Design system (dark, purple accent)
│   │   └── js/
│   │       ├── data.js               # Local-first data layer (localStorage)
│   │       ├── gemini.js             # Gemini API wrapper + AI feature functions
│   │       ├── ui.js                 # Screen templates (pure render functions)
│   │       └── app.js                # Router, event wiring, focus timer, orchestration
│   ├── java/com/focusflow/
│   │   └── MainActivity.kt           # WebView host + AndroidBridge (haptics, native STT)
│   ├── res/
│   │   ├── layout/activity_main.xml  # FrameLayout with WebView
│   │   ├── values/themes.xml         # Dark NoActionBar theme
│   │   └── mipmap-*/                 # Adaptive launcher icons
│   └── AndroidManifest.xml
├── build.gradle
└── settings.gradle
```

## Android-specific additions

- **Full immersive display** — hides status bar and navigation bar, edge-to-edge rendering
- **Native haptic feedback** — `AndroidBridge` wires task completion and focus actions to the device vibration motor
- **Native speech-to-text** — `AndroidBridge.startListening()` launches Android's `SpeechRecognizer` for Brain Dump and quick capture, with a Web Speech API fallback when running outside the WebView shell
- **Back navigation** — hardware back button navigates WebView history before exiting
