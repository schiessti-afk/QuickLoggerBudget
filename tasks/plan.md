# Implementation Plan: Sprint 5 — Share and CSV

## Overview

One expense (from Log's Save & Share) and a filtered period (from History) can leave the device only through the system share sheet — text, or text plus the receipt JPEG. History can also export the filtered period to a real CSV file and hand it to the same chooser. Nothing here is a second database: CSV is written to `cacheDir/exports/` on demand and exposed only through the existing `FileProvider`.

## The one structural decision worth calling out

`FormatExpenseShareText`, `BuildPeriodSummary`, and `BuildExpensesCsv` are pure formatters with no repository dependency, but ARCHITECTURE §4 lists them alongside `SaveExpense` and `ObserveExpenses` as domain **use cases**, not as `object`s next to `MoneyFormatter`/`ExpenseDateFormatter`. They get `@Inject constructor()` classes in `domain/usecase` for that reason — DI-consistent and trivially fakeable, even though today they have no fields. `ExportExpensesCsv` is the one real use case in this group: it owns the export-date naming (`Clock`, not the period's `PeriodBounds` start) and the write to a new `CsvExportStore` port, mirroring `ReceiptStore`'s shape (relative file name in, relative file name out — never a `File` or `Uri` above the data layer).

## Architecture Decisions

- **Both screens' one-shot share effects merge into the screen's existing `uiEvents` Flow**, not a second stream. Log already had `uiEvents: Flow<ReceiptAttachmentUiEvent>`; it becomes `Flow<LogUiEvent>` (`LaunchCamera` | `Share`), built with `kotlinx.coroutines.flow.merge` over the controller's events (mapped) and a new private `Channel<LogUiEvent.Share>`. History gets a new `uiEvents: Flow<HistoryUiEvent>` (`ShareText` | `ShareCsv`) — it had none before sprint 5.
- **History keeps the raw `List<Expense>` / category-name map behind the currently rendered `UiState`** in two plain `private var`s, assigned at the end of `buildState()` — the same "keep what was loaded" shape `ExpenseEditViewModel.loaded` already uses. Share and CSV build from exactly what's on screen, not a fresh query, and period switches invalidate them for free since `buildState` reruns on every emission.
- **`CsvExportStore` is `cacheDir`, not `filesDir`.** A CSV is regenerated on demand and never referenced by a stored `Expense`, unlike a receipt; `cacheDir` communicates "the OS may reclaim this" honestly and needs its own `<cache-path>` entry in `file_paths.xml` alongside the existing `<files-path>` for receipts.
- **The CSV file name always comes from `Clock`, evaluated inside `ExportExpensesCsv`** — never from `PeriodBounds`' `start`. This is what makes the sprint's "week/month export is still named for today" exit criterion true by construction, mirroring how sprint 4 made the Monday-week rule true by construction (no `Locale` parameter at all).
- **`Intent` construction and `startActivity` stay in `presentation/components` (`ShareIntents.kt`), called from each screen's stateful composable** — same boundary sprint 3 drew for `FileProvider`/`Uri` (`ReceiptFiles.kt`). ViewModels only ever produce a `String` caption and an optional relative path; they never see `Intent`, `Uri`, or `Context`.
- **Save & Share is the existing `save()` with a `Boolean` flag**, not a parallel code path — identical validation, identical Room write, identical form reset. The only difference is one `if` block after a successful result that builds the caption from the *just-saved* `Expense` (so the receipt path is whatever the save actually persisted, not whatever the form happened to hold) and sends it down the new share channel.
- **CSV quoting is RFC 4180 by hand**, not a dependency: three fields (occurred_at, currency, has_receipt) can never need it, category name is the only field that realistically contains a comma or quote, and pulling in a CSV library for one `if` is disproportionate.

## Task List

### Phase 1: Domain
- [x] Task 1: `FormatExpenseShareText`, `BuildPeriodSummary`, `BuildExpensesCsv` + JVM tests
- [x] Task 2: `CsvExportStore` port + `EXPORTS_DIRECTORY` const; `ExportExpensesCsv` use case (export-date naming)

### Checkpoint: Domain
- [x] `test` green; domain still free of `android.*` / Room / Compose / `Uri`

### Phase 2: Data
- [x] Task 3: `CsvFileStore` over `cacheDir/exports/`; bind `CsvExportStore` in `RepositoryModule`
- [x] Task 4: `<cache-path>` entry in `file_paths.xml`; `ManifestPrivacyTest` extended

### Checkpoint: Persistence
- [x] `assembleDebug` green

### Phase 3: Share plumbing (shared by both screens)
- [x] Task 5: `presentation/components/ShareIntents.kt` (text / receipt+text / CSV `ACTION_SEND` builders, chooser launch); `presentation/components/ExportFiles.kt` (`exportFile` / `exportUri`, mirrors `ReceiptFiles.kt`)

### Phase 4: Log — Save & Share
- [x] Task 6: `LogUiEvent` (`LaunchCamera` | `Share`); `LogEvent.SaveAndShare`; `LogViewModel` merges controller events with a new share channel, `save()` takes a `shareAfterSave` flag; `LogScreen` handles `LogUiEvent.Share` and gets a second button + JVM/Compose coverage

### Phase 5: History — period share and CSV export
- [x] Task 7: `HistoryUiEvent` (`ShareText` | `ShareCsv`); `HistoryEvent.SharePeriodText` / `ExportCsv`; `HistoryViewModel` retains the last-rendered expenses/category-names and wires `BuildPeriodSummary` / `ExportExpensesCsv`; `HistoryScreen`'s overflow menu gains the two actions + JVM coverage

### Checkpoint: Sprint complete
- [x] `lint`, `test`, `assembleDebug` green
- [x] Sprint 5 exit criteria checked (device checks still need a human)

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| CSV export date silently drifts to the period start | Med | `ExportExpensesCsv` reads `Clock` itself; a dedicated test exports a week/month view and asserts the file name is still today's date |
| A share event gets stuck in `UiState` and replays on rotation | Med | Both screens use a `Channel`-backed one-shot `Flow`, the same shape sprint 3/4 already validated for `ReceiptAttachmentUiEvent` / `ExpenseEditUiEvent.NavigateBack` |
| `Channel`-backed events collected via a JVM-test-only `backgroundScope.launch` never deliver under `StandardTestDispatcher` | Low | Discovered during this sprint (see note below); tests read the next event with a direct, foreground `flow.filterIsInstance<T>().first()` (wrapped in `withTimeoutOrNull` for the "no event fired" case) instead of a long-lived background collector |
| CSV quoting mishandles a category name with a comma or embedded quote | Med | `BuildExpensesCsvTest` covers both cases explicitly |
| `androidTest` cannot run here (no device/emulator) | Med | Not needed for this sprint — sprint 5 added no new Room schema or DAO surface |

## A test-infrastructure issue found during this sprint (not a production bug)

`backgroundScope.launch { someChannelBackedFlow.collect { ... } }` — the exact pattern sprint 4's `HistoryViewModelTest.keepUiStateAlive` already uses successfully for a `StateFlow` — reliably failed to ever run its coroutine body when the collected flow was `Channel.receiveAsFlow()`-backed (directly, or through `merge()`), in both a from-scratch isolated repro and in this sprint's new tests, regardless of full-suite vs. single-test execution. The cause was not fully root-caused (it is not a scheduler-identity mismatch — `TestScope.testScheduler === dispatcher.scheduler` holds). Rather than land a flaky or silently-broken test suite on an unresolved kotlinx-coroutines-test question, every new share/CSV test reads its event with a direct `suspend` call in the test body (`flow.filterIsInstance<T>().first()`, wrapped in `withTimeoutOrNull(1)` where "no event fired" must be provable) instead of accumulating into a list via a background collector. This is reliable because the event is already sitting in the channel's buffer by the time the test calls for it, so `first()` returns without a real suspension. `keepUiStateAlive`'s existing `StateFlow` usage is untouched, since it already works.

## Open Questions

None blocking. History's share/export actions live in the existing overflow menu (`DropdownMenu`) rather than new top-bar icons, to avoid pulling in `material-icons-extended` for a "download" glyph — say so if a dedicated icon is wanted instead.
