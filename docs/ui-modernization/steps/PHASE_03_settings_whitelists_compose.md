# Phase 03 — Settings, Whitelists & Small Screens in Compose (first user-facing Compose)

> Goal: ship the first visible Compose surfaces — the full settings graph (7 legacy screens), the 4
> whitelists as one parameterized screen, About/Help dialogs, and Feedback. Lowest-risk, highest-familiarity
> screens make the perfect Compose beachhead. Legacy settings activities are hidden (not deleted).
> Effort: 5–7 days · Depends on: P01, P02 · Ships: **release M1**

## Screen map

| New route | Replaces |
|---|---|
| `settings` | `SettingsActivity` + `FragmentSettings` (`preference_setting.xml`) — minus its AdView banner moves to P08 policy |
| `settings/appearance` | `SettingsUIActivity` / `preference_ui.xml` (theme/font/invert/toolbar/nav-position/overview prefs) |
| `settings/privacy` | `SettingsStartActivity` / `preference_start.xml` (images/save-data/history/location + 4 toggles+whitelists) |
| `settings/gestures` | `SettingsGestureActivity` / `preference_gesture.xml` |
| `settings/data` | `SettingsDataActivity` / `preference_data.xml` (backup/export/import) |
| `settings/clear` | `SettingsClearActivity` / `preference_clear.xml` (+ delete-database action) |
| `settings/filter` | `SettingsFilterActivity` / `preference_filter.xml` (bookmark color filters) |
| `whitelist/{kind}` ×4 | `WhitelistAdBlock/Javascript/Cookie/Remote` (one screen, `kind∈{adblock,js,cookie,remote}`) |
| `feedback` | `FeedbackActivity` (same RTDB schema `Feedback/{Type}/{push}{Time,Feedback,Email}`) |
| `about` sheets | Help + Info/license bottom-sheets from `FragmentSettings.showLicenseDialog` |

## Steps

### 3.1 Scaffold & root list
- [ ] `SettingsViewModel(container)` exposing `settings: StateFlow<BBSettings>` and `update()` helpers.
- [ ] Root screen: large-title header + search field that **filters all settings** (flat index of
      `(route,title,keywords)`), sections: Browser, Privacy & security, Appearance, Behavior, Gestures,
      Data, About. Items navigate; toggles flip inline.
- [ ] Every change writes via `SettingsRepository.update` → applies live (no `restart_changed`; B11 fixed).

### 3.2 Section screens (map every legacy pref 1:1)
- [ ] **Appearance:** theme selector (System/Light/Dark/AMOLED radio-row using `BBThemeMode`),
      Material You dynamic-color switch (S+, default off — decision D2), font size slider (75–175,
      snap points of legacy array), “Invert web content” switch (kept until P09 replaces semantics),
      hide-toolbar-on-scroll, show-toolbar-on-back, nav-button position (Right/Left/Center/Hidden),
      overview-on-start, default overview tab.
- [ ] **Browser (root section):** favorite URL text row, search engine picker (DDG/Google/Bing/Baidu/
      Searx/Qwant/custom) + custom prefix field, user-agent override field w/ reset, desktop-mode toggle
      (label “Desktop sites (beta)” preserving today’s copy), start-page control entry, clear-control entry.
- [ ] **Privacy:** images, save-data header, save-history, location master switch; AdBlock/JS/Cookies/
      Remote groups each = switch + chevron into its whitelist; third-party cookie note copy.
- [ ] **Behavior:** confirm-exit, confirm-tab-close, reopen-last-tab.
- [ ] **Gestures:** enable switch + two groups (Toolbar swipes, Nav-button swipes) each with 4 direction
      pickers listing the 10 actions (codes preserved: `"01".."10"`); show live preview hint line.
- [ ] **Data:** export/import app data, export/import bookmarks (SAF groundwork may defer actual file IO to
      P10 — if so, keep old activities reachable via an “Advanced (legacy)” link until then).
- [ ] **Clear:** cache/history/indexedDB/cookies checkboxes, clear-on-quit, destructive Delete-database row
      using `ConfirmSheet`.
- [ ] **Filter:** the 11 color filter switches (`filter_01..11`) with `ColorDotPicker`.

### 3.3 WhitelistScreen(kind)
- [ ] One generic screen: title/icon per kind, add-domain field with validation (`UrlInterpreter.isUrl`
      domain mode), list with per-item remove icon, empty state, menu: Clear-all (ConfirmSheet),
      Export txt, Import txt (file IO can reuse existing tasks initially; SAF swap in P10).
- [ ] Backing calls go through `WhitelistRepository(kind)` which delegates to existing
      `AdBlock/Javascript/Cookie/Remote` classes (single implementation, four tables — deletes ~600 LOC of copies).

### 3.4 Feedback screen
- [ ] Compose form: type radios (Bug/Idea/Other — match RTDB node names exactly), message (≥15 chars rule),
      optional email checkbox + field with same regex validation, send → RTDB path identical; success/error snackbar.

### 3.5 About / Help / Licenses
- [ ] Bottom-sheet style screens rendering the same strings (`dialogHelp_tipTitle/Text`, changelog) with
      working links (Linkify equivalent via `ClickableText` or `AndroidView` TextView for simplicity).

### 3.6 Entry points rewiring
- [ ] Overview gear icon (legacy XML) now opens new `settings` route (Compose) instead of `SettingsActivity`.
- [ ] Fast-toggle font row opens Appearance route.
- [ ] Keep old activities registered but unreachable except via the Data “legacy” link (removed in P10/P11).

### 3.7 Ads note
- [ ] The Settings banner (`activity_settings.xml` R.id.adView) disappears with this screen. Per placement
      policy decision (P08 step 8.6): either re-add inside new Settings footer via `AdsController` (recommended:
      yes, parity) or accept revenue change consciously. Implement the footer banner in this phase only if
      `AdsController` exists; else temporarily open old SettingsActivity for ads-parity and revisit in P08.
      Default plan: implement minimal `BannerAd(androidView)` wrapper here (30 min) and reuse in P08.

## Acceptance criteria
- [ ] Every setting from Appendex A list present & functioning; side-by-side audit against old screens signed off.
- [ ] Theme switch reflects instantly across new screens (and legacy ones on next open — acceptable interim).
- [ ] Whitelist CRUD + import/export verified per kind; ad-block actually unblocks a test domain after whitelisting.
- [ ] Feedback posts to RTDB with identical node shape (verify in Firebase console).
- [ ] TalkBack labels on all new rows; min touch target 48dp.

## Rollback
Flip entry points back to legacy activities (constants in one file). No data changes.
