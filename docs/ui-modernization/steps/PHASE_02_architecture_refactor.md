# Phase 02 — Architecture Refactor (MVVM, repositories, TabManager, navigation skeleton)

> Goal: extract all logic out of `MainActivity` (2,407 lines) into testable Kotlin classes so later UI
> phases only *render state*. No visual change yet.
> Effort: 5–7 days · Depends on: P00 (Kotlin) · Ships: refactor release, behavior-identical

## Target shape

```
ui/            Compose hosts + ViewModels (added in P03+)
domain/        TabManager, GestureRouter, UrlInterpreter, AdBlockEngine facade
data/repo/     RecordsRepository, WhitelistRepository, SettingsRepository, BackupRepository
data/db/       RecordAction wrappers (Room arrives P10; today thin suspend wrappers)
engine/        SWebView & clients (moved package only)
AppContainer   manual DI on BrowserApplication
```

## Steps

### 2.1 Application + AppContainer
- [ ] Create `BrowserApplication` (register in manifest `android:name`).
```kotlin
class BrowserApplication : Application() {
    lateinit var container: AppContainer; private set
    override fun onCreate() { super.onCreate(); container = AppContainer(this) }
}
class AppContainer(app: Application) {
    val settings = SettingsRepository(app)          // SharedPreferences-backed for now
    val records = RecordsRepository(app)            // wraps RecordAction on Dispatchers.IO
    val whitelists = WhitelistRepository(app)       // AdBlock/Javascript/Cookie/Remote unified
    val urlInterpreter = UrlInterpreter(app, settings)
}
```
- [ ] Move the first-run defaults block from `MainActivity.onCreate` (`saved_key_ok`, gesture codes,
      location=false) into `SettingsRepository.ensureFirstRunDefaults()` called from `AppContainer.init`.

### 2.2 SettingsRepository (typed, observable)
- [ ] Expose typed data class `BBSettings(theme, fontSize, invertWebContent, hideToolbar, toolbarShowOnBack,
      navButtonPos, startTabPref, overviewOnStart, confirmExit, confirmTabClose, reopenLastTab, searchEngine,
      customSearchPrefix, favoriteUrl, userAgent, desktopMode, incognito, saveHistory, images, saveData,
      location, adBlock, javaScript, cookies, remoteContent, gesturesEnabled, gestureToolbar: Map<Dir,Action>,
      gestureNav: Map<Dir,Action>, clearOnExit: ClearFlags, …)`.
- [ ] API: `val state: StateFlow<BBSettings>` (refreshed via `OnSharedPreferenceChangeListener`),
      `suspend fun update(transform: (BBSettings) -> BBSettings)` writing through to legacy keys.
      Keep **exact key names/values** (Appendix A of master plan) — DataStore migration is P10.
- [ ] Delete the “restart required” pattern for theme/search/UA: consumers observe flows (B11 groundwork).

### 2.3 RecordsRepository
- [ ] Wrap every `RecordAction` call in `suspend fun … = withContext(Dispatchers.IO){}`:
      `history(add/list/delete/clear)`, `bookmarks(add/edit/delete/list/filter/clear)`,
      `startTiles(list/add/delete/clear/reorder)`, `autocomplete(): List<Record>` (merges GRID+HISTORY+BOOKMARK,
      porting `RecordAction.listEntries`). Return immutable models (`Record(title,url,time,colorHint)`).
- [ ] Port `BookmarkList`/`pass_DB_v01` legacy migration into a one-shot guard that runs once and records
      completion (fix B8): check marker key `legacy_bookmark_migrated`; close cursors; delete DB after success.

### 2.4 UrlInterpreter (port `BrowserUnit.queryWrapper` + `isURL`)
- [ ] Copy logic verbatim into Kotlin with unit tests FIRST (golden tests comparing old vs new outputs for
      30+ cases incl. play-store unwrap, bare domains, IPs, mailto/about/intent schemes, engine 0–6).
- [ ] Keep `BrowserUnit` delegating to it so old callers still work.

### 2.5 TabManager (replaces static BrowserContainer usage in UI)
```kotlin
data class TabState(val id: Long, val title: String, val url: String?, val isForeground: Boolean,
                    val isIncognito: Boolean = false, val progress: Int = 100)

class TabManager(/* engine hooks */) {
    val tabs: StateFlow<List<TabState>>
    val activeId: StateFlow<Long?>
    fun newTab(url: String?, foreground: Boolean, incognito: Boolean = false): Long
    fun close(id: Long); fun activate(id: Long); fun move(from: Int, to: Int)
    fun next(delta: Int): Long?           // for gestures 06/07
    // WebView instances stay owned here (Map<Long, SWebView>) — never recreated by recomposition
}
```
- [ ] Implement over existing `BrowserContainer` initially (thin façade), so `MainActivity` keeps working;
      full ownership flip happens in P05.
- [ ] Session restore groundwork: persist `(TITLE,URL,POSITION,IS_INCOGNITO)` rows into the unused `TAB`
      table on tab open/close/navigation (debounced). Restore on cold start behind pref (decision D4:
      titles+URLs only).

### 2.6 GestureRouter
- [ ] Port `performGesture` switch (codes `"01".."10"` semantics: none/forward/back/pageUp/pageDown/
      nextTab/prevTab/showHomeOverview/newTab/closeTab) into `GestureRouter.execute(action: GestureAction)`
      with an interface `GestureHost` implemented by the future browser VM. Unit-test mapping table.

### 2.7 AdBlockEngine facade
- [ ] Wrap `AdBlock` (+hosts set) behind `AdBlockEngine { fun isAd(url): Boolean; fun isWhitelisted(url): Boolean }`;
      load hosts once at app start on IO dispatcher; expose `blockedCount: StateFlow<Long>` (for the future badge).

### 2.8 MainActivity slim-down (behavior identical)
- [ ] Replace direct `sp.` reads/writes with `container.settings`.
- [ ] Replace `RecordAction` inline uses with repository calls.
- [ ] Route add/remove/show-tab calls through `TabManager`.
- [ ] Extract each bottom-sheet builder into its own class under `legacy/` (e.g., `LegacyOverviewSheet`,
      `LegacyPageMenu`) — they get deleted in P04/P06; this makes `MainActivity` readable now.
- [ ] Fix while touching (cheap wins allowed): B3 missing `break` on KEYCODE_MENU; B14 duplicate
      `filePathCallback` fields consolidated.
- [ ] Target: `MainActivity` ≤ ~800 lines after extraction (final <300 happens in P05/P06).

### 2.9 Navigation skeleton
- [ ] Host a `NavHost` inside `MainActivity` content (Compose) with routes registered but only one real
      destination: nothing visible yet — or a debug-only gallery route. Old XML remains as-is until P03+.
- [ ] Define route constants now: `browser`, `overview`(sheet handled in-place later), `settings`,
      `settings/{section}`, `whitelist/{kind}`, `feedback`, `games`, `downloads`.

## Acceptance criteria
- [ ] All golden tests for `UrlInterpreter` pass (old vs new parity).
- [ ] Full manual smoke: browse, tabs, bookmarks/history CRUD, whitelists toggles, gestures, downloads,
      backup screens — unchanged behavior.
- [ ] `MainActivity` no longer touches `SharedPreferences`/`RecordAction` directly.
- [ ] Crashlytics shows no regression vs previous release build.

## Rollback
Refactor-only branch revert; no schema/pref changes (TAB writes are additive and ignored by old code).
