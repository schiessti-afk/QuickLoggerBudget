# Implementation Plan: Sprint 8 — Stationery polish

## Overview

Make the Log screen match `assets/github-social-preview.png` as the shape / button-hierarchy / type reference, without adding taps, routes, or domain behavior. Two DESIGN rows are wrong before any Kotlin lands; those amendments are task 1. The rest is presentation-only: a theme `Shapes` scale, an explicit button-shape override (M3 buttons ignore the theme scale), filled receipt tiles, a side-by-side Save row, a divider, and bundled Inter.

## Architecture Decisions

- **Docs first.** DESIGN §6 currently maps Save & Share to `FilledTonalButton` and §5.3 / §3 still say platform type. Those reversals land before Kotlin so no commit (and no working tree, once this sprint is committed) contradicts the spec.
- **`small = 8.dp` is a pin, not a restyle.** FilterChip reads `shapes.small` (`CornerSmall`). The M3 default is already 8 dp ([Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)); pinning it keeps category chips byte-identical. `CategoryChips.kt` is not touched.
- **Buttons need a call-site `shape =`.** `ButtonDefaults.shape` resolves `CornerFull` → a hard-coded `CircleShape` that never reads `MaterialTheme.shapes` ([Shapes](https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/-shapes/-shapes.html): "by default, buttons use the shape style full"). The shared `QuickLoggerButtonShape` (`RoundedCornerShape(12.dp)`, matching `medium`) is passed at every `Button` / `OutlinedButton` site. `TextButton` / `IconButton` are out of scope.
- **Inter is bundled, not downloaded.** OFL Regular / Medium / SemiBold from the official Inter 4.1 release, under `res/font/`. M3 `Typography` has no `defaultFontFamily` ([Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)); each `TextStyle` gets `fontFamily = Inter`.
- **No new tests.** Sprint 8 adds no domain behavior. Existing Compose assertions stay byte-identical; the JVM count stays 209.

## Task List

### Phase 0: Spec change
- [x] Task 1: DESIGN §3 / §5.3 / §6 / §7 / §10 match the sprint 8 In list

### Phase 1: Theme
- [x] Task 2: `Shape.kt` (`QuickLoggerShapes` + `QuickLoggerButtonShape`) wired through `Theme.kt`
- [x] Task 3: Inter in `res/font/` + `QuickLoggerTypography`

### Checkpoint: Theme
- [x] `assembleDebug` succeeds; no domain/data files touched

### Phase 2: Log + remaining buttons
- [x] Task 4: Log row, `OutlinedButton`, divider, receipt tiles, thumbnail `shapes.small`
- [x] Task 5: `shape = QuickLoggerButtonShape` on Expense-edit `Button` / `OutlinedButton`

### Checkpoint: Sprint complete
- [x] `lint`, `test` (213, no tests added), `assembleDebug` green
- [x] No assertion edits in `LogScreenTest` / `DashboardScreenTest` / `ExpenseEditScreenTest`
- [x] No schema, DAO, repository, use case, or ViewModel file modified

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Clickable `Surface` tiles split semantics so `onNodeWithContentDescription("Take photo")` no longer clicks | High (Compose tests fail) | Keep `contentDescription` on the `Icon` inside a clickable `Surface` so merge-descendants behaves like `IconButton` |
| Labels wrap at 200% font scale | Low (exit criterion) | Do not relayout; show wrapping to a human |
| Inter zip is large (32 MB) | Low | Extract only Regular / Medium / SemiBold + OFL |

## Open Questions

None blocking. The sprint's "§7 becomes `OutlinedButton`" refers to the Save & Share *row*, which lives in the §6 components table in the current DESIGN.md; both §6 and §7 are amended so they match the shipped code.
