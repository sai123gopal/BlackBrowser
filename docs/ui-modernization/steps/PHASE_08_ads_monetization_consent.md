# Phase 08 — Ads & Monetization (AdMob done right + UMP consent)

> Goal: keep banner revenue but fix the lifecycle bugs (B1), centralize all ad code in one controller,
> add legally-required UMP consent, and modernize formats (adaptive banners). No new intrusive formats in v2.0.
> Effort: 3–4 days · Depends on: P04 (overview footer) · Ships: release M3

## Current-state problems recap
- `MainActivity.showOverview()` calls `mAdView.loadAd()` **after** `hideOverview()` already ran
  `mAdView.destroy()` → destroyed-view reuse (B1). Ads also load once at init AND again on show.
- `MobileAds.initialize()` runs in both `MainActivity` and `SettingsActivity` cold paths.
- Fixed-size `BANNER` unit `ca-app-pub-8397175258659671/5437849349`; app id
  `ca-app-pub-8397175258659671~6223570953` (manifest meta-data).
- No consent flow (UMP) — required for EEA traffic.

## Steps

### 8.1 AdsController singleton
```kotlin
class AdsController(private val container: AppContainer) {
    enum class Placement(val tag: String) { OVERVIEW("overview"), SETTINGS("settings") }
    fun warmUp(context: Context)            // MobileAds.initialize ONCE from BrowserApplication, async
    fun maybeShowConsent(activity: Activity, onDone: () -> Unit)   // UMP: gatherAndConsent, canShowAds gate
    @Composable fun Banner(placement: Placement, modifier: Modifier)  // AndroidView(AdView)
}
```
- [ ] Move `MobileAds.initialize` to `BrowserApplication.onCreate` **post-launch** (idle handler), removing
      both activity-level calls.
- [ ] BuildConfig ad units: `AD_UNIT_OVERVIEW`, `AD_UNIT_SETTINGS`; debug overrides point at Google test ids
      (`ca-app-pub-3940256099942544/6300978111`) so QA never violates policy.

### 8.2 Compose banner with correct lifecycle
- [ ] `Banner` uses `AndroidView(factory = { AdView(it) })`, `DisposableEffect(placement)` doing
      `loadAd` in effect and `destroy()` on dispose — the destroy/load mismatch becomes impossible by
      construction. Keep one AdView instance per placement for the sheet’s lifetime; re-`loadAd` on show
      only if `isLoaded==false && wasDestroyed==false`.
- [ ] Adaptive size: `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(widthDp)`; height reserved via
      Modifier to avoid layout shift; collapse gracefully (GONE) when no-fill (`onAdFailedToLoad`).

### 8.3 Placements (parity + policy)
- [ ] OVERVIEW: bottom of overview sheet (replaces P04 placeholder).
- [ ] SETTINGS: footer banner under the last section (parity with old Settings screen).
- [ ] Explicitly **not** placed: browser content screen (zero chrome during reading — product principle),
      start page (clean first impression), games hub (Gamezop carries its own monetization).
- [ ] Frequency guard: overview banner loads max once per N=… minutes (configurable const) to cut request waste.

### 8.4 UMP consent flow
- [ ] Add `play-services-ads` UMP API (`com.google.android.gms:consent` module or bundled — use current
      artifact); create a ConsentInformation config in the AdMob console first (message ID stored as constant).
- [ ] Flow: first launch after this release → `ConsentInformation.requestConsentInfoUpdate` +
      `loadAndShowConsentFormIfRequired` before first `loadAd`; result cached by SDK. “Privacy options” entry
      added in Settings→About (required by Google policy to allow re-opening the form).
- [ ] If `canRequestAds()==false`: banners render nothing and skip requests entirely.

### 8.5 Analytics parity & cleanup
- [ ] Keep `screen_view` event for overview (params modernized: `screen_name=overview`).
- [ ] Log `ad_impression` proxy via AdListener `onAdImpression` per placement (new, useful for placement policy).
- [ ] Delete dead ad-adjacent code: none besides B1 path (already gone with legacy sheet deletion).

### 8.6 Policy checklist (ship gate)
- [ ] Ads not shown inside incognito sessions (product choice, document it).
- [ ] No clickable ads within 48dp of interactive browser controls (sheet footer spacing check).
- [ ] Play Console “Ads” declaration = Yes (banner); Data safety form updated for UMP/consent storage.

## Acceptance criteria
- [ ] Stress test: open/close overview 20× rapidly → zero leaks (`leakcanary` debug build), zero
      “AdView was destroyed” exceptions, request count sane.
- [ ] EEA test (VPN or consent-debug geography): form shows once; decline → no ad requests in logs.
- [ ] Revenue metrics comparable ±10% over a 2-week staged window vs previous release (banners adaptive may vary).

## Rollback
BuildConfig flag `ads_enabled=false` blanks placements without touching other UI.
