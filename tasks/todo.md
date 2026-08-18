# Sprint 6 tasks

- [x] Task 1: Launcher fold (`ic_launcher_foreground`, `ic_launcher_monochrome`)
  - Acceptance: reads as a folded receipt (crease + tally lines + dog-eared corner) inside the adaptive-icon safe zone; monochrome variant is a single-color silhouette for themed icons
  - Verify: `assembleDebug` green (resource compiles); visual read needs a device
  - Files: `res/drawable/{ic_launcher_foreground,ic_launcher_monochrome}.xml`

- [x] Task 2: Top-bar glyph (`ic_toolbar_receipt`)
  - Acceptance: same silhouette as the launcher, 24×24 viewport, single color (tintable)
  - Verify: wired into Log's `TopAppBar` title row
  - Files: `res/drawable/ic_toolbar_receipt.xml`, `presentation/log/LogScreen.kt`

- [x] Task 3: Six category pictograms
  - Acceptance: each is single-color, 24×24, distinct silhouette per DESIGN §8.5's description (coffee cup, bus, open carton, light bulb, tote bag, asterisk-in-square)
  - Verify: `CategoryStyleTest` — each seeded name resolves to a distinct pictogram resource id
  - Files: `res/drawable/ic_category_{food,transport,supplies,utilities,personal,other}.xml`

- [x] Task 4: Empty-History illustration
  - Acceptance: folded receipt + wax seal + closed ledger, baked cream/wax/ink palette, lots of negative space, no checklist
  - Verify: wired above the existing one-line `history_empty` string
  - Files: `res/drawable/ic_empty_history.xml`, `presentation/history/HistoryScreen.kt`

- [x] Task 5: Camera/gallery action glyphs
  - Acceptance: single-color, same stroke weight as the pictograms; documented deviation from "Material Symbols" (no `material-icons-extended` dependency added)
  - Verify: wired into `ReceiptAttachment`; `LogScreenTest`'s content-description assertions still pass compilation
  - Files: `res/drawable/ic_action_{camera,gallery}.xml`, `presentation/components/ReceiptAttachment.kt`

- [x] Task 6: `CategoryStyle.kt`
  - Acceptance: each of the six seeded names resolves to a distinct accent + pictogram; any other name (including every custom category) resolves to Other's — never a lookup miss
  - Verify: `CategoryStyleTest` — 5 tests green
  - Files: `presentation/theme/CategoryStyle.kt`

- [x] Task 7: `CategoryChips` — pictogram + accent + selected-state contrast
  - Acceptance: unselected chip shows an accent-tinted outline and pictogram on a cream fill; selected chip fills to ~24% accent with an accent border; the label is `onSurface` ink in both states, never white-on-accent
  - Verify: existing `LogScreenTest` chip-selection assertions (label text, click behavior) still hold — pictogram is decorative (`contentDescription = null`)
  - Files: `presentation/components/CategoryChips.kt`

- [x] Task 8: Log top bar + Save & Share button style
  - Acceptance: 24 dp glyph precedes the "QuickLogger" title; Save stays `Button`, Save & Share becomes `FilledTonalButton` per DESIGN §6's component table
  - Verify: `assembleDebug`; no new tap added to the primary path
  - Files: `presentation/log/LogScreen.kt`

- [x] Task 9: `ReceiptAttachment` icon buttons
  - Acceptance: camera/gallery are `IconButton`s (48 dp touch target by default) with `contentDescription`, not visible-text buttons
  - Verify: `LogScreenTest` — 3 assertions moved from `onNodeWithText` to `onNodeWithContentDescription`, same accessible names
  - Files: `presentation/components/ReceiptAttachment.kt`, `androidTest/.../LogScreenTest.kt`

- [x] Task 10: History row pictogram + empty-state illustration
  - Acceptance: each row shows a 16 dp accent-tinted pictogram before "category · date"; empty state shows the illustration above the unchanged one-sentence copy
  - Verify: `assembleDebug`; existing `HistoryScreenTest` text assertion (`"No expenses in this period."`) still holds
  - Files: `presentation/history/HistoryScreen.kt`

- [x] Task 11: `AmountField` tabular figures
  - Acceptance: `displaySmall` text style carries `fontFeatureSettings = "tnum"`
  - Verify: `assembleDebug` (no JVM-testable behavior — this is a text-rendering hint, not logic)
  - Files: `presentation/components/AmountField.kt`

## Checkpoint: Sprint 6 done
- [x] `.\gradlew.bat lint`
- [x] `.\gradlew.bat test` — 169 tests, 0 failures
- [x] `.\gradlew.bat assembleDebug`
- [x] `.\gradlew.bat compileDebugAndroidTestKotlin` — androidTest sources still compile (no execution; no device)
- [x] Dynamic color still off; no dark `ColorScheme`
- [x] No generated asset lookup can fail — `categoryStyleFor` always resolves
- [ ] `.\gradlew.bat connectedDebugAndroidTest` (needs a device/emulator)
- [ ] Human review before the sprint is treated as closed — "reads as one ink family at device size" is a visual call this session cannot make

## Follow-ups noticed, not actioned
- `material-icons-extended` was deliberately not added; if a future sprint wants true Material Symbols for camera/gallery instead of the hand-drawn glyphs, that's a one-file swap in `ReceiptAttachment.kt`.
- `categoryStyleFor` matches by `name`, not by a stable id — a custom category renamed to collide with a seeded name (e.g. "Food") would silently take that seed's style. Not asked for; flagged for whoever adds a color/icon column later.
- `ManageCategoriesDialog`'s category rows (History's overflow → manage) do not show pictograms — DESIGN doesn't ask for it there (only chips and History rows are specified), left as plain rows.
