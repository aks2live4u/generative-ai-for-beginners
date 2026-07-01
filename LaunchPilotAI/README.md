# LaunchPilot AI — Your India Business Launch OS

An Android app that takes a founder from "I have an idea" to "I've launched" —
specific to starting a business in **India**. Not generic advice: a
location-aware, industry-aware roadmap, dynamic checklist, cost estimator,
sourcing finder, funding directory, document generator and an AI Business
Mentor, all grounded in a built-in India business knowledge base.

## How this app is built

This is a native Android project (Gradle + Kotlin) — the same pattern as the
`TinyDecisions` app already in this repo. `MainActivity.kt` is a thin
`WebView` shell that loads a full offline app bundled under
`app/src/main/assets/`:

- `index.html` — all 12 screens (onboarding, dashboard, checklist, task
  detail, sourcing finder, research hub, cost estimator, AI mentor,
  documents & templates, timeline planner, funding & support, profile).
- `css/style.css` — the purple "LaunchPilot AI" visual theme.
- `js/data.js` — the **India Business Knowledge Base**: checklist items
  (why/cost/time/documents/official links/steps), real manufacturing hubs
  (Tiruppur, Ludhiana, Panipat, Jaipur, Morbi, Jalandhar, etc.), real
  government funding schemes (PMEGP, Mudra, Startup India Seed Fund,
  Stand-Up India, CGTMSE…), cost-estimator templates and document templates.
- `js/app.js` — app logic: state, navigation, the checklist rule engine
  (which items apply to a given business profile), the cost estimator, the
  timeline generator, the document generator, and the AI Business Mentor's
  reply engine.

Everything runs **fully offline** — all your business data is stored only on
the device (`localStorage` inside the WebView). Tapping a real link (GST
portal, IndiaMART, MCA, etc.) hands off to your phone's browser instead of
loading inside the app.

### Why WebView instead of Flutter/Compose?

It opens directly in Android Studio with zero extra SDK setup (no Flutter
SDK to install), keeps the whole UI in one place for fast iteration, and
matches the existing convention already used by `TinyDecisions` in this
repo. If you later want a fully native UI, the JS in `app.js` maps cleanly
onto Kotlin/Compose screens — the knowledge base in `data.js` can be reused
as-is (or ported to a Kotlin data layer / Room database).

## Opening the project

1. Unzip and open the `LaunchPilotAI` folder in **Android Studio** (File →
   Open). Let Gradle sync — it needs internet access the first time to
   download the Android Gradle Plugin, Kotlin plugin and AndroidX libraries.
2. Run on an emulator or a physical device (Run ▶ or `Shift+F10`).
3. To build an installable APK: **Build → Build Bundle(s) / APK(s) → Build
   APK(s)**, or from a terminal: `./gradlew assembleDebug` (output lands in
   `app/build/outputs/apk/debug/`).

> Note: this project's Gradle build could not be executed inside the sandbox
> this app was generated in, because that sandbox blocks network access to
> Google's Maven repository (`dl.google.com`), which both the Android
> Gradle Plugin and AndroidX libraries are hosted on. The project structure
> exactly mirrors `TinyDecisions/` in this repo (same Gradle/AGP/Kotlin
> versions), and the actual app logic was verified end-to-end with a
> headless-browser walkthrough of every screen. Once opened in Android
> Studio with normal internet access, Gradle sync will fetch these
> dependencies normally.

## What's real vs. illustrative

- **Government portals, funding schemes and manufacturing hub cities** are
  real, well-known public information (GST, MCA, Udyam, Startup India,
  DGFT, GeM, FSSAI, IP India, PMEGP, Mudra, Stand-Up India, CGTMSE,
  Tiruppur/Ludhiana/Panipat/etc. as India's known industrial clusters).
  Costs and timelines are approximate, general-knowledge estimates — the
  app always tells you to confirm current figures on the linked official
  portal.
- **Individual manufacturer/supplier contact details are intentionally not
  invented.** Rather than fabricate fake company names, phone numbers or
  ratings, the Sourcing Finder links out to real directories (IndiaMART,
  TradeIndia, GeM, Google Search) so you always reach real, currently-listed
  suppliers.
- **The AI Business Mentor is a rule-based engine grounded in the bundled
  knowledge base** — it works fully offline, with no API key required. It
  parses budget mentions, business keywords (GST, trademark, manufacturing,
  funding, passive income, ADHD-friendly planning, etc.) and answers using
  the same data that powers your checklist. If you want to upgrade it to a
  real LLM (OpenAI/Anthropic) later, `generateMentorReply()` in `app.js` is
  the single function to replace with an API call — the knowledge base is
  already structured to serve as retrieval context (RAG) for that.

## Customizing / extending

- Add more checklist items, manufacturing hubs, funding schemes or document
  templates by editing `app/src/main/assets/js/data.js` — no app logic
  changes needed for new data.
- App icon: `app/src/main/res/drawable/ic_launcher_{background,foreground}.xml`
  (adaptive icon, purple rocket).
- App name / package: `com.launchpilot.ai` (see `app/build.gradle` and
  `AndroidManifest.xml`) — change `applicationId` before publishing to Play
  Store if you want a different package name.
