# MindShift — Android App

A native Android app wrapping the **MindShift** panic/CBT support tool in a full-screen immersive WebView, with native haptic feedback for the breathing exercise and native dialer hand-off for emergency contacts.

MindShift is offline-first and requires no account or login — everything is stored locally on the device with `localStorage`.

## Why this exists

CBT alone often fails *during* a panic spike because panic is a physiology problem that creates a logic illusion, not a logic problem on its own. MindShift follows this hierarchy instead:

1. **Body regulation** — Crisis Mode
2. **Emotional stabilisation** — short, non-analytical grounding scripts
3. **Cognitive reframing** — CBT Mode, only once intensity has dropped
4. **Reflection / prevention** — Recovery Mode patterns + Daily Strength habits

## Modes

### 🆘 Crisis Mode
A literal 5-step flow, body-first and non-analytical:
1. **You are safe** — names the stress spike and that it will pass
2. **Breathe** — guided breathing circle (4s inhale / 2s hold / 6s exhale)
3. **Grounding** — 5-4-3-2-1 senses checklist
4. **Reality Anchor** — three short reality-check reminders
5. **Support** — call someone you trust, contact a support service, or stay with someone nearby

**Safety gate**: "Need immediate help?" and the Home help panel route straight to crisis hotlines, saved emergency contacts, and a "do not stay alone" prompt — no CBT reasoning in that state.

### 🧠 CBT Mode
A literal 5-step flow, unlocked once intensity is reported below 6/10 from the Home gauge:
1. What happened — the trigger
2. Automatic thought — what your mind says will happen
3. Distortion detected — a guided self-check (Catastrophising, Future Prediction, Emotional Reasoning, Overgeneralisation, Mind Reading, All-or-Nothing) — not automated NLP, since the app is fully offline
4. Check evidence — for / against
5. Reframe — build a more balanced thought

### 💪 Daily Strength (Log)
A simple daily checklist — breathing practice, walking/movement, eating on time, healthy food, sleep, hydration — with a streak counter and a 7-day completion grid.

### 📊 Recovery Mode (Insights)
A 4-tab dashboard:
- **Dashboard** — average mood, a 14-day mood sparkline, high-stress time window, top trigger
- **Entries** — recent crisis/CBT entries grouped by day with colour-coded intensity pills
- **Insights** — a weekly summary of factors correlated with higher stress
- **Triggers** — percentage bars of the most common CBT trigger keywords, plus a medication change log

A heuristic early-warning banner combines recent high-intensity entries, recent medication changes, and recent Daily Strength completion — informational only, **not** a diagnosis, and it says so.

### ⚙️ Settings
- Light / dark mode toggle
- Emergency contacts (tap-to-call via the device dialer)
- Crisis hotline region (Global / US-Canada / UK-Ireland / India)
- About MindShift — mode legend (Crisis / CBT / Recovery / Private / Offline First)
- Clear all local data

## Android-Specific Additions

- **Full immersive display** — hides status bar and navigation bar, edge-to-edge rendering
- **Native haptic feedback** — `AndroidBridge` wires the breathing timer's inhale/exhale cues to the device vibration motor
- **Dialer hand-off** — `tel:` links for hotlines and contacts open the native dialer (`ACTION_DIAL`) instead of auto-calling, so the user always confirms before a call is placed
- **Back navigation** — hardware back button navigates WebView history before exiting
- **Offline-first** — no network permission requested; all data lives in local storage on-device
- **Light / dark theme** — CSS custom properties switched at runtime, persisted to `localStorage`, defaulting to the device's system theme on first launch

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

Or simply open the `MindShift/` folder in Android Studio, let it sync Gradle, and press Run with your phone connected (USB debugging enabled) or an emulator.

## Project Structure

```
MindShift/
├── app/src/main/
│   ├── assets/
│   │   ├── mindshift.html            # Full app: Crisis, CBT, Log, Insights
│   │   └── mindshift-mark.png        # In-app header logo mark
│   ├── java/com/mindshift/
│   │   └── MainActivity.kt           # WebView host + AndroidBridge + tel: hand-off
│   ├── res/
│   │   ├── layout/activity_main.xml  # FrameLayout with WebView
│   │   ├── values/themes.xml         # Theme.MindShift, light NoActionBar theme
│   │   ├── drawable-xxxhdpi/         # Adaptive icon foreground/background PNGs
│   │   └── mipmap-*/                 # Adaptive launcher icon XML referencing the above
│   └── AndroidManifest.xml
├── branding/
│   └── mindshift-logo-source.png     # Original uploaded logo (reference only, not bundled)
├── build.gradle
└── settings.gradle
```

## Important note

MindShift is a support tool, not a replacement for clinical care. The early-warning banner and distortion checklist are simple heuristics, not medical advice. If suicidal thoughts come up, the app's safety gate intentionally pushes toward a human (a contact, a hotline, in-person presence) rather than trying to resolve it within the app.
