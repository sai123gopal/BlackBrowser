# BlackBrowser — Modern UI Rewrite & App Modernization — MASTER PLAN

> **Product:** BlackBrowser (`com.saigopal.browser`) — Simple, lightweight, privacy-first Android browser
> **Current:** v1.5.43 (versionCode 21) · Java + XML Views · minSdk 22 · target/compileSdk 35 · AGP 8.3.1
> **Target:** Same product promise, rebuilt UI in **Kotlin + Jetpack Compose + Material 3**, cleaned-up internals, same-or-less APK weight
> **Date:** 2026-08-24 · Status: Planning approved for execution

---

## 0. This document set

One master plan (this file) + 12 step-by-step migration files. Execute phases **in order**; each phase ships a runnable APK.

| # | File | Scope |
|---|------|-------|
| 00 | [`steps/PHASE_00_foundation_and_tooling.md`](steps/PHASE_00_foundation_and_tooling.md) | Kotlin, Compose toolchain, version catalog, baselines |
| 01 | [`steps/PHASE_01_design_system_theming.md`](steps/PHASE_01_design_system_theming.md) | Material 3 design system, themes (System/Light/Dark/AMOLED), shared components |
| 02 | [`steps/PHASE_02_architecture_refactor.md`](steps/PHASE_02_architecture_refactor.md) | Split `MainActivity`, MVVM + repositories, navigation skeleton, DI container |
| 03 | [`steps/PHASE_03_settings_whitelists_compose.md`](steps/PHASE_03_settings_whitelists_compose.md) | All 7 settings screens + 4 whitelists + About/Help in Compose (first Compose surfaces) |
| 04 | [`steps/PHASE_04_overview_tabs_home_lists.md`](steps/PHASE_04_overview_tabs_home_lists.md) | Overview panel: tab grid, home tiles, bookmarks, history, sort/filter |
| 05 | [`steps/PHASE_05_browser_shell_omnibox_webview.md`](steps/PHASE_05_browser_shell_omnibox_webview.md) | Browser screen: bottom omnibox, progress, scroll-hide, find-in-page, fullscreen video, gestures |
| 06 | [`steps/PHASE_06_menus_dialogs_fast_toggles.md`](steps/PHASE_06_menus_dialogs_fast_toggles.md) | Page menu redesign, link/list context menus, fast-toggles, SSL/auth/download dialogs |
| 07 | [`steps/PHASE_07_native_start_page_gamezop.md`](steps/PHASE_07_native_start_page_gamezop.md) | Native start page (retires `Newtab.html`), editable tiles, Gamezop Games hub |
| 08 | [`steps/PHASE_08_ads_monetization_consent.md`](steps/PHASE_08_ads_monetization_consent.md) | AdMob in Compose with correct lifecycles, UMP consent, placement policy |
| 09 | [`steps/PHASE_09_privacy_hardening.md`](steps/PHASE_09_privacy_hardening.md) | Per-session incognito, geolocation fix, HTTPS-first, cleartext policy, storage modernization |
| 10 | [`steps/PHASE_10_data_layer_room_datastore.md`](steps/PHASE_10_data_layer_room_datastore.md) | Room over `data.db`, DataStore for prefs, SAF-based backup/restore |
| 11 | [`steps/PHASE_11_polish_performance_release.md`](steps/PHASE_11_polish_performance_release.md) | R8, baseline profiles, tests, cleanup of legacy XML/Java, staged rollout |

---

## 1. Executive summary

BlackBrowser works, but its entire UI is hand-built View/XML driven by one 2,407-line `MainActivity`
that mixes UI, business logic, database access, ad lifecycle and gesture routing. The UI is functional
but dated (Material Components 1.x, bottom-sheet-everything, a WebView-rendered HTML start page),
and several monetization/privacy behaviors have latent bugs (AdView destroyed then re-used, geolocation
auto-granted, global-only incognito).

This plan rewrites **100% of the user-facing UI in Jetpack Compose (Material 3)** behind a thin,
testable MVVM core, while **preserving every existing user-facing feature** (full inventory in §7).
It deliberately keeps the app *simple and lightweight*: single module, no Hilt, no heavy new deps,
strict size/startup budgets (§12), and an explicit non-goals list (§2.2).

**Execution model:** strangler pattern. New Compose screens mount inside the existing activities first;
legacy XML is deleted only in Phase 11. Every phase ends green-buildable and releasable.

**Estimated total effort:** ~55–75 engineer-days for one developer who knows the app (phases range 2–10 days).

---

## 2. Product goals & guardrails

### 2.1 Goals
1. **Modern UI** — Material 3, edge-to-edge, dynamic color (optional), smooth motion, one-hand friendly bottom-anchored controls.
2. **Simple** — every core action reachable in ≤2 taps; uncluttered browsing canvas.
3. **Lightweight** — APK ≤ 8 MB, cold start ≤ 1.2 s on mid-range hardware, RAM ceiling unchanged (drop `largeHeap`).
4. **Feature-complete** — nothing users have today disappears (§7 is the contract).
5. **Private by default** — ad-block on, history control, per-domain whitelists, honest incognito, no silent permission grants.
6. **Maintainable** — Kotlin, MVVM, repositories, typed settings (DataStore), Room; `MainActivity` shrinks from 2,407 lines to a thin host (<300 lines).

### 2.2 Non-goals (explicitly out of scope)
- No Chromium/Gecko fork, no extensions/add-on system, no account/sync, no news feed, no PWA install flow,
  no custom download engine (system DownloadManager stays), no multi-module refactor beyond packaging.
- No redesign of the ad *business model* — banners remain the primary unit; new formats are opt-in decisions (§14).

---

## 3. Current-state audit (verified against source)

### 3.1 Stack snapshot

| Area | Today |
|---|---|
| Language | 100% Java (~6.8k LOC), no Kotlin plugin |
| UI | XML layouts + `BottomSheetDialog`s + `GridView`/`ListView` + Material Components **1.11 (M2)**, 4 XML themes |
| Architecture | God-class `MainActivity implements BrowserController`; static `BrowserContainer`; direct SQLite + SharedPreferences everywhere |
| Engine | System `WebView` via `SWebView` (+`SWebViewClient`, `SWebChromeClient`, `SDownloadListener`, `SClickHandler`, `SGestureListener`) |
| Data | SQLite `data.db` v4 — tables `HISTORY, BOOKMARK, GRID, WHITELIST, JAVASCRIPT, COOKIE, REMOTE, TAB`(unused); giant default `SharedPreferences` file |
| Ads | `play-services-ads 23.5.0`, banner `…/5437849349` in overview sheet + Settings; `MobileAds.initialize` duplicated in 2 activities |
| Games | Gamezop banner (`img_games`) → `https://10788.play.gamezop.com/` via Custom Tabs or in-app tab |
| Firebase | Analytics, Crashlytics, Perf, RTDB (feedback form), In-App Messaging, Storage dep, `play-services-auth` (sign-in dead code) |
| Images | Picasso (only for favicon fetches from `google.com/s2/favicons`) |
| Build | Groovy DSL, no version catalog, `minifyEnabled false` (!), Jetifier on, `largeHeap`, `usesCleartextTraffic=true`, `requestLegacyExternalStorage` |

### 3.2 Screen inventory (all to be rebuilt in Compose)

| Surface | Implemented by today | Notes |
|---|---|---|
| Browser screen | `activity_main.xml` + `activity_main_content.xml`: bottom appBar w/ omnibox pill, progress bar, FAB-style nav button (position configurable) | hide-on-scroll, tap-title-to-edit |
| Overview sheet | `dialog_overview.xml` in `BottomSheetDialog`: title+search, 8-tile home grid, Gamezop banner, tabs list / start-page list / bookmarks list / history list, bottom icon rail (Tabs/Home/Bms/Hist/Settings), AdView docked bottom | center of the app |
| Page menu | `dialog_menu_overflow.xml`: 4 category FABs (Tab/Share/Save/Other) swapping `GridView`s + incognito & refresh row | |
| Fast toggles | `dialog_toggle.xml` sheet: JS/AdBlock/Cookies/Remote switches + whitelist indicators, history/location/images/invert/font, reload | long-press menu button |
| Context menus | link long-press sheet; bookmark/history/home item sheet (open fg/bg, delete, edit+color icon picker) | |
| Dialogs | confirm/action sheet (`dialog_action`), edit title, edit homescreen tile, save-as filename+extension, SSL error, HTTP auth, form resubmission, progress | all bottom-sheets/AlertDialog |
| Settings | `SettingsActivity` + 6 sub-activities hosting `PreferenceFragmentCompat` XML screens (setting/start/ui/gesture/filter/clear/data) | AdView in Settings root |
| Whitelists ×4 | AdBlock / JavaScript / Cookie / Remote — near-identical `ListActivity` CRUD + txt export/import | prime candidates for one parameterized screen |
| Feedback | `FeedbackActivity` → Firebase RTDB (`Feedback/{Type}`) with optional email | |
| Start page | `assets/Newtab.html` (Bootstrap CDN!, JS bridge `Android.Search/sign/feedback`) rendered in a tab | replaced in Phase 07 |

### 3.3 Findings — bug & debt ledger (fix scheduled in brackets)

| # | Finding | Where | Sev |
|---|---|---|---|
| B1 | `hideOverview()` calls `mAdView.destroy()`; next `showOverview()` calls `loadAd()` on the destroyed view → leaked/crash-prone ad lifecycle | `MainActivity.java:470-482` | High [P08] |
| B2 | Geolocation prompt **auto-grants** origin (`callback.invoke(origin, true, false)`) after runtime permission | `SWebChromeClient.java:53-58` | High [P09] |
| B3 | `KEYCODE_MENU` case falls through (missing `break`) so menu key also runs back-press logic | `MainActivity.java:407-409` | Med [P05] |
| B4 | Incognito is a single global flag (`IsIncognito`), not per-tab/session; cache & webstorage survive | `MainActivity`, `SWebViewClient` | High [P09] |
| B5 | Desktop mode forces Chrome/**85** Windows UA globally on every navigation and is passed `enabled=true` regardless of setting | `SWebView.setDesktopMode`, `SWebViewClient.handleUri` | Med [P09] |
| B6 | Bookmark “color” stored in the `TIME` column (values 1–11); filter compares raw times — schema abuse | `RecordAdapter`, `RecordAction.listBookmark` | Med [P10] |
| B7 | SQL built by string concat in `deleteDomain/deleteURL` (local-only data, still bad practice) | `RecordAction.java:221-274` | Low [P10] |
| B8 | Legacy `initBookmarkList()` migration loops a never-closed cursor and calls `deleteDatabase` inside loop, every launch | `MainActivity.java:1147-1163` | Low [P02] |
| B9 | Backup/import assumes literal `/document/primary:` path rewriting — breaks on many SAF providers | `FragmentSettingsData.ConvertPath`, `BrowserUnit.export*` (hardcoded `/sdcard/BlackBrowser_backup`) | Med [P10] |
| B10 | `release` build has **R8/minify off**, Jetifier on, `largeHeap`, global cleartext, `requestLegacyExternalStorage` | `app/build.gradle`, manifest | Med [P11]/[P09] |
| B11 | Settings changes to search engine/UA require app restart (`restart_changed` dance) | `FragmentSettings` | Low [P03] |
| B12 | Autocomplete passes URLs through a hidden `TextView` (`record_item_time`) — fragile click plumbing | `MainActivity.updateAutoComplete` | Low [P04] |
| B13 | `sp_invert` defaults **true** → Force-Dark web content even in light theme | `MainActivity.onCreate`, `HelperUnit.initRendering` | Med [P09] |
| B14 | Duplicate `filePathCallback` fields; `dispatchIntent` action-string comparisons fragile | `MainActivity.java:200-205,492-540` | Low [P05] |
| B15 | `play-services-auth` + commented-out sign-in JS bridge are dead weight; Storage dep likely unused | `build.gradle`, `WebAppInterface.sign()` | Low [P11] |
| B16 | `TABLE_TAB` created but never used — tabs die on process death (no session restore) | `RecordUnit` | Med [P04+] |

---

## 4. Target experience

**UX principles:** thumb-zone first (all chrome at the bottom), one canvas (content never covered by
permanent chrome), progressive disclosure (advanced stuff behind the page menu), instant feedback
(state changes animate; toggles act immediately), zero restarts (every setting applies live).

### 4.1 Browser screen (Phase 05)
```
┌──────────────────────────────────────┐
│                                      │
│            web content               │   ← edge-to-edge WebView
│                                      │
│                                      │
├──────────────────────────────────────┤   ← auto-hides on scroll down
│ ▌site-title / url pill        ⋮  ▢  │   ← omnibox pill • page-menu • tab-count chip
└──────────────────────────────────────┘     3px animated progress line on top edge of pill
```
- Omnibox = rounded pill (M3 `TextField` in surface-variant), lock/shield icon shows HTTPS + blocked-count.
- Tab-count chip opens Overview. Long-press menu button = Fast Toggles. Progress = thin LinearProgressIndicator overlaid on pill top.
- Address editing focuses inline with history/bookmark autocomplete (typed suggestions, not dropdown hack B12).

### 4.2 Overview (Phase 04)
Full-height `ModalBottomSheet` (drag-to-dismiss preserved):
```
│  Search or type URL            ✕    │
│  [TABS]  Home  Saved  History  ⚙   │  ← segmented tabs w/ animated indicator
│ ╭────╮ ╭────╮ ╭────╮              │
│ │tab1│ │tab2│ │ +  │   ← tab cards │  (favicon, title, close X; swipe to dismiss)
│ ╰────╯ ╰────╯ ╰────╯              │
│         …or tile grid / lists      │
│ 🎮 Play games (Gamezop)      ›     │  ← Games entry card
├────────────────────────────────────┤
│           [adaptive banner]        │  ← single, correctly-lifecycled AdView
└────────────────────────────────────┘
```

### 4.3 Page menu (Phase 06)
Single scrollable M3 menu (no more 4-FAB category switching): New tab, Incognito (toggle w/ state icon),
Find in page, Add bookmark ★, Set favorite, Add to Home, Share, Copy link, Save as / PDF, Shortcut,
Downloads, Settings. Link context menu and list context menus become `DropdownMenu`-style sheets.

### 4.4 Settings (Phase 03)
One Compose settings graph: root list → Browser / Privacy & Security / Appearance / Behavior / Gestures /
Data (backup & clear) / About. Every item applies instantly; search field filters all settings; no restarts (B11).

### 4.5 Start page (Phase 07)
Native Compose screen (retires `Newtab.html` + Bootstrap CDN): brand mark, big centered search field,
editable shortcut tiles (add/remove/reorder, favicon + letter-tile fallback offline), Games card,
subtle footer links. Respects theme (no more black-on-light mismatch).

---

## 5. Target architecture & tech stack

```
app/ (single module, packages only)
  ui/
    browser/   BrowserScreen, BrowserViewModel, Omnibox, FindBar…
    overview/  OverviewSheet, TabCard, HomeTiles, BookmarkList, HistoryList…
    menu/      PageMenu, ContextMenus, FastToggles
    settings/  SettingsRoot + sections, WhitelistScreen(type), FeedbackScreen
    startpage/ StartPageScreen
    components/BBTheme, BBCard, BBTile, BBDialog, ConfirmSheet, IconTile…
  domain/      TabManager, GestureRouter, UrlInterpreter(queryWrapper port), AdBlockEngine…
  data/        db (Room), prefs (DataStore), repo/* , backup/*
  engine/      SWebView (kept), clients, interceptors  ← stays Java/Kotlin non-Compose
  ads/         AdsController (UMP + single-lifecycle banners)
```

| Decision | Choice | Why |
|---|---|---|
| Language | Kotlin for all new code; Java converted opportunistically per-file when touched | incremental, low-risk |
| UI | Jetpack Compose + **Material 3**, BOM-pinned; `AndroidView` interop for WebView & AdView | the rewrite mandate; interop avoids engine rewrite |
| DI | Manual `AppContainer` on `Application` | no codegen bloat; app is small; Hilt rejected for size/build-time |
| Async | Coroutines + `StateFlow` | Compose-native |
| Settings | DataStore Preferences (typed repository), migrated from `SharedPreferences` (key map in P10) | async, no restarts, migration-safe |
| DB | Room over existing `data.db` (schema-preserving migration; adds `COLOR` col fixing B6) | type-safe queries, testable |
| Nav | Navigation Compose, **single Activity** (`MainActivity` becomes Compose host); old activities become routes, deleted P11 | kills 10 activities’ boilerplate |
| Images | Coil (Compose-native) replaces Picasso | smaller integration, Compose painter |
| Ads | `AdsController` singleton: UMP consent → Application-level `MobileAds.initialize` → adaptive banners owned by ViewModel-scoped holder | fixes B1 permanently |
| Engine | Keep system WebView + existing client classes (ported to Kotlin during P05/P09 touches) | the engine is not the problem |
| Tests | JUnit units (URL/search logic), Robolectric DAO tests, Compose UI tests for smoke flows | CI-ready |

Version pins live in `gradle/libs.versions.toml`; exact numbers = **latest stable at execution time**
(examples in PHASE_00; don’t hardcode stale versions from this doc).

---

## 6. Design system (detail in PHASE_01)

- **Color:** M3 schemes generated from brand seed `#FF793F` (current accent) for Light/Dark + **AMOLED scheme** (pure-black `surface`, `surfaceContainer` #0A0A0A). Optional Material You dynamic color on Android 12+ (default **off** initially — brand consistency; setting provided). WebView content follows app theme via `isAlgorithmicDarkeningAllowed` (replaces deprecated Force-Dark path, fixes B13 semantics).
- **Type:** Default Roboto ramp (display/title/body/label); tabular figures for counts.
- **Shape:** pills 999dp; cards 16dp; sheets 28dp top radius (M3 default).
- **Elevation/tonal:** tonal surfaces over shadows; AMOLED uses borders/hairlines instead of elevation tint.
- **Motion:** 200–250 ms emphasized easing; shared-X for omnibox↔edit; sheet fade-through; springy tab-card dismiss.
- **Components to build once (ui/components):** `BBIconTile` (letter/favicon tile), `BBListItem`, `BBSegmentedRow` (overview rail), `ConfirmSheet`, `BBTextFieldPill`, `SectionHeader`, `SwitchRow`, `ColorDotPicker` (bookmark colors 1–11 preserved), `EmptyState`.

---

## 7. Complete feature inventory & disposition (the contract)

Legend: **KEEP** = carry as-is · **MODERN** = same behavior, rebuilt in Compose/M3 · **IMPROVE** = behavior upgraded (note) · **NEW** · **DROP** (user-invisible dead code only)

### 7.1 Core browsing
| Feature | Today | Disposition |
|---|---|---|
| Multi-tab browsing (unlimited, fg/bg open) | `BrowserContainer` + SWebView | MODERN — `TabManager` + Overview grid; add process-death session restore using unused `TAB` table (fixes B16, IMPROVE) |
| URL/search unified omnibox + autocomplete (history+bookmarks+tiles) | AutoCompleteTextView hack | MODERN — typed suggestion list (fixes B12) |
| Search engines: DDG(default)/Google/Bing/Baidu/Searx/Qwant + custom prefix | `BrowserUnit.queryWrapper` | KEEP logic, port to `UrlInterpreter`; live-apply (fixes B11) |
| Google-Play/Plus redirect unwrap | `queryWrapper` prefixes | KEEP (port) |
| Find-in-page (up/down/counter/cancel) | `findAllAsync` panel | MODERN — FindBar with match count |
| Fullscreen video + landscape + keep-screen-on | decor custom-view | MODERN (Compose host window) |
| File upload (file chooser) | `showFileChooser` | KEEP |
| Downloads via DownloadManager + rename/extension (“Save as”) + completion prompt | `BrowserUnit.download`, `HelperUnit.save_as` | KEEP + NEW downloads list screen (queries DM) |
| Print page to PDF | `printPDF` | KEEP |
| Share link / copy link / open-with / new-tab-from-link / save-image-link | context menu | MODERN |
| Home-screen shortcut pinning | `createShortcut` | KEEP |
| Favorite URL (⭐ homepage) | `favoriteURL` | KEEP |
| App shortcuts (start/bookmarks/history) + SEND/VIEW/WEB_SEARCH intents | manifest + `dispatchIntent` | KEEP (regression-test in P05) |
| Gestures: 4 swipes × toolbar & nav-button, actions 01–10 (none/fwd/back/pgUp/pgDown/next/prev tab/overview/new tab/close) | `performGesture` switch | MODERN — same codes, `GestureRouter`; editor UI improved |
| Back behavior: webview-back → close tab → quit-confirm; reopen-last-tab option; toolbar-show-on-back option | `onKeyDown` (bug B3) | MODERN + fix |
| Hide toolbar on scroll-down; nav-button position right/left/center/hidden | prefs | MODERN |
| Desktop mode (beta) | global UA override | IMPROVE — per-toggle from menu + modern Chrome UA, correct param use (B5) |
| User-agent override | pref | KEEP, live-apply |
| Text zoom/font sizes 75–175% | pref | KEEP |
| Third-party context: Custom Tabs fallback for external links (Gamezop/intents) | `isCustomTabAvailable` | KEEP |

### 7.2 Privacy & security (headline features)
| Feature | Disposition |
|---|---|
| Ad blocker (bundled `hosts.txt`) + on/off | KEEP + show **blocked counter** badge (NEW) |
| Per-domain whitelists ×4: AdBlock / JavaScript / Cookies / Remote-content | MODERN — one parameterized screen + export/import |
| JavaScript toggle (+per-site whitelist) | KEEP |
| Cookies toggle + whitelist | KEEP |
| Remote content/storage toggle + whitelist | KEEP |
| Images off (data saver) + `Save-Data` header + `DNT: 1` | KEEP + add `GPC: 1` (NEW, P09) |
| Location permission gate | KEEP + **fix silent grant** (B2) with real site-permission prompt |
| History on/off + history list/delete | KEEP |
| Incognito mode | IMPROVE — per-tab session incognito (visual mask UI), clears on last incognito tab close; documented engine limits |
| Clear-on-exit options: cache/cookies/history/indexedDB (+`ClearService`) | KEEP (service reworked P09) |
| Delete whole database | KEEP |
| SSL error interstitial dialog | KEEP (proceed/cancel), clearer M3 dialog |
| HTTP basic auth prompt | MODERN |
| Form resubmission prompt | MODERN |
| Safe Browsing | KEEP (already on ≥API26) |
| Force-dark web content | MODERN — AlgorithmicDarkening tied to app theme (B13) |
| Cleartext policy | IMPROVE — network security config instead of global `usesCleartextTraffic` (P09) |

### 7.3 Content & organization
| Feature | Disposition |
|---|---|
| Bookmarks: add/edit(title,color-icon)/delete, sort title/time, filter by color group | MODERN — color stored properly (B6 fixed in P10 migration) |
| History: list w/ dates, delete item/all | MODERN |
| Start-page tiles: 8 slots editable name/url, favicon via Google S2 | IMPROVE — unlimited tiles, add/remove/reorder, offline letter-tile fallback (Coil) |
| Overview quick search box | MODERN |
| Overview start-tab choice (home/bookmarks/history/tabs) | KEEP |
| Overview-on-start toggle | KEEP |
| Sort menus (home/bookmarks) | MODERN |

### 7.4 Monetization & growth
| Feature | Disposition |
|---|---|
| Banner ads (overview + settings) | MODERN — adaptive banners, correct lifecycle (B1), Application-level init; Settings banner kept |
| UMP consent (EEA) flow | NEW (required for compliance) |
| Gamezop portal | IMPROVE — native Games card + in-app Games tab (`games` route), adblock-exempt host for that tab, analytics events |
| Firebase Analytics events (screen_view…) | KEEP, event names modernized |
| Crashlytics + Performance | KEEP |
| In-App Messaging | KEEP |
| Feedback form → RTDB (type/text/optional email) | MODERN (Compose screen, same RTDB schema) |
| Dead code: `play-services-auth`, sign-in JS bridge, Storage dep | DROP (B15) |

### 7.5 Platform/appearance
| Feature | Disposition |
|---|---|
| Themes: System/Light/Dark/AMOLED | MODERN — M3 schemes, instant switch, per-theme WebView handling |
| Dynamic color (Material You) | NEW (toggle, default off) |
| Edge-to-edge + inset-aware chrome | NEW (required by targetSdk 35) |
| Predictive back | NEW where compatible (sheets/menus); WebView-back stays classic (platform limit) |
| Backup/restore settings XML, bookmarks Netscape HTML in/out, whitelist txt in/out | MODERN — SAF pickers (B9), same formats for cross-compat |
| Languages | KEEP strings centralized; drop redundant `values-en` duplicate (P11) |

---

## 8. Consolidated change list (grouped)

**Build/tooling** — Kotlin + Compose plugins, version catalog, Compose/M3/activity-compose/navigation-compose/lifecycle/datastore/room(+KSP)/coil deps; remove picasso, play-services-auth, firebase-storage, legacy-preference-v14; Jetifier off; R8 + resource shrinking on with rules; `enableOnBackInvokedCallback`; network security config; drop `largeHeap`/`requestLegacyExternalStorage`/global cleartext; BuildConfig ad-unit placeholders + debug test units.

**UI rewrite** — every surface in §3.2 rebuilt per wireframes §4; new shared component kit; new iconography pass (consistent outline set); motion spec; empty/error/loading states everywhere.

**Architecture** — `AppContainer`; repositories (`RecordsRepository`, `WhitelistRepository`, `SettingsRepository`, `BackupRepository`); `TabManager` + `TabState`; `UrlInterpreter`; `GestureRouter`; `AdsController`; ViewModels per screen; single-activity navigation.

**Data** — Room entities/DAOs for 7 used tables (+session restore); `TIME→COLOR` repair migration; DataStore key migration map (Appendix A); SAF backup/restore; remove legacy `pass_DB_v01` migration path (B8).

**Privacy** — per-tab incognito, site permission prompts (geolocation), HTTPS-first upgrade w/ fallback, GPC header, algorithmic darkening, NSC cleartext policy, receiver-flag audit, scoped-storage backups.

**Quality** — unit/Robolectric/Compose-UI tests; lint re-enabled progressively; baseline profile; Crashlytics gates per phase; perf budgets tracked in CI-lite script.

---

## 9. Roadmap

| Phase | Deliverable (ship-worthy) | Effort | Depends |
|---|---|---|---|
| P00 Foundation | Kotlin+Compose building beside Java; baselines captured | 1–2 d | – |
| P01 Design system | `BBTheme` + component kit; themes demoable | 3–4 d | P00 |
| P02 Architecture | Repos/VMs/TabManager; `MainActivity` logic extracted; nav skeleton | 5–7 d | P00 |
| P03 Settings+Whitelists | First user-facing Compose (settings graph, 4 whitelists, about/help, feedback) | 5–7 d | P01,P02 |
| P04 Overview | Tabs grid/session-restore, home tiles, bookmarks/history, sort/filter | 6–8 d | P02,P03 |
| P05 Browser shell | Omnibox, progress, scroll-hide, find-bar, fullscreen, gestures, intents | 6–8 d | P04 |
| P06 Menus/dialogs | Page menu, context menus, fast toggles, all dialogs; delete legacy sheets | 4–5 d | P05 |
| P07 Start page + Games | Native start page; Games hub (Gamezop) | 3–4 d | P04 |
| P08 Ads + consent | AdsController, UMP, adaptive banners, placements | 3–4 d | P04 |
| P09 Privacy hardening | Incognito/session, geo prompt, HTTPS-first, NSC, storage | 4–5 d | P05 |
| P10 Data layer | Room + DataStore migrations, SAF backup | 5–7 d | P02 |
| P11 Polish/release | R8, baseline profiles, tests, legacy deletion, staged rollout | 4–6 d | all |

Parallelizable: P10 any time after P02; P08 after P04. Milestones: **M1 = P03 (first Compose release)**,
**M2 = P05 (browser fully Compose)**, **M3 = P07+P08 (growth surfaces)**, **GA = P11 (v2.0)**.

---

## 10. Risks & mitigations

| Risk | Mitigation |
|---|---|
| WebView ↔ Compose attachment bugs (view stealing/re-parenting) | One persistent `AndroidView` slot per screen; never recreate WebView on recomposition; `TabManager` owns instances; P05 spike first |
| Ad revenue dip during rollout | Keep placements identical until P08 measured; UMP only gates EEA; monitor ARPU in staged rollout |
| Regression in intents/shortcuts (browser apps live on these) | §7.1 regression checklist executed per milestone; instrumentation test for VIEW/SEND dispatch |
| Data migration corrupting user bookmarks/history | Room migration copies in place (same file); pre-migration auto-backup to app files; beta-channel soak 2 weeks |
| Compose APK size growth | R8+shrinking, baseline profile; budget §12 enforced; Coil only (no Glide+Picasso) |
| Two UI stacks coexisting confusion | Rule: a surface flips wholesale (no hybrid screens); legacy res deleted only in P11 |
| Solo-dev scope creep | Non-goals §2.2; backlog section in each phase doc for “later” ideas |

---

## 11. Testing & quality strategy

- **Unit:** `UrlInterpreter` (isURL/queryWrapper parity tests vs old outputs), `GestureRouter` mapping, whitelist matching, backup serializers.
- **Robolectric:** Room DAOs incl. migration test (v4 fixture → assert COLOR repair), SettingsRepository migration.
- **Compose UI tests:** omnibox submit, overview tab open/close/switch, settings toggle round-trip, whitelist add/remove.
- **Manual QA matrix per milestone:** Android 5.1/8/10/13/15+ phone+tablet, light/dark/AMOLED, RTL smoke, TalkBack pass on new screens (labels, 48 dp targets), intent matrix (link from Gmail, share-to, WEB_SEARCH, 3 shortcuts), low-end device perf spot-check.
- **Gates:** Crashlytics crash-free ≥ 99.5% on staged rollout before widening; ANR rate ≤ baseline.

## 12. Performance & size budgets (measure in P00, enforce in P11)

| Metric | Baseline (measure P00) | Target |
|---|---|---|
| APK (release) | ? | ≤ 8 MB |
| Cold start → first frame | ? | ≤ 1.2 s mid-range |
| Overview open latency | ? | ≤ 150 ms p95 |
| RAM steady browsing | ? (then drop `largeHeap`) | ≤ current p50 |
| Frame drops on tab switch | ? | < 5% |

## 13. Rollout & rollback

Per-phase releases on the existing track (or a `beta` track from P03): staged 10% → 50% → 100%,
each stage ≥ 3 days & crash-free ≥ 99.5%. Because UI phases ship beside legacy code, rollback =
halt staged percentage; data migrations (P10) additionally gated behind pre-upgrade auto-backup.

## 14. Open decisions (owner: product/dev — decide by phase start)

1. **minSdk**: raise 22 → 24+? (drops MultiDex + legacy paths; check Play device stats). Default: raise to 24.
2. **Dynamic color default** on Android 12+: off (brand) vs on. Default: off, toggle in Appearance.
3. **Interstitial/native formats**: none in v2.0 (banners only) — revisit post-GA.
4. **Session restore depth**: titles+URLs (v2.0) vs thumbnails (backlog). Default: titles+URLs.
5. **Games tab default visibility** on start page: visible vs setting-gated. Default: visible, dismissible.

---

## Appendix A — SharedPreferences key inventory (migration input for P10)

Behavior/theme: `sp_theme`(0-3), `sp_fontSize`, `sp_invert`, `hideToolbar`, `sp_toolbarShow`,
`nav_position`(0-3), `start_tabStart`, `start_tab`, `sp_close_browser_confirm`, `sp_close_tab_confirm`,
`sp_reopenLastTab`.
Browser/privacy: `favoriteURL`, `SP_SEARCH_ENGINE_9`, `sp_search_engine_custom`, `userAgent`,
`desktop_mode`, `IsIncognito`, `saveHistory`, `SP_IMAGES_9`, `SP_SAVEDATA_9`(`ic_save_data` pref key
`@string/sp_savedata`), `SP_LOCATION_9`, `SP_AD_BLOCK_9`, `SP_JAVASCRIPT_9`, `SP_COOKIES_9`,
`sp_remote`, `sp_gestures_use`, `setting_gesture_tb_{up,down,left,right}`, `setting_gesture_nav_{…}`.
Clear: `SP_CLEAR_QUIT_9`, `SP_CLEAR_CACHE_9`, `SP_CLEAR_COOKIE_9`, `SP_CLEAR_HISTORY_9`,
`sp_clearIndexedDB`. Sort/filter: `sort_startSite`, `sort_bookmark`, `filter_01..11`, `icon_01..11`.
Home tiles: `{0..7}_name`, `{0..7}_url`, `counter`. Internal: `restart_changed`, `pdf_create`,
`saved_key_ok`. Legacy: `pass_title/pass_content` (pass_DB_v01.db migration, remove B8).

## Appendix B — Database schema today (Room target in P10)

`data.db` v4 — `HISTORY(TITLE,URL,TIME)` · `BOOKMARK(TITLE,URL,TIME←misused as color 1-11)` ·
`GRID(TITLE,URL,FILENAME,ORDINAL)` · `WHITELIST/JAVASCRIPT/COOKIE/REMOTE(DOMAIN)` · `TAB(TITLE,URL,TIME)` unused.
P10: v5 adds `BOOKMARK.COLOR INTEGER NOT NULL DEFAULT 0` + copies `TIME<1_000_000` → `COLOR`, `TIME=now`;
activates `TAB` as `TAB_SESSION(ID INTEGER PK, TITLE, URL, POSITION, IS_INCOGNITO)`.

## Appendix C — External surface contract (must keep working)

Manifest intent-filters: `MAIN/LAUNCHER`, `VIEW http/https` (default browser), `SEND text/plain`,
`WEB_SEARCH`; shortcuts `sc_startPage/sc_bookmark/sc_history` (`xml-v25/shortcuts.xml`);
searchable meta-data; AdMob APPLICATION_ID meta-data; `ClearService`; pinned-shortcut intents
(ACTION_VIEW with URL). Ad services config property (GMA). All preserved through the rewrite;
verified by the P05/P11 intent matrix.
