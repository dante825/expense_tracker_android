# Expense Tracker Android — Testing Roadmap

- [x] Verify the build compiles — run `./gradlew assembleDebug` and fix any errors
- [ ] Set up device or emulator — install Android Studio, create an AVD or enable USB Debugging on a physical device, verify `adb devices` shows a target
- [ ] Manual smoke test all 5 tabs — Dashboard (seeding), Expenses (list), Add/Edit expense, Recurring (materialization), Tags & Categories (CRUD)
- [ ] Add test dependencies to build files — add JUnit4, Room Testing, Espresso, and Compose UI Test libs to `libs.versions.toml` and `app/build.gradle.kts`
- [ ] Write Room DAO tests (highest priority) — use `Room.inMemoryDatabaseBuilder` to test `ExpenseDao` and `CategoryDao`; cover insert, query, delete, and `ExpenseWithDetails` relation
- [ ] Write DataSeeder unit tests — unit test `DataSeeder.generateRecurring` with fixed `LocalDate` inputs for daily/weekly/monthly frequencies
- [ ] Write ViewModel unit tests — test `AddEditExpenseViewModel` create vs. edit path and tag delete-then-reinsert; test `ExpensesViewModel.filtered` combine() logic
- [ ] Write Compose UI tests — use `composeTestRule.setContent{}` to test Dashboard and add-expense flow in isolation
