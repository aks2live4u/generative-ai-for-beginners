# FinSight

A private, local-first personal finance assistant for Android. There is no
account/registration, no cloud database, and no analytics or ad SDKs. All
financial data lives in an encrypted on-device database, and every external
data source (SMS, notifications, Gmail) is read-only.

## Architecture

The project is a two-module Gradle build:

- **`core`** — pure Kotlin/JVM module (no Android dependency) containing the
  domain models, SMS/notification/email text parsers, the merchant/category
  classifier, and the on-device "AI" logic (financial health score,
  hidden-subscription/savings detector, rule-based chat assistant). Because
  this module has no Android dependency, its unit tests run with a plain
  `java`/`kotlin` toolchain — no Android SDK required.
- **`app`** — the Android application: Jetpack Compose UI, Room +
  SQLCipher encrypted storage, biometric/PIN security, SMS/notification/Gmail
  integrations, and WorkManager-scheduled backups.

```
app/  → Compose UI, Android data layer, security, integrations, DI container
core/ → domain models, parsers, categorizer, AI engines (pure Kotlin)
```

## Privacy & security model

- **Local-first**: all transactions, subscriptions, and goals are stored in a
  SQLCipher-encrypted SQLite database on-device. There is no cloud sync and
  no companion server.
- **Encrypted at rest**: the SQLCipher passphrase is generated randomly and
  stored only in `EncryptedSharedPreferences`, which is itself backed by the
  Android Keystore.
- **Read-only data sources**: the app only ever *reads* bank/UPI SMS,
  notifications from an explicit allow-list of finance/delivery apps, and
  Gmail (via the `gmail.readonly` OAuth scope). It never sends SMS, posts/
  dismisses notifications, or sends email, and never touches banking apps
  directly.
- **PIN/biometric app lock**: the PIN is salted and hashed before storage
  (never stored in plaintext); biometric unlock is optional and backed by
  `BiometricPrompt`.
- **Backups stay encrypted**: `BackupManager` only ever copies the
  already-encrypted SQLCipher database file byte-for-byte — it's never
  decrypted or re-encrypted with a weaker scheme. A stolen backup file is
  useless without the Keystore-protected passphrase that lives only on the
  original device.
- **No ads, no analytics, no third-party trackers.**

## Requirements

- Android Studio (Hedgehog or newer recommended)
- JDK 17 (bundled with recent Android Studio)
- Android SDK with `compileSdk 34` / `minSdk 26` installed (Android Studio
  will prompt to install missing SDK components on first sync)

## Building

Open the project root in Android Studio and let it sync — Android Studio
will regenerate `gradle/wrapper/gradle-wrapper.jar` automatically if it's
missing (this repo's sandbox has no network access to `services.gradle.org`,
so that binary couldn't be committed from here). Alternatively, from a
machine with normal internet access, run `gradle wrapper --gradle-version
8.4` once to generate it, then use the wrapper as usual:

```bash
./gradlew :app:assembleDebug
```

> **Sandbox/CI note:** environments without network access to Google's Maven
> repository (`dl.google.com`) cannot resolve the Android Gradle Plugin, so
> `:app` can only be built somewhere with normal internet access (a real
> dev machine, Android Studio, or CI with the usual Maven mirrors). The
> `:core` module has no such dependency and can always be built and tested
> standalone — see below.

## Running tests

The `core` module's unit tests (parsers, categorizer, financial health score,
savings detector, chat assistant) require no Android SDK and no network
access beyond Maven Central:

```bash
./gradlew :core:test
```

Android instrumented/unit tests under `app` (Room DAOs, security, UI state
mapping) require a configured Android SDK/emulator and should be run from
Android Studio or a CI runner with the Android SDK installed:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest   # requires a device/emulator
```

## Gmail setup (optional)

Gmail scanning is optional and read-only. To enable it:

1. Create a project in the [Google Cloud Console](https://console.cloud.google.com/).
2. Enable the **Gmail API**.
3. Configure an OAuth consent screen (internal/testing is fine for personal
   use) and request only the `https://www.googleapis.com/auth/gmail.readonly`
   scope.
4. Create an **OAuth 2.0 Client ID** of type "Android", using your app's
   package name (`com.finsight`) and the SHA-1 fingerprint of your
   signing key (`./gradlew signingReport` will print it for your debug/
   release keystore).
5. Copy the generated client ID and replace the placeholder in
   `app/build.gradle`:

   ```groovy
   resValue "string", "gmail_oauth_client_id", "\"YOUR_CLIENT_ID.apps.googleusercontent.com\""
   ```

If you skip this setup, the Gmail permission step in onboarding can simply
be skipped — it's marked optional, and SMS/notification scanning work
independently of it.

## AI features (optional, Gemini)

FinSight's chat assistant, financial-health/Hidden-Expense explanations, and
duplicate/fraud/insurance Smart Scan can optionally be upgraded from the
built-in rule-based engine to Google's Gemini Flash model. This is **off by
default** — nothing is sent off-device until you opt in.

1. Get a free Gemini API key at [Google AI Studio](https://aistudio.google.com/app/apikey).
2. In the app, go to **Settings**, paste the key, and tap **Save API Key**.
3. Turn on **Enable AI features**. Use **Test Connection** to confirm the key
   works.
4. Tap **Remove API Key** at any time to delete the key and turn the feature
   back off.

Your API key is stored on-device only, encrypted via Android Keystore
(`EncryptedSharedPreferences`, the same mechanism used for your app PIN) — it
is never sent anywhere except as the `key` query parameter on calls you've
explicitly opted into. When enabled:

- The **chat assistant** sends a summary of your transactions (merchant,
  amount, date, category) and subscriptions to Gemini — never the raw SMS/
  email text, account numbers, or phone numbers.
- **Insights "Ask AI to explain"** sends the same kind of summary plus your
  computed health-score factors / hidden-expense findings, so Gemini can
  explain them in plain language.
- **Smart Scan** sends your imported transactions, including the original
  SMS/email/notification text, so Gemini can spot likely cross-source
  duplicates, fraud-like anomalies, and insurance policies — but any digit
  run of 9+ characters (account/card numbers) is masked first, keeping only
  the last 4 digits.

If you leave AI features off, or any Gemini call fails for any reason
(network, invalid key, quota), the rule-based engine answers instead — it
always works fully offline.

## Google Drive backup (not yet implemented)

Local encrypted backups (manual, from the Insights screen, or automatic,
once a day via `BackupWorker`) work out of the box. Uploading those same
encrypted backup files to a user's own Google Drive app-data folder is
stubbed out in `BackupManager.uploadToDriveAppData()` — wiring it up requires
adding the `drive.appdata` scope to the OAuth client described above and
implementing the Drive REST API upload call. No financial data would ever be
decrypted as part of that upload; only the already-encrypted backup file
would be sent.

## Project status

All planned features are implemented: onboarding (privacy intro, PIN/
biometric setup, permission grants, initial SMS scan), Dashboard,
Transactions, Insights, and Chat screens, the on-device rule-based AI
engines, and scheduled + manual encrypted backups. The on-device AI is a
deterministic rule/heuristic engine (financial health scoring, subscription/
hidden-expense detection, and a small rule-based Q&A assistant) rather than
an LLM, keeping everything explainable and fully offline.
