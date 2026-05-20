# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented (on-device) tests
./gradlew connectedAndroidTest

# Run a single test class
./gradlew test --tests "com.kangwei.expensetracker.SomeTestClass"

# Clean build
./gradlew clean assembleDebug
```

Room generates code via KSP at build time — always build before expecting DAO/database code to resolve in the IDE.

## Architecture

**MVVM with Jetpack Compose, no Repository layer.** ViewModels access DAOs directly through the `AppDatabase` singleton exposed on `ExpenseTrackerApp`:

```
UI (Compose Screens) → ViewModel (AndroidViewModel) → DAO → Room DB
```

`ExpenseTrackerApp.database` is a lazy singleton. Every ViewModel casts `app as ExpenseTrackerApp` to get it.

### Navigation

Five bottom-nav tabs defined as `sealed class Tab` in `AppNavigation.kt`. Each tab maps to one screen composable. There is no deep-link or nested navigation — adding a new screen means adding a new `Tab` object and a `composable()` entry in `NavHost`.

### Database Schema

Five Room entities:
- `CategoryEntity` — expense categories (seeded with defaults by `DataSeeder`)
- `ExpenseEntity` — individual expenses/income entries; `isIncome: Boolean` distinguishes type; `receiptData: ByteArray?` stores receipt image inline
- `RecurringExpenseEntity` — recurring templates with `frequency` (`"daily"`, `"weekly"`, `"monthly"`), `nextDate`, and optional `endDate`
- `TagEntity` — user-defined tags
- `ExpenseTagCrossRef` — junction table for the many-to-many Expense↔Tag relationship

`ExpenseWithDetails` (in `data/db/relation/`) is the primary read model — a `@Transaction` relation that embeds `CategoryEntity` and resolves tags via `@Junction`.

### ViewModel Patterns

- All ViewModels extend `AndroidViewModel` (not `ViewModel`) to access `Application`
- Reactive data is exposed as `StateFlow` using `SharingStarted.WhileSubscribed(5000)`
- Multi-source filtering uses `combine()` — see `ExpensesViewModel.filtered` for the canonical pattern
- `AddEditExpenseViewModel` handles both create and edit by checking `editingId != null`; tag updates always delete-then-reinsert via `deleteAllTagsForExpense`

### App Startup

`DashboardViewModel.seed()` is called on Dashboard load and handles two startup tasks:
1. Seeds default categories if none exist (`DataSeeder.seedDefaultCategories`)
2. Materializes any overdue recurring expenses into actual `ExpenseEntity` rows (`DataSeeder.generateRecurring`)

### Shared UI Utilities

`ui/components/SharedComponents.kt` contains utility functions and composables shared across screens: `formatCurrency()`, `formatDate()`, `TagChip`, `SummaryCard`, and `ExpenseListItem`.
