# Sprint 4 tasks

- [x] Task 1: `DateRange`, `Period`, `PeriodBounds`, `ExpenseTotals`, `ExpenseDateFormatter`
  - Acceptance: week always starts ISO Monday regardless of locale; totals never combine currency codes
  - Verify: `PeriodBoundsTest` (7), `ExpenseTotalsTest` (4), `ExpenseDateFormatterTest` (2) — all green
  - Files: `domain/model/{DateRange,PeriodBounds,ExpenseTotals,ExpenseDateFormatter}.kt`

- [x] Task 2: `ExpenseError` rename; `UpdateExpense`, `DeleteExpense`, `ObserveExpensesInRange`
  - Acceptance: update preserves id/createdAt, stamps updatedAt, same validation as save; delete removes row then best-effort deletes its receipt
  - Verify: `ExpenseCorrectionUseCasesTest` — 8 tests green
  - Files: `domain/usecase/{ExpenseError,UpdateExpense,DeleteExpense,ObserveExpensesInRange}.kt`, `SaveExpense.kt`

- [x] Task 3: `CategoryError`; extended `CategoryRepository`; `CreateCategory`/`RenameCategory`/`DeleteCategory`
  - Acceptance: name trimmed, length-capped at 40, case-insensitive duplicate rejected; delete refuses the protected row and reassigns everything else
  - Verify: `CategoryUseCasesTest` — 15 tests green
  - Files: `domain/usecase/{CategoryError,CategoryUseCases}.kt`, `domain/repository/CategoryRepository.kt`

- [x] Task 4: Extended DAOs; `RoomExpenseRepository`/`RoomCategoryRepository`; transactional delete
  - Acceptance: reassign + delete run in one `db.withTransaction`; Room's unique-constraint exception never crosses into domain as itself
  - Verify: `assembleDebug` green; Hilt graph resolves
  - Files: `data/local/Daos.kt`, `data/repository/Room*Repository.kt`

- [~] Task 5: Room instrumentation tests for the new queries
  - Acceptance: range excludes the upper bound; reassignment is atomic with the FK; constraint mapping proven through the real repository
  - Status: **written and compiling, never executed** — no device or emulator in this session
  - Verify: `.\gradlew.bat :app:connectedDebugAndroidTest`
  - Files: `QuickLoggerDatabaseTest.kt` (+9 tests), `RoomCategoryRepositoryTest.kt` (4 tests, new file)

- [x] Task 6: Extract `ReceiptAttachmentController`; `LogViewModel` delegates to it
  - Acceptance: identical behavior to sprint 3, now shared by two screens; `canSave` still reflects an in-flight copy synchronously
  - Verify: `ReceiptAttachmentControllerTest` (17), slimmed `LogViewModelReceiptTest` (8) — all green
  - Files: `presentation/receipt/ReceiptAttachmentController.kt`, `presentation/log/LogViewModel.kt`
  - Note: found and fixed a real regression mid-task — see below

- [x] Task 7: Log `+` chip — `CreateCategoryDialog`, auto-select on success
  - Acceptance: dialog is not a nav route; a successful create is usable immediately; a duplicate/blank/too-long name surfaces an error without touching state otherwise
  - Verify: 3 new `LogViewModelReceiptTest` cases + 3 new `LogScreenTest` smoke cases (unexecuted, see Task 5 note)
  - Files: `presentation/categories/CreateCategoryDialog.kt`, `presentation/components/CategoryChips.kt`, `presentation/log/{LogEvent,LogUiState,LogViewModel,LogScreen}.kt`

- [x] Task 8: `HistoryViewModel`/`UiState`/`Event`; `HistoryScreen`; `PeriodChips`
  - Acceptance: default day, switch to week/month changes bounds; totals one line per currency; reacts to a live insert without restarting
  - Verify: `HistoryViewModelTest` — 8 tests green
  - Files: `presentation/history/*.kt`, `presentation/components/PeriodChips.kt`

- [~] Task 8b: `HistoryScreen` Compose smoke tests
  - Status: written and compiling, unexecuted (Task 5 note)
  - Files: `HistoryScreenTest.kt` — 5 tests

- [x] Task 9: `ExpenseEditViewModel`/`UiState`/`Event`; nav route with `SavedStateHandle`; `ExpenseEditScreen` with Material3 date/time pickers
  - Acceptance: loads by id; not-found is handled; save persists amount/category/receipt/`occurredAt` and preserves id/createdAt/currency; delete removes the row and its file; save/delete both navigate back
  - Verify: `ExpenseEditViewModelTest` — 12 tests green
  - Files: `presentation/expenseedit/*.kt`, `presentation/navigation/{Routes,QuickLoggerNavHost}.kt`

- [~] Task 9b: `ExpenseEditScreen` Compose smoke tests
  - Status: written and compiling, unexecuted (Task 5 note)
  - Files: `ExpenseEditScreenTest.kt` — 6 tests

- [x] Task 10: `CategoriesViewModel`; `ManageCategoriesDialog` from History's overflow
  - Acceptance: rename/delete reach the list; a duplicate rename and a protected-row delete both surface an error and change nothing
  - Verify: `CategoriesViewModelTest` — 7 tests green
  - Files: `presentation/categories/{CategoriesUiState,CategoriesViewModel,ManageCategoriesDialog}.kt`

## Checkpoint: Sprint 4 done
- [x] `.\gradlew.bat lint`
- [x] `.\gradlew.bat test` — 139 tests, 0 failures
- [x] `.\gradlew.bat assembleDebug`
- [x] Domain has no `android.*` / Room / Compose / `Uri` imports
- [x] All four ViewModels hold no `Context`, `Uri`, or `AndroidViewModel`
- [x] Merged manifest still has no `INTERNET`
- [ ] `.\gradlew.bat connectedDebugAndroidTest` (needs a device/emulator)
- [ ] Human review before the sprint is treated as closed

## A bug found and fixed during this sprint (not in the original code)
Extracting `ReceiptAttachmentController` initially broke `saveIsBlockedWhileACopyIsStillRunning`. Root cause: the synchronous "mark isAttaching=true" flip that used to sit directly in `LogViewModel.importPickedReceipt` (outside any `launch`) got trapped inside the new controller's single suspend `pick()` function, which is itself only entered once `viewModelScope.launch { … }` is scheduled — under `StandardTestDispatcher` that never happens without a pump, so the synchronous observability was lost. Fixed by splitting `pick()` into a non-suspend `beginPick()` (called directly, synchronously) and a suspend `finishPick()` (launched), mirroring the shape `save()` already used. A second, related issue: mirroring `receiptAttachment.state` into `LogViewModel`'s own `_uiState` via a `viewModelScope.launch { collect {} }` also needed a dispatcher pump to propagate — moved that one collector to `Dispatchers.Unconfined` so it never waits on the test (or real) dispatcher's schedule, since it does no async work of its own. Both fixes are in production code, not test-only workarounds; the reasoning is documented as comments at each site.

## Follow-ups noticed, not actioned
- `createComposeRule()` is still the deprecated v1 API — unchanged from sprints 2–3 for the same reason (an unrun test shouldn't be migrated to a dispatcher with different timing before it's ever been green once).
- An abandoned receipt attachment during an edit that is cancelled via Delete (rather than Save) is not cleaned up — same class of orphan-file gap flagged in sprint 3, now also reachable from the edit screen. Still no sweep; still not asked for.
- `ManageCategoriesDialog`'s rename affordance commits on a separate "Rename" tap, not on IME done / focus loss. Minor UX polish, not a correctness gap; left for a design pass.
