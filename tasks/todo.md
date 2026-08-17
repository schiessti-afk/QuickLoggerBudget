# Sprint 2 tasks

- [x] Task 1: Domain models + `MoneyFormatter`
  - Acceptance: `Money(minor, currencyCode)`; `Expense`; `Category`; `NewExpense`; formatter converts digits→minor by currency fraction digits and formats by explicit locale
  - Verify: `.\gradlew.bat :app:testDebugUnitTest --tests com.quicklogger.app.MoneyFormatterTest` — 7 tests green
  - Files: `domain/model/{Money,Category,Expense,MoneyFormatter}.kt`

- [x] Task 2: Repository interfaces + `SaveExpense` / `ObserveExpenses` / `ObserveCategories`
  - Acceptance: save rejects `minor <= 0` and unknown categories; observers return domain models
  - Verify: `SaveExpenseTest` (8) + `ObserveUseCasesTest` (4) green
  - Files: `domain/repository/*.kt`, `domain/usecase/*.kt`

- [x] Task 3: Room v1 — entities, DAOs, database, seed callback, schema export
  - Acceptance: `quicklogger.db` v1, `exportSchema = true`, six defaults seeded on create, `Other` protected
  - Verify: `assembleDebug` green; `app/schemas/com.quicklogger.app.data.local.QuickLoggerDatabase/1.json` written with `COLLATE NOCASE` on `categories.name`
  - Files: `data/local/{Entities,Daos,QuickLoggerDatabase,Mappers}.kt`, `app/build.gradle.kts`

- [x] Task 4: Mappers, repository impls, `LastCategoryStore`, Hilt module
  - Acceptance: domain never sees `@Entity`; last category id persisted in SharedPreferences off the main thread
  - Verify: `assembleDebug` green; Hilt graph resolves
  - Files: `data/repository/Room*.kt`, `data/preferences/SharedPreferencesLastCategoryStore.kt`, `di/DataModule.kt`

- [~] Task 5: Room instrumentation tests (seed + insert)
  - Acceptance: fresh in-memory DB has the six defaults in order; insert round-trips; newest-first ordering; FK rejects an orphan
  - Status: **written and compiling, never executed** — no device or emulator available in this session
  - Verify: `.\gradlew.bat :app:connectedDebugAndroidTest`
  - Files: `app/src/androidTest/java/com/quicklogger/app/QuickLoggerDatabaseTest.kt`

- [x] Task 6: `LogViewModel` — categories, radio selection, digit buffer, save + reset
  - Acceptance: cold start selects last/fallback without a tap; save clears amount, keeps category; empty/zero amount does not write
  - Verify: `LogViewModelTest` — 18 tests green
  - Files: `presentation/log/{LogUiState,LogEvent,LogViewModel,AmountBuffer}.kt`

- [~] Task 7: Compose — amount field, `FlowRow` radio chips, Save
  - Acceptance: chips render selection; Save disabled while the amount is empty; tapping the selected chip is a no-op
  - Status: screen and components done and building; the 6 smoke tests are **written and compiling, never executed** (no device)
  - Verify: `.\gradlew.bat :app:connectedDebugAndroidTest`
  - Files: `presentation/log/LogScreen.kt`, `presentation/components/{AmountField,CategoryChips}.kt`

## Checkpoint: Sprint 2 done
- [x] `.\gradlew.bat lint`
- [x] `.\gradlew.bat test` — 49 tests, 0 failures
- [x] `.\gradlew.bat assembleDebug`
- [x] Domain has no `android.*` / Room / Compose / `Uri` imports
- [x] Merged manifest still has no `INTERNET`; `allowBackup` still false
- [ ] `.\gradlew.bat connectedDebugAndroidTest` (needs a device/emulator)
- [ ] Cold start shows a selected category without a tap (needs a device)
- [ ] Save writes one row with the device-locale currency code (needs a device)
- [ ] Uninstall removes the database (needs a device)
- [ ] Human review before the sprint is treated as closed

## Follow-ups noticed, not actioned
- `createComposeRule()` is deprecated in favour of `androidx.compose.ui.test.junit4.v2.createComposeRule` (StandardTestDispatcher instead of UnconfinedTestDispatcher). Left on the v1 API deliberately: these tests have never been executed, and migrating an unrunnable test to a dispatcher with different timing would make a first emulator run fail for reasons unrelated to the app. Migrate once `connectedDebugAndroidTest` has gone green at least once.
- `presentation/categories/Categories.kt` and `data/receipt/Receipt.kt` are still empty package holders. They belong to sprints 4 and 3; left in place.
- `ExpenseDao` / `CategoryDao` carry only the reads and the insert sprint 2 needs. ARCHITECTURE §7.1 lists a wider surface (`observeInRange`, `update`, `delete`, `getById`); those land with the sprints that use them.
