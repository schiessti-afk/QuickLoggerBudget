# Implementation Plan: Sprint 3 — Private receipts

## Overview

Let a log optionally carry one photo, from the system camera or the photo picker, stored only under `filesDir/receipts/{uuid}.jpg`. The file never reaches the gallery, a cancelled capture leaves nothing behind, and replacing a receipt deletes the file it replaced. `SaveExpense` already accepts `receiptRelativePath` — sprint 3 fills it in.

## The central constraint

Exit criteria say the ViewModel holds no `Context`, `Uri`, or `AndroidViewModel`. But the flow needs both: `TakePicture` wants a `FileProvider` Uri for a file that must exist *before* the camera launches, and the picker hands back a `content://` Uri.

The seam:

```
UI (has Context)          ViewModel (has neither)        Data (has Context)
─────────────────────────────────────────────────────────────────────────────
tap camera        ──►  LogEvent.CaptureReceipt
                            │
                            ├─► CreateReceiptDraft ──►  creates {uuid}.jpg, empty
                            │                             returns "uuid.jpg"
                       LogUiEvent.LaunchCamera("uuid.jpg")
                            │
  resolve File +  ◄─────────┘
  FileProvider Uri,
  launch TakePicture
        │
        └─────────►  LogEvent.ReceiptCaptured(success)
                            │
                            ├─ true  → confirm, delete the receipt it replaced
                            └─ false → DeleteReceipt(draft)
```

- The ViewModel only ever handles the **relative path string** and a **Uri string**. Never an `android.net.Uri`, never a `Context`.
- Uri resolution and both Activity Result launchers live in the composable, which ARCHITECTURE §3.1 explicitly permits.
- Camera launch is a **one-shot** `LogUiEvent` over a `Channel`, not `UiState` (ARCHITECTURE §5 rule 4). Sprint 5 reuses the same channel for the share sheet.

## Architecture Decisions

- **`RECEIPTS_DIRECTORY` lives in domain.** Data resolves it against `filesDir`, presentation resolves it for `FileProvider` and Coil. Putting it in data would force presentation to import data and invert the dependency arrow.
- **The picker Uri crosses layers as a `String`.** `LogEvent.ReceiptPicked(sourceUri: String)`; `ReceiptFileStore` calls `Uri.parse` on the far side. The ViewModel stays JVM-testable with a fake and holds no Android type — which is what the rule protects.
- **Draft path is ViewModel-private, not in `UiState`.** The thumbnail must not appear until the capture actually succeeds, so the in-progress file is tracked separately from the confirmed `receiptRelativePath`.
- **Oversized picks are rejected, not downscaled.** The sprint allows either. Rejecting needs no `Bitmap` decode in the data layer and gives the user a clear message; downscaling is a silent quality change. The copy streams with a running byte count and aborts past 10 MB, so a source that misreports its size cannot slip through.
- **Save clears the receipt without deleting it.** After a successful write the file belongs to the persisted expense. Only replace, remove, and failed capture delete.
- **`file_paths.xml` gets `files-path` only.** The `cache-path` entry ARCHITECTURE §7.3 lists is for CSV export in sprint 5; adding it now would be pulling roadmap work forward.
- **FileProvider authority is `"${context.packageName}.fileprovider"`** resolved at runtime, matching the manifest's `${applicationId}` placeholder without turning on the `buildConfig` feature.
- **No orphan sweep.** A receipt attached and then abandoned by a process kill leaves one file. Cleaning that up is not in the sprint, and a sweep on launch would be new unspecified behavior.

## Task List

### Phase 1: Domain
- [ ] Task 1: `ReceiptStore` + `ReceiptError` + `CreateReceiptDraft` / `ImportReceipt` / `DeleteReceipt` + JVM tests

### Checkpoint: Domain
- [ ] `test` green; domain still free of `android.*` / Room / Compose / `Uri`

### Phase 2: Data and platform
- [ ] Task 2: `ReceiptFileStore` under `filesDir/receipts/`, streaming copy with the 10 MB cap
- [ ] Task 3: `FileProvider` manifest entry + `res/xml/file_paths.xml` + Hilt binding
- [ ] Task 4: `ReceiptFileStore` instrumentation tests

### Checkpoint: Storage
- [ ] `assembleDebug` green; merged manifest still has no `CAMERA` or media permission

### Phase 3: Log screen
- [ ] Task 5: `LogUiEvent` channel, receipt state, capture/import/remove/replace in `LogViewModel` + JVM tests
- [ ] Task 6: Camera and gallery actions, Coil thumbnail, remove control + Compose smoke tests

### Checkpoint: Sprint complete
- [ ] `lint`, `test`, `assembleDebug` green
- [ ] Sprint 3 exit criteria checked (device checks still need a human)

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| `TakePicture` returns success but writes nothing | High | Confirm only on `success == true`; treat a zero-length file as a failed capture and delete it |
| Replace leaks the previous file | High | Confirming a new path deletes the old one in the same state update; JVM test asserts the delete |
| Picker Uri persisted by accident | High | Only the copied relative path reaches state; the source string is used once and dropped |
| Coil caches the empty pre-capture file | Med | Thumbnail renders only after success, and every draft is a fresh uuid, so nothing stale can be keyed |
| `androidTest` cannot run here (no device) | Med | Written and compiled; flagged unverified, same as sprint 2 |

## Open Questions

None blocking. The 10 MB cap and reject-over-downscale are implementation choices the sprint left open — say so if you want downscaling instead.
