# Not Now — Behavioral Guardrail App

> Get Off the Impulse Train

A personal Android app that creates intentional friction between an impulse and an action.

## How It Works

1. **Enable Accessibility Service** → detects when a restricted app opens
2. **Draw Over Apps permission** → shows the countdown overlay on top
3. **Usage Access** → logs which apps you attempted to open

## Features

| Feature | Description |
|---|---|
| **Countdown Delays** | 30s (YouTube) · 10min (Social) · 60min (Shopping) |
| **Shopping Vault** | Save items for later, reminded after 24h |
| **Night Lockdown** | 11 PM–7 AM: blocks social, shopping, entertainment |
| **Focus Mode** | Strict blocking during work/study |
| **Emergency Unlock** | 15-minute override for genuine needs |
| **Future Self Messages** | Your own words shown during countdowns |
| **Weekly Dashboard** | Minimal stats: friction events, peak hour |

## Build

Open in **Android Studio Iguana** or newer.

```
minSdk 26 (Android 8.0)
targetSdk 34
Kotlin 1.9 · Compose · Room · DataStore · WorkManager
```

## Default App Rules

| App | Delay |
|---|---|
| YouTube | 30 seconds |
| Instagram, Facebook, X, Reddit, Snapchat | 10 minutes |
| Amazon, Myntra, Flipkart, Ajio | 60 minutes |

All rules can be toggled on/off from the home screen.

## Architecture

```
data/
  entity/      Room entities
  dao/         Database access
  repository/  Business logic
  preferences/ DataStore
service/
  GuardrailAccessibilityService  ← core detection engine
ui/screen/
  overlay/     CountdownOverlayActivity + ShoppingOverlayActivity
  home/        Mode toggle + rules list
  vault/       Shopping Vault
  messages/    Future Self Messages
  dashboard/   Weekly Review
worker/        WorkManager tasks (reminders, pruning)
```

## Privacy

- **Zero network requests**
- **No accounts**
- **No analytics**
- All data stays on your device in a local Room database
