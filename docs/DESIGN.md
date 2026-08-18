# DESIGN.md — QuickLogger

This file is the **visual** source of truth (look, feel, motion, generated art). Product intent lives in [`docs/IDEA.md`](IDEA.md). Mechanics live in [`docs/ARCHITECTURE.md`](ARCHITECTURE.md).

If this file disagrees with IDEA on product intent, IDEA wins. If it disagrees with ARCHITECTURE on screens, navigation, or taps on the log path, ARCHITECTURE wins. If it disagrees with either on *how it looks*, this file wins and the others stay silent on visuals.

**Status:** visual direction is decided. Brand seed `#9A4A32` is confirmed. Generated masters live in [`assets/`](../assets/). Copy them into `app/src/main/res/` when the Gradle module exists.

---

## 1. Objective

QuickLogger must *feel* as fast as it is: a pocket POS for cash and receipts, not a finance dashboard. Beauty here means paper, ink, and a quiet receipt — never extra chrome on the two-second log path.

Success looks like:

- A stranger can log an amount in under two seconds without reading a tutorial.
- The launcher icon and first screen are recognizable as the same product.
- History, empty states, and share copy feel like the same brand as Log.
- Generated assets are few, sharp, and one family — ink-line stationery, not a different illustration on every screen.

---

## 2. Locked product constraints (do not restyle away)

These are already decided. Visual work may only *express* them.

| Constraint | Design implication |
| --- | --- |
| Open → amount → category → optional receipt → save | Log is one screen. No onboarding carousel, no bottom nav on the primary path. |
| Amount focused + keyboard up on launch | The amount is the hero. Everything else is secondary. |
| Category chips in radio mode | Chips must read as a single selection, default-visible on a typical phone. |
| Dashboard is a top-bar list action on Log | App bars stay thin. The dashboard is a destination, not a tab. It carries a back arrow to Log. |
| An unset budget is invisible | No meter, no placeholder bar, no "set a budget" prompt. The screen must look finished with zero targets. |
| Empty history is a short line, not a marketing screen | One illustration; copy stays one sentence. |
| English UI strings; numbers/dates follow device locale | Layouts must survive long currency strings (`R$ 1.234,56`, `$1,234.56`). |
| Material 3 + Compose | Tokens, components, and shapes come from M3. Custom art sits *on* that system. |
| No network, no analytics, no gallery writes | No remote fonts, no Lottie from CDN, no “sign in to unlock themes.” |

**Always**

- Keep the log path a sub-two-second action. Extra taps need an explicit product reason (ARCHITECTURE).
- Force **light** color scheme. Do not follow `uiMode` dark. Do not use Material You / dynamic color — the seed below is the brand.
- Keep touch targets at least 48 dp for Save, Save & Share, camera, gallery, History, and the budget meter / bars (they are tappable).

**Never**

- Add a splash delay, mascot walk-on, or branded animation that blocks the amount field.
- Put receipts, amounts, or category art in the device gallery.
- Use a second typeface downloaded at runtime.
- Decorate empty states into a landing page.
- Color a **logged amount** green/red. The hero amount on Log, the rows in the list, and the period totals stay ink `#2A241F`. This is a logger, not a P&L.
  *(Budget surfaces are the exception — see §5.4. A meter has a status; a receipt does not.)*

---

## 3. Visual direction

| Token | Decision | Notes |
| --- | --- | --- |
| Personality | **Warm stationery** | Paper, iron-gall ink, a folded receipt. Quiet, tactile, not a bank app. |
| Color world | **Fixed seed** (not Material You) | Sealing-wax primary on cream paper. See §5. |
| Theme | **Light only** | No dark `ColorScheme` in MVP. |
| Density | **Compact POS on paper** | Tight Log layout; the dashboard can breathe a little more. |
| Shape | **Soft paper corners** | Chips and cards ~8–12 dp. Not stadium-pill candy, not sharp tickets. |
| Type | **Platform M3** | Amount uses the largest display/headline role with tabular figures. No bundled display font unless we add `res/font` later. |
| Icon language | **Custom 6-category set + Material for chrome** | Camera, gallery, History, delete stay Material Symbols. Seeded categories use generated pictograms (§8.5). Custom user categories fall back to a generic mark. |
| Motion | **Instant paper** | Chip fill, no bounce. Save clears immediately. No confetti. |

**Line weight (all generated art):** even ink stroke, ~2 px at 24 dp, slightly rounded ends. Not sketchy, not 3D, not neon.

**No chart library.** The meter and bars are hand-drawn with Compose `Canvas` / `drawBehind`. A charting dependency would bring its own type scale, palette, and animation curves — a second design system inside a file whose whole point is one ink family. Two shapes do not justify that.

---

## 4. Screens

Layout and behavior follow ARCHITECTURE §8. This section only states *visual* intent.

### 4.1 Log (start destination)

The amount is the largest number on screen, ink on paper. Category chips sit directly under it, each with its pictogram and a muted accent. Receipt is a small optional strip. Save is the filled sealing-wax button; Save & Share is the tonal/outlined sibling so the fast path stays one glance.

```
┌─────────────────────────────────┐
│  [mark] QuickLogger     [list]  │  ← cream bar; list → Dashboard
├─────────────────────────────────┤
│                                 │
│         R$  0,00                │  ← focused, keyboard up, ink
│ Food 120,00 · Month 430,00 left │  ← §5.4; absent when no target
│                                 │
│  [🍴 Food] [🚌 Transport] …     │  ← FlowRow, radio chips + pictograms
│  [ + ]                          │
│                                 │
│  [camera] [gallery]   (thumb)   │  ← optional; thumb + remove
│                                 │
│  [ Save ]     [ Save & Share ]  │
└─────────────────────────────────┘
     ~~~~~~~~ system IME ~~~~~~~~
```

Do not add a date picker, merchant field, or note on this screen.

The remaining line is `bodySmall`, one line, ellipsized rather than wrapped — it must never push the chips down or grow into a second block. It is text, not a chip or a button: nothing on it is tappable.

### 4.2 Dashboard

The old History screen with a budget overview above it. Same cream surface, one scroll, back arrow to Log.

```
┌─────────────────────────────────┐
│  ←   Dashboard            [⋮]   │  ← back to Log; overflow = share/CSV
├─────────────────────────────────┤
│         ╭─────────────╮         │
│        ╱   430,00      ╲        │  ← remaining, ink, largest number here
│       │     left        │       │     arc: ledger green, red past 100%
│        ╲  of 1.200,00  ╱        │
│         ╰─────────────╯         │
│                                 │
│  Food        ███████▌ ┊    770  │  ← accent fill, ┊ = target tick
│  Transport   ████▏    ┊    120  │
│  Supplies    ██▊           45   │  ← no tick: no target set
│                                 │
├─────────────────────────────────┤
│  [Day] [Week] [Month]           │  ← filters the LIST only
│  R$ 45,00   Food     14:22   📎 │
│  R$ 12,00   Transport 09:05     │
└─────────────────────────────────┘
```

**Overview.** The arc is the only place a number beats the list for size. Track is `outline`; fill is the §5.4 status color. The centre shows remaining in ink, with `of <target>` beneath it in on-surface variant. Past the target the centre reads `over by …`.

**Bars.** One row per category that has spend this month **or** a target, spend-descending. The fill is that category's §5.2 accent — identity beats status here, otherwise every bar in a good month is the same green and the kit stops scanning. The target tick is a thin vertical ink rule at the target position; the segment drawn *past* the tick switches to over-red. A category with no target has no tick and never turns red.

**Nothing set, nothing spent → draw neither.** The screen is then byte-for-byte the old History, including the §8.3 empty illustration.

**Target dialog.** Tapping the arc or a bar opens a short dialog: the category name (or "Monthly budget"), one amount field reusing Log's digit buffer, confirm. Clearing the field and confirming removes the target. No second screen, no slider, no percentage picker.

Period chips (day / week / month) use the brand seed, not per-category color, and filter only the list. List rows: amount (ink), category name + small pictogram, local date/time, receipt indicator.

Empty state: one short line (`strings.xml`) plus the §8.3 illustration. No “Get started” checklist.

### 4.3 Expense edit

Same visual language as Log, plus occurred-at and delete. This is not the fast path — it can use a normal scaffold and confirmation for delete.

### 4.4 Category create / rename

Dialog on Log (`+` chip), not a new route. Keep the dialog short: name field + confirm. New custom categories do **not** get a generated pictogram in MVP; they use the generic “Other” mark (or a Material `Label` fallback) and the Other accent.

### 4.5 System surfaces

Share sheet, camera, and photo picker are OS UI. The app only owns the caption and the thumbnail *before* those sheets open. Do not skin the chooser.

---

## 5. Color, type, and motion

### 5.1 Brand seed

Confirmed. The Material 3 seed is **sealing-wax** `#9A4A32`.

| Role | Hex | Use |
| --- | --- | --- |
| Primary | `#9A4A32` | Save, selected brand chrome, period chips |
| On primary | `#FFF8F3` | Text/icons on Save |
| Surface (paper) | `#F6F1E8` | Screen background |
| Surface container | `#EFE7D8` | Top bar, dialogs, thumb well |
| On surface (ink) | `#2A241F` | Amount, titles, body |
| On surface variant | `#6F675E` | Timestamps, hints |
| Outline | `#C9BBA8` | Unselected chip stroke, dividers |
| Error / over budget | `#9B2F2F` | Validation, delete, budget past 100% |
| Ledger green | `#3F6B45` | Budget under target (meter fill, remaining text) |

Map these through Material 3 roles (`primary`, `onPrimary`, `surface`, `onSurface`, `error`, `outline`). Build the Compose `lightColorScheme` from this table (or from a seed-generated scheme that is then **edited** so surface stays cream, not pink-tinted).

Ledger green is a **brand extension, not a Material role.** Do not map it to `tertiary` or invent a green `ColorScheme`; it is a named constant used by budget surfaces only (§5.4). Both status colors clear WCAG 2.2 AA on paper `#F6F1E8` (green ≈ 5.4:1, red ≈ 6.6:1), so status text is readable, not just decorative.

Do **not** enable `dynamicColor`. Light scheme only.

### 5.2 Per-category accents

Seeded categories each have a distinct **ink** accent. Unselected chip: cream fill, accent-tinted outline + pictogram. Selected chip: accent at ~24% fill, accent outline, ink label. Accents never color the amount.

| Category | Accent name | Hex | Pictogram (see §8.5) |
| --- | --- | --- | --- |
| Food | Terracotta | `#C45C3E` | Coffee cup + saucer |
| Transport | Indigo ink | `#3D5A80` | Simple bus |
| Supplies | Moss | `#5C7A4A` | Open carton |
| Utilities | Ochre | `#C4922A` | Light bulb |
| Personal | Wine | `#8B4D63` | Tote bag |
| Other | Warm stone | `#6F675E` | Asterisk in a soft square |

Custom (user-created) categories: Other accent + Other pictogram.

On the dashboard, a category bar is filled with its accent from this table. Status color never replaces the accent — it only paints the over-target overflow segment (§5.4).

### 5.3 Type

- Amount: largest type on Log (`displaySmall` or `headlineLarge`). Prefer tabular/lining figures so the value does not jump while typing (`FontFeatureSettings("tnum")` if the platform face supports it).
- Category chips: `labelLarge`; one line; pictogram 18 dp + 8 dp gap + name.
- History rows: amount `titleMedium`; metadata `bodySmall` in on-surface variant.
- No custom downloadable font in MVP.

### 5.4 Budget status color

The one place this app is allowed to speak in green and red. Scope it tightly:

| Surface | Under target | Over target |
| --- | --- | --- |
| Overall meter arc fill | Ledger green `#3F6B45` | Error `#9B2F2F` for the whole arc |
| Meter centre text | Ink `#2A241F` | Error `#9B2F2F` |
| Category bar fill | Category accent (§5.2) | Accent up to the tick, error `#9B2F2F` beyond it |
| Remaining line on Log | On-surface variant `#6F675E` | Error `#9B2F2F` |
| Logged amounts, list rows, period totals | **Ink. Always.** | **Ink. Always.** |

Two states only — under and over. No amber "getting close" band: a third color turns a readout into a traffic light, and the number already says how close you are.

Status is never carried by color alone (WCAG 2.2 §1.4.1): over budget also changes the wording from `430,00 left` to `over by 120,00`, so the meaning survives a grayscale screenshot and a red-green color vision deficiency.

### 5.5 Motion

- IME appearance is the only “entrance.”
- Chip selection: M3 default color/elevation change, no bounce.
- After Save: amount clears immediately; no confetti, no success full-screen.
- Navigation Log → History: standard Compose transition.

---

## 6. Components (Material 3 mapping)

| Piece | Component | Notes |
| --- | --- | --- |
| Amount | Custom field on `BasicTextField` / outlined field | Numeric IME; formatted as digits arrive; no heavy box — a baseline or light outline on paper |
| Categories | `FilterChip` in `FlowRow`, radio | Selected chip is not toggle-off; leading pictogram |
| Add category | `FilterChip` or `AssistChip` with `+` | Opens dialog; no custom art |
| Save | `Button` (filled, primary) | Disabled when amount empty |
| Save & Share | `FilledTonalButton` | Same disable rule; cream-tonal, sealing-wax label |
| Camera / gallery | `IconButton` | Material Symbols; content descriptions required |
| Receipt thumb | Small rounded image + remove | Coil; private file path; 8 dp corners |
| History row | Compact row | Pictogram 20 dp; receipt indicator is Material `photo` |
| Period | `FilterChip` | Day / week / month; brand seed, not category color |
| Top bar | `TopAppBar` | Wordmark/glyph 24 dp + title; Dashboard action on Log, back arrow on Dashboard |
| Remaining line (Log) | `Text`, `bodySmall` | One line, ellipsized; §5.4 color; not tappable; absent when no target |
| Budget meter | Compose `Canvas` arc | Rounded caps, ~12 dp stroke, track `outline`; 48 dp hit box; opens the target dialog |
| Category bar | Compose `Canvas` row | 8 dp rounded track, accent fill, 1 dp ink target tick; whole row is the 48 dp hit box |
| Target dialog | `AlertDialog` | Title = category name or "Monthly budget"; reuses the Log amount field; empty = clear |

---

## 7. Accessibility and locale

- Amount field and both save actions announced clearly; camera/gallery are not unlabeled glyphs.
- Contrast: ink on paper and white-on-primary must meet WCAG 2.2 AA. Selected chip fill is tinted — **label stays ink** (`#2A241F`), not white on ochre.
- Currency strings can grow; the amount must shrink or wrap *down*, never overlap chips.
- Touch targets 48 dp; chips can be visually shorter if the hit box is not.
- Pictograms are decorative next to visible labels. `contentDescription` null on the image; the chip label is enough. Launcher icon is the exception (system labeled).

---

## 8. Assets to generate

Generate **only** the set below. One family: warm stationery, even ink line, cream/wax/ink palette, no photoreal people, no text baked into images.

**Masters (generated):** [`assets/`](../assets/). Raster PNGs are the source. When `:app` exists, trace pictograms and the toolbar glyph to XML vectors in `res/drawable/`, and slice the launcher into adaptive mipmaps. The GitHub still stays in this folder and in the README — it is not shipped in the APK.

Shared prompt fragment (prepend to every asset):

```
Warm stationery, paper and iron-gall ink, even vector stroke with slightly rounded ends,
palette cream #F6F1E8, sealing-wax #9A4A32, ink #2A241F, no photorealism, no gradients,
no drop shadows, no UI chrome, no letters unless the prompt asks for a wordmark.
```

### 8.1 Adaptive launcher icon

| | |
| --- | --- |
| **Why** | The only brand the user sees before the keyboard. Must read at 48 dp and on a circular mask. |
| **Deliverables** | Foreground: abstract folded receipt glyph (safe zone). Background: cream `#F6F1E8` or a slightly deeper paper `#EFE7D8`. Optional monochrome (ink on transparent) for themed icons. Adaptive mipmaps. |
| **Do not** | Tiny receipt photos, unreadably small “QL” ligatures, a full wordmark, a wallet or bank building. |
| **File** | [`assets/launcher-receipt-fold.png`](../assets/launcher-receipt-fold.png) |
| **Prompt** | `App launcher icon, adaptive Android, centered abstract folded paper receipt (one dog-eared rectangle with a single crease and two or three tally lines), warm stationery ink-line, cream background #F6F1E8, sealing-wax #9A4A32 stroke, generous padding for circular crop, 1024×1024, flat vector, no text, no coin, no card reader.` |

### 8.2 Wordmark / in-app mark (top bar)

| | |
| --- | --- |
| **Why** | Ties Log’s thin top bar to the launcher without a second art style. |
| **Deliverables** | The same receipt-fold glyph as 8.1, simplified to 24 dp. Single-color `currentColor` / ink. XML vector. Optional tiny “QuickLogger” is **system type**, not drawn in the asset. |
| **Do not** | A wide logo that crowds out the History action. |
| **File** | [`assets/toolbar-receipt-glyph.png`](../assets/toolbar-receipt-glyph.png) |
| **Prompt** | `Minimal 24 dp Android toolbar glyph: the same folded receipt as the launcher, single color (black on transparent), high-contrast silhouette, no wordmark, no tagline, even ink stroke, optical size 24.` |

### 8.3 Empty History illustration

| | |
| --- | --- |
| **Why** | The only place a picture can add warmth without slowing logging. |
| **Deliverables** | One light illustration (~320 dp wide), lots of paper-negative space. Vector or PNG with cream already in the scene. Copy lives in `strings.xml`, not in the image. |
| **Do not** | Checklists, coins raining, 3D clay, dark variant (light-only app). |
| **File** | [`assets/empty-history.png`](../assets/empty-history.png) |
| **Prompt** | `Quiet empty-state illustration: a small folded paper receipt lying on a cream desk next to a thin closed ledger, warm stationery, ink-line, palette #F6F1E8 #9A4A32 #2A241F, lots of whitespace, no people, no text, no UI chrome, suitable for an Android empty list.` |

### 8.4 README / GitHub social preview (repo only)

| | |
| --- | --- |
| **Why** | Sideloaded APK still needs a recognizable storefront on GitHub. Not shipped in the APK. |
| **Deliverables** | One 1280×640 still: phone frame with Log, huge cream canvas, amount focused, six chips with pictograms. No fake store badges. |
| **Do not** | Invent extra tabs, a cloud-sync banner, or a dark theme. |
| **File** | [`assets/github-social-preview.png`](../assets/github-social-preview.png) |
| **Prompt** | `Product still for GitHub: Android phone showing a minimal cream-paper expense logger, huge currency amount focused, six stationery category chips with small ink pictograms, warm Material UI, sealing-wax save button, no smiling humans, no cloud icons, 1280×640.` |

### 8.5 Category pictograms (six)

| | |
| --- | --- |
| **Why** | You asked for a custom set so chips scan as a kit, not generic Material food/bus icons. |
| **Deliverables** | Six XML vectors, 24×24 viewport, **single color** (they are tinted at runtime to the category accent). Optical size 18–20 dp. Matching stroke. |
| **Do not** | Filled colorful stickers, emoji, brand logos, letters. |
| **Shared prompt** | `Android vector icon 24×24, single-color ink line, even stroke, rounded caps, no fill (or minimal), centered, transparent background, stationery, no text.` |
| **Files** | [`ic-category-food.png`](../assets/ic-category-food.png) · [`ic-category-transport.png`](../assets/ic-category-transport.png) · [`ic-category-supplies.png`](../assets/ic-category-supplies.png) · [`ic-category-utilities.png`](../assets/ic-category-utilities.png) · [`ic-category-personal.png`](../assets/ic-category-personal.png) · [`ic-category-other.png`](../assets/ic-category-other.png) |
| **Per icon** | **Food:** coffee cup on a saucer, tiny steam curl. **Transport:** front three-quarter of a small city bus. **Supplies:** open cardboard carton. **Utilities:** light bulb with a simple filament. **Personal:** tote bag with two handles. **Other:** asterisk centered in a rounded square. |

Custom user categories reuse **Other**.

---

## 9. Success criteria

This design is doing its job when:

- [ ] Log still matches ARCHITECTURE §8.1 (focus, chips, optional receipt, two save actions).
- [ ] `lightColorScheme` matches §5; dynamic color is off; dark theme is absent.
- [ ] Each seeded category chip shows its pictogram and accent; selected state stays readable (ink label).
- [ ] Launcher fold, top-bar glyph, empty state, and six pictograms look like one ink family.
- [ ] No generated asset is required to complete a save.
- [ ] With no targets set, Log and the dashboard look exactly as they did before sprint 7 — no empty meter, no prompt.
- [ ] Green and red appear only on budget surfaces (§5.4). No logged amount is ever tinted.
- [ ] Over-budget is legible in grayscale, because the wording changes too.
- [ ] Implementers can theme Compose from this file without inventing a second palette.

---

## 10. Decisions and leftovers

### Decided

1. Personality: warm stationery (paper, ink, receipt).
2. Color: fixed brand seed, not Material You.
3. Category color: distinct accent per seeded category (§5.2).
4. Theme: light only.
5. Launcher mark: abstract receipt fold.
6. Empty History: generate §8.3.
7. Category pictograms: generate the six in §8.5; chrome icons stay Material.
8. Amount type: platform M3 (not asked; assumed).
9. No Figma required for MVP.
10. Beauty must not add taps, routes, or a splash delay.
11. Brand seed is `#9A4A32` (confirmed).
12. Raster masters for §8 are in `assets/` (2026-08-17).
13. Budget status may use green and red (§5.4), overriding the original blanket "never green/red" rule. The override is scoped to budget surfaces; logged amounts stay ink. Approved 2026-08-18.
14. Ledger green `#3F6B45` is a brand constant, not a Material role. No green `ColorScheme`.
15. The meter and category bars are hand-drawn Compose `Canvas`. No charting dependency.
16. Second page is the **Dashboard**: budget overview above the existing list, not a replacement for it.
17. Targets are set by tapping the meter or a bar (dialog), not on a settings screen.

### Still open

- **Amount typeface.** Assumed platform M3. Say if you want a bundled tabular font in `res/font`.

### Sprint 6 implementation notes

- **Vector trace (resolved).** The launcher fold, toolbar glyph, six pictograms, and the empty-History illustration are hand-drawn `VectorDrawable`s in `res/drawable/` (`ic_launcher_foreground`, `ic_launcher_monochrome`, `ic_toolbar_receipt`, `ic_category_*`, `ic_empty_history`) rather than a pixel trace of the `assets/` PNGs — same silhouettes and line weight, redrawn as clean path data.
- **Camera/gallery deviation from §6's "Material Symbols."** `material-icons-core` has no camera or gallery glyph; `material-icons-extended` would add a large dependency for two icons. `ic_action_camera` / `ic_action_gallery` are hand-drawn in the same ink-line family instead — arguably a closer match to "one ink family" (§8) than mixing in a filled Material Symbol would have been. Delete, History, and the top-bar list action stay Material as specified.
- **Category → style mapping.** `Category` has no color/icon column (out of this sprint's scope), so `presentation/theme/CategoryStyle.kt` maps by the fixed seed name (`Food`, `Transport`, …) and falls back to Other's accent/pictogram for anything else, including every custom category — matching §4.4 by construction.
