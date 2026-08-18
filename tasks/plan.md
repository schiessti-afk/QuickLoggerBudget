# Implementation Plan: Sprint 4 — History and corrections

## Overview

History becomes real: a newest-first list filtered by day/week/month, per-currency totals, edit (amount/category/receipt/`occurredAt`), delete with confirmation, and full category CRUD (create from Log's `+` chip, rename/delete from a History-reachable dialog). This is the largest sprint so far — it touches every layer and adds two new screens.

## The one structural decision worth calling out

The receipt attach/replace/remove/capture state machine that sprint 3 built into `LogViewModel` is needed **identically** by the new expense-edit screen: same draft-before-launch sequencing, same "success but zero bytes is a failure" check, same "replacing deletes the old file" rule. Duplicating ~150 lines of file-lifecycle logic across two ViewModels is a correctness risk (the two copies *will* drift), not a style preference, so it moves into a shared `ReceiptAttachmentController` — a plain injectable class (not a ViewModel) holding its own `StateFlow`/`Channel`, driven by each owning ViewModel's `viewModelScope`. `LogViewModel` and `ExpenseEditViewModel` each get their own instance (unscoped Hilt binding) and project its state into their own `UiState` shape, so the public screen contracts don't change. This is sprint 2/3 code being touched to *serve* sprint 4, not a drive-by refactor.

## Architecture Decisions

- **`SaveExpenseError` becomes `ExpenseError`.** `UpdateExpense` needs the same two validation rules as `SaveExpense` (`minor > 0`, category exists). One error hierarchy, used by both — mechanical rename across ~5 files, not a scope expansion.
- **Week/month bounds never touch `Locale`.** `PeriodBounds` takes `(period, today: LocalDate, zone: ZoneId)` and finds Monday with `TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)` — the ISO constant, not `WeekFields.of(locale)`. That is what makes the exit criterion ("always Monday, including on Sunday-`firstDayOfWeek` locales") true *by construction* rather than by a runtime check.
- **Bounds are half-open `[start, end)`.** All three periods end at "start of tomorrow" in the device zone; day/week/month only differ in where they start. One shared `end` computation, three `start` branches.
- **Totals are a pure grouping function**, `ExpenseTotals.byCurrency(List<Expense>): List<Money>` — group by `currencyCode`, sum minor units within each group, never across groups. This is the only code path that produces a "total"; nothing upstream (History or, later, share/CSV in sprint 5) is allowed to add two `Money` values directly.
- **Category name-uniqueness stays a Room constraint, not a Kotlin scan.** The unique index from sprint 2 already enforces case-insensitive uniqueness. `RoomCategoryRepository` is the *only* place that imports `SQLiteConstraintException` and turns it into `CategoryError.DuplicateName` — a domain-safe type — before it ever reaches a use case. Domain never sees the Android exception type.
- **Delete-category is one Room transaction, not two writes from Kotlin.** `ExpenseDao.reassignCategory(fromId, toId, updatedAt)` (a single `UPDATE`) then `CategoryDao.deleteUnprotected(id)`, wrapped in `db.withTransaction { }` inside `RoomCategoryRepository`. The domain use case only decides *what* (delete this id, reassign to the protected row); the data layer decides *how* atomically. `deleteUnprotected`'s `WHERE isProtected = 0` is a second line of defense against ever deleting `Other`, on top of the use case's own check.
- **New custom categories append (`sortOrder = max + 1`).** Nothing in the sprint asks for `Other` to stay visually last forever; keeping it there would mean quietly renumbering existing rows on every create. Simplest correct behavior: append.
- **Creating a category from the `+` dialog auto-selects it.** Not explicitly required, but a category you just created that isn't usable on the next tap fails the spirit of "adds a chip" — and it's the existing `CategorySelected` path, not new logic.
- **Dialog visibility is local Compose state; the destructive/mutating action is a ViewModel event.** Applies to the categories-management dialog and the delete-expense confirmation: opening/closing has no business logic to test, so it doesn't need `UiState` plumbing. The actual `Delete`, `Create`, `Rename` events do.
- **`ExpenseEditViewModel` receives the id via `SavedStateHandle`**, the officially recommended Navigation Compose + Hilt pattern (already cited in ARCHITECTURE's reference list) — not in the "no `Context`/`Uri`/`AndroidViewModel`" prohibition.
- **Date/time editing uses Material 3's `DatePicker`/`TimePicker`**, already on the Compose BOM — no new dependency. Tap → date dialog → time dialog → combine into one `Instant` in the device zone.
- **Display formatting stays out of Composables.** `HistoryViewModel` and `ExpenseEditViewModel` format amount and date/time strings using injected `Clock` + `Provider<ZoneId>` + `Provider<Locale>`, mirroring `LogViewModel`'s existing `Provider<Locale>` pattern, so screens stay pure functions of `UiState` and formatting stays JVM-testable.

## Task List

### Phase 1: Domain
- [ ] Task 1: `DateRange`, `Period`, `PeriodBounds`, `ExpenseTotals`, `ExpenseDateFormatter` + JVM tests
- [ ] Task 2: Rename `SaveExpenseError` → `ExpenseError`; add `UpdateExpense`, `DeleteExpense`, `ObserveExpensesInRange` + JVM tests
- [ ] Task 3: `CategoryError`; extend `CategoryRepository`; add `CreateCategory`, `RenameCategory`, `DeleteCategory` + JVM tests (against an extended fake)

### Checkpoint: Domain
- [ ] `test` green; domain still free of `android.*` / Room / Compose / `Uri`

### Phase 2: Data
- [ ] Task 4: Extend `ExpenseDao`/`CategoryDao`; `RoomExpenseRepository`/`RoomCategoryRepository` implement the new surface; category delete/create wrapped in `db.withTransaction`
- [ ] Task 5: Room instrumentation tests for the new queries (range boundaries, reassignment, constraint mapping)

### Checkpoint: Persistence
- [ ] `assembleDebug` green

### Phase 3: Shared receipt controller
- [ ] Task 6: Extract `ReceiptAttachmentController`; `LogViewModel` delegates to it; `LogViewModelReceiptTest` slims to a wiring check, exhaustive cases move to a new controller test

### Phase 4: Log — category creation
- [ ] Task 7: `LogEvent.CreateCategory`, `+` chip, dialog, auto-select on success + JVM/Compose tests

### Phase 5: History
- [ ] Task 8: `HistoryViewModel`/`UiState`/`Event`, period switching, totals, `HistoryScreen` + JVM/Compose tests

### Phase 6: Expense edit
- [ ] Task 9: `ExpenseEditViewModel`/`UiState`/`Event` (amount/category/receipt/`occurredAt`, save, delete), route with nav arg, `ExpenseEditScreen` + JVM/Compose tests

### Phase 7: Category management
- [ ] Task 10: `CategoriesViewModel`, rename/delete dialog reachable from History overflow + JVM/Compose tests

### Checkpoint: Sprint complete
- [ ] `lint`, `test`, `assembleDebug` green
- [ ] Sprint 4 exit criteria checked (device checks still need a human)

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Week bounds silently depend on locale after all | High | `PeriodBounds` takes no `Locale` parameter at all — nothing to accidentally read |
| Reassign-then-delete not atomic, crash leaves orphaned rows | High | Both statements run inside one `db.withTransaction` block |
| Extracting the receipt controller regresses sprint 3 behavior | High | Controller code is moved, not rewritten; existing `LogViewModelReceiptTest` cases are ported to the controller test unchanged, then a small wiring test replaces them in `LogViewModelTest` |
| FK `RESTRICT` on `categoryId` blocks the delete before reassignment lands | Med | Reassignment `UPDATE` runs first in the same transaction, so no row still points at the old id when `DELETE` runs |
| `androidTest` cannot run here (no device/emulator) | Med | Written and compiled; flagged unverified, same as sprints 2–3 |

## Open Questions

None blocking. Auto-selecting a freshly created category and appending new categories after `Other` are implementation defaults the sprint left open — say so if you want different behavior.
