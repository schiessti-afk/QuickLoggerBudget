# Sprint 3 tasks

- [x] Task 1: `ReceiptStore` + `ReceiptError` + receipt use cases
  - Acceptance: create/import/delete/hasContent behind a domain interface with no Android types; oversized and unreadable sources fail
  - Verify: `.\gradlew.bat :app:testDebugUnitTest --tests com.quicklogger.app.ReceiptUseCasesTest` — 8 tests green
  - Files: `domain/repository/ReceiptStore.kt`, `domain/usecase/ReceiptUseCases.kt`

- [x] Task 2: `ReceiptFileStore` under `filesDir/receipts/`
  - Acceptance: `{uuid}.jpg` only; streaming copy aborts past 10 MB and leaves no partial file; path traversal cannot escape the directory
  - Verify: `assembleDebug` green
  - Files: `data/receipt/ReceiptFileStore.kt`

- [x] Task 3: `FileProvider` + `file_paths.xml` + Hilt binding
  - Acceptance: `exported=false`, `grantUriPermissions=true`, `files-path` → `receipts/` only; authority `${applicationId}.fileprovider`
  - Verify: `ManifestPrivacyTest` — 4 new guards green; merged manifest inspected
  - Files: `AndroidManifest.xml`, `res/xml/file_paths.xml`, `di/DataModule.kt`

- [~] Task 4: `ReceiptFileStore` instrumentation tests
  - Acceptance: draft is empty and lands in `receipts/`; import copies bytes and drops the source name; oversized import leaves nothing; traversal delete is refused
  - Status: **written and compiling, never executed** — no device or emulator in this session
  - Verify: `.\gradlew.bat :app:connectedDebugAndroidTest`
  - Files: `app/src/androidTest/java/com/quicklogger/app/ReceiptFileStoreTest.kt` (13 tests)

- [x] Task 5: `LogUiEvent` channel + receipt state in `LogViewModel`
  - Acceptance: draft created before the camera launches; no thumbnail until success; cancel deletes; replace deletes the previous file; save persists the path and clears it without deleting
  - Verify: `LogViewModelReceiptTest` — 14 tests green
  - Files: `presentation/log/{LogEvent,LogUiState,LogViewModel}.kt`

- [~] Task 6: Camera and gallery actions, Coil thumbnail, remove
  - Acceptance: two actions when empty, thumbnail + remove when attached, error text on an oversized pick, Save blocked mid-copy
  - Status: screen and components done and building; the 6 new smoke tests are **written and compiling, never executed**
  - Verify: `.\gradlew.bat :app:connectedDebugAndroidTest`
  - Files: `presentation/log/LogScreen.kt`, `presentation/components/{ReceiptAttachment,ReceiptFiles}.kt`

## Checkpoint: Sprint 3 done
- [x] `.\gradlew.bat lint`
- [x] `.\gradlew.bat test` — 78 tests, 0 failures
- [x] `.\gradlew.bat assembleDebug`
- [x] Domain has no `android.*` / Room / Compose / `Uri` imports
- [x] `LogViewModel` holds no `Context`, `Uri`, or `AndroidViewModel`
- [x] Merged manifest has no `CAMERA`, no media/storage permission, still no `INTERNET`
- [x] No `MediaStore` reference anywhere in `src/main`
- [ ] `.\gradlew.bat connectedDebugAndroidTest` (needs a device/emulator)
- [ ] A real capture stores a JPEG and shows a thumbnail (needs a device)
- [ ] The device gallery gains no image from capture or pick (needs a device)
- [ ] Human review before the sprint is treated as closed

## Follow-ups noticed, not actioned
- `createComposeRule()` is still the deprecated v1 API. Unchanged from sprint 2 and for the same reason: migrating tests that have never run would make a first emulator run fail for reasons unrelated to the app.
- A receipt attached and then abandoned by a process kill leaves one orphan file under `receipts/`. No sweep exists; the sprint does not ask for one. Worth a decision before sprint 7.
- `presentation/categories/Categories.kt` is still an empty package holder — sprint 4 fills it.
