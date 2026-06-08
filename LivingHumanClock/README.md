# Living Human Clock — Android App

A native Android app that hosts an animated, dark-themed circular "living diorama" clock: three miniature humans walk, jog and slowly turn around a luxury miniature park to tell the time — built as an immersive WebView experience (HTML/CSS/SVG/JS) wrapped in a thin native shell, the same pattern used by **Tiny Decisions** in this repo.

> **Why a WebView wrapper?** A real Android home-screen widget (`AppWidgetProvider`/`RemoteViews`/Glance) cannot host a `WebView` or run continuous, smooth, GPU-accelerated canvas/CSS animation — it's limited to static `RemoteViews` layouts redrawn on a timer. The rich, cinematic diorama this spec describes (60fps figures, fountain reflections, ambient particles, an isometric camera) is only achievable as a full-screen app experience, so — mirroring the Tiny Decisions approach — it ships as a standalone app you can pin/launch like a live wallpaper.

## What it shows

- A perfect circle made of four concentric rings, exactly as specified:
  1. **Fountain** (center) — animated water, ripples, light jets, falling droplets, ambient glow
  2. **Hour ring** — the *Elegant Gentleman* (slow, continuous, "almost imperceptible" motion at 65% radius)
  3. **Minute ring** — the *Explorer*, walking with a backpack at 85% radius
  4. **Second ring** — the *Runner*, jogging a full lap every 60 seconds at 95% radius
- Twelve brushed-gold, softly-glowing serif numerals that always stay visible on the rim
- A dark, "night-only" luxury park: stone pavement, perimeter bushes/flowers/grass, warm gold lamp posts
- An **isometric camera** — faked with a robust 2.5D "squash the ground into an ellipse, counter-scale the figures" technique (≈40° elevation, `sin 40° ≈ 0.64`) instead of fragile nested 3D transforms
- **Hourly celebration** — at the top of every hour all three humans glide toward 12, a banner announces the hour, soft fireworks burst and a synthesized bell chimes, then everyone glides back to their real-time position
- **Accessibility settings** (gear icon, persisted via `localStorage`): reduce animation, disable the hourly celebration, disable fountain effects, toggle ambient fireflies/leaves
- A small digital readout (`HH:MM:SS` + date) for instant readability

## Time accuracy & animation strategy

Matches the spec's "Animation System" section directly:

| Human | Update cadence | How |
|---|---|---|
| Hour | once per second | target angle recomputed from `hours + minutes/60 + seconds/3600`; a linear 1s CSS transition glides to it — continuous, never jumps on the hour |
| Minute | once per second | target angle from `minutes + seconds/60`; same 1s linear glide |
| Second | every animation frame | angle computed from `seconds + milliseconds/1000`; written directly (no transition) for perfectly continuous, never-snapping motion, one revolution every 60s |

Only `transform`/`left`/`top` are touched (GPU-friendly), CSS `@keyframes` drive the walk/run/sway limb cycles so the browser compositor — not JavaScript — does the per-frame work, and the second-hand loop is the only thing ticking every frame. This keeps the diorama smooth at 60fps with low battery impact, and `FLAG_KEEP_SCREEN_ON` plus hardware layer rendering are enabled in `MainActivity`.

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

## Project Structure

```
LivingHumanClock/
├── app/src/main/
│   ├── assets/
│   │   ├── livingclock.html      # Scene markup, palette & all styling/animation
│   │   └── livingclock.js        # Builds the diorama + drives the real-time clock
│   ├── java/com/livinghumanclock/
│   │   └── MainActivity.kt       # Immersive WebView host
│   ├── res/
│   │   ├── layout/activity_main.xml
│   │   ├── values/{colors,strings,themes}.xml
│   │   └── mipmap-*/             # Adaptive launcher icons (gold ring + fountain)
│   └── AndroidManifest.xml
├── build.gradle
└── settings.gradle
```

## Future themes (from the spec, not yet implemented)

Version 2 ideas worth a settings-panel "theme" switcher: Medieval (King / Knight / Messenger), Space (Commander / Astronaut / Rover), Office (CEO / Manager / Intern), Fantasy (Wizard / Elf / Goblin Runner).
