# Expense Tracker Android — Testing Roadmap

- [x] Verify the build compiles — run `./gradlew assembleDebug` and fix any errors
- [x] Set up device or emulator — install Android Studio, create an AVD or enable USB Debugging on a physical device, verify `adb devices` shows a target
- [x] Manual smoke test all 5 tabs — Dashboard (seeding), Expenses (list), Add/Edit expense, Recurring (materialization), Tags & Categories (CRUD)
- [x] Add test dependencies to build files — add JUnit4, Room Testing, Espresso, and Compose UI Test libs to `libs.versions.toml` and `app/build.gradle.kts`
- [x] Write Room DAO tests (highest priority) — use `Room.inMemoryDatabaseBuilder` to test `ExpenseDao` and `CategoryDao`; cover insert, query, delete, and `ExpenseWithDetails` relation
- [x] Write DataSeeder unit tests — unit test `DataSeeder.generateRecurring` with fixed `LocalDate` inputs for daily/weekly/monthly frequencies
- [x] Write ViewModel unit tests — test `AddEditExpenseViewModel` create vs. edit path and tag delete-then-reinsert; test `ExpensesViewModel.filtered` combine() logic
- [x] Write Compose UI tests — TagChip/SummaryCard/ExpenseListItem (8 tests), DashboardScreen period selector + summary cards (2 tests), AddEditExpenseSheet heading + form fields (4 tests) via Robolectric; `formatCurrency`/`formatDate` utilities (5 tests) as plain JVM tests

# Play Store Pre-Submission Checklist

## Code / Architecture (blockers)
- [x] Fix receipt storage — move `ByteArray` photos out of the Room entity into the app's private files directory (`context.filesDir`); store only the file path in `ExpenseEntity`; this prevents `TransactionTooLargeException` crashes on large photos
- [x] Encapsulate ViewModel state — change `var amount/description/date/…` `MutableStateFlow` fields in `AddEditExpenseViewModel` to private `_field` + public `field: StateFlow` pattern so the UI cannot mutate ViewModel state directly
- [x] Enable R8 minification — set `isMinifyEnabled = true` in the release build type and verify ProGuard rules don't break Room or Compose; reduces APK size ~40–60% and hardens against reverse engineering
- [x] Add currency selection — let users pick their currency (symbol + locale) in a settings screen; `formatCurrency()` currently uses device locale with no override

## UX / Data safety
- [x] Add data export — implement export-to-CSV (or JSON) so users can back up their data; no export = leading cause of 1-star reviews on finance apps
- [x] Verify `android:allowBackup` behaviour — add a `fullBackupContent` XML descriptor to `AndroidManifest.xml` to explicitly control which files are included in Auto Backup (exclude the raw DB if you implement file-based receipt storage)

## Play Store admin (no code required)
- [x] Write and host a Privacy Policy — required because the app declares `READ_MEDIA_IMAGES`; add the URL to the Play Console listing (hosted at https://github.com/kangwei/expense_tracker_android/blob/main/PRIVACY_POLICY.md)
- [ ] Create store listing assets — icon (512×512), feature graphic (1024×500), at least 2 phone screenshots per supported screen size
- [ ] Complete content rating questionnaire in Play Console
- [x] Configure release signing — add a `signingConfigs` block in `app/build.gradle.kts` for the release build type (or use Play App Signing)
