# Expense Tracker Android — Testing Roadmap

- [x] Verify the build compiles — run `./gradlew assembleDebug` and fix any errors
- [x] Set up device or emulator — install Android Studio, create an AVD or enable USB Debugging on a physical device, verify `adb devices` shows a target
- [x] Manual smoke test all 5 tabs — Dashboard (seeding), Expenses (list), Add/Edit expense, Recurring (materialization), Tags & Categories (CRUD)
- [x] Add test dependencies to build files — add JUnit4, Room Testing, Espresso, and Compose UI Test libs to `libs.versions.toml` and `app/build.gradle.kts`
- [x] Write Room DAO tests (highest priority) — use `Room.inMemoryDatabaseBuilder` to test `ExpenseDao` and `CategoryDao`; cover insert, query, delete, and `ExpenseWithDetails` relation
- [x] Write DataSeeder unit tests — unit test `DataSeeder.generateRecurring` with fixed `LocalDate` inputs for daily/weekly/monthly frequencies
- [x] Write ViewModel unit tests — test `AddEditExpenseViewModel` create vs. edit path and tag delete-then-reinsert; test `ExpensesViewModel.filtered` combine() logic
- [x] Write Compose UI tests — TagChip/SummaryCard/ExpenseListItem (8 tests), DashboardScreen period selector + summary cards (2 tests), AddEditExpenseSheet heading + form fields (4 tests) via Robolectric; `formatCurrency`/`formatDate` utilities (5 tests) as plain JVM tests
