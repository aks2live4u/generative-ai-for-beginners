# Stash — Personal AI-Powered Content Vault

A local-first Android app for saving and rediscovering everything you bookmark
across Instagram, YouTube, Facebook, Reddit, X, and the web — plus your own
notes and articles — in one searchable, Pinterest-style vault. No login, no
cloud account, no social features.

## Stack

- **Flutter** (Android only, min SDK 26 / Android 8+, targets Android 12+)
- **Riverpod** for state management
- **SQLite** (`sqflite`) with an **FTS5** virtual table for global full-text search
- **Gemini API** (optional) for AI tagging/summarization, with an on-device
  keyword-heuristic fallback so the app works fully offline without any key
- Local file storage only — backups are exported as a ZIP you control

## Implemented (MVP Phases 1–5)

- Bottom navigation: Home, Collections, Articles, Search, Settings
- Pinterest-style masonry Home feed with type filter chips and an Inbox
- "Share to Stash" via the Android share sheet (`receive_sharing_intent`) plus
  manual "Add Link" with OpenGraph metadata extraction
- AI organization engine: auto tags, auto collection suggestions, and a short
  summary for every saved item (Gemini if configured, heuristic otherwise)
- Collections: create, rename, delete, merge, pin, favorite; items can belong
  to many collections
- Global search across titles, descriptions, summaries, tags, and note bodies
  via SQLite FTS5, with sort by newest/oldest/most opened
- Articles tab (web + personal) with reading-time estimate
- Personal Notes / Articles editor with lightweight Markdown formatting
  toolbar (headings, bold, italic, lists, checkboxes, quotes, code, links),
  preview mode, drafts, and autosave on exit
- Favorites (star) and open-count tracking
- Themes: Light, Dark, AMOLED Dark, System default
- Local backup: export the whole vault to a ZIP (DB + JSON) and restore it
  later — no account required

## Deferred (not yet built)

Voice capture, the "Daily Rediscovery" surfacing screen, and the future
"AI Chat With Vault" feature are not implemented in this MVP. The data model
and AI service are structured so they can be added without a rework.

## Running it

```sh
cd Stash
flutter pub get
flutter run
```

Set a Gemini API key from **Settings → AI Settings** to enable cloud-based
tagging/summarization; otherwise Stash organizes saves using a local
heuristic, no network or API key needed.

## App icon

`assets/icon/icon.png` is the legacy/Play Store icon and
`assets/icon/icon_foreground.png` + `icon_background.png` are the Android
adaptive icon layers, generated from the supplied artwork with extra padding
so the badge is never clipped by circular/squircle/rounded-square launcher
masks. Regenerate Android launcher icons after changing the source art with:

```sh
dart run flutter_launcher_icons
```
