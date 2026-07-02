# RevRadio — Global Radio App

*"Tune in. Rev up."* — A sports-car-themed world radio player built with Expo /
React Native, streaming from [Radio-Browser](https://www.radio-browser.info)'s
free, open directory of 50,000+ internet radio stations.

This is the Phase 1 MVP implementation of the RevRadio App Plan: Home, Search,
Tuning (filters), Now Playing, My Garage (favorites), background playback, and
onboarding — plus a Phase-2-flavored "Track Map" browse-by-country screen and
Settings.

## Quick start

```bash
npm install
npx expo start
```

RevRadio uses `react-native-track-player` for background playback with lock-
screen controls, so it needs a **custom dev client** — it will not run inside
plain Expo Go. To build one:

```bash
npx expo prebuild
npx expo run:ios      # or
npx expo run:android
```

`npm run web` also works (Metro/react-native-web) for quick UI iteration in a
browser — playback controls work but background/lock-screen behavior is
native-only.

## Project structure

```
app/                      expo-router routes (file-based navigation)
  _layout.tsx              root stack, theme, playback-error bridge
  index.tsx                Splash / Launch (engine-start animation)
  onboarding.tsx            3-step onboarding (languages, genres)
  (tabs)/                  bottom tab bar — "center console"
    _layout.tsx             custom cockpit-styled tab bar + MiniPlayer
    index.tsx                Home Dashboard
    search.tsx                Search (+ voice search, recents)
    garage.tsx                My Garage (favorites / fleets / history)
    tuning.tsx                 Tuning filter panel
  results.tsx               Station list results
  now-playing.tsx           Full-screen Now Playing dashboard
  settings.tsx               Settings
  browse-country.tsx         "Track Map" — browse by country

src/
  api/radioBrowser.ts        Radio-Browser client: mirror discovery,
                               round-robin, search/tags/countries/languages,
                               click registration, voting
  api/cache.ts                TTL cache over AsyncStorage
  store/                      Zustand stores (settings, favorites/garage,
                               filters/presets, player)
  services/PlaybackService.ts  react-native-track-player headless service
  services/voiceSearch.ts      Voice search wrapper (native STT)
  components/                 TachometerDial, StationCard, GenreShowroom,
                               FilterChip, MiniPlayer, ScreenHeader, EmptyState
  theme/                       Cockpit dark / Daylight light palettes,
                               typography, spacing
  types/station.ts             Station, filter, preset, fleet types
  constants/genres.ts          Genre showrooms, EQ presets, onboarding langs
```

## Tech stack (per plan §5)

| Layer | Choice |
|---|---|
| Framework | React Native (Expo, Router for navigation) |
| Audio engine | `react-native-track-player` |
| State | Zustand (persisted to AsyncStorage) |
| API layer | Radio-Browser REST API via `fetch`, with mirror round-robin + TTL cache |
| Local storage | AsyncStorage (favorites, fleets, history, presets, settings) |
| Maps | Deferred to Phase 2 — see "Known simplifications" below |
| Voice search | `@react-native-voice/voice` (native STT) |

## Known simplifications vs. the full plan

These are deliberate MVP scope cuts, not oversights — each is called out
in-code where relevant:

- **World Map view** — the plan scopes the interactive `react-native-maps`
  "track map" to Phase 2. This MVP ships a dependency-free **"Track Map"
  browse-by-country grid** (`app/browse-country.tsx`) as a functional
  stand-in with the same entry point from Home.
- **Bitrate filter** — implemented as discrete quality chips (Any / 128+ /
  192+ / 256+ / 320) instead of a continuous slider, to avoid pulling in
  `@react-native-community/slider` for one control.
- **Equalizer presets** — the picker UI and preference are implemented and
  persisted (`Highway` / `City` / `Track Day` / `Flat`), but there's no real
  DSP EQ engine wired up yet; that needs a native audio-processing module.
- **"Report broken stream"** — Radio-Browser only exposes a positive `vote`
  endpoint, not a negative/broken-report one. Reports are queued locally
  (`favoritesStore.brokenReportedIds`) for a future Phase 3 backend sync.
- **CarPlay / Android Auto** — `react-native-track-player` is the right
  engine for this, but the platform-specific screens/templates aren't wired
  up in this pass.
- **Accounts / cross-device sync** — explicitly Phase 3 in the plan; My
  Garage is local-only for now (open decision #1 from the plan, §10).

## Open decisions (carried over from the plan, §10)

1. Should My Garage sync across devices (needs accounts/backend) or stay
   local for MVP? *(currently: local-only)*
2. One-time paid unlock vs. subscription vs. ad-supported free forever?
3. App name/brand — keep "RevRadio," or go with an alternative (Pit Radio,
   Redline Radio, Turbo Tune)?

## Verification performed this session

- `tsc --noEmit` — clean, no type errors.
- `npx expo export --platform web` — full Metro bundle succeeds (1104
  modules resolved).
- Served the web export and drove it with Playwright: onboarding flow, Home
  dashboard, Search, My Garage, and Tuning all render correctly with the
  cockpit dark theme. Live Radio-Browser API calls fail in this sandbox
  (outbound network is proxied/restricted here) — the app's offline/error
  states render as designed rather than crashing.
- Not yet verified: an actual device/simulator run through `expo prebuild` +
  `expo run:ios`/`run:android` (needed to test lock-screen controls,
  CarPlay/Android Auto, and real audio playback) — do this before shipping.
