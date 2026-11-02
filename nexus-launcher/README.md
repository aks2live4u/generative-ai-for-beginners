# NEXUS — JARVIS Edition

An AI-first Android home-screen launcher. The look is obsidian black with luminous cyan, apps sit on orbital rings, widgets adapt to the time of day, and a voice-driven command centre runs on Google Gemini.

The design aim: it should feel like technology from 2035 without being hard to use. Every futuristic surface has a plain fallback: search, a classic app grid, reduced motion, and large touch targets.

## The five screens

| Screen | What it does |
| --- | --- |
| **Home** | A large clock and date with a greeting that changes through the day. NEXUS suggests actions that adapt to the time and to low battery. Widgets show a battery ring, today's focus minutes or the live timer, and a count of urgent notifications. A row of "Predicted for you" apps is ranked by how often you open them. Swipe up for apps, down for notifications. |
| **AI Command Centre** | Tap the orb and speak, or type. Gemini answers, and the answer is read aloud. NEXUS can also act on the phone: open apps, start or stop Focus Mode, set alarms and timers, open the dialler, run a web search, or switch screens. It can look up live facts such as weather and news with Google Search grounding. |
| **App Navigation** | Your 30 most-used apps sit on three counter-rotating rings. Drag to spin them, with momentum. Tap the core for every app as a grid with category filters. Search is always at the top. Long-press any app for App info. |
| **Notifications** | Real notifications (once you grant access), split into **Priority** (people, calls, reminders, alarms, events) and a quiet **Digest**. "Brief me" gives a spoken Gemini summary of what needs you and what can wait. You can dismiss one notification or clear the whole digest. |
| **Focus Mode** | A 15, 25, 50 or 90-minute countdown ring. It can switch Do Not Disturb to priority-only and limit the launcher to up to four allowed apps; the rest dim. Today's minutes and sessions are tracked. |

A **Settings** page (the gear icon on Home) holds your API key, the model, your name, and toggles for web answers, spoken replies, reduced motion, the classic grid and interface sounds. It also has a shortcut to set NEXUS as your default home app.

## Build and install

You need Android Studio (Ladybug or newer) or the Android SDK with platform 35, and JDK 17+.

```bash
cd nexus-launcher
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or open the `nexus-launcher` folder in Android Studio and press Run. Then:

1. Press Home and choose **NEXUS**, or go to Settings › Make NEXUS the default home app.
2. Open Settings (gear icon), paste a Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey), and tap **Test connection**.
3. Tap **Grant notification access** on the Notifications screen.
4. Optionally allow Do Not Disturb access from Focus Mode so sessions can silence interruptions.

## How the AI works

NEXUS runs on **Google Gemini**. `app/src/main/java/com/nexus/launcher/ai/GeminiClient.kt` is a small REST client for the Gemini API (OkHttp plus Android's built-in `org.json`). `NexusAI.kt` holds the conversation, the prompts and the phone actions.

- **Key:** a free Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey), pasted into Settings. Settings › **Test connection** checks the key and model end to end and reports the result.
- **Model:** `gemini-2.5-flash` by default, with thinking switched off so spoken replies are quick. If the model you set is retired or your key can't use it, NEXUS reads your key's model list, switches to a working Flash model, and saves the choice.
- **Live facts:** Grounding with Google Search for weather, news, scores and similar. You can turn it off in Settings. If a model rejects search, NEXUS answers without it.
- **Phone actions:** the system prompt teaches Gemini a small tag vocabulary such as `[[open:Spotify]]`, `[[focus:25]]`, `[[alarm:07:30|Gym]]`, `[[timer:300|Tea]]`, `[[dial:+15551234567]]`, `[[search:query]]` and `[[screen:alerts]]`. The launcher strips the tags from the reply and runs them. Dialling only opens the dialler; it never places a call.
- **Errors:** a bad key, quota limits, an unknown model, a server outage or no network each get a plain spoken message.
- **Privacy:** notification text goes to Gemini only when you ask for a briefing. Your API key is stored in the app's private storage.

## Project layout

```
app/src/main/java/com/nexus/launcher/
├── MainActivity.kt, NexusApplication.kt   entry point and service graph
├── ai/                                    Gemini client, prompts and action-tag parser
├── data/                                  settings and the installed-app repository
├── system/                                focus timer and DND, speech in and out,
│                                          UI sounds, notification listener, battery and clock
└── ui/
    ├── NexusApp.kt                        navigation, the orbital dock, action runner
    ├── components/                        orb, glass cards, ring gauge, pills, background
    ├── screens/                           Home, Command Centre, Orbit, Alerts, Focus, Settings
    └── theme/                             the obsidian and cyan palette and type
```

## Notes and limits

- The minimum Android version is 8.0 (API 26). The target is API 35.
- Voice input uses the phone's built-in speech recogniser. Some devices need the Google app installed.
- Release builds are minified with R8.
- Weather and calendar have no separate widgets. Ask NEXUS instead; it uses web search for live answers.
