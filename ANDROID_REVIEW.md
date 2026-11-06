# Android App Review

**Review date:** 2026-11-06  
**Scope:** Static end-to-end review of the shipped Compose browser shell, WebView lifecycle, navigation, persistence, settings, and user-facing feature paths. `LEGACY_SHELL` is `false`; the Compose shell is the active implementation.  
**Method:** Source review guided by the official Android Developers `jetpack-compose-performance` and `android-profiler` skills. This is not a device benchmark or security certification.

## Executive Summary

The app has a broad feature set and a deliberate Compose/WebView architecture. This pass fixes the reviewed privacy data loss, renderer recovery, duplicate history completion, unbounded history loading, high-memory WebView cap, preference parsing, incomplete feedback writes, and the identified Compose lazy-item reuse gaps.

Incognito now uses AndroidX WebKit's multi-profile support when the installed WebView provider offers it. Older providers fall back to suppressing cookies and do not claim site-storage isolation. This is a source-level fix pass; Gradle and device validation remain blocked by missing Java and an attached device.

## Findings

### Resolved: Incognito no longer clears regular browsing data

Incognito WebViews use a named AndroidX WebKit profile when `MULTI_PROFILE` is supported. That profile is deleted after the last private tab closes and cleaned at the next browser-shell startup. Profile-specific cookie acceptance is used, and private WebViews bypass the HTTP cache. The old global `removeAllCookies()` and `WebStorage.deleteAllData()` calls have been removed from private-tab close.

On WebView providers without multi-profile support, cookie acceptance is disabled for private tabs, but site storage can still be shared. The UI now discloses this fallback rather than promising complete isolation.

Evidence: [BrowserWebProfiles.kt](app/src/main/java/com/saigopal/browser/browser/BrowserWebProfiles.kt), [SWebView.java](app/src/main/java/com/saigopal/browser/view/SWebView.java), [ShellController.kt](app/src/main/java/com/saigopal/browser/ui/shell/ShellController.kt).

### Resolved: WebView renderer termination is recovered

`onRenderProcessGone` now removes and destroys the failed WebView, keeps the tab metadata, and invalidates the active AndroidView key so the tab is recreated and reloaded. No device-level renderer-kill test could be run here.

Evidence: [SWebViewClient.java](app/src/main/java/com/saigopal/browser/browser/SWebViewClient.java), [WebViewPool.kt](app/src/main/java/com/saigopal/browser/ui/shell/WebViewPool.kt), [ShellController.kt](app/src/main/java/com/saigopal/browser/ui/shell/ShellController.kt).

### Resolved: History completion is idempotent per navigation

The WebView now resets a completion gate at navigation start and notifies the controller only once even if progress and page-finished callbacks both report completion. A JVM regression test covers repeat completion and reset for a new navigation.

Evidence: [BrowserHistoryCompletionGate.kt](app/src/main/java/com/saigopal/browser/domain/BrowserHistoryCompletionGate.kt), [BrowserHistoryCompletionGateTest.kt](app/src/test/java/com/saigopal/browser/domain/BrowserHistoryCompletionGateTest.kt), [SWebView.java](app/src/main/java/com/saigopal/browser/view/SWebView.java).

### Resolved: History is loaded in pages and grouped off-main

History now fetches 100 newest rows per page and exposes a load-older action. Sorting/grouping runs on `Dispatchers.Default`, so opening History no longer materializes the entire table or performs its grouping on the UI dispatcher.

Evidence: [HistorySegment.kt](app/src/main/java/com/saigopal/browser/ui/screens/overview/HistorySegment.kt), [RecordAction.java](app/src/main/java/com/saigopal/browser/database/RecordAction.java), [RecordsRepository.kt](app/src/main/java/com/saigopal/browser/data/repo/RecordsRepository.kt).

### Resolved: WebView pool is capped on all device classes

The pool now caps renderers at 3 on low-RAM devices, 6 on mid-memory devices, and 8 on high-memory devices. The active renderer remains protected; evicted tabs are rehydrated from saved URL metadata.

Evidence: [WebViewPool.kt](app/src/main/java/com/saigopal/browser/ui/shell/WebViewPool.kt).

### Resolved: Lazy-list content types are declared

Content types have been added to the reviewed history, bookmark, start-page/home, overview segment, and color-dot lazy items.

## Feature Coverage

Implemented in the active code paths:

- Browser navigation: omnibox URL/search interpretation, external `VIEW`/share intents, app shortcuts, start page, back/forward, tabs, fullscreen video, gestures, and find-in-page.
- Browser actions: downloads, print-to-PDF, link sharing/open-with, bookmark/start-page tile management, and history.
- Privacy/settings: JavaScript, cookies, ad blocking and allowlists, remote-content controls, history controls, geolocation prompts, desktop mode, and incognito mode.
- Supporting screens: onboarding/tutorial, games hub, settings, feedback submission, ads/consent, and backup/import/export utilities.

Observed limitations or follow-up areas:

- Session restoration persists tab title/URL metadata, not page scroll position, form state, or WebView back-forward state. Incognito tabs are intentionally omitted from the saved session.
- On providers without WebKit multi-profile support, private tabs suppress cookies but site storage may be shared. The interface now discloses this limitation.
- Feedback metadata and payload are now written atomically with one Firebase `updateChildren` operation.
- Cleartext traffic remains enabled because the product is a general-purpose browser expected to open HTTP sites. File-origin universal access now defaults off consistently in the Compose, legacy, and WebView settings paths. Existing users' saved preference is preserved. Evidence: [AndroidManifest.xml](app/src/main/AndroidManifest.xml#L23), [SWebView.java](app/src/main/java/com/saigopal/browser/view/SWebView.java).

## Performance and Crash Outlook

- **Startup:** Ads initialization is delayed until after the first frame; release minification/resource shrinking are enabled. No baseline-profile setup was found, so cold-start and primary navigation journeys are not precompiled through Baseline Profiles.
- **WebView memory:** The LRU pool caps all device classes, inactive views are paused, and renderer termination recreates the active tab from metadata.
- **Database:** Repository operations use IO dispatch and a mutex. History is paged; grouping runs off-main; completion persistence is gated per navigation.
- **Compose:** Reviewed lazy containers now provide stable keys and content types. No direct Compose diagnostic errors were reported for touched files.
- **Potential crashes:** Font-size parsing now falls back to 100 and clamps to the supported 50-200 range. Editor diagnostics report no errors in touched source files.

## Validation Limits

- `./gradlew testDebugUnitTest lintDebug` could not start: this environment has no Java/JDK executable or configured `JAVA_HOME`.
- No emulator or physical Android device is connected (`adb devices` returned an empty device list).
- The workspace contains 12 JVM unit-test files and no `androidTest` files. A completion-gate regression test was added. Editor diagnostics reported no errors in touched files, but these are not a substitute for a Gradle build.
- No runtime performance numbers, crash reproduction, release-build profiling, or device-specific memory measurements are claimed.

## Suggested Fix Order

1. Run unit tests and lint in an environment with a configured JDK.
2. Verify incognito profile creation, switching, and deletion on WebView providers with and without `MULTI_PROFILE` support.
3. Exercise renderer-loss recovery and large-history paging on API 26+ and a low-memory device.
4. Profile release startup and scrolling on-device; no Baseline Profile or runtime benchmark is included in this pass.