# Phase 07 — Native Start Page & Gamezop Games Hub

> Goal: replace the WebView-rendered `assets/Newtab.html` (Bootstrap from CDN, JS bridge hacks) with a
> native Compose start page, and give Gamezop a proper in-app home instead of an image banner.
> Effort: 3–4 days · Depends on: P04 (tiles/lists), P05 (shell) · Ships: release M3

## Start page design
```
            (status bar insets)
               ⬢ brand mark
          Black Browser
   ┌──────────────────────────────┐
   │  🔍 Search or type address   │   ← BBTextFieldPill; submit via UrlInterpreter
   └──────────────────────────────┘
   PINNED                                    [edit]
   [G][F][IG][X][AMZ][W][DDG][BB] …           ← unified tiles (DB GRID + legacy 8 quick tiles)
   🎮 GAMES                                   ← GamesCard → route `games`
   footer: website · terms · privacy · feedback
```

## Steps

### 7.1 Route + rendering rules
- [ ] Define pseudo-scheme `about:startpage` recognized by `UrlInterpreter`/`TabManager`; new tabs default
      to it when no `favoriteURL` override is set. If user has a custom `favoriteURL`, honor it exactly as
      today (compat).
- [ ] Render inside the normal tab slot as a Compose destination (not WebView) — tab title “New Tab”,
      history untouched, back from start page closes tab per standard chain.

### 7.2 Tiles unification
- [ ] One source of truth: migrate the 8 SharedPreferences quick tiles into GRID table on first run
      (idempotent guard), then render purely from repository. Keep write-back compat for one release.
- [ ] Edit mode: add / rename / change URL / remove / reorder (long-press drag). Favicon via Coil with
      letter-tile fallback — **no network fetch required to look good offline** (improvement over Google-S2).

### 7.3 Games hub (Gamezop)
- [ ] New route `games`: full-screen Compose screen embedding `SWebView` pointed at
      `https://10788.play.gamezop.com/` with slim top chrome (back, title, open-in-browser) — replaces
      Custom-Tabs-first behavior (keeps users in-app; Custom Tabs remains fallback if engine fails).
- [ ] Engine policy for this tab: ad-block **exempt** for `*.gamezop.com` (games SDKs break under hosts
      blocking); cookies/storage allowed regardless of master toggles *for this tab only* — implemented via
      per-tab overrides in `TabState` consumed by `SWebView.initPreferences`.
- [ ] Entry points: GamesCard on start page + overview Home segment card (from P04). Analytics events:
      `games_open`, `games_session_start/end` (duration), replacing the old banner-click path.
- [ ] Remove old `img_games` click handler incl. its `Toasty.show(context,"Error")` debug leftovers.

### 7.4 Retire Newtab.html assets
- [ ] Stop loading `file:///android_asset/Newtab.html` everywhere (`favoriteURL` defaults, dispatchIntent,
      gesture 08/09 paths now use `about:startpage`). Keep the asset file one release for upgraders whose
      stored `favoriteURL` still points at it, then delete in P11.
- [ ] The old JS bridge methods die with it: `Android.Search` handled natively; `feedback()` link replaced by
      route; `sign()` was dead code (also drop B15 deps in P08/P11).

## Acceptance criteria
- [ ] Cold-start → interactive start page ≤ shell budget; zero network requests on first paint (vs CDN CSS today).
- [ ] All tile CRUD persists across restarts; legacy prefs migrated cleanly (re-run safe).
- [ ] Games loads, plays a demo game, back navigation returns to prior tab; ad-block exemption verified by
      checking blocked-counter delta while playing.
- [ ] Dark/AMOLED/Light all correct (no more hardcoded black HTML page).

## Rollback
Feature-flag `native_start_page=false` restores `favoriteURL` default behavior for one release.
