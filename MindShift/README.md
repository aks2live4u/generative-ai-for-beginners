# CalmAnchor — Android App

A native Android app wrapping the **CalmAnchor** panic/CBT support tool in a full-screen immersive WebView, with native haptic feedback for the breathing exercise and native dialer hand-off for emergency contacts.

CalmAnchor is offline-first and requires no account or login — everything is stored locally on the device with `localStorage`.

## Why this exists

CBT alone often fails *during* a panic spike because panic is a physiology problem that creates a logic illusion, not a logic problem on its own. CalmAnchor follows this hierarchy instead:

1. **Body regulation** — Crisis Mode
2. **Emotional stabilisation** — short, non-analytical grounding scripts
3. **Cognitive reframing** — CBT Mode, only once intensity has dropped
4. **Reflection / prevention** — Recovery patterns + Daily Strength habits

## Modes

### 🆘 Crisis Mode (0–10 min)
- "Name the state" — Panic, Overwhelm, Intrusive thoughts, Dissociation, or Suicidal thoughts
- Body-first tools, not thinking: cold water/splash prompt, 5-4-3-2-1 grounding, feet-pressure exercise, and a slow-exhale breathing timer (4s in / 6s out, the longer exhale calms the nervous system)
- An ultra-short stabilising script per state — no analysis, no debating thoughts
- **Safety gate**: selecting "Suicidal thoughts" skips straight to crisis hotlines, saved emergency contacts, a "do not stay alone" prompt, and breathing/grounding only — no CBT reasoning in that state

### 🧠 CBT Reframing Mode
Unlocked once intensity is reported below 6/10 (from the home slider or after a crisis check-in):
1. Trigger — what happened just before the spike
2. Automatic thought — what your mind says will happen
3. Evidence split screen — for / against
4. Cognitive distortion self-check — Catastrophising, Fortune telling, Emotional reasoning, Overgeneralisation, Mind reading, All-or-nothing (a guided checklist, not automated NLP detection, since the app is fully offline)
5. Balanced thought builder

### 💪 Daily Strength (prevention habits)
A simple daily checklist — breathing practice, walking/movement, eating on time, healthy food, sleep, hydration — with a streak counter and a 7-day completion grid. This is the layer most panic apps skip, and where day-to-day resilience is actually built.

### 📊 Patterns (recovery / tracking)
- Time-of-day chart for crisis entries
- Trigger keyword tally pulled from CBT journal entries
- Medication change log (date, change, notes) to correlate med changes with anxiety spikes
- A heuristic early-warning banner combining recent high-intensity entries, recent medication changes, and recent Daily Strength completion — informational only, **not** a diagnosis, and it says so

### ⚙️ Settings
- Emergency contacts (tap-to-call via the device dialer)
- Crisis hotline region (Global / US-Canada / UK-Ireland / India)
- Clear all local data

## Android-Specific Additions

- **Full immersive display** — hides status bar and navigation bar, edge-to-edge rendering
- **Native haptic feedback** — `AndroidBridge` wires the breathing timer's inhale/exhale cues to the device vibration motor
- **Dialer hand-off** — `tel:` links for hotlines and contacts open the native dialer (`ACTION_DIAL`) instead of auto-calling, so the user always confirms before a call is placed
- **Back navigation** — hardware back button navigates WebView history before exiting
- **Offline-first** — no network permission requested; all data lives in local storage on-device

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

Or simply open the `CalmAnchor/` folder in Android Studio, let it sync Gradle, and press Run with your phone connected (USB debugging enabled) or an emulator.

## Project Structure

```
CalmAnchor/
├── app/src/main/
│   ├── assets/
│   │   └── calmanchor.html           # Full app: Crisis, CBT, Daily Strength, Patterns
│   ├── java/com/calmanchor/
│   │   └── MainActivity.kt           # WebView host + AndroidBridge + tel: hand-off
│   ├── res/
│   │   ├── layout/activity_main.xml  # FrameLayout with WebView
│   │   ├── values/themes.xml         # Dark teal NoActionBar theme
│   │   └── mipmap-*/                 # Adaptive launcher icons
│   └── AndroidManifest.xml
├── build.gradle
└── settings.gradle
```

## Important note

CalmAnchor is a support tool, not a replacement for clinical care. The early-warning banner and distortion checklist are simple heuristics, not medical advice. If suicidal thoughts come up, the app's safety gate intentionally pushes toward a human (a contact, a hotline, in-person presence) rather than trying to resolve it within the app.
