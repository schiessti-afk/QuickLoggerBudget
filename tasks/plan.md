# Implementation Plan: Sprint 2 — Two-second log

## Overview

Make the primary path work end to end with seeded categories and no receipt: open → type amount → tap category → Save → form resets and the next log can be typed immediately. This fills the empty domain/data package holders left by sprint 1 with `Money`/`Expense`/`Category`, repository interfaces, the three use cases needed to log and observe, Room `quicklogger.db` v1 with a seed callback, and a SharedPreferences `LastCategoryStore`. The Log screen gains a digit-buffer amount field and radio category chips.

## Architecture Decisions

- **Amount is a digit buffer, not free text.** `LogUiState` holds `amountDigits` (digits only, capped at 12) and a derived `amountFormatted`. `LogEvent.AmountChanged(raw)` strips every non-digit from whatever the IME produced, so paste and locale separators cannot corrupt the buffer. The field renders `TextFieldValue` with the caret pinned to the end because the formatted text is rewritten on every keystroke.
- **Formatting lives in domain, not Compose.** `MoneyFormatter` wraps `NumberFormat.getCurrencyInstance(locale)` and `Currency.getInstance(code)`. It takes `Locale` and `currencyCode` as parameters, so JVM tests never depend on the machine's default locale (ARCHITECTURE §6.1). History (sprint 4) and CSV (sprint 5) reuse it. `java.text` / `java.util` are not `android.*`, so domain stays clean.
- **Fraction digits come from the currency, not a hard-coded 2.** `Currency.defaultFractionDigits` drives digits→minor conversion, so a JPY device gets `¥4500` from "4500" rather than `¥45`.
- **The ViewModel reads the locale, the use case does not.** `SaveExpense` receives a `currencyCode`; `LogViewModel` resolves it with `Currency.getInstance(Locale.getDefault()).currencyCode` at save time (ARCHITECTURE §6.1). An unsupported locale falls back to `USD` rather than crashing.
- **Category existence is validated in `SaveExpense`**, against `CategoryRepository`, not in Compose. Domain returns `Result.failure(InvalidAmount | UnknownCategory)`.
- **Seeding runs in a Room `onCreate` callback** using raw `execSQL` on the supplied `SupportSQLiteDatabase`. Calling back into the DAO from `onCreate` would re-enter a database that is still being created.
- **`LastCategoryStore` is written on select, not on save** (ARCHITECTURE §6.3), so a user who selects a chip and closes the app still gets that chip on cold start.
- **Schema export uses the KSP argument** (`room.schemaLocation`) rather than adding the `androidx.room` Gradle plugin — no new plugin, and `app/schemas/` already exists with a `.gitkeep`.
- **New test dependencies:** `kotlinx-coroutines-test` (JVM, needed for `Dispatchers.setMain`), plus `androidx.test.ext:junit`, `androidx.test:runner`, `androidx.room:room-testing`, and `compose-ui-test-junit4` for the `androidTest` source set ARCHITECTURE §12 already specifies. Turbine is skipped: `LogUiState` is a single `MutableStateFlow` and `.value` assertions are enough. No production dependency is added.

## Task List

### Phase 1: Domain
- [ ] Task 1: `Money`, `Category`, `Expense`, `NewExpense`, `MoneyFormatter` + JVM tests
- [ ] Task 2: Repository/store interfaces and the three use cases + JVM tests with repository fakes

### Checkpoint: Domain
- [ ] `test` green; no `android.*`, Room, Compose, or `Uri` import under `domain/`

### Phase 2: Data
- [ ] Task 3: Room entities, DAOs, database, seed callback, schema export
- [ ] Task 4: Mappers, repository implementations, `LastCategoryStore`, Hilt module
- [ ] Task 5: Room `androidTest` for seed + insert

### Checkpoint: Persistence
- [ ] `assembleDebug` green; schema JSON committed under `app/schemas/`

### Phase 3: Log screen
- [ ] Task 6: `LogUiState` / `LogEvent` / `LogViewModel` — categories, radio selection, digit buffer, save + reset
- [ ] Task 7: Chips + amount field + Save in Compose; Compose smoke test

### Checkpoint: Sprint complete
- [ ] `lint`, `test`, `assembleDebug` green
- [ ] Sprint 2 exit criteria checked (device checks still need a human)

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Formatted text fights the IME caret | High | `TextFieldValue` with selection pinned to the end; digits re-extracted from raw input every change |
| Seed callback races the first `observeAll` collection | Med | Seed in `onCreate` via `execSQL` before any DAO read can return |
| Tests inherit the machine's default locale | Med | `MoneyFormatter` takes `Locale` + `currencyCode`; no test calls `Locale.getDefault()` |
| `Currency.getInstance` throws on an exotic locale | Med | ViewModel catches and falls back to `USD` |
| `androidTest` cannot run here (no device/emulator) | Low | Write them, run `test`/`lint`/`assembleDebug` locally, flag the gap for human review |

## Open Questions

None blocking. Amount digit cap (12) and the `USD` locale fallback are implementation defaults, not spec decisions — correct them if a different bound is wanted.
