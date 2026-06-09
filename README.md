# Expense Tracker

A personal finance app for Android built with Jetpack Compose and Room. Track expenses and income, set category budgets, attach receipt photos, and manage recurring transactions — all stored locally on your device.

## Features

- **Dashboard** — spending summary with income/expense/net cards, bar and donut charts, and a recent entries list; filter by time period (week, month, year, all time) or by tag
- **Expenses** — full transaction list with search, category filter, and tag filter; swipe to delete; income/expense totals for the current filter
- **Add / Edit** — amount, description, date, income toggle, category, tags, and optional receipt photo; new tags can be created inline
- **Recurring** — define daily, weekly, or monthly templates that automatically materialise into real expense entries on app open
- **Categories** — create categories with optional monthly budget limits; progress bar shows spend vs. budget for the current month
- **Tags** — create and manage tags; delete a tag and all its associations are cleaned up
- **Settings** — choose display currency (24 currencies supported); export all data as a CSV file

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| State | ViewModel + StateFlow |
| Database | Room (KSP) |
| Language | Kotlin |
| Min SDK | 26 (Android 8.0) |

## Architecture

MVVM with no repository layer — ViewModels access DAOs directly through the `AppDatabase` singleton:

```
Compose Screen → AndroidViewModel → DAO → Room DB
```

Five bottom-nav tabs, each backed by its own ViewModel. See [`CLAUDE.md`](CLAUDE.md) for a detailed architecture reference.

## Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device or emulator
./gradlew installDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (requires connected device)
./gradlew connectedAndroidTest
```

Room generates DAO code via KSP at build time — run a build before expecting generated code to resolve in the IDE.

### Release signing

Copy the example properties file and fill in your keystore details:

```bash
cp keystore.properties.example keystore.properties
# edit keystore.properties with your values
./gradlew assembleRelease
```

See [`keystore.properties.example`](keystore.properties.example) for the expected format and the `keytool` command to generate a new keystore.

## Privacy

All data is stored locally on the device. Nothing is transmitted to any server. See the full [Privacy Policy](PRIVACY_POLICY.md).
