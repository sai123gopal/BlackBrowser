# Phase 09 — Privacy Hardening (the “privacy-first” promise made true)

> Goal: fix the real privacy defects found in audit and modernize platform privacy posture without
> changing what users can configure.
> Effort: 4–5 days · Depends on: P05 (shell owns engine lifecycle) · Ships: release

## Defects addressed (from MASTER_PLAN §3.3)
B2 geolocation auto-grant · B4 global-only incognito · B5 desktop-mode UA · B13 force-dark default ·
plus cleartext/storage hardening (B10) and DNT→GPC upgrade.

## Steps

### 9.1 Real site-permission prompts (fixes B2)
- [ ] `SWebChromeClient.onGeolocationPermissionsShowPrompt` now:
      runtime permission via ActivityResult (rationale dialog preserved), then
      `callback.invoke(origin, userAllowed, retain=false)` — never auto-true.
- [ ] Per-origin memory cache so prompts don’t repeat within a session; Settings→Privacy gains
      “Reset website permissions” row (clears cache + `WebView` geolocation permissions).

### 9.2 Incognito sessions (upgrades B4)
- [ ] `TabState.isIncognito`; new-tab menu offers “New incognito tab”; incognito tabs render with themed
      mask styling in tab card + subtle tint on omnibox.
- [ ] Engine behavior while an incognito tab is foreground: `CookieManager.setAcceptCookie(false)` +
      no history writes (existing), plus on last incognito tab close: clear cookies set during that window,
      `clearCache`, `WebStorage.deleteAllData()` — best-effort given stock WebView limits; document honestly
      in a one-time explainer shown on first incognito use.
- [ ] Global `IsIncognito` key retained as “default for new tabs” pref (Settings→Privacy) to not break parity.

### 9.3 Desktop mode correctness (fixes B5)
- [ ] Remove forced UA from `handleUri`/`loadUrl` global path. New model: per-tab toggle from page menu
      (icon in omnibox when active). Modern UA string constant (Chrome ≥ current stable, Windows x64),
      applied only to that tab + `useWideViewPort`.
- [ ] Global Settings switch becomes “Default new tabs to desktop sites”.

### 9.4 Web-content theming sanity (fixes B13)
- [ ] Replace deprecated Force-Dark with `WebSettingsCompat.isAlgorithmicDarkeningAllowed = (appTheme != LIGHT)`
      (webkit ≥1.12); drop the ColorMatrix inversion fallback (minSdk 24 keeps API<29 devices covered by
      algorithmic darkening where supported; else plain). Default now follows theme instead of always-on invert;
      legacy `sp_invert` migrates: true→follow-theme, false→never.

### 9.5 Network & transport policy
- [ ] Add `network_security_config.xml`: cleartext **blocked** by default; HTTPS-first loader: try https,
      on `SSL_UNTRUSTED`/connection-fallback offer “Load insecure http” once per domain (replaces blanket
      `usesCleartextTraffic=true`). Manifest drops the global flag.
- [ ] Keep Safe Browsing enabled; add Settings note.

### 9.6 Tracking signals
- [ ] Request headers: keep `DNT: 1`; add `Sec-GPC: 1`. Both togglable together under Privacy→“Send do-not-track signals”
      (default on, preserving current behavior).

### 9.7 Storage & receivers audit
- [ ] Drop `requestLegacyExternalStorage` + `WRITE_EXTERNAL_STORAGE` requests on Q+ paths; downloads go to
      public Downloads via DownloadManager (already scoped-safe), backups move to SAF (full swap in P10).
- [ ] `downloadReceiver`: keep `RECEIVER_EXPORTED` only as required for system broadcast on 33+ (document why);
      verify all other receivers/services export minimal surface (`ClearService` stays exported=false unless
      shortcut needs it — check manifest today says exported="true": flip to false if unused externally).

### 9.8 Clear-on-exit service rework
- [ ] `ClearService` → `ClearOnExitWorker` logic executed synchronously before `finish()` when possible;
      keep service only as fallback; add unit test covering each flag combination.

## Acceptance criteria
- [ ] Geolocation prompt requires explicit user choice; denial blocks origin (verify on maps-style site).
- [ ] Incognito: no history rows, cookies cleared after last incognito tab closes (verify via dumpsys/site login state).
- [ ] http:// site loads only after explicit user consent prompt; https attempted first.
- [ ] Light theme shows normal colors (no forced dark) — regression vs B13 fixed; Dark theme darkens pages.
- [ ] Play Console pre-launch report: no new storage permission warnings.

## Rollback
Each subsection is independently revertible; flags: `https_first`, `per_tab_incognito` default ON but flippable.
