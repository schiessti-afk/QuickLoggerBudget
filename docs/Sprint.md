# Sprint.md — QuickLogger

This file is the **implementation sequence** source of truth (what lands, in what order, and when a slice is done). Product intent lives in [`docs/IDEA.md`](IDEA.md). Mechanics live in [`docs/ARCHITECTURE.md`](ARCHITECTURE.md). Visual language lives in [`docs/DESIGN.md`](DESIGN.md).

If this file disagrees with IDEA on product intent, IDEA wins. If it disagrees with ARCHITECTURE on layers, data, or platform rules, ARCHITECTURE wins. If it disagrees with DESIGN on look, DESIGN wins. This file only sequences work; it does not invent features.

Sprints are ordered by dependency. They have **no calendar, duration, or estimate**. A sprint starts when the previous sprint’s exit criteria are all checked. A sprint is done when its exit criteria and the standing quality bar below are all true.

---

## How to use this document

- Exit criteria are binary. If you cannot check the box, the sprint is not done.
- Each sprint leaves a debug-installable app. Do not merge a slice that only compiles.
- Work inside a sprint is still broken into small tasks at implementation time (`tasks/` once a sprint is in flight). This file does not replace that task list.
- Out-of-scope items in IDEA / ARCHITECTURE stay out. Do not pull roadmap work forward to “fill” a sprint.

---

## Standing bar (every sprint)

Apply this on top of that sprint’s exit criteria. A sprint that meets its feature list but fails this bar is not done.

- [ ] `.\gradlew.bat lint`, `.\gradlew.bat test`, and `.\gradlew.bat assembleDebug` succeed (Unix: `./gradlew` with the same tasks).
- [ ] New behavior has tests that fail without the change and pass with it. Domain and ViewModel tests stay on the JVM with fakes; Room/Compose checks stay in `androidTest`.
- [ ] Domain has no `android.*`, Room, Compose, or `Uri` imports.
- [ ] Money is integer minor units. Receipts exist only under `filesDir/receipts/`. No `INTERNET` permission, no gallery/`MediaStore` writes, no analytics.
- [ ] UI state is `StateFlow`; the UI sends events up and does not write state.
- [ ] ARCHITECTURE / DESIGN / this file are updated if a decision changed.
- [ ] Human review before the sprint is treated as closed.

---

## Sprint map

```
1 Runnable shell          ✓ done
      │
      ▼
2 Two-second log          ← first vertical slice (open → amount → category → Save)
      │
      ├──► 3 Private receipts
      │
      └──► 4 History and corrections
                │
                ▼
           5 Share and CSV
                │
                ▼
           6 Stationery identity
                │
                ▼
           7 Signed release     ← MVP
```

Sprints 3 and 4 both depend only on sprint 2. They may run in either order, but not in parallel on the same Room schema without a single owner for migrations. Sprint 5 depends on 3 (receipt on the share intent) and 4 (period list). Sprints 6 and 7 are sequential after 5.

---

## Sprint 1 — Runnable shell

**Status:** Done.

**Outcome:** A signed-off Gradle app launches to a themed Log screen. CI proves lint, JVM tests, and a debug APK on every push to `main` and every PR.

**In**

- Single `:app` module, JDK 17, `minSdk` 26, `compileSdk` / `targetSdk` 36, `applicationId` `com.quicklogger.app`.
- Package layout from ARCHITECTURE §4. Hilt, Navigation Compose, Room + KSP, Coil on the classpath.
- `MainActivity` + `QuickLoggerApp`. Start destination is Log. History is a top-bar action that pushes an empty History route.
- Material 3 **light-only** scheme from DESIGN §5.1 (cream paper, sealing-wax primary). `dynamicColor` off. No dark `ColorScheme`.
- Manifest: no `INTERNET`, no `CAMERA`, no media permissions. `allowBackup="false"` plus deny-all backup/extraction rules.
- GitHub Actions CI: `lint`, `test`, `assembleDebug`.
- Placeholder JVM test so `test` is a real gate, not an empty task.

**Out**

- Room entities, use cases, amount formatting, chips, save, receipts, share, CSV, generated brand assets, release signing.

**Exit criteria**

- [x] Opening the debug APK lands on Log. The amount field is focused and the IME is shown.
- [x] History is reachable from the Log top-bar list action and pops back to Log.
- [x] Merged manifest has no `INTERNET` permission.
- [x] `android:allowBackup` is `false`.
- [x] CI on `main` and PRs runs lint, JVM tests, and `assembleDebug`, and is green.
- [x] Domain / data / presentation packages exist even if some are still empty holders.

---

## Sprint 2 — Two-second log

**Status:** Implemented. `lint`, `test`, and `assembleDebug` are green (49 JVM tests). The criteria below that need a device or emulator — and the human review in the standing bar — are still open.

**Outcome:** The primary path works end to end with seeded categories and no receipt: open → type amount → tap category → Save. The form resets for the next log.

**In**

- Domain: `Money`, `Expense`, `Category`, repository interfaces, `LastCategoryStore` interface.
- Use cases needed to log and observe: `SaveExpense`, `ObserveExpenses`, `ObserveCategories` (and validation: amount `minor > 0`, category must exist).
- Room `quicklogger.db` v1, entities/DAOs from ARCHITECTURE §7.1, `exportSchema = true`, schema JSON committed. Seed the six defaults if the category table is empty. `Other` is `isProtected`.
- `LastCategoryStore` in SharedPreferences. Chip selection is radio. Persist on select. Cold-start fallback: last id if it still exists → lowest `sortOrder` → `Other`.
- Log UI: digit-buffer amount with `NumberFormat.getCurrencyInstance(Locale.getDefault())`. Currency code at save is `Currency.getInstance(Locale.getDefault()).currencyCode`, stored on the expense, never rewritten.
- Save persists, then clears amount and receipt (none yet) and **keeps** the selected category.
- Empty amount: Save disabled / inline validation; no Room write.
- ViewModel tests with use-case fakes. Use-case tests with repository fakes. Room `androidTest` for seed + insert. Compose smoke: amount focus; Save disabled when amount empty.

**Out**

- Camera, gallery, FileProvider, History list contents, edit/delete, custom categories, share, CSV.

**Exit criteria**

- [x] Cold start shows a selected category without a tap (last-selected or fallback). *(ViewModel logic covered by JVM tests; the seeded-database half needs a device.)*
- [x] Typing digits formats a currency amount; Save with a positive amount writes one Room row whose `currencyCode` matches the device locale at save. *(Formatting and currency selection covered on the JVM; the Room write needs a device.)*
- [x] Save with empty / zero amount writes nothing and stays on Log.
- [x] After Save, amount is cleared, category is unchanged, and the next expense can be typed immediately.
- [ ] Uninstalling the app removes the database (private storage). *(Needs a device.)*
- [x] Domain tests run on the JVM with no Android runtime.
- [x] Money is never stored or totaled as `Double` / `Float`.

---

## Sprint 3 — Private receipts

**Status:** Implemented. `lint`, `test`, and `assembleDebug` are green (78 JVM tests). The criteria below that need a device or emulator — and the human review in the standing bar — are still open.

**Outcome:** A log can optionally attach a photo from the system camera or the photo picker. The file never enters the gallery. Cancel does not leave junk files.

**In**

- `ReceiptFileStore` under `filesDir/receipts/{uuid}.jpg`.
- `FileProvider` (`exported=false`, `grantUriPermissions=true`) with `files-path` → `receipts/`.
- Camera: `ActivityResultContracts.TakePicture()`. Create the file first, expose it, delete on cancel/failure.
- Gallery: `PickVisualMedia(ImageOnly)`, copy bytes into private storage, do not persist the picker Uri. Reject or downscale copies above ~10 MB.
- Log: camera and gallery actions, thumbnail via Coil from the private file, remove-receipt.
- Replace receipt deletes the previous file. Delete-expense (when it exists) deletes the file best-effort after a successful row delete.
- Activity Result launchers live in the UI, not in the ViewModel or data layer.

**Out**

- Sharing the image, CameraX preview, `CAMERA` / media permissions, `MediaStore` inserts.

**Exit criteria**

- [x] A successful camera capture stores a JPEG under `filesDir/receipts/` and shows a thumbnail on Log. Saving persists `receiptRelativePath`. *(Path persistence and the state machine are covered on the JVM; the real capture needs a device.)*
- [x] A gallery pick copies into the same directory. The original picker Uri is not stored.
- [x] Cancelled camera / failed capture deletes the empty file. Remove-receipt deletes the in-progress file.
- [x] The device gallery / `MediaStore` does not gain a new image from capture or pick. *(No `MediaStore` reference exists in `src/main`; both contracts delegate to system apps.)*
- [x] Manifest still has no `CAMERA` and no media/storage permissions.
- [x] ViewModel still holds no `Context`, `Uri`, or `AndroidViewModel`.

---

## Sprint 4 — History and corrections

**Status:** Implemented. `lint`, `test`, and `assembleDebug` are green (139 JVM tests). The criteria below that need a device or emulator — and the human review in the standing bar — are still open.

**Outcome:** The user can review newest-first history, filter day / week / month, edit or delete an expense (including `occurredAt`), and manage categories without leaving the product rules.

**In**

- History list: formatted amount (stored `currencyCode`, not current-locale currency), category name, local date/time, receipt indicator.
- Period chips: day / week / month per ARCHITECTURE §6.4. Week is always Monday 00:00 local through today. Mixed-currency totals are one line per `currencyCode`.
- Empty history: one short `strings.xml` line (illustration may wait for sprint 6).
- `expense_edit/{id}`: amount, category, receipt, `occurredAt`, delete with confirmation.
- Category create from Log `+` chip (dialog, not a new route). Name trimmed, unique case-insensitive, length-capped (~40).
- Category rename/delete from History overflow or a lightweight categories UI. Delete **reassigns** expenses to `Other`, then removes the row. `Other` cannot be deleted.
- Custom categories use the Other accent/mark until sprint 6 assets exist.

**Out**

- Share, CSV, date picker on Log, notes/merchant, bottom navigation.

**Exit criteria**

- [x] History lists newest first and updates after a log without a process kill. *(Proven on the JVM: a new expense inserted while `HistoryViewModel` is subscribed reaches `uiState` without reconstructing it. The on-device recomposition still needs a device.)*
- [x] Day / week / month filters change the list. A week range always starts Monday, including on locales whose `firstDayOfWeek` is Sunday. *(`PeriodBounds` takes no `Locale` at all — nothing to accidentally read — and is JVM-tested for a Monday, a midweek, and a Sunday "today".)*
- [x] Totals never add two currency codes into one number.
- [x] Edit changes amount, category, receipt, and `occurredAt`; delete removes the row and best-effort deletes its receipt file.
- [x] Creating a category from `+` adds a chip. Deleting a category moves its expenses to Other. Other has no delete action.
- [x] Log still has no date picker, note, or merchant field.
- [x] Domain tests cover period bounds, mixed-currency totals, and category reassignment.

---

## Sprint 5 — Share and CSV

**Status:** Implemented. `lint`, `test`, and `assembleDebug` are green (164 JVM tests). The criteria below that need a device or emulator — and the human review in the standing bar — are still open.

**Outcome:** One expense and a period can leave the device only through the system share sheet. CSV is a real spreadsheet file via `FileProvider`, not a second database.

**In**

- Domain: `FormatExpenseShareText`, `BuildPeriodSummary`, `BuildExpensesCsv`.
- Single-expense share text: WhatsApp-friendly plain text with optional `*star*` markup, no HTML/Markdown. Shape from ARCHITECTURE §9.1.
- **Save & Share** on Log: persist, reset form, then a one-shot UI event (not leftover `UiState`) that launches `ACTION_SEND`. With a receipt: `image/jpeg` + `EXTRA_STREAM` (FileProvider) + `EXTRA_TEXT`. Without: `text/plain` only.
- Period share from History: title + one line per expense + total(s).
- CSV: UTF-8, header row, RFC-style quotes, major units with two decimals. Columns from ARCHITECTURE §9.3. Written to `cacheDir/exports/quicklogger-YYYY-MM-DD.csv` (export date). MIME `text/csv`. `cache-path` in `file_paths.xml`.
- System chooser only. Copy may say “WhatsApp”. Do not target `com.whatsapp`.

**Out**

- Hard-coded WhatsApp package, Google Sheets, any network upload.

**Exit criteria**

- [x] Save & Share with no receipt opens the system sheet with the caption only. *(`LogViewModel` builds the caption and fires a one-shot `LogUiEvent.Share(text, receiptRelativePath = null)`, proven on the JVM; the actual system sheet needs a device.)*
- [x] Save & Share with a receipt opens the system sheet with the JPEG and the same caption. The receiving app can read the Uri (temporary grant). *(`buildReceiptShareIntent` sets `image/jpeg` + `EXTRA_STREAM` via the same `FileProvider` authority as receipts, plus `FLAG_GRANT_READ_URI_PERMISSION`; the on-device grant still needs a device.)*
- [x] History can share the current period as text and as CSV named `quicklogger-YYYY-MM-DD.csv` for the export date. *(`HistoryEvent.SharePeriodText` / `ExportCsv` — the CSV file name uses the export-date `Clock`, not the period start, covered on the JVM including week/month exports.)*
- [x] CSV opens in a spreadsheet with a header and one data row per expense in range. *(`BuildExpensesCsv` — header row, one row per expense, RFC-style quoting; opening a real spreadsheet app needs a device.)*
- [x] Merged manifest still has no `INTERNET`. No `<queries>` for WhatsApp is required for the share path. *(`ManifestPrivacyTest`; share always goes through `Intent.createChooser`.)*
- [x] Share payloads are not kept in `UiState` after the sheet closes. *(`LogUiEvent.Share` / `HistoryUiEvent.ShareText` / `ShareCsv` are one-shot `Channel` events, not `UiState` fields.)*
- [x] JVM tests cover share text, period summary lines, CSV quoting, and minor→major amount conversion. *(`FormatExpenseShareTextTest`, `BuildPeriodSummaryTest`, `BuildExpensesCsvTest`.)*

---

## Sprint 6 — Stationery identity

**Status:** Implemented. `lint`, `test`, and `assembleDebug` are green (169 JVM tests). The criteria below that need a device or emulator — and the human review in the standing bar — are still open. This is a visual sprint; "reads as one ink family" is ultimately a human call.

**Outcome:** The running app matches DESIGN: one ink-on-paper family from launcher to empty History. Beauty does not add taps.

**In**

- Generated assets from DESIGN §8: adaptive launcher (receipt fold), 24 dp top-bar glyph, empty-History illustration, six single-color category pictograms.
- Seeded chips: pictogram + accent from DESIGN §5.2. Selected chip keeps an ink label. Custom categories reuse Other.
- Amount uses the largest display/headline role; tabular figures if the platform face supports them.
- Touch targets ≥ 48 dp for Save, Save & Share, camera, gallery, History.
- README / GitHub social still is repo-only (not in the APK).

**Out**

- Dark theme, Material You, downloadable fonts, splash delay, extra illustration on Log, Figma-as-blocker.

**Exit criteria**

- [x] Log still matches ARCHITECTURE §8.1 (focus, radio chips, optional receipt, two save actions). No new tap on the primary path. *(Chips gained a leading pictogram and the receipt actions became icon buttons; no new screen, dialog, or tap was added — `LogScreenTest`'s existing flows still hold, updated only where a label moved from visible text to `contentDescription`.)*
- [ ] Launcher, top-bar glyph, empty state, and six pictograms read as one ink family at device size. *(Hand-drawn `VectorDrawable`s at a consistent ~2 px/24 dp stroke weight, all sourced from the same `assets/` masters — the actual "reads as one family" call needs a device/human, same as every visual criterion in this sprint.)*
- [x] Each seeded chip shows its pictogram and accent; selected state stays readable (ink label, not white on ochre). *(`CategoryChips` now sets `selectedLabelColor` to `onSurface` explicitly — the fill tints, the label never does.)*
- [x] Empty History is one sentence plus the illustration — not a marketing checklist. *(`ic_empty_history` above the existing single-line `history_empty` string; no copy changed.)*
- [x] Dynamic color is off. There is no dark `ColorScheme`. *(Unchanged from sprint 1 — `BrandColorsTest` still covers this.)*
- [x] No generated asset is required to complete a save. *(`categoryStyleFor` always resolves — unknown/custom names fall back to Other rather than failing a lookup; covered by `CategoryStyleTest`.)*

---

## Sprint 7 — Signed release

**Outcome:** A git tag `v*` produces a minified, signed APK on GitHub Releases. Secrets never enter git. `main` stays releasable.

**In**

- `assembleRelease` with R8/minification. Keep rules for Room and Kotlin metadata as required.
- GitHub Actions release workflow on tags `v*`. Signing via Actions secrets only (`storeFile`, passwords, alias). `versionName` matches the tag (`1.0.0` for `v1.0.0`); `versionCode` is monotonic.
- `.gitignore` excludes keystore, `keystore.properties`, and passwords.
- MIT license already in tree; copyright Micha Schiess.

**Out**

- Play Store listing, Play App Signing as a requirement, crash telemetry, anything from the MVP roadmap-out list.

**Exit criteria**

- [ ] Tagging `v1.0.0` (or the agreed first tag) uploads a signed release APK to GitHub Releases.
- [ ] Local or CI `assembleRelease` succeeds with secrets injected; the same build fails closed if secrets are missing (no unsigned “success”).
- [ ] The repository does not contain a keystore, `keystore.properties`, or signing passwords.
- [ ] Release APK still has no `INTERNET` permission. Uninstall still removes expenses and receipts.
- [ ] R8 is on for release; the app cold-starts to the focused amount field.

---

## MVP is done when

All seven sprints’ exit criteria are checked, and ARCHITECTURE §16 holds:

- An implementer can explain the tree from ARCHITECTURE §4 without a second pattern.
- The log path is open → type amount → tap category → optional receipt → save. No extra screen.
- Domain tests run on the JVM with zero Android runtime.
- Uninstall removes all expenses and receipt files.
- Share and CSV work with no `INTERNET` permission.
- A tagged commit produces a signed APK in CI without secrets in git.

---

## Explicitly not a sprint

Do not schedule, stub APIs, or “leave hooks” for:

- Cloud / REST sync, Google Sheets, WorkManager network jobs
- Biometric or PIN lock
- OCR, in-app CameraX
- Multi-currency wallets, accounts, budgets, recurring expenses
- Note / merchant field, date picker on Log, bottom navigation on the primary path

Those need a spec change in IDEA and ARCHITECTURE before they get a sprint.

---

## Assumptions

1. Sprints are capability gates, not a calendar. Capacity and dates stay out of this file.
2. Sprint 1 includes DESIGN color/type so later screens inherit paper/ink; generated art waits for sprint 6.
3. Custom categories wait for sprint 4 so sprint 2 stays the seeded POS loop.
4. Save & Share waits for sprint 5 even though the button is specified on Log; sprint 2 ships Save only.
5. One `:app` module for the whole sequence.

→ Correct these if they are wrong; the sprint boundaries should change before implementation, not after.
