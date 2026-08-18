# Implementation Plan: Sprint 7 — Targets and dashboard

## Overview

The user can set a monthly ceiling — one overall, one per category — see what it leaves while typing on Log, and open a dashboard that shows the month at a glance above the existing expense list. Setting nothing changes nothing: no meter, no line, no prompt.

## The one structural decision worth calling out

Budgets were explicitly out of scope in IDEA §6, ARCHITECTURE §2.3, and Sprint.md's "not a sprint" list. That reversal is task 1 of this sprint, landed as a doc-only commit before any Kotlin, so no commit in the history ever contradicts the spec it's implementing. The doc edits also carry the one visual rule this sprint bends: DESIGN's blanket "never color amounts green/red" now has a scoped exception for budget surfaces (§5.4) — the amount itself still never changes color, only meters/bars/the remaining line do.

## Architecture Decisions

- **Room v1 → v2, one migration, approved once.** ARCHITECTURE §15 gates schema changes after v1 behind "ask first"; this sprint is that one approval (§17 decision 21), and it's recorded as not extending to a v3. `budget_targets` has an `ON DELETE CASCADE` FK to `categories` — deliberately the opposite of `expenses`' `RESTRICT` — because a target isn't data worth preserving once its category is gone.
- **The overall target's uniqueness isn't the unique index's job.** SQLite indexes treat `NULL` as distinct from every other `NULL`, so a `UNIQUE(categoryId)` index can't stop two overall (`categoryId IS NULL`) rows on its own. `RoomBudgetTargetRepository.upsertOverall/upsertForCategory` do get-then-insert-or-update inside `withTransaction`, mirroring `RoomCategoryRepository`'s existing pattern for multi-step writes — the DAO stays plain CRUD.
- **Currency exclusion lives in exactly one place.** `BudgetProgress.of`/`remainingIncludingPending` are the only code that decides whether an expense counts toward a target; every caller (Log's live line, the dashboard meter, the dashboard bars) goes through them, the same discipline `ExpenseTotals` already enforces for period totals.
- **The remaining line is live against the buffer, not the pre-entry balance.** `LogViewModel` tracks `budgetTargets` / `expensesThisMonth` privately (not in `LogUiState`) and recomputes both halves on every amount keystroke, category change, and target/expense update — never blocking Save, only informing it.
- **No chart library.** The meter is a hand-drawn Compose `Canvas` arc; bars are hand-drawn rounded rects with a tick line. DESIGN §6 rules out a charting dependency for two shapes — it would import its own type scale and animation curves into a file whose whole point is one ink family.
- **Bar fill is the category's own accent, not the status color.** Only the segment past the target tick turns error-red. If the whole bar changed to status color, every bar in a good month would be the same green and the kit would stop scanning as a set (DESIGN §4.2).
- **Ledger green is a `BrandColors` constant, not a `ColorScheme` role.** `budgetStatusColor(isOver)` is the only function that picks between it and the theme's own `error`; it is never mapped to `tertiary` and there is still no dark `ColorScheme`.
- **`history` → `dashboard` is a rename, not a rewrite.** The list, period chips, share text, and CSV export are untouched — same use cases, same tests (renamed, not rewritten in substance) — with the budget overview inserted above them and nothing removed.

## Task List

### Phase 0: Spec change
- [x] Task 1: IDEA / ARCHITECTURE / DESIGN / Sprint.md updated and committed before any Kotlin

### Phase 1: Domain
- [x] Task 2: `BudgetTarget`, `BudgetProgress` (`of`, `remainingIncludingPending`), `BudgetTargetRepository` interface
- [x] Task 3: `ObserveBudgetTargets`, `SetBudgetTarget`, `ClearBudgetTarget`, `BudgetError`

### Checkpoint: Domain
- [x] `BudgetProgressTest` (11), `BudgetTargetUseCasesTest` (11) — all green on the JVM

### Phase 2: Data
- [x] Task 4: `BudgetTargetEntity` (nullable `categoryId`, `CASCADE` FK, unique index), `BudgetTargetDao`, database version 2
- [x] Task 5: `Migration(1, 2)`, `2.json` committed, `RoomBudgetTargetRepository`, DI wiring

### Checkpoint: Data
- [x] `QuickLoggerDatabaseTest` DAO/cascade tests, `DatabaseMigrationTest` against committed `1.json` — both green on-device

### Phase 3: Log
- [x] Task 6: `LogViewModel` tracks targets + this month's expenses; `LogUiState.categoryBudgetLine` / `monthBudgetLine`; `LogScreen` renders one line, error-colored only when over

### Checkpoint: Log
- [x] `LogViewModelTest` remaining-line tests (8) — no line without a target, live against the buffer, over-target wording, foreign-currency exclusion

### Phase 4: Dashboard
- [x] Task 7: `presentation/history/` → `presentation/dashboard/`; `Routes.HISTORY` → `Routes.DASHBOARD`
- [x] Task 8: `BudgetOverview.kt` (`BudgetMeter`, `BudgetBarRow` — Canvas), `BudgetTargetDialog.kt`
- [x] Task 9: `DashboardViewModel` — overview always current month regardless of the period chips; target dialog state machine

### Checkpoint: Sprint complete
- [x] `lint`, `test` (209, +40 over sprint 6), `assembleDebug` green
- [x] `connectedDebugAndroidTest`: non-Compose suites green; Compose suites blocked by a pre-existing emulator/Espresso issue unrelated to this sprint (see Risks)
- [x] Sprint 7 exit criteria checked (human visual review still open, as every visual criterion has been since sprint 6)

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| `room-testing:2.8.4`'s own metadata pins `kotlinx-serialization-core` inconsistently with `room-migration`'s `kotlinx-serialization-json:1.8.1`, throwing `AbstractMethodError` inside `MigrationTestHelper` | High (migration untestable) | `resolutionStrategy.force` on the `androidTest` configurations in `app/build.gradle.kts`, scoped to test dependencies only |
| A category bar with expenses in two currencies and no target picks one "dominant" currency (largest total) and silently excludes the rest | Low | Documented in code and in `tasks/todo.md` follow-ups; matches the target-currency exclusion rule already in place |
| Compose UI instrumented tests (`LogScreenTest`, `DashboardScreenTest`, `ExpenseEditScreenTest`) fail on this emulator image with `NoSuchMethodException: InputManager.getInstance` | Med (can't prove UI wiring on-device this session) | Confirmed pre-existing and unrelated: `ExpenseEditScreenTest`, untouched this sprint, fails identically. JVM `LogViewModelTest`/`DashboardViewModelTest` cover the same logic without Espresso. |
| The overall target's "one row" invariant depends on the repository always going through `withTransaction`, not on the schema alone | Low | `RoomBudgetTargetRepository` is the only writer; `BudgetTargetDao` has no other insert path exposed outside the repository |

## Open Questions

None blocking. Whether the dashboard should eventually gain a settings-style list view of every target (instead of tap-the-meter / tap-a-bar) is future work if the category count grows large enough that scrolling to find a bar becomes the bottleneck — not asked for, not needed at six-to-a-dozen categories.
