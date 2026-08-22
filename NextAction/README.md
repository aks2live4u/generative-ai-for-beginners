# Next Action — AI-Powered Next-Action To-Do App

A native Android app wrapping a self-contained **Next Action** web experience: an
AI to-do list that hides project planning behind one obvious next step, planned
by the **Gemini API**.

> You tell it what you need to accomplish. It figures out the plan. You only
> have to do the next thing.

## The core loop

```
DUMP → AI PLANS → SHOW NEXT ACTION → DO → COMPLETE → NEXT ACTION
```

Add a goal in **Inbox** ("Plan a 7-day trip to London", "Create a team
performance report"). Gemini breaks it into the smallest useful next actions
— specific, executable, with a time estimate and dependency order — entirely
in the background. **Today** shows exactly one action at a time; mark it done
and the next best action appears automatically. **Plan** is the optional
full-project view for anyone who wants to see the whole breakdown.

## Features (V1)

- **Inbox** — free-text (and voice, where supported) capture of any goal,
  task or vague idea. Input is saved immediately and never lost, even if
  planning fails.
- **AI planning** — Gemini returns structured JSON (goal, optional deadline,
  and an ordered list of actions with `estimated_minutes`, `dependencies`,
  and `priority`) via `responseSchema`, not free-form text.
- **Today** — a next-action selection algorithm runs entirely on-device:
  filter to pending actions whose dependencies are done, then rank by
  priority, project deadline, and plan order. Only the winner is shown.
- **Plan** — per-goal progress bar and full step list (done / next / pending),
  with a "Tell the AI what changed" box that triggers a scoped re-plan of the
  remaining steps only (completed steps are never touched).
- **Time estimates** — snapped to the fixed bucket set `5 · 10 · 15 · 20 ·
  30 · 45 · 60 · 90+` min, deliberately avoiding false precision.
- **Settings** — Gemini API key and model name, stored only on-device.

## What V1 intentionally leaves out

Per the product plan: no accounts/backend, no analytics, no gamification, no
calendar integration, no kanban/Gantt views. This keeps the app to a single
self-contained HTML/CSS/JS file with no build step, matching the rest of this
repo's small hand-built apps (see `../TinyDecisions`).

## Architecture

There's no backend. The web app calls the Gemini REST API
(`generativelanguage.googleapis.com`) directly from the device using a
user-supplied API key, and persists everything else — inbox items, goals,
actions, settings — in `localStorage` inside the WebView. That means:

- **Your data stays on your device.** Nothing is sent anywhere except the
  Gemini endpoint, and only when a goal is (re)planned.
- **AI failures never lose your input.** An inbox item is saved before
  planning starts; if the request fails (no key, network error, malformed
  response) the item is marked with a retry affordance and the raw text is
  kept.
- **Re-planning is scoped.** Completed actions are immutable; a re-plan only
  regenerates the remaining steps of one project, using the completed
  history and your feedback as context.

```
NextAction/
├── app/src/main/
│   ├── assets/
│   │   └── nextaction.html        # Full web app: UI, state, Gemini calls
│   ├── java/com/nextaction/
│   │   └── MainActivity.kt        # WebView host + haptics + mic permission
│   ├── res/                       # Launcher icon, light theme, layout
│   └── AndroidManifest.xml
├── build.gradle
└── settings.gradle
```

## Android-specific additions

- **WebView shell** loading `nextaction.html` from assets, with
  `domStorageEnabled` for `localStorage` persistence across launches.
- **Native haptic feedback** on completing an action, via the same
  `AndroidBridge` JavaScript-interface pattern used in `TinyDecisions`.
- **Voice capture** — the page uses the standard Web Speech API
  (`webkitSpeechRecognition`); `MainActivity` requests `RECORD_AUDIO` at
  startup and grants the WebView's mic permission request only once the OS
  permission is held, so typing keeps working if the user declines it.
- **Back navigation** — hardware back button navigates WebView history
  before exiting.

## Getting a Gemini API key

Open the app's **Settings** (gear icon) and paste a key from
[aistudio.google.com/apikey](https://aistudio.google.com/apikey). The
default model is `gemini-2.5-flash`; any Gemini model name that supports
`generateContent` with a JSON `responseSchema` can be typed in instead.

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

`nextaction.html` also runs standalone in any modern desktop or mobile
browser — open the file directly, or serve it with any static file server —
which is how it was developed and tested.

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- minSdk 24 (Android 7.0+)
- Kotlin 1.9.x
