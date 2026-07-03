# CareerPilot AI

An autonomous AI career agent: it finds jobs, scores how well they fit your
background, tailors a resume and cover letter for each one, tracks every
application on a Kanban board, schedules follow-ups, preps you for
interviews, and helps you negotiate offers. It stops short of anything that
would violate a job platform's Terms of Service — it drafts, you approve and
send.

## What's real vs. what's simulated

This is a working full-stack app, not a mockup. A few things are worth
knowing before you rely on it:

- **Job data**: `RemoteOK`, `Arbeitnow`, `Greenhouse`, and `Lever` connectors
  hit real, public, free job board APIs (no key required). LinkedIn, Indeed,
  Glassdoor, and Wellfound don't offer a public jobs API, and scraping them
  violates their Terms of Service, so they're intentionally **not**
  automated — a `manual_import` connector ships a handful of realistic sample
  listings so the app has data to work with, and it's the natural place to
  wire up a CSV import for jobs you found manually on those sites.
- **AI generation** (resumes, cover letters, match rationale, interview prep,
  outreach drafts): calls OpenAI when `OPENAI_API_KEY` is set. Without a key,
  every agent falls back to deterministic, template-based logic built from
  your Career Profile — the app is fully usable with zero external
  dependencies, just with less polished prose.
- **Applications and outreach are never sent automatically.** The
  Application Agent tracks status but never submits a form; the Outreach
  Agent drafts a message but requires you to mark it "approved" and copy it
  yourself. This is a deliberate compliance boundary, not a missing feature.
- **Salary data** is a small curated baseline table with a cost-of-labor
  multiplier per country — directionally useful, not a market survey.

## Architecture

```
careerpilot-ai/
├── backend/            FastAPI + SQLAlchemy + SQLite/Postgres
│   ├── app/
│   │   ├── agents/     The 10 AI agents (Scout, Matcher, Resume, CoverLetter,
│   │   │               Application, Outreach, FollowUp, Interview,
│   │   │               Negotiation, CareerCoach)
│   │   ├── services/   AI wrapper, job connectors, scoring, salary, exports
│   │   ├── routers/    REST API endpoints, one file per module
│   │   ├── models.py   Database schema
│   │   └── main.py     App wiring
│   └── seed.py         Demo user + sample data
├── frontend/           React + TypeScript + Vite, installable PWA
│   └── src/pages/       One page per module (Jobs, Applications, Profile, ...)
└── android/            Android Studio project: native WebView shell around
                         the same frontend, for a real installable .apk
```

### Why a WebView shell instead of a native Flutter/Android app

The original brief called for Flutter so it could be "installed as an
Android app." This build environment doesn't have the Flutter/Android SDK
available, so `frontend/` ships as a **Progressive Web App** (Chrome →
**⋮ → Add to Home screen**) and `android/` wraps that same build in a thin
native Kotlin shell you can open directly in Android Studio and build into
a real `.apk` — see `android/README.md` for setup. Both give you an
installed home-screen app; the Android Studio project is the one to use if
you specifically need a `.apk` file. If you want a true Flutter build
later, the FastAPI backend is UI-agnostic and needs no changes.

## Running it locally

### Backend

```bash
cd backend
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env   # fill in OPENAI_API_KEY if you have one
python seed.py          # creates demo@careerpilot.ai / demo1234 with sample data
uvicorn app.main:app --reload
```

API docs at `http://localhost:8000/docs`.

### Frontend

```bash
cd frontend
npm install
cp .env.example .env
npm run dev
```

Open `http://localhost:5173`, log in with the demo credentials seed.py
created (or register your own account).

### Docker Compose (Postgres instead of SQLite)

```bash
cp backend/.env.example backend/.env   # edit as needed
docker compose up --build
```

## Configuration

All configuration is via environment variables — see `backend/.env.example`.
Notable ones:

- `OPENAI_API_KEY` — enables real AI generation; omit to run on fallback logic.
- `GREENHOUSE_COMPANIES` / `LEVER_COMPANIES` — comma-separated board slugs to
  pull live listings from (e.g. `stripe,gitlab,cloudflare`).
- `DATABASE_URL` — defaults to local SQLite; point at Postgres for production.

## Compliance notes

- No credentials are ever stored for third-party job sites.
- Nothing auto-submits an application or auto-sends a message.
- Job connectors only call public, documented, free APIs.
- Outbound network calls to job connectors will silently return zero
  results if network access is blocked — the app degrades gracefully rather
  than erroring, since the `manual_import` sample connector still provides
  data to work with.
