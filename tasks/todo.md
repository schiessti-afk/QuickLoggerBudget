# Sprint 8 tasks

- [x] Task 1: Spec change (DESIGN)
  - Acceptance: Save & Share is `OutlinedButton`; receipt thumb is 8 dp (`shapes.small`); camera/gallery are filled 56 dp tiles; Inter is the bundled face; `Shapes` scale is documented. §6 and §7 match the code this sprint will ship.
  - Verify: docs land before any Kotlin
  - Files: `docs/DESIGN.md`

- [x] Task 2: Theme shapes
  - Acceptance: `small = 8.dp`, `medium = 12.dp`, `large = 16.dp`; `QuickLoggerButtonShape` is 12 dp and is the only way `Button` / `OutlinedButton` escape M3's `CornerFull` pill
  - Verify: `CategoryChips` untouched; chips still use theme `small`
  - Files: `presentation/theme/{Shape,Theme}.kt`

- [x] Task 3: Bundle Inter
  - Acceptance: Regular / Medium / SemiBold under `res/font/` with OFL; every `Typography` role uses that family; `AmountField` still sets `fontFeatureSettings = "tnum"`
  - Verify: `assembleDebug`; no new Gradle dependency
  - Files: `app/src/main/res/font/*`, `third_party/inter/OFL.txt`, `presentation/theme/Type.kt`

- [x] Task 4: Log layout polish
  - Acceptance: Save + Save & Share in one `Row` (`weight(1f)` each); Save & Share is `OutlinedButton` with primary border/label; divider between chips and receipt; camera/gallery are 56 dp `Surface` tiles; thumbnail uses `shapes.small`; `canSave` and content descriptions unchanged
  - Verify: `LogScreenTest` with **no assertion edits**
  - Files: `presentation/log/LogScreen.kt`, `presentation/components/ReceiptAttachment.kt`

- [x] Task 5: Remaining button call sites
  - Acceptance: every `Button` and `OutlinedButton` in the app passes `shape = QuickLoggerButtonShape`
  - Verify: grep for `Button(` / `OutlinedButton(`; `ExpenseEditScreenTest` with no assertion edits
  - Files: `presentation/expenseedit/ExpenseEditScreen.kt`

## Checkpoint: Sprint 8 done
- [x] `.\gradlew.bat lint`
- [x] `.\gradlew.bat test` — 213 tests, 0 failures (no tests added this sprint)
- [x] `.\gradlew.bat assembleDebug`
- [x] No schema / DAO / repository / use case / ViewModel file modified
- [x] DESIGN §6 and §7 match the shipped code
- [ ] Human review: 200% font-scale wrapping on Save / Save & Share is accepted as-is, not relayouted
