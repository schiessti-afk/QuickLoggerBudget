# Implementation Plan: Sprint 6 — Stationery identity

## Overview

The running app gets its generated ink-on-paper family: the launcher fold, a 24 dp top-bar glyph, six category pictograms with per-category accents on the chips, an empty-History illustration, and camera/gallery action glyphs in the same line style. Nothing here adds a tap, a route, or a dependency on a generated asset actually resolving — DESIGN §2 and §8 are explicit that beauty must not slow the log path.

## The one structural decision worth calling out

`Category` has no color or icon column, and adding one is out of this sprint's scope (ARCHITECTURE's schema doesn't call for it, and Sprint.md's "explicitly not a sprint" list keeps schema changes out unless a spec change asks for them). So accent + pictogram resolution has to happen entirely in presentation, keyed by the category's `name` — which is stable and known for the six seeded rows (`QuickLoggerDatabase`'s seed list). `presentation/theme/CategoryStyle.kt` is a pure `name -> CategoryStyle` function with a hard `else -> OTHER` branch, which is what makes DESIGN §4.4's "custom categories reuse Other" and this sprint's "no generated asset required to complete a save" true by construction: every name resolves to *something*, there is no lookup that can fail.

## Architecture Decisions

- **Assets are hand-drawn `VectorDrawable` XML, not a pixel trace of the `assets/` PNG masters.** There's no image-tracing tool available in this environment; the masters were used as a visual reference (same silhouette, same even-stroke ink-line language) and redrawn as clean path data instead of auto-traced, which also keeps the files small and crisp at every density (a real requirement — DESIGN §8.1 explicitly needs the launcher to "read at 48 dp and on a circular mask").
- **Camera and gallery stay hand-drawn ink glyphs, not `material-icons-extended`.** DESIGN §6 says "Material Symbols" for chrome, but `material-icons-core` (already on the classpath) has no camera/gallery glyph, and the extended artifact is a large dependency to add for two icons. Two more paths in the same stroke-weight/line-cap family as the pictograms and toolbar glyph serve DESIGN §8's actual goal — "read as one ink family" — at least as well as pulling in a different icon style would have. This is a documented deviation from the letter of §6, not a silent one (see DESIGN.md's "Sprint 6 implementation notes").
- **The empty-History illustration carries baked color; the six pictograms and toolbar glyph do not.** DESIGN §8 draws this line itself: pictograms are "single color (tinted at runtime to the category accent)" while the illustration explicitly is allowed "cream already in the scene." Vector paths for the former use a placeholder `#FF000000` fill/stroke that Compose's `Icon(tint = …)` recolors uniformly; the illustration's path colors are the real, final palette values and it's drawn with `Image`, not `Icon`.
- **Selected-chip color comes from `FilterChipDefaults.filterChipColors(...)`, not a custom composable.** `selectedContainerColor` is the category accent at 24% alpha (DESIGN §5.2's "~24% fill"); `selectedLabelColor` and `labelColor` are both pinned to `onSurface` regardless of selection, which is what makes "label stays ink, never white-on-accent" (DESIGN §7) true unconditionally rather than something a future edit could regress by copying M3's own selected-state default (`onSecondaryContainer`, which isn't guaranteed to be the right contrast against an arbitrary accent color).
- **Save & Share becomes `FilledTonalButton`**, matching DESIGN §6's component table exactly (`Save` is `Button`, `Save & Share` is `FilledTonalButton`) — it was `OutlinedButton` as an implementation default in sprint 5, which predates this table being wired up.
- **Amount field gets `fontFeatureSettings = "tnum"`** on its `displaySmall` text style (DESIGN §5.3) — additive, no layout change, degrades silently on faces without the OpenType feature.

## Task List

### Phase 1: Generated assets
- [x] Task 1: Launcher fold (`ic_launcher_foreground`, `ic_launcher_monochrome`) redrawn to the receipt-fold silhouette, inside the adaptive-icon safe zone
- [x] Task 2: Top-bar glyph (`ic_toolbar_receipt`), single-color, 24×24 viewport
- [x] Task 3: Six category pictograms (`ic_category_{food,transport,supplies,utilities,personal,other}`), single-color, 24×24 viewport
- [x] Task 4: Empty-History illustration (`ic_empty_history`), baked cream/wax/ink palette
- [x] Task 5: Camera/gallery action glyphs (`ic_action_camera`, `ic_action_gallery`) — see the deviation note above

### Checkpoint: Assets
- [x] `assembleDebug` green (all new drawables parse and compile)

### Phase 2: Category style + chips
- [x] Task 6: `presentation/theme/CategoryStyle.kt` (`categoryStyleFor(name)`) + JVM tests
- [x] Task 7: `CategoryChips` — leading pictogram, accent-tinted border/fill, ink label pinned regardless of selection

### Phase 3: Screens
- [x] Task 8: Log top bar gets the 24 dp glyph before the title; Save & Share becomes `FilledTonalButton`
- [x] Task 9: `ReceiptAttachment` — camera/gallery become icon buttons (was: plain labelled `OutlinedButton`s, sprint 3's stand-in); `LogScreenTest` updated where a visible label moved to `contentDescription`
- [x] Task 10: `HistoryScreen` — 16 dp pictogram + accent on each row's category line; empty state gets the illustration above the existing one-line copy
- [x] Task 11: `AmountField` — tabular figures (`fontFeatureSettings = "tnum"`)

### Checkpoint: Sprint complete
- [x] `lint`, `test`, `assembleDebug` green
- [x] Sprint 6 exit criteria checked (the "reads as one ink family" / device checks still need a human)

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| A malformed `pathData` arc (wrong sweep/large-arc flag) silently fails to parse or renders a bowtie | High | Every circle uses the standard two-semicircle full-circle idiom (`M (cx-r) cy A r r 0 1 0 (cx+r) cy A r r 0 1 0 (cx-r) cy Z`), verified in an SVG preview using the identical path data before committing |
| Selected chip regresses to white-on-accent later (e.g. a drive-by M3 default) | Med | `labelColor` / `selectedLabelColor` are both set explicitly to `onSurface` in `filterChipColors(...)`, not left to inherit M3's `onSecondaryContainer` default |
| Removing visible "Take photo" / "Choose image" text breaks existing Compose tests | Med | `LogScreenTest`'s three affected assertions moved from `onNodeWithText` to `onNodeWithContentDescription`, same accessible name, still unexecuted pending a device per prior sprints |
| A future custom category with a name that happens to collide with a seeded one (e.g. renamed to "Food") silently takes Food's style | Low | Out of scope: `categoryStyleFor` matches by name on purpose (no id column to key on), and DESIGN doesn't ask for per-category identity beyond the seeded six — flagged, not fixed |

## Open Questions

None blocking. The camera/gallery deviation from "Material Symbols" (hand-drawn glyphs instead of `material-icons-extended`) is a judgment call in DESIGN §8's favor over §6's letter — say so if `material-icons-extended` is preferred instead.
