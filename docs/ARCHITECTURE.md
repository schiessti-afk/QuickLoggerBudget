# Architecture & Technical Specification — QuickLogger

This document is the technical source of truth for **QuickLogger**. It defines layers, data flow, persistence, platform integrations, and the rules implementers must follow. Product intent lives in [`docs/IDEA.md`](IDEA.md). Visual language lives in [`docs/DESIGN.md`](DESIGN.md). This file answers *how* that product is built.

Official references this design follows:

- [Guide to app architecture](https://developer.android.com/topic/architecture)
- [Architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [UI layer / state holders](https://developer.android.com/topic/architecture/ui-layer/stateholders)
- [Room](https://developer.android.com/training/data-storage/room)
- [FileProvider](https://developer.android.com/reference/androidx/core/content/FileProvider)
- [Activity Result API](https://developer.android.com/training/basics/intents/result)

---

## 1. Architectural Overview

QuickLogger is a single-activity, offline-first Android app. It uses **Clean Architecture**, **MVVM**, and **Unidirectional Data Flow (UDF)**.

Primary goals:

- Strict separation of concerns across presentation, domain, and data.
- Predictable UI state driven only by `StateFlow`. The UI never mutates state directly.
- 100% offline data integrity. No network stack, no cloud, no analytics, no telemetry.
- Logging an expense stays a sub-two-second action. Extra taps on the primary path need an explicit reason.

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                      │
│   Jetpack Compose, Material 3                               │
│   ViewModels expose StateFlow; UI sends events upward       │
│   Log, History, chips, amount field, share sheet            │
└──────────────────────────────┬──────────────────────────────┘
                               │ depends on
┌──────────────────────────────▼──────────────────────────────┐
│                        Domain Layer                         │
│   Pure Kotlin models, use cases, repository interfaces      │
│   No Android, Room, Compose, or Context types               │
└──────────────────────────────┬──────────────────────────────┘
                               │ implemented by
┌──────────────────────────────▼──────────────────────────────┐
│                         Data Layer                          │
│   Room (SQLite), receipt files, FileProvider paths          │
└─────────────────────────────────────────────────────────────┘
```

Dependency direction is one-way: **presentation → domain ← data**. The data layer implements domain interfaces. Nothing in domain imports Android.

The official Android guide treats the domain layer as optional. QuickLogger keeps it anyway: use cases are the unit of product behavior (save, export, summarize), and they must be testable on the JVM without Robolectric.

---

## 2. Goals and Constraints

### 2.1 Product constraints

| Constraint | Rule |
| --- | --- |
| Offline | No `INTERNET` permission. No HTTP client. No WorkManager network jobs. |
| Privacy | No analytics SDK, crash reporter with telemetry, ads, or cloud backup of receipts. |
| Speed | Cold start lands on the amount field with the keyboard up. Category is one tap. |
| Receipts | Images live under `context.filesDir`. They never enter the device gallery or shared media. |
| Sharing | Files leave the app only through `FileProvider` and system `ACTION_SEND` intents. |
| Distribution | Standalone signed APK via GitHub Actions on git tags. Not a store-first product. |

### 2.2 Platform baseline

| Item | Value |
| --- | --- |
| Language | Kotlin |
| JDK | 17 |
| `minSdk` | 26 (Android 8.0) |
| `compileSdk` / `targetSdk` | 36 (Android 16) |
| UI | Jetpack Compose + Material 3 |
| Persistence | Room with KSP |
| Async | Coroutines + Flow |
| DI | Hilt |
| Navigation | Navigation Compose, single `Activity` |
| Images (decode/display) | Coil |
| Module layout | One Gradle module (`:app`), packages by layer |

`minSdk` 26 gives native `java.time` without desugaring and matches the Compose/Material 3 floor without extra compatibility work. The app is sideloaded, so Play’s install-base pressure is weaker than for a store app. Change this only if a real device below API 26 is a requirement.

### 2.3 MVP in vs out

In scope (must be designed here):

- Local CRUD for expenses and categories
- Keyboard-first amount input with currency masking
- Camera capture via `ActivityResultContracts.TakePicture()`
- Gallery import via the system photo picker (`PickVisualMedia`)
- WhatsApp-oriented text share for one expense and for day/week/month summaries
- CSV export via `FileProvider`
- Signed APK on git tag

Out of scope (do not design or stub APIs for these):

- Cloud / REST sync
- Google Sheets / WorkManager
- Biometric or PIN lock
- OCR
- In-app CameraX preview
- Multi-currency wallets, accounts, or budgets
- Recurring expenses

---

## 3. Layer Responsibilities

### 3.1 Presentation

Owns Compose screens, navigation, ViewModels, and Android UI contracts (camera, picker, share sheet).

- Each screen has one ViewModel that exposes a single `uiState: StateFlow<UiState>`.
- The UI sends events through `onEvent(LogEvent)` (thin named wrappers are fine if they only forward). The UI never writes to state.
- ViewModels do **not** extend `AndroidViewModel`, hold `Context`, `Activity`, `Resources`, or `Uri`.
- Activity Result launchers and `Intent` dispatch (including `ACTION_SEND`) live in composables or a small UI-side helper, not in the ViewModel or the data layer.
- Collect UI state with lifecycle awareness (`collectAsStateWithLifecycle`).

### 3.2 Domain

Owns business rules and the vocabulary of the app.

- Models: `Expense`, `Category`, `Money`, `DateRange`, share/CSV payloads.
- Repository and storage *interfaces* only (`ExpenseRepository`, `CategoryRepository`, `LastCategoryStore`).
- Use cases as one-class-one-action types (`SaveExpense`, `ObserveExpenses`, `BuildPeriodSummary`, `FormatExpenseShareText`, `BuildExpensesCsv`).
- Validation (amount must be > 0, category must exist, etc.) lives here, not in Compose.
- Forbidden imports: `android.*`, `androidx.room.*`, `androidx.compose.*`, `android.net.Uri`.

### 3.3 Data

Owns Room, file I/O, and mapping.

- Entities, DAOs, `QuickLoggerDatabase`, TypeConverters if any.
- Repository implementations map entity ↔ domain model. Domain never sees `@Entity`.
- `ReceiptFileStore` writes and deletes files under `filesDir/receipts/`.
- `LastCategoryStore` persists the last selected category id in SharedPreferences.
- CSV bytes are written to `cacheDir` only when the user exports, then handed to `FileProvider`.

---

## 4. Project Structure

Single module. Feature modules would split a small offline app without earning the cost.

```
app/
  src/main/
    java/com/quicklogger/app/
      QuickLoggerApp.kt              // @HiltAndroidApp
      MainActivity.kt                // single activity, setContent
      di/                            // Hilt modules
      presentation/
        navigation/                  // NavHost, routes
        log/                         // Log screen + ViewModel
        history/                     // History + period export
        categories/                  // add/rename/delete (not on the primary path)
        components/                  // amount field, category chips, receipt thumb
        theme/
      domain/
        model/
        repository/                  // interfaces (incl. LastCategoryStore)
        usecase/
      data/
        local/                       // Room database, entities, DAOs, mappers
        receipt/                     // ReceiptFileStore
        preferences/                 // LastCategoryStore (SharedPreferences)
        repository/                  // interface implementations
    res/
      xml/file_paths.xml             // FileProvider paths
      values/
    AndroidManifest.xml
  src/test/java/                     // JVM unit tests (domain, ViewModels, formatters)
  src/androidTest/java/              // Room, Compose UI
  schemas/                           // committed Room schema JSON
```

Gradle:

```
/
  settings.gradle.kts
  build.gradle.kts
  gradle/libs.versions.toml
  app/build.gradle.kts
  .github/workflows/ci.yml
  .github/workflows/release.yml
```

Namespace / `applicationId`: `com.quicklogger.app` until a different id is chosen. FileProvider authority is `${applicationId}.fileprovider`.

---

## 5. Unidirectional Data Flow

```
┌──────────┐  event (user intent)   ┌────────────┐  invoke   ┌──────────┐
│   UI     │ ─────────────────────► │ ViewModel  │ ────────► │ Use case │
│ Compose  │                        │            │           └────┬─────┘
│          │ ◄── StateFlow<UiState> │            │                │
└──────────┘                        └────────────┘                ▼
                                                              Repository
                                                                  │
                                                                  ▼
                                                         Room / filesDir
```

Rules:

1. Room DAOs expose `Flow<T>`. Repositories re-map to domain models and keep the stream cold until collected.
2. ViewModels combine those flows into one immutable `UiState` with `stateIn(WhileSubscribed(5_000))` when the state is a stream from lower layers. Simple form state (amount digits while typing) may be a `MutableStateFlow` exposed as read-only `StateFlow`.
3. Write operations are `suspend` functions on use cases, called from `viewModelScope`.
4. One-off UI effects (show share sheet, show snackbar) are **not** stuffed into `UiState`. Use a `SharedFlow<UiEvent>` or a `Channel` consumed as `UiEvent`s, then clear. Do not keep “share this text” sitting in state after the sheet closes.
5. The UI is a function of `UiState`. No remembered business data that the ViewModel does not own, except purely visual state (scroll, focus, IME).

Example shape (illustrative, not final source):

```kotlin
data class LogUiState(
    val amountInput: String,
    val amountMinor: Long,
    val categories: List<Category>,
    val selectedCategoryId: Long?,
    val receiptPreviewPath: String?,
    val isSaving: Boolean,
    val canSave: Boolean,
)

sealed interface LogEvent {
    data class AmountChanged(val raw: String) : LogEvent
    data class CategorySelected(val id: Long) : LogEvent
    data object Save : LogEvent
    data object SaveAndShare : LogEvent
    data object CaptureReceipt : LogEvent
    data object PickReceipt : LogEvent
    data object RemoveReceipt : LogEvent
}

class LogViewModel @Inject constructor(
    private val observeCategories: ObserveCategories,
    private val saveExpense: SaveExpense,
) : ViewModel() {
    val uiState: StateFlow<LogUiState>
    fun onEvent(event: LogEvent) { /* … */ }
}
```

---

## 6. Domain Model

### 6.1 Money

Never store or compute money as `Double` or `Float`.

```kotlin
data class Money(
    val minor: Long,          // integer minor units (cents for BRL/USD)
    val currencyCode: String, // ISO 4217, e.g. "BRL"
)
```

- Input is a digit buffer. The amount field formats that buffer with `NumberFormat.getCurrencyInstance(Locale.getDefault())` as the user types.
- Persist `minor` as `INTEGER` and `currencyCode` as `TEXT`.
- **Currency follows the device locale at save time.** `currencyCode` is `Currency.getInstance(Locale.getDefault()).currencyCode` (ISO 4217). It is stored on the expense and never rewritten if the user later changes locale.
- Display uses the **stored** `currencyCode`, not the current locale’s currency. A US-logged `$4.00` still shows as USD after the phone is switched to Brazil.
- Period totals **must not** add mixed currencies into one number. Sum each `currencyCode` separately and show one total line per currency. If every row in the range shares one code, a single total is enough.
- The ViewModel (not the use case) reads `Currency.getInstance(Locale.getDefault()).currencyCode` and passes it into `SaveExpense`. Tests inject a code; they must not depend on the JVM default locale.

### 6.2 Expense

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `Long` | Room auto-generate. `0` means not yet persisted. |
| `amount` | `Money` | Must be `minor > 0` to save. |
| `categoryId` | `Long` | FK to category. Required. |
| `occurredAt` | `Instant` | Defaults to `now` at save. No date picker on the primary path. |
| `receiptRelativePath` | `String?` | Path relative to `filesDir/receipts/`. Null if none. |
| `createdAt` / `updatedAt` | `Instant` | System clocks. |

No merchant name, tags, payment method, or note in MVP. Those add fields and taps. Add them only with a spec change.

### 6.3 Category

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `Long` | Auto-generate. |
| `name` | `String` | Trimmed, unique (case-insensitive). |
| `sortOrder` | `Int` | Chip order. |
| `isProtected` | `Boolean` | `Other` cannot be deleted. |

No `isDefault` column. Factory rows are the six names seeded when the table is empty; user-created rows are everything else. `Other` is identified by `isProtected`, not by name checks in business logic (name may still be shown in UI).

Seeded defaults, in order:

1. Food
2. Transport
3. Supplies
4. Utilities
5. Personal
6. Other (`isProtected = true`)

Custom categories can be added from a `+` chip on the log screen (dialog, not a nested navigator). Deleting a category **reassigns** its expenses to `Other`, then removes the row. `Other` itself cannot be deleted.

**Chip selection (radio):** exactly one category is always selected. Persist the last selected id in app preferences (`LastCategoryStore` in data, SharedPreferences — no extra Gradle library). On cold start, select that id if the row still exists; else the lowest `sortOrder` (Food until it is deleted or reordered); else `Other` (always exists). Persist immediately on `CategorySelected`, not only on save. After save, the form keeps that category (section 8.1).

### 6.4 Time ranges for summaries

| Period | Definition |
| --- | --- |
| Day | Local calendar date of “today” |
| Week | Monday 00:00 local through today (inclusive). Always Monday, not the device `firstDayOfWeek`. |
| Month | First day of current month through today (inclusive) |

All timestamps stored as epoch millis UTC. Display and range bounds use the device default `ZoneId`. Weeks are ISO-style Monday start even on US locales.

---

## 7. Data Layer

### 7.1 Room

Database name: `quicklogger.db`. Version starts at `1`. `exportSchema = true`; schema JSON is committed under `app/schemas/`.

```text
CategoryEntity
  id            INTEGER PK AUTOINCREMENT
  name          TEXT NOT NULL COLLATE NOCASE
  sortOrder     INTEGER NOT NULL
  isProtected   INTEGER NOT NULL
  UNIQUE(name)

ExpenseEntity
  id                   INTEGER PK AUTOINCREMENT
  amountMinor          INTEGER NOT NULL
  currencyCode         TEXT NOT NULL
  categoryId           INTEGER NOT NULL  → CategoryEntity.id
  occurredAtEpochMs    INTEGER NOT NULL
  receiptRelativePath  TEXT
  createdAtEpochMs     INTEGER NOT NULL
  updatedAtEpochMs     INTEGER NOT NULL

INDEX expenses(occurredAtEpochMs DESC)
INDEX expenses(categoryId)
```

DAOs:

- `CategoryDao`: `observeAll(): Flow<List<CategoryEntity>>`, insert, update, delete, `getById`.
- `ExpenseDao`: `observeAllNewestFirst(): Flow<List<ExpenseEntity>>`, `observeInRange(from, to)`, insert, update, delete, `getById`.

Queries stay parameterized. No string-concatenated SQL.

On first open, a Room callback seeds the six default categories if the table is empty.

Migrations: every schema change gets an explicit `Migration`. Destructive `fallbackToDestructiveMigration()` is allowed only in debug.

### 7.2 Receipt files

```
filesDir/
  receipts/
    {uuid}.jpg
```

Lifecycle:

1. **Camera.** Create `filesDir/receipts/{uuid}.jpg`, expose it with `FileProvider`, launch `TakePicture(uri)`. On success, keep the file and put the relative name on the in-progress expense. On cancel or failure, delete the empty file.
2. **Gallery.** `ActivityResultContracts.PickVisualMedia(ImageOnly)` returns a content `Uri`. Copy bytes into `filesDir/receipts/{uuid}.jpg`. Do not persist the picker Uri.
3. **Display.** Coil loads `File(filesDir, "receipts/$relative")`. Never insert into `MediaStore`.
4. **Delete expense.** Delete the receipt file in the same transaction/use case as the row. Best-effort file delete after a successful DAO delete; do not fail the whole delete if the file is already gone.
5. **Replace receipt.** Delete the previous file, then write the new one.

Do not declare `CAMERA`, `READ_MEDIA_IMAGES`, or storage permissions. `TakePicture` delegates to the system camera app; `PickVisualMedia` uses the photo picker. Declaring `CAMERA` in the manifest would *force* a runtime permission before the contract runs — leave it out.

### 7.3 FileProvider

Manifest provider: `androidx.core.content.FileProvider`, `exported=false`, `grantUriPermissions=true`.

`res/xml/file_paths.xml`:

- `files-path` → `receipts/` (share a receipt image)
- `cache-path` → `exports/` (CSV)

CSV files are created under `cacheDir/exports/` immediately before the share sheet, named `quicklogger-YYYY-MM-DD.csv` (export date, not the period start). They are not a second database.

---

## 8. Presentation: Screens and Navigation

One `MainActivity`. Start destination is **Log**. There is no bottom navigation on the primary path — a persistent tab bar is extra chrome for a POS-style logger. History is a top-bar action (list icon) on Log.

```
Log  ──push──►  History  ──push──►  ExpenseEdit
                  │
                  └── dialog / sheet: period share / CSV
```

| Route | Role |
| --- | --- |
| `log` | Amount (focused), category chips, optional receipt thumb, Save, Save & Share. |
| `history` | Newest-first list, period chips (day / week / month), share text, export CSV. |
| `expense_edit/{id}` | Edit amount, category, receipt, occurred time; delete. Not on the fast path. |

Category create is a dialog on Log (`+` chip), not its own nav route. The `presentation/categories/` package holds that dialog (and rename/delete UI). Rename/delete can live on History overflow or a lightweight categories screen if the dialog would otherwise overflow.

### 8.1 Log screen behavior

1. On first composition, request focus on the amount field and show the IME.
2. Amount uses a numeric keyboard. Formatting is applied as digits arrive (thousands separators, currency symbol, two fraction digits).
3. Categories render as a `FlowRow` of `FilterChip`s in **radio** mode: exactly one selected; tapping another chip changes the selection; tapping the selected chip does nothing. Defaults visible without scrolling on a typical phone. Cold start uses last-selected id (section 6.3).
4. Receipt is optional: camera and gallery icons. Thumbnail + remove.
5. **Save** persists and resets the form (amount cleared, category kept, receipt cleared) so the next log is immediate.
6. **Save & Share** persists, resets, then emits a share event with the formatted text (and receipt image if present).
7. Invalid save (empty amount) does not navigate and does not write Room. Show inline validation. A missing category is a defensive check only (seed + radio selection should make it unreachable).

### 8.2 History

- List rows: formatted amount, category name, local date/time, receipt indicator.
- Period filter changes the list and the payload for share/CSV.
- Empty state is a short line, not a marketing screen.

---

## 9. Sharing and Export

Sharing is a *presentation* concern. Domain builds payloads; the UI fires `ACTION_SEND`.

### 9.1 Text payload (single expense)

Plain text, WhatsApp-friendly. No HTML and no Markdown. Bold via WhatsApp `*star*` markup is allowed.

Examples below use BRL and a Brazil offset as **one** locale-at-save illustration. Currency still follows the device locale at save time (section 6.1); it is not a product default.

```
*QuickLogger*
R$ 45.00 — Supplies
17 Aug 2026, 14:32
```

If a receipt exists, use `ACTION_SEND` with `image/jpeg`, `EXTRA_STREAM` = FileProvider Uri, `EXTRA_TEXT` = the same caption, `FLAG_GRANT_READ_URI_PERMISSION`. If no receipt, `text/plain` only.

Do **not** hard-target `com.whatsapp`. Use the system share sheet. Copy can say “WhatsApp”; the OS lets the user pick the destination. Targeting a package needs `<queries>` and breaks if WhatsApp Business or a fork is installed.

### 9.2 Period summary text

Title line + one line per expense + total. Same share sheet.

### 9.3 CSV

UTF-8, header row, comma-separated, RFC-style quotes when needed.

```
occurred_at,amount,currency,category,has_receipt
2026-08-17T14:32:00-03:00,45.00,BRL,Supplies,true
```

`amount` in CSV is major units with two decimal places (not minor ints) so spreadsheets stay readable. Domain formatter owns this conversion.

MIME type for the share intent: `text/csv`.

---

## 10. Dependency Injection and Threading

- Hilt at the `Application` and `Activity`/`ViewModel` boundary.
- `@Inject` constructors on ViewModels (`@HiltViewModel`) and use cases.
- Room, `ReceiptFileStore`, `LastCategoryStore`, and repository impls provided from `@Module` / `@InstallIn(SingletonComponent::class)`.
- Database I/O on Room’s executors / `Dispatchers.IO`. Use cases do not hop dispatchers unless they do CPU work (CSV build for large lists — then `default`).
- File copy from a gallery `Uri` happens on `Dispatchers.IO` in the data layer.
- Main thread is for Compose and ViewModel state updates only.

Constructor injection of domain interfaces keeps tests free of Hilt: pass fakes.

---

## 11. Security and Privacy

| Topic | Decision |
| --- | --- |
| Network | No permission, no client, no ads, no crash-upload SDK. |
| Backup | `android:allowBackup="false"` and `dataExtractionRules` / `fullBackupContent` deny-all. Expense amounts and receipt photos must not silently land in Google Backup. |
| Secrets | Release keystore, `keystore.properties`, and signing passwords are GitHub Actions secrets. Never committed. |
| Intents | `FileProvider` only; `exported="false"` on the provider; grant is per-Uri and temporary. |
| Input | Amount parsed from digits only. Category names length-capped (e.g. 40 chars). Receipt copy size-capped (reject or downscale above ~10 MB). |
| Logging | No receipt paths or amounts in logcat in release. No crash-upload SDK. Standing “observability” checklists do **not** authorize analytics, metrics pipelines, or network telemetry. |
| SQL | Room parameterized queries only. |

Biometric lock is roadmap, not MVP. Unencrypted Room is accepted for a local single-user app with backup disabled. Do not add SQLCipher in MVP.

---

## 12. Testing Strategy

TDD for domain logic and ViewModels. Framework: JUnit 4, `kotlinx-coroutines-test`, fakes instead of mocking Room.

Turbine is **not** on the classpath. `LogUiState` is a single `MutableStateFlow` that tests assert against with `.value` after `advanceUntilIdle()`, so a Flow-assertion library has nothing to earn yet. Add it if a later sprint needs to assert on an emission *sequence* rather than a settled value.

| Layer | Where | What |
| --- | --- | --- |
| Domain use cases & formatters | `src/test` | Save validation, CSV lines, share text, period bounds (Monday week), mixed-currency totals, category reassignment. |
| ViewModels | `src/test` | Events → `UiState`; use case fakes; last-category radio fallback; `StandardTestDispatcher`. |
| Room DAOs | `src/androidTest` | In-memory database, migrations against committed schemas. |
| Compose | `src/androidTest` (smoke) | Amount focus on launch; save disabled when amount empty. Not a full screenshot suite in MVP. |

Do not mock use cases *and* repositories in the same test. ViewModel tests fake use cases; use case tests fake repositories.

Commands (once the Gradle project exists):

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

Windows: `.\gradlew.bat` with the same tasks.

Release:

```bash
./gradlew assembleRelease
```

---

## 13. Build, CI, and Release

Trunk-based: short-lived branches into `main`. `main` stays releasable.

**CI (every PR and push to `main`):**

1. `lint`
2. `test` (JVM)
3. `assembleDebug`

**Release (git tag `v*`):**

1. `assembleRelease` with signing secrets
2. Upload the APK to GitHub Releases

Versioning: SemVer. `versionName` matches the tag (`1.0.0` for `v1.0.0`). `versionCode` is a monotonic integer (CI can use the run number or a tag counter).

R8/minification on for release. Keep Room and Kotlin metadata rules as required by those libraries.

---

## 14. Code Style

- Kotlin official style, 4-space indent, no wildcard imports.
- Types: `PascalCase`. Functions/properties: `camelCase`. Use case class names are verbs: `SaveExpense`.
- UiState is a `data class`. Events are a `sealed interface`.
- Prefer `val` and immutable lists in state.
- No comments that restate the code. Comments explain *why* when the reason is not obvious (for example, why `CAMERA` is absent).

```kotlin
class SaveExpense(
    private val expenses: ExpenseRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(input: NewExpense): Result<Expense> {
        if (input.amount.minor <= 0L) return Result.failure(InvalidAmount)
        val now = clock.instant()
        return Result.success(
            expenses.insert(
                Expense(
                    id = 0L,
                    amount = input.amount,
                    categoryId = input.categoryId,
                    occurredAt = now,
                    receiptRelativePath = input.receiptRelativePath,
                    createdAt = now,
                    updatedAt = now,
                ),
            ),
        )
    }
}
```

---

## 15. Boundaries

**Always**

- Keep domain free of Android types.
- Persist money as integer minor units.
- Store receipts only under `filesDir/receipts/`.
- Expose UI state as `StateFlow`; push events up.
- Run tests and lint before treating a slice as done.
- Update this file when a layering or persistence decision changes.

**Ask first**

- New Gradle dependencies.
- New screens or taps on the log flow.
- Room schema changes after version 1 is shipped.
- Changing `minSdk`, `applicationId`, or how currency is chosen.
- Enabling backup, encryption, or any permission.
- Adding a note/merchant field, date picker on Log, or bottom navigation.

**Never**

- Network calls, analytics, ads, or cloud SDKs.
- Commit keystores, `keystore.properties`, or signing passwords.
- Write receipts to the gallery / `MediaStore`.
- Use `Double` for money.
- Use `AndroidViewModel` or put `Context` in a ViewModel.
- Target WhatsApp by package name as the only share path.
- `fallbackToDestructiveMigration()` in release.

---

## 16. Success Criteria

This architecture is doing its job when:

- An implementer can create the Gradle project and packages from section 4 without inventing a second pattern.
- The log path is: open → type amount → tap category → optional receipt → save. No extra screen.
- Domain tests run on the JVM with zero Android runtime.
- Uninstalling the app removes all expenses and receipt files (private storage).
- Share and CSV work with no `INTERNET` permission in the merged manifest.
- A tagged commit produces a signed APK in CI without secrets in git.

---

## 17. Assumptions and Open Questions

Decisions below are in force until contradicted. Correct them before implementation if they are wrong.

### Decisions (confirmed)

1. Single `:app` module, Hilt, Navigation Compose, Coil, JDK 17, `minSdk` 26, `targetSdk` 36.
2. **Currency follows the device locale at save time** and is stored on each expense. Mixed-currency totals are split by code. Not a user setting in MVP.
3. UI strings are **English only** in `values/strings.xml`. Numbers and dates still follow the device locale.
4. `applicationId` is `com.quicklogger.app` unless a different reverse-DNS is supplied later.
5. Gallery import is in MVP, not camera-only.
6. **Save & Share** sends the receipt image plus caption when a photo exists; otherwise text only.
7. No note/merchant field.
8. No date picker on the Log screen; `occurredAt` defaults to now. **History edit can change `occurredAt`.**
9. Deleting a category moves its expenses to **Other**.
10. No in-app CameraX; system camera + photo picker only.
11. `allowBackup` is false.
12. Share uses the system chooser, not a hard-coded WhatsApp package. `ACTION_SEND` lives in presentation; FileProvider paths live in data.
13. Weekly summaries **always start on Monday**.
14. Share/CSV copy may say “WhatsApp”; payloads are plain text with optional `*star*` markup, not Markdown.
15. Log category chips are **radio**: last-selected id persisted in SharedPreferences; fallback lowest `sortOrder`, then `Other`.
16. No `Category.isDefault` column. Seed-if-empty plus `isProtected` on `Other` is enough.
17. License is **MIT**, copyright Micha Schiess.
18. CSV file name is `quicklogger-YYYY-MM-DD.csv` using the **export** date, including for week/month exports.
19. GitHub clone URL is `https://github.com/schiessti-afk/QuickLoggerBudget.git`.

### Still open (non-blocking)

None.
)
