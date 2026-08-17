# Implementation Plan: Sprint 1 — Runnable shell

## Overview

Stand up a single `:app` Gradle project that launches to a Material 3 light-only Log screen (amount focused, IME shown), with History reachable from the top bar. Hilt, Navigation Compose, Room+KSP, and Coil sit on the classpath. CI runs `lint`, `test`, and `assembleDebug` on `main` and PRs. No Room entities, save, receipts, share, CSV, or generated brand art.

## Architecture Decisions

- Library versions from official docs (ARCHITECTURE does not pin them): AGP 9.3.0 + Gradle 9.5.0, Kotlin 2.4.10 (AGP 9 built-in Kotlin; do not apply `org.jetbrains.kotlin.android`), Compose BOM `2026.06.01`, Hilt 2.60.1 + KSP, Navigation Compose 2.9.8 (not Navigation 3), Room 2.8.4, Coil 3.5.0 compose-only (no OkHttp/network artifact). `lifecycle` 2.9.4 and `hilt-navigation-compose` 1.2.0 are pinned below the latest stable because 2.11.0 / 1.4.0 require `compileSdk` 37; ARCHITECTURE keeps compile/target at 36.
- String routes `log` / `history`. Type-safe Navigation serialization waits until a destination needs arguments (`expense_edit/{id}` in sprint 4).
- Log is a themed shell: focused amount field, no chips, no Save. Amount formatting is sprint 2.
- Adaptive launcher is a cream/wax XML vector, not DESIGN §8 generated art (sprint 6).
- Release signing workflow is sprint 7. Do not add `.github/workflows/release.yml`.
- Coil 3 without `coil-network-*` so the merged manifest stays free of `INTERNET`. Manifest also uses `tools:node="remove"` as a backstop.

## Task List

### Phase 1: Gradle foundation
- [ ] Task 1: Wrapper, version catalog, `:app` module, local SDK pointer
- [ ] Task 2: Manifest privacy + deny-all backup rules + JVM tests

### Checkpoint: Foundation
- [ ] Module syncs; privacy tests fail until the manifest exists, then pass

### Phase 2: Themed shell
- [ ] Task 3: DESIGN §5.1 color tokens + light-only `MaterialTheme` + JVM hex tests
- [ ] Task 4: `QuickLoggerApp`, `MainActivity`, NavHost, Log (focus + IME), empty History
- [ ] Task 5: Layer package holders; Room/Coil/Hilt on the classpath

### Checkpoint: Runnable APK
- [ ] `assembleDebug` produces an APK; opening it lands on Log

### Phase 3: CI
- [ ] Task 6: GitHub Actions `ci.yml` (`lint`, `test`, `assembleDebug`)

### Checkpoint: Complete
- [ ] `lint`, `test`, `assembleDebug` succeed locally
- [ ] Sprint 1 exit criteria are met (human still reviews the installed APK)

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| AGP 9 built-in Kotlin vs Compose/Hilt plugins | High | Follow AGP 9.0 notes: no `kotlin-android`; keep Compose compiler + Hilt + KSP plugins |
| Coil/Room merging `INTERNET` | High | Coil compose-only; `tools:node="remove"`; JVM test on source manifest + lint on merged |
| IME not shown on some devices | Med | `windowSoftInputMode=stateVisible` plus `FocusRequester` + `SoftwareKeyboardController` |
| Hilt vs AGP 9 new DSL | Med | Hilt 2.60.1 (≥ 2.59, documented AGP 9 floor) |

## Open Questions

None blocking. Version pins are taken from official current stables; correct them if a device/CI constraint requires an older AGP.
