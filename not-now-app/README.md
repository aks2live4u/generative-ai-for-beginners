# Not Now — Behavioral Guardrail System

> **Get Off the Impulse Train**

A personal Android app that creates intentional friction between an impulse and an action. Not a password lock — a waiting period.

## Philosophy

Most app-blocking tools fail because the same person who set the password also wants to bypass it. This app uses **time delays instead of passwords**. If the urge survives the wait, it was probably intentional.

## How It Works

1. **Accessibility Service** monitors which app is in the foreground
2. When a restricted app opens, a **fullscreen overlay** appears immediately
3. A countdown runs — no skip, no bypass (except Emergency Unlock)
4. After the countdown, the app becomes accessible

## Friction Levels

| Level | Category | Delay |
|---|---|---|
| 1 | YouTube, News | 30 seconds |
| 2 | Instagram, Facebook, Reddit, Twitter | 10 minutes |
| 3 | Amazon, Myntra, Flipkart, Ajio | 60 minutes |
| 4 | Night Lockdown (11 PM–7 AM) | Blocked |

## Key Features

### Shopping Vault
When a shopping app opens, you choose:
- **Save For Later** — title, price, link stored locally; 24h later the app asks "Do you still want this?"
- **Buy Now** — 60-minute countdown starts first

### Night Lockdown
11 PM to 7 AM — shopping, social media, and entertainment are blocked. Calls, messages, alarms, maps still work. Scheduled via WorkManager, persists across reboots.

### Emergency Unlock
For genuine emergencies. Unlocks everything for 15 or 30 minutes, then restrictions return automatically. Usage is tracked so you can see patterns.

### Future Self Messages
Write honest notes during clear-headed moments. They appear randomly during countdown screens. Your own words, not generic quotes.

### Weekly Dashboard
Sunday reflection: protected hours, potential spending avoided, peak trigger time, most-attempted app, emergency unlock count.

## Technical Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (dark theme, no accounts, no cloud)
- **Database:** Room (fully local)
- **Preferences:** DataStore
- **Scheduling:** WorkManager
- **Detection:** AccessibilityService (`TYPE_WINDOW_STATE_CHANGED`)
- **Blocking:** Fullscreen Activity overlay over blocked app

## Required Permissions

| Permission | Purpose |
|---|---|
| `BIND_ACCESSIBILITY_SERVICE` | Detect foreground app changes |
| `SYSTEM_ALERT_WINDOW` | Draw countdown overlay over all apps |
| `PACKAGE_USAGE_STATS` | Weekly usage dashboard |

No internet permission. No analytics. No accounts. Everything stays on device.

## Building

```bash
cd not-now-app
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

Install via: `adb install app/build/outputs/apk/debug/app-debug.apk`

## First Launch

1. Grant the three permissions in the Setup screen
2. Enable the Accessibility Service (find "Not Now Guardrail" in the list)
3. The guardrail is active — try opening Instagram

## Version

**v0.1.0 (MVP)** — All core features implemented. No website blocking yet (v0.2).
