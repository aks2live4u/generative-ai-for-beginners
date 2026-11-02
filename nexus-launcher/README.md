# NEXUS — JARVIS Edition

An AI-first Android home-screen launcher. The look is obsidian black with luminous cyan, apps sit on orbital rings, widgets adapt to the time of day, and a voice-driven command centre runs on Google Gemini.

The design aim: it should feel like technology from 2035 without being hard to use. Every futuristic surface has a plain fallback: search, a classic app grid, reduced motion, and large touch targets.

## The five screens

| Screen | What it does |
| --- | --- |
| **Home** | A large clock and greeting, then **your four most-used apps**, the same apps that sit on the orbit's inner ring. Suggestions adapt to the time of day and low battery, and the battery and focus widgets sit below. Swipe up for apps, down for alerts. |
| **AI Command Centre** | Tap the orb in the dock and NEXUS is **already listening**; tap it again to stop. It acts rather than just talks: it calls and texts your contacts (after a 3-second countdown with a Cancel button), opens WhatsApp chats with your message typed in, drafts emails, starts navigation, plays music, sets alarms, timers and calendar events, switches the torch, sets the volume, opens Wi-Fi, Bluetooth and other settings panels, opens apps and runs Focus Mode. Live facts such as weather come from Google Search grounding. |
| **App Navigation** | Your 30 most-used apps on three rings, with the most-used closest to the core. Ranking uses Android's real screen time once you turn on **Usage access**. Drag to spin the rings; tap the core for every app as a grid. Search is always at the top, and a long-press opens App info. |
| **Alerts** | The one place for notifications. The dock's Alerts tab shows an amber badge with the number of priority alerts. Inside, alerts are split into **Priority** (people, calls, reminders, alarms, events) and a quiet **Digest**, and "Brief me" reads you a short summary. |
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
3. On the **Alerts** tab, tap **Allow notification access**.
4. Tap the card **Rank apps by how you really use them** (or Settings › Usage access) and switch NEXUS on, so Home and the orbit show your real most-used apps.
5. The first time you say "call …" or "text …", Android asks for Phone, SMS and Contacts permission. Allow them.
6. Optionally allow Do Not Disturb access from Focus Mode so sessions can silence interruptions.

## How the AI works

NEXUS runs on **Google Gemini**. `app/src/main/java/com/nexus/launcher/ai/GeminiClient.kt` is a small REST client for the Gemini API (OkHttp plus Android's built-in `org.json`). `NexusAI.kt` holds the conversation, the prompts and the phone actions.

- **Key:** a free Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey), pasted into Settings. Settings › **Test connection** checks the key and model end to end and reports the result.
- **Model:** `gemini-2.5-flash` by default, with thinking switched off so spoken replies are quick. If the model you set is retired or your key can't use it, NEXUS reads your key's model list, switches to a working Flash model, and saves the choice.
- **Live facts:** Grounding with Google Search for weather, news, scores and similar. You can turn it off in Settings. If a model rejects search, NEXUS answers without it.
- **Phone actions:** the system prompt teaches Gemini action tags such as `[[call:Mum]]`, `[[text:Raj|Running late]]`, `[[whatsapp:Priya|On my way]]`, `[[navigate:home]]`, `[[play:lofi]]`, `[[torch:on]]`, `[[volume:40]]`, `[[event:Dentist|2026-11-03T15:00|30]]`, `[[panel:wifi]]`, `[[alarm:07:30|Gym]]` and `[[focus:25]]`. The launcher strips the tags and runs them (`ui/ActionRunner.kt`, `system/PhoneActions.kt`). Contact names are matched on the phone; names are never sent to Gemini beyond what you say. Calls and texts wait 3 seconds with **Cancel** and **Now** buttons, shown on every screen.
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
