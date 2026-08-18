# Sprint 5 tasks

- [x] Task 1: `FormatExpenseShareText`, `BuildPeriodSummary`, `BuildExpensesCsv`
  - Acceptance: single-expense caption matches ARCHITECTURE §9.1's three-line shape exactly; period summary is title + one line per expense + one total per currency; CSV amount is always major units with two decimals, even for zero-fraction-digit currencies
  - Verify: `FormatExpenseShareTextTest` (3), `BuildPeriodSummaryTest` (5), `BuildExpensesCsvTest` (9) — all green
  - Files: `domain/usecase/{FormatExpenseShareText,BuildPeriodSummary,BuildExpensesCsv}.kt`

- [x] Task 2: `CsvExportStore` port; `ExportExpensesCsv` use case
  - Acceptance: exported file is named for **today** (the export date) in every period, never the period's `start`
  - Verify: covered by `HistoryViewModelTest`'s export tests (Task 7)
  - Files: `domain/repository/CsvExportStore.kt`, `domain/usecase/ExportExpensesCsv.kt`

- [x] Task 3: `CsvFileStore` over `cacheDir/exports/`; DI binding
  - Acceptance: writes UTF-8 text to `cacheDir/exports/{fileName}`, overwriting any prior file of the same name
  - Verify: `assembleDebug` green; Hilt graph resolves
  - Files: `data/export/CsvFileStore.kt`, `di/DataModule.kt`

- [x] Task 4: `<cache-path>` for exports; manifest test extended
  - Acceptance: `file_paths.xml` exposes `receipts/` and `exports/` only — no external storage, no whole-`filesDir` entry
  - Verify: `ManifestPrivacyTest.fileProviderExposesOnlyTheReceiptsAndExportsDirectories`
  - Files: `res/xml/file_paths.xml`

- [x] Task 5: `ShareIntents.kt`, `ExportFiles.kt`
  - Acceptance: every `ACTION_SEND` goes through `Intent.createChooser`; no hard-coded WhatsApp package; a receipt share carries `image/jpeg` + `EXTRA_STREAM` + `EXTRA_TEXT` + `FLAG_GRANT_READ_URI_PERMISSION`, a CSV share carries `text/csv` + `EXTRA_STREAM`
  - Verify: code review (these are thin `Intent` builders with no branching logic worth a JVM test; they use Android types end to end)
  - Files: `presentation/components/{ShareIntents,ExportFiles}.kt`

- [x] Task 6: Log — `LogUiEvent`, `LogEvent.SaveAndShare`, `LogViewModel`, `LogScreen`
  - Acceptance: Save & Share persists and resets the form identically to Save, then fires exactly one `Share` event carrying the just-saved expense's caption and receipt path (`null` when there was none); plain Save never fires a `Share` event
  - Verify: `LogViewModelTest` — 4 new tests (`saveAndShareWritesTheExpenseAndFiresAShareEventWithNoReceipt`, `saveAndShareIncludesTheReceiptWhenOneIsAttached`, `plainSaveDoesNotFireAShareEvent`, `saveAndShareResetsTheFormJustLikePlainSave`) — all green
  - Files: `presentation/log/{LogUiEvent,LogEvent,LogViewModel,LogScreen}.kt`

- [x] Task 7: History — `HistoryUiEvent`, `HistoryEvent.{SharePeriodText,ExportCsv}`, `HistoryViewModel`, `HistoryScreen`
  - Acceptance: both actions build from the currently visible (filtered) rows, not a fresh unfiltered query; CSV export is named for today regardless of the selected period
  - Verify: `HistoryViewModelTest` — 3 new tests (`sharePeriodTextFiresAShareEventBuiltFromTheVisibleRows`, `exportCsvWritesTheVisibleRowsAndFiresAShareCsvEventNamedForToday`, `exportCsvUsesTheExportDateNotThePeriodStartForWeekAndMonth`) — all green
  - Files: `presentation/history/{HistoryUiEvent,HistoryEvent,HistoryViewModel,HistoryScreen}.kt`

## Checkpoint: Sprint 5 done
- [x] `.\gradlew.bat lint`
- [x] `.\gradlew.bat test` — 164 tests, 0 failures
- [x] `.\gradlew.bat assembleDebug`
- [x] Domain has no `android.*` / Room / Compose / `Uri` imports
- [x] `LogViewModel` / `HistoryViewModel` still hold no `Context`, `Uri`, or `AndroidViewModel`
- [x] Merged manifest still has no `INTERNET`; no `<queries>` block was added
- [ ] `.\gradlew.bat connectedDebugAndroidTest` (no new Room surface this sprint — nothing new to run here)
- [ ] Human review before the sprint is treated as closed

## A test-infrastructure issue found during this sprint (not a production bug)
See `tasks/plan.md`'s "A test-infrastructure issue found during this sprint" section: `backgroundScope.launch { channelBackedFlow.collect { ... } }` never ran its coroutine body under `StandardTestDispatcher` in this project, so the new share/CSV tests read events with a direct foreground `first()` instead of a background collector. `HistoryViewModelTest.keepUiStateAlive`'s pre-existing `StateFlow` usage was left untouched since it already works.

## Follow-ups noticed, not actioned
- History's share/export actions live inside the existing overflow `DropdownMenu` rather than dedicated top-bar icons, to avoid adding `material-icons-extended` for a single "download" glyph. Revisit in sprint 6 if DESIGN wants dedicated iconography.
- The root cause of the `backgroundScope` + `Channel.receiveAsFlow()` test-collection issue (above) was not fully identified. It did not block this sprint since the direct-`first()` pattern is reliable, but it may be worth a focused investigation before it's hit again in a context where that workaround doesn't fit.
- CSV rows are written in whatever order `expenses` arrives in (History's newest-first order); nothing in ARCHITECTURE asks for chronological CSV rows, so this was not changed.
