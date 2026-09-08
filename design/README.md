# BlackBrowser Design System — “Obsidian & Aurora”

The complete, fresh UI language for the v2 rewrite. Implemented in code at
`app/src/main/java/com/saigopal/browser/ui/design/` — this folder is its human-readable spec.

## Concept

BlackBrowser is named *black*, so the surfaces behave like **stone**: polished obsidian in dark
mode (never pure black — that is AMOLED’s job), cool porcelain in light mode. Life enters through
**two accent temperatures**, so the system never collapses into one-accent flatness:

| Role | Name | Dark value | Light value | Used for |
|---|---|---|---|---|
| Primary | **Aurora mint** `#7FDCC7` / `#00685A` | mint 70 | mint 40 | interactive chrome: switches, links, focus, omnibox icon |
| Tertiary | **Dusk violet** `#C9BFF2` / `#685BA6` | violet 80 | violet 40 | playful surfaces only: Games card, highlights |
| Semantic | **Signal amber** `#FFBE4C` / `#825500` | amber 80 | amber 40 | insecure-HTTP state & destructive confirmations — never decoration |
| Neutral | Obsidian ramp `#0D1312 → #2B3331` | — | porcelain ramp `#F5FAF8 → #DDE4E2` | surfaces |

The legacy brand orange survives exactly once, demoted to the semantic amber above — upgraders
still recognise “caution”, but it no longer owns the identity.

## Signature element — the aurora edge

One memorable thing, spent deliberately: a **3 dp gradient hairline (mint → sky → lavender)
across the top of the omnibox pill that doubles as the page-load progress bar.**

- Idle: hidden.
- Loading: sweeps gently; sweep fraction = load fraction (real progress clamps from below).
- Reduced motion: static bar, no loop.

Everything else in the system stays quiet M3 discipline. Code: `components/BBOmniboxPill.kt`.

## Typography

System faces only (keeps the APK lean and text-zoom legibility intact). Personality comes from
treatment:

- Display/titles: SemiBold, tight tracking (−0.1…−0.2 sp).
- **Eyebrow labels**: uppercase `labelMedium`, +1.4 sp tracking, primary colour — used ONLY to
  mark real section groupings (settings sections, overview segments). Structure is information.
- Counters (tab chip, blocked count): tabular figures (`tnum`) so digits don’t jiggle.

## Shape, spacing, motion

- Pills: full capsule (`RoundedCornerShape(50%)`). Cards: 20 dp. Sheets: M3 28 dp.
- Spacing scale: 4 · 8 · 12 · 16 · 24 · 32 · 48 (`BBSpace`); min touch target 48 dp everywhere.
- Motion: 140 ms controls · 200 ms standard · 260 ms sheets; aurora loop 1200 ms.
  All loops respect reduced-motion.

## Themes

`BBTheme(mode)` with four modes mapping the legacy `sp_theme` values verbatim
(`0` system · `1` light · `2` dark · `3` amoled) — old installs migrate with zero code.
AMOLED = absolute `#000000` surfaces with hairline containers instead of elevation tint.
Material You dynamic colour available on Android 12+ behind a setting (default off — brand first).

## Component kit (`ui.design.components`)

| Component | Purpose |
|---|---|
| `BBOmniboxPill` + `AuroraEdge` + `rememberAuroraEdgeFraction` | collapsed address pill w/ signature edge |
| `BBTextFieldPill` | the only input shape (omnibox edit, search, whitelist add…) |
| `BBIconTile` / `LetterTile` | favicon tile w/ deterministic offline letter fallback |
| `BBListItem` | 56 dp list row (leading slot, title+subtitle, trailing slot) |
| `SwitchRow` | toggle row — whole row is the target |
| `BBSegmentedRow` | overview rail / exclusive choices |
| `BBConfirmSheet` | the one confirmation sheet (destructive variant included) |
| `SectionHeader`, `EmptyState`, `BBCard`, `BBIconButton` | structural basics |
| `ColorDotPicker` + `BBDotColor` | bookmark colours; legacy codes 1–11 preserved as data |

Preview everything live via the debug gallery:
```
adb shell am start -n com.saigopal.browser/.ui.design.gallery.DesignGalleryActivity
```

## Screen blueprints

See `docs/ui-modernization/UI_REWRITE_MASTER_PLAN.md` §4 for wireframes (browser shell,
overview sheet, page menu, settings graph, start page). This system is the material those
screens are built from.
