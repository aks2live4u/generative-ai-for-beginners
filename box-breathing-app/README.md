# Box Breathing

A minimal, self-contained box-breathing (4-phase paced breathing) timer app.

## Features

- Four phase timers — **Breathe In**, **Hold**, **Breathe Out**, **Hold** — each adjustable from 1 to 60 seconds.
- Two ways to size a session, switchable at any time:
  - **Set rounds**: pick the number of rounds and the total session time is calculated and shown automatically.
  - **Set duration**: pick a total session length in whole-minute increments, and the number of rounds that fit is calculated automatically.
- Animated breathing circle that grows/shrinks in sync with each phase's duration.
- Optional background video, picked from your device and played on loop behind the session.
- Optional background music, picked from your device and looped during the session.
- Pause/resume and stop controls, plus a session-complete summary.

All media files are read locally in the browser (via `URL.createObjectURL`) and are never uploaded anywhere.

## Development

```bash
npm install
npm run dev
```

## Build

```bash
npm run build
```
