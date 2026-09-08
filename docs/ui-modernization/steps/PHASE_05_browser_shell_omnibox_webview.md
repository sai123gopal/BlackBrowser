# Phase 05 — Browser Shell: Omnibox, WebView interop, Find-in-page, Fullscreen, Gestures

> Goal: the core browsing screen becomes Compose — bottom omnibox pill, animated progress, scroll-hide,
> tab-count chip, find bar, fullscreen video host, back/keyboard handling, intent dispatch — with the
> existing `SWebView` engine untouched underneath. This is the highest-risk phase; do the interop spike first.
> Effort: 6–8 days · Depends on: P04 (sheet exists), P02 (TabManager/GestureRouter) · Ships: **release M2**

## Steps

### 5.1 WebView interop spike (half-day, throwaway branch)
- [ ] Prove pattern: single persistent `AndroidView` slot whose `factory` returns the *current* tab’s
      `SWebView`; switching tabs re-parents the instance (never recreates). Verify: scroll state kept,
      video keeps playing across sheet open, no input capture issues, IME resize OK.
- [ ] Document the chosen approach in this file (either “one AndroidView + manual re-parent in update” or
      “AndroidView per active index with movable content”) — decision recorded below.

### 5.2 BrowserScreen composition
```
Box(edge-to-edge) {
  TabContentArea(currentTab)                       // AndroidView slot
  AnimatedVisibility(visible = chromeVisible, slideVertically) {
    BrowserBottomBar(omniboxState, onMenu, onTabs, progress)
  }
  FindBar(visible = findActive)                    // overlays above bottom bar
  FullscreenHost(activeVideo)                      // hides all chrome, landscape lock
}
```
- [ ] Edge-to-edge: `WindowCompat.setDecorFitsSystemWindows(window,false)`; bottom bar applies
      `windowInsetsPadding(WindowInsets.navigationBars)`; top content consumes status-bar insets.
- [ ] Progress: 3dp `LinearProgressIndicator` docked to pill’s top edge, animated; hide at 100.

### 5.3 Omnibox behavior parity + polish
- [ ] Collapsed state shows title-or-URL + security icon (`icon_alert` semantics for non-HTTPS preserved);
      tap → edit mode (focus, select-all, autocomplete list from repository, imeOptions GO).
- [ ] Submit routes through `UrlInterpreter`; empty-submit toast preserved.
- [ ] Swipe gestures on omnibox & nav button wired through `GestureRouter` (codes unchanged); long-press
      menu button opens FastToggles (P06 placeholder until then — keep legacy dialog callable).
- [ ] Scroll-hide ported from old `updateProgress` logic incl. cutoff math and `sp_toolbarShow`-on-back;
      find-mode pins chrome visible (parity with `searchOnSite`).

### 5.4 Back & keyboard (fix B3, keep contract)
- [ ] `BackHandler` order: fullscreen → find bar → overview sheet (P04) → address-edit → webview
      `canGoBack` → close-tab (confirm pref) → quit-confirm / reopen-last-tab pref.
- [ ] Hardware menu key opens page menu only (no fall-through).
- [ ] Predictive-back opt-in for sheets/menus; document that WebView-back can’t animate (platform limit).

### 5.5 Find-in-page
- [ ] `FindBar`: query field (live `findAllAsync`), count display via `setFindListener`, prev/next/cancel;
      cancel restores chrome state exactly as legacy did.

### 5.6 Fullscreen video
- [ ] Port custom-view handling into a `FullscreenHost` composable: add chrome-client view to a dedicated
      Compose-owned `AndroidView`, immersive flags via `WindowInsetsControllerCompat`, orientation lock +
      restore, keep-screen-on, VideoView error/completion listeners (port `VideoCompletionListener`).

### 5.7 Intent & shortcut dispatch
- [ ] Move `dispatchIntent` into `IntentDispatcher` (handles VIEW/SEND/WEB_SEARCH/sc_* shortcuts/empty-cold-
      start w/ favoriteURL + overview-on-start). Regression matrix executed:
      open http link from Gmail · share URL into app · WEB_SEARCH from Quick Findbox · 3 launcher shortcuts ·
      cold start · warm start · second VIEW while running (singleTask `onNewIntent`).

### 5.8 File upload + downloads prompts
- [ ] Keep `showFileChooser` Activity-result plumbing (move to modern `ActivityResultContracts.OpenDocument`
      where trivial); download confirmation still handled by engine listener until P06 restyles it.

### 5.9 Flip the shell, retire legacy chrome
- [ ] `activity_main.xml` appBar/omnibox/search-panel/FABs removed from use (files dormant till P11);
      `MainActivity` now hosts `BBTheme { AppNavHost() }` only (~<300 lines target met).

## Interop decision record
- Approach chosen: ______________ (fill during spike)
- Notes/pitfalls discovered: ______________

## Acceptance criteria
- [ ] Full §7.1 regression checklist passes (intents, gestures, back-chain, fullscreen, uploads, downloads,
      PDF print, find-in-page, desktop mode toggle path).
- [ ] No WebView recreated on tab switch (assert via logging in debug build).
- [ ] Cold-start delta vs baseline ≤ +100 ms; jank on tab switch <5% dropped frames.
- [ ] Crash-free ≥99.5% across staged rollout before M2 marked complete.

## Rollback
BuildConfig flag `legacy_shell=true` restores old layout path for one release cycle if needed.
