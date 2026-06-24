# DoseMate — Smart Medicine Tracker (Android)

A native Android app for tracking medicine schedules, exact-time reminders, adherence, and trends — built with Kotlin + Jetpack Compose in a "lifted glass" (glassmorphism) UI.

## Features

- **Add medicines** with dosage, time, and one of five frequency types: Daily, Specific Days, Alternate Days, Every X Hours, or **SOS (As Needed)**
- **Exact reminders** via `AlarmManager`, surviving reboot, with actionable notification buttons (Taken / Snooze / Skip)
- **Taken / Missed tracking** with exact timestamps and delay-from-scheduled measurement
- **SOS dosing** — for as-needed medicines, no reminder fires; you log each dose from the Dashboard with one tap, and it still feeds into adherence trends
- **History** browsable by Today / Week / Month
- **Analytics** — adherence %, average delay, streaks, weekly/timing trend charts, and a smart-insights summary
- **Light & dark mode** — the entire glass UI (backdrop gradient, frosted cards, text, charts) adapts to the system theme

## Tech Stack

- Kotlin + Jetpack Compose (Material3)
- Room (SQLite) for medicines and dose logs
- AlarmManager + BroadcastReceivers for exact, boot-resilient scheduling
- AndroidViewModel + Kotlin Flow for reactive state
- Custom Canvas-based trend charts (no external charting library)
- Accompanist Permissions for runtime permission requests

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34, minSdk 26 (Android 8.0+, required for exact-alarm APIs)
- Kotlin 1.9.x

## Building

```bash
./gradlew assembleDebug
```

The debug APK will be at:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```
DoseMate/
├── app/src/main/java/com/dosemate/
│   ├── data/            # Room entities, DAOs, repository
│   ├── scheduling/       # AlarmScheduler, notification + boot receivers
│   ├── analytics/        # Adherence calculations + smart insights
│   ├── viewmodel/        # Dashboard / AddMedicine / History / Analytics view models
│   ├── ui/
│   │   ├── theme/        # Light/dark color sets, glass backdrop, typography
│   │   ├── components/   # GlassCard, StatusPill, TrendLineChart
│   │   ├── screens/      # Welcome, Permissions, Dashboard, AddMedicine, History, Analytics
│   │   └── navigation/   # DoseMateNavHost (bottom-tab Scaffold)
│   └── MainActivity.kt
├── build.gradle
└── settings.gradle
```

## Deferred (V2) Features

Per the original product blueprint, the following are intentionally out of scope for this build: photo-based pill recognition, refill reminders, family/caregiver monitoring, doctor PDF export, and cloud sync.
