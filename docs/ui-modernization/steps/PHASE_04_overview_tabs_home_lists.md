# Phase 04 — Overview: Tab Switcher, Home Tiles, Bookmarks, History

> Goal: replace the legacy `dialog_overview.xml` bottom sheet with a modern full-height Compose sheet —
> tab cards, editable home tiles, bookmarks & history lists with sort/filter — while the browser shell
> underneath is still the old XML (it flips in P05).
> Effort: 6–8 days · Depends on: P02 (TabManager/RecordsRepository), P01 components · Ships: **release M1.5**

## Layout (from MASTER_PLAN §4.2)
```
ModalBottomSheet (skipPartiallyExpanded, drag-to-dismiss)
 ├─ Search pill            → submits query via UrlInterpreter into current/new tab
 ├─ BBSegmentedRow         [Tabs] [Home] [Saved] [History]        (+ ⚙ settings icon right)
 ├─ content pager:
 │   Tabs    → LazyVerticalGrid of TabCard(favicon/title/close) + “+” tile; swipe-to-dismiss
 │   Home    → LazyGrid of BBIconTile tiles (long-press = edit/remove menu) + StartPage list rows
 │   Saved   → bookmark list rows (color dot, title, url) ; sort menu; color-filter chips
 │   History → date-grouped list; per-item delete; clear-all
 └─ footer: adaptive banner slot (wired properly in P08; placeholder box until then)
```

## Steps

### 4.1 OverviewViewModel
- [ ] State: `tabs: List<TabState>`, `activeId`, `homeTiles`, `bookmarks(sorted,filtered)`, `history`,
      `segment`, `query`; intents call `TabManager` / `RecordsRepository`.
- [ ] Sort prefs read from `sort_startSite` / `sort_bookmark` keys (values `ordinal|title|time`) to stay
      compatible until P10.

### 4.2 Sheet host
- [ ] New composable `OverviewSheet(onDismiss)` shown by the **legacy** toolbar button initially
      (legacy `bottomSheetDialog_OverView` is bypassed): keep `showOverview()` API but present Compose.
- [ ] Preserve start-tab behavior (`start_tab`: home/bookmarks/history/tabs) and overview-on-start pref.
- [ ] Keep shortcut deep-links working: `sc_history/sc_bookmark/sc_startPage` open the sheet pre-selected
      on the right segment.

### 4.3 Tabs segment
- [ ] `TabCard`: favicon tile, single-line title, close button; active card highlighted; drag-reorder later
      (backlog), swipe-dismiss now; tap activates + closes sheet; “New tab” card opens start page tab.
- [ ] Session restore UI hook: on cold start with saved sessions (P02 groundwork), show restored tabs.

### 4.4 Home segment
- [ ] Render DB start-site records (GRID table) as tiles **and** the 8 SharedPreferences quick tiles
      (`{i}_name/{i}_url`) in one grid, visually grouped (“Pinned” vs “Quick links”) until P07 unifies them.
- [ ] Long-press tile → edit dialog port (`dialog_edit_homescreen` equivalent) writing same keys.
- [ ] Gamezop banner replaced by a proper `GamesCard` row here (full hub lands P07).

### 4.5 Saved (bookmarks)
- [ ] Rows show `ColorDotPicker`-compatible dot from legacy `TIME` hack (codes 1–11) — read via repository’s
      `colorHint`.
- [ ] Overflow menu per screen: sort (title/time), filter-by-color (chips of enabled colors from
      `filter_01..11`), clear-all (ConfirmSheet). Item long-press: open fg/bg, delete, edit (title/color)
      — ports `show_contextMenu_list`.

### 4.6 History
- [ ] Grouped by day (formatting via `SimpleDateFormat("MMM dd")` parity), tap loads in current tab,
      long-press context (open new tab, delete), header action clear-all.
- [ ] Respect incognito/saveHistory semantics (nothing new recorded when off).

### 4.7 Autocomplete parity fix (B12)
- [ ] Omnibox search field inside sheet uses `UrlInterpreter` + repository suggestions rendered as typed
      list items (`Record` models) — no hidden-TextView plumbing.

### 4.8 Legacy deletion (sheet only)
- [ ] Remove `initOverview()` bottom-sheet code path once Compose sheet passes QA; keep `dialog_overview.xml`
      file dormant until P11 cleanup. Delete `CustomGridViewAdapter` inner class usage.

## Acceptance criteria
- [ ] All four segments feature-complete vs §7.3 inventory; no dead ends (every old action has a home).
- [ ] Open latency ≤150 ms p95 on mid-range device (measure with `adb shell am start`… + manual stopwatch/logcat).
- [ ] Swipe-dismiss, back-gesture, and IME resize behave; sheet never covers more than intended in landscape.
- [ ] Ad revenue instrumentation unchanged this phase (placeholder footer logs event only).

## Rollback
Re-enable legacy `initOverview()` path behind a BuildConfig flag kept during the phase.
