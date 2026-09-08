# BlackBrowser 2.0.0 — changelog & release runbook

Generated from the UI-modernization phase notes (`docs/ui-modernization/`), P03–P11.

## ⚠️ Build-verification status

As of this writing the whole rewrite (P00–P11) has **not yet passed a compiler** — the session's
tooling outage blocked every `gradlew` invocation. Before any release activity:

1. `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
2. Fix findings wave-by-wave (reports live in `.superpowers/sdd/ui-rewrite-plan/`)
3. Land the parked P10 work: Room over `data.db` (MIGRATION_4_5 fixes bookmark-color abuse B6)
   + DataStore preferences migration — deliberately deferred until the gate is green
   (ruling in `task-G-report.md`).
4. Only then: `assembleRelease`, lint baseline, staged rollout below.

## What's new in 2.0

### All-new Compose UI ("Obsidian & Aurora" design system)
- Complete Jetpack Compose + Material 3 rewrite behind a feature flag (`LEGACY_SHELL=false`;
  flip to `true` restores the old UI for one rollback cycle).
- Dynamic color (Material You), four theme modes (system/light/dark/AMOLED), reduced-motion support.
- New native start page with pinned tiles migrated from the legacy quick-tile grid.
- Unified tab-switcher sheet with tabs/bookmarks/history segments, swipe-to-close, grouped history.

### Browser engine behavior
- **Real geolocation prompts** per origin (the old build silently granted every site!),
  remembered per-session; "Reset website permissions" in Settings → Privacy. (B2)
- **Per-tab incognito**: incognito is now a default-for-new-tabs setting, cookies are blocked while
  an incognito tab is foregrounded, history is never written, and closing the last incognito tab
  wipes its traces. One-time explainer on first use. (B4)
- **Per-tab desktop-site mode** with a current Chrome UA replacing the stale global Chrome/85
  forced-UA hack; toggle in the page menu, indicator in the omnibox. (B5)
- **Dark web pages done right**: WebView algorithmic darkening follows app theme; force-dark and
  color-matrix inversion removed. Old "inverted colors" switch now means "darken web pages". (B13)
- Global Privacy Control (`Sec-GPC: 1`) sent alongside Do Not Track.
- Find-in-page bar, link context menu, print-to-PDF, share/copy/open-with all ported and wired.

### Data & backup
- Backup/restore via the system file picker (SAF) — no more hardcoded `/storage/emulated/0`
  writes that broke on modern Android. (B9)
- Full-profile JSON backups (settings, bookmarks, home tiles, whitelists, open-tab session) with
  merge-with-dedupe restore; Netscape HTML bookmark export/import stays byte-compatible with 1.x.

### Ads & consent
- Ad lifecycle bug fixed by construction (no more destroyed-view reuse crashes). (B1)
- Adaptive banners in the overview sheet footer and settings footer only — never on browser
  content, the start page, or games. Incognito sessions see no ads.
- Google UMP consent flow (EEA) + persistent "Privacy options" entry in About.
- Debug builds always use Google test ad units.

### Under the hood
- Manual DI container, repositories over the existing database (no user data touched),
  versioned JSON session store, pure-JVM unit tests for every decision core
  (URL interpretation, gestures, migrations, backups, clear-on-exit).
- Removed dead dependencies (`play-services-auth`, `firebase-storage`); R8 shrinking +
  resource shrinking enabled for release; versionCode 30 / versionName 2.0.0.

## Rollout runbook (11.6)

| Stage | Gate to advance |
|---|---|
| Internal | assembleRelease installs, smoke pass |
| Closed beta (2 weeks) | crash-free ≥ 99.5%, no data-migration complaints (old-install cohort), ad revenue ±15% |
| Production 10% | crash-free ≥ 99.5%, ANR ≤ 0.47% |
| Production 50% → 100% | same gates held |

Rollback = halt percentage; `LEGACY_SHELL=true` hotfix branch restores the old UI without a store
re-review of new binaries' UI behavior. Pre-upgrade DB copy lands with the parked Room work.

## Post-GA backlog (11.7, parked)
Reader mode · tab thumbnails · EasyList auto-update · downloads manager enhancements · per-site
settings unification · custom gesture recorder · Material You default-on evaluation.
