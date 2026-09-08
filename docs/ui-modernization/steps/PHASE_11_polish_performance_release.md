# Phase 11 — Polish, Performance, Testing & v2.0 Release

> Goal: make it small, fast, tested — and delete the old world. Ends with the v2.0 staged rollout.
> Effort: 4–6 days · Depends on: P03–P10 · Ships: **v2.0 GA**

## Steps

### 11.1 Legacy deletion sweep (the payoff)
- [ ] Delete files now unreferenced:
      layouts `activity_main*.xml`, `activity_settings.xml`, `activity_whitelist.xml`, `activity_feedback.xml`,
      all `dialog_*.xml` still remaining, list/grid item XMLs, `preference_*.xml` screens;
      classes `MainActivity` remnants (legacy sheets), `SettingsActivity`+6 sub-activities, 4 `Whitelist*`
      activities, `FeedbackActivity`, fragments `FragmentSettings*`, `FragmentClear`,
      `CompleteAdapter/RecordAdapter/GridAdapter/GridItem/MSGridItems/WhitelistAdapter/SwipeTouchListener/
      Toasty/AlbumItem` if not already ported, tasks `ExportWhiteListTask/ImportWhitelistTask`.
- [ ] Remove deps: `picasso`, `play-services-auth`, `firebase-storage`, `androidx.legacy:legacy-preference-v14`,
      `material` (M2 views) once zero references remain; then turn off Jetifier permanently.
- [ ] Remove manifest cruft: `largeHeap`, `requestLegacyExternalStorage`, `usesCleartextTraffic`,
      `AppTheme*` XML themes (Compose-only), redundant `values-en` duplicate of default English strings,
      unused drawables (Android Studio “unused resource” pass + manual review for reflection/manifest uses).
- [ ] `Newtab.html` + `0.png` removed (post one-release grace period from P07).

### 11.2 Release build hardening
- [ ] `minifyEnabled true` + `shrinkResources true`; R8 full mode; write keep rules only where needed
      (`WebAppInterface`-style JS bridges if any remain, GMA/Firebase consumer rules are bundled).
- [ ] Baseline profile: `androidx.profileinstaller` + macrobenchmark-generated profile for cold start &
      overview open; verify install-time compilation via `adb shell dumpsys package | grep -i profile`.
- [ ] Startup audit: nothing heavy on main thread before first frame (ads warm-up idle, hosts file IO off-main,
      Room ready lazily). Re-measure budgets (MASTER_PLAN §12) — fix regressions before proceeding.

### 11.3 Test suite completion
- [ ] Unit: UrlInterpreter goldens, GestureRouter map, backup JSON round-trip, whitelist matching.
- [ ] Robolectric: DAO CRUD + migration fixture, SettingsRepository import idempotency.
- [ ] Compose UI: omnibox submit flow, overview tab switch/close, settings toggle persistence,
      whitelist add/remove, incognito new-tab.
- [ ] CI-lite: single GitHub Actions workflow — `assembleRelease`, `testDebugUnitTest`, `lint` (re-enable
      `checkReleaseBuilds` with baseline issues suppressed explicitly), upload APK artifact + mappings to Crashlytics.

### 11.4 Accessibility & i18n final pass
- [ ] TalkBack walk-through of every screen (labels on icon buttons, state descriptions on toggles,
      headings for sections); contrast check in all four themes; focus order sane in omnibox edit mode.
- [ ] All user-visible strings via `stringResource` (grep for hard-coded literals in Compose code);
      keep `values-en` removal only after confirming default locale strings complete.

### 11.5 Versioning & store assets
- [ ] versionCode 30+, versionName `2.0.0`; changelog written from phase notes.
- [ ] Play listing screenshots refreshed (new UI); Data-safety form re-check (feedback RTDB email field,
      UMP consent storage, analytics); Ads declaration unchanged (banners).
- [ ] In-app “What’s new” via existing Firebase In-App Messaging campaign.

### 11.6 Staged rollout & watch
- [ ] Internal → Closed beta (2 weeks soak, esp. P10 data migration cohort on old installs)
      → Production 10% → 50% → 100%, gates per stage: crash-free ≥99.5%, ANR ≤0.47%, no migration complaints,
      ad revenue ±15%.
- [ ] Rollback plan: halt percentage; hotfix branch policy documented. Pre-upgrade DB backup (P10) covers worst case.

### 11.7 Post-GA backlog (parked ideas, not commitments)
Reader mode · tab thumbnails in switcher · EasyList auto-update for AdBlock · downloads manager enhancements ·
per-site settings UI unification · custom gestures recorder · Material You default-on evaluation.

## Acceptance criteria (GA gate)
- [ ] APK ≤ 8 MB; cold start ≤1.2 s median on reference mid-range device; budgets in MASTER_PLAN §12 met or
      consciously waived by product owner.
- [ ] Zero references to deleted legacy classes/resources; `lint` release check passes with explicit baseline.
- [ ] Full §7 feature inventory re-audited against the binary — every row KEEP/MODERN verified working.
- [ ] Staged rollout gates green at each step.

## Rollback
Standard staged-rollout halt; migrations covered by pre-upgrade DB copy (P10). No server-side dependencies
except Firebase configs (no schema changes required for v2.0).
