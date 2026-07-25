# Autonomous Accounting — Android App

A native Android app that turns plain-language transaction descriptions ("Salary received of 1 lakh", "Gave a loan of 2,002 to Y") into GAAP-style, auto-balanced double-entry bookkeeping — no manual debit/credit selection required.

## How it works

Every input passes through a 4-stage pipeline:

1. **Entity & Intent Parsing** (`domain/IntentParser.kt`) — on-device regex/keyword parsing extracts the amount, counterparty, and transaction intent (salary, loan, credit-card spend/bill, payment received, contingency reserve). Handles Indian numbering shorthand (`lakh`, `crore`, `k`).
2. **Chart of Accounts auto-provisioning** (`data/AccountingDao.kt#getOrCreateAccount`) — sub-ledgers (e.g. `Loans - Y`, `AR - X`) are created on first use, nested under the correct master root account (Assets/Liabilities/Equity/Revenue/Expenses).
3. **Double-entry journal generation** (`domain/AccountingEngine.kt`) — maps each intent to a balanced pair of `LineItemEntity` rows and asserts `Σdebits == Σcredits` before committing, all inside a single Room transaction.
4. **Real-time statement recalculation** (`domain/FinancialReportCalculator.kt`) — Profit & Loss and the Balance Sheet (with an Assets = Liabilities + Equity integrity check) are derived reactively from the ledger via Kotlin `Flow`s; there is no separate persisted "balance" anywhere to fall out of sync.

## Translation rules implemented

| Input | Debit | Credit | P&L impact |
|---|---|---|---|
| "Received 2,000 from X" | Bank | AR - X | None (AR collection) |
| "Gave a loan of 2,002 to Y" | Loans - Y | Bank | None |
| "Salary received of 1 lakh" | Bank | Salary Income | +1,00,000 |
| "Spent 2,000 on credit card for dining" | Dining | Credit Card Payable | -2,000 |
| "Paid credit card bill 15,000" | Credit Card Payable | Bank | None |
| "Set aside 10% for contingency" | Retained Earnings | Contingency Reserve | None |

## Security

- **Encrypted at rest**: the Room database is opened through `net.zetetic:android-database-sqlcipher`'s `SupportFactory`, keyed by a random passphrase generated once and stored in `EncryptedSharedPreferences` (itself backed by an AES-256 Android Keystore key) — see `data/DatabasePassphraseProvider.kt`.
- **`FLAG_SECURE`** blocks screenshots, screen recording, and recents-preview of financial data (`MainActivity`).
- **Biometric/device-credential gate** via `BiometricPrompt` before the ledger is shown (`security/BiometricGate.kt`).

## Automation

`worker/ContingencyReserveWorker.kt` runs monthly via WorkManager, automatically posting the same "set aside X% for contingency" journal entry a user could trigger manually, using the percentage stored in DataStore (`data/UserPreferences.kt`, default 10%).

## Exports

`export/ReportExporter.kt` writes the full ledger to CSV and the P&L + Balance Sheet to a PDF (via `android.graphics.pdf.PdfDocument`, no extra dependency), into the app's external-files `reports/` directory.

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- minSdk 26 (Android 8.0+ — required for the Keystore-backed `EncryptedSharedPreferences` APIs used to protect the SQLCipher passphrase)
- Kotlin 1.9.x

## Building

```bash
./gradlew assembleDebug        # Debug APK
./gradlew test                 # JVM unit tests (IntentParser, FinancialReportCalculator)
./gradlew connectedAndroidTest # Instrumented tests (AccountingEngine round-trip, needs a device/emulator)
```

## Project Structure

```
AutonomousAccounting/
├── app/src/main/java/com/accounting/engine/
│   ├── data/            # Room entities, DAO, encrypted AppDatabase, DataStore preferences
│   ├── domain/           # IntentParser, AccountingEngine, FinancialReportCalculator
│   ├── repository/       # AccountingRepository — single read/write facade for the UI + worker
│   ├── worker/            # ContingencyReserveWorker (WorkManager)
│   ├── security/          # BiometricGate
│   ├── export/            # CSV/PDF ReportExporter
│   ├── ui/                # Compose screens (Quick Input, P&L, Balance Sheet), nav, theme
│   ├── AccountingApplication.kt
│   └── MainActivity.kt
└── app/src/test / androidTest/  # Unit + instrumented tests
```
