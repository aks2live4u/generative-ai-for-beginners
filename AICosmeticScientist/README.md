# AI Cosmetic Scientist — Android App

An intelligent cosmetic formulation platform that acts as your personal cosmetic chemist, formulation mentor, and R&D assistant.

## Features

### Formulation Wizard
Step-by-step formula creation across all major product categories with goal-based ingredient selection.

| Category | Product Types |
|---|---|
| Skincare | Face Moisturizer, Serum, Face Wash, Toner, Face Mask |
| Haircare | Shampoo, Conditioner, Hair Oil |
| Beard Care | Beard Oil, Beard Balm |
| Body Care | Body Lotion, Body Butter |
| Lip Care | Lip Balm |
| Bath Products | Bath Bomb |
| Deodorants | Roll-On Deodorant |

### Ingredient Intelligence
Scientific database of 70+ cosmetic ingredients, each with:
- INCI name, functions, usage ranges
- Mechanism of action
- Evidence score (0–100)
- Safety profile
- Cost intelligence
- Alternatives

### Formula Generation Engine
- Goal-based ingredient selection (Hydration, Anti-Aging, Brightening, Acne, Sensitive, etc.)
- Skin/hair type optimization
- Budget filtering (Economy / Mid-Range / Premium)
- Automatic percentage normalization to 100%
- Gram calculations per batch size
- Real-time cost estimate per batch

### Formula Analysis
- Safety Score, Stability Score, Difficulty Score
- Phase-by-phase manufacturing instructions (Phase A / B / C)
- Equipment checklist
- Cost breakdown by ingredient

### Smart Inventory
- Track owned ingredients with quantity, cost, supplier, expiry date
- Expiry warnings
- Total inventory value calculation

### Lab Notebook
- Document experiments, observations, and results
- Star rating system
- Persistent storage across sessions

## Formulation Modes

| Mode | Used For |
|---|---|
| Safe (Economy) | Proven industry-standard formulations |
| Smart (Mid) | Goal-optimized with balanced cost/performance |
| Premium | Highest-performance ingredient selection |

## Architecture

```
AICosmeticScientist/
├── app/src/main/
│   ├── assets/
│   │   └── cosmetic_scientist.html   ← Complete SPA app
│   ├── java/com/aicosmeticscientist/
│   │   └── MainActivity.kt           ← WebView host
│   ├── res/
│   │   ├── layout/activity_main.xml
│   │   ├── values/themes.xml         ← Dark theme
│   │   └── mipmap-*/                 ← Adaptive icons
│   └── AndroidManifest.xml
├── build.gradle
└── settings.gradle
```

## Building

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- minSdk 24 (Android 7.0+)
- Kotlin 1.9.x

## Offline Capabilities

All core features work offline:
- Ingredient database (70+ ingredients)
- Formula generation engine
- Inventory tracking
- Lab notebook
- Cost calculations
- Formula comparisons

## Data Persistence

All user data (formulas, inventory, notes) is stored in `localStorage` within the WebView — no internet or account required.
