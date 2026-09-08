# Phase 00 — Foundation & Tooling (Kotlin + Compose beside Java)

> Goal: the project builds Kotlin + Compose **without changing any user-visible behavior**, and we capture
> performance/size baselines to enforce later.
> Effort: 1–2 days · Depends on: nothing · Ships: internal build only

## Preconditions
- Repo on a branch `ui-rewrite/p00`. AGP 8.3.1 present; use Android Studio with matching or newer SDK.

## Steps

### 0.1 Baselines (do first — numbers feed MASTER_PLAN §12)
- [ ] `./gradlew assembleRelease` → record APK size (`app/build/outputs/apk/release/`).
- [ ] Install release build on a mid-range device; record cold start:
      `adb shell am start -W com.saigopal.browser/.activity.MainActivity` (run 5×, take median).
- [ ] Record RAM while browsing one heavy site: `adb shell dumpsys meminfo com.saigopal.browser`.
- [ ] Paste results into this file under “Baseline results” (bottom) and into MASTER_PLAN §12.

### 0.2 Add the version catalog
- [ ] Create `gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.7.3"              # example — set to latest stable your Studio supports
kotlin = "2.1.0"
composeBom = "2025.01.00"  # example — latest stable BOM at execution time
activityCompose = "1.10.1"
navigationCompose = "2.8.6"
lifecycle = "2.8.7"
coroutines = "1.10.1"
datastore = "1.1.1"
room = "2.7.0-alpha12"     # or latest stable room
ksp = "2.1.0-1.0.29"       # must match kotlin
coil = "2.7.0"
webkit = "1.12.1"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
webkit = { group = "androidx.webkit", name = "webkit", version.ref = "webkit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
crashlytics = { id = "com.google.firebase.crashlytics", version = "3.0.2" }   # note: plugin id changed from firebase-perf naming
```

> ⚠️ Versions above are **examples** — resolve latest stables when executing. Keep Firebase/GMS plugins as-is if upgrade friction appears.

### 0.3 Convert Gradle files minimally (keep Groovy if preferred)
- [ ] Root `build.gradle`: move plugin declarations to `plugins {}` block using aliases with `apply false`
      (or keep Groovy classpath style and just add Kotlin classpath — both work; catalog still resolves).
- [ ] `settings.gradle`: add `dependencyResolutionManagement` block pointing at google()/mavenCentral()
      and enable feature versions catalog access.

### 0.4 Enable Kotlin + Compose in `app/build.gradle`
```groovy
plugins {
    alias(libs.plugins.android.application) apply false // (root) ; in app: apply true form below
}
// app/build.gradle top:
apply plugin: 'com.android.application'
apply plugin: 'org.jetbrains.kotlin.android'
apply plugin: 'org.jetbrains.kotlin.plugin.compose'

android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = '17' }
    buildFeatures { compose true; buildConfig true }
    defaultConfig {
        // decision D1 (master §14): minSdk 22 -> 24
        minSdkVersion 24
    }
}

dependencies {
    implementation platform(libs.compose.bom)
    implementation libs.compose.ui
    implementation libs.compose.material3
    implementation libs.activity.compose
    implementation libs.navigation.compose
    implementation libs.lifecycle.viewmodel.compose
    implementation libs.lifecycle.runtime.compose
    implementation libs.coroutines.android
    debugImplementation libs.compose.ui.tooling
    implementation libs.compose.ui.tooling.preview
}
```
- [ ] Remove `multiDexEnabled true` (minSdk ≥ 21 handles it; with 24 it’s inert).
- [ ] Keep every existing dependency for now (removals happen P08/P11).

### 0.5 Smoke-proof the toolchain
- [ ] Add `app/src/main/java/com/saigopal/browser/ui/ComposeSmokeTest.kt`:
```kotlin
package com.saigopal.browser.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class ComposeSmokeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text("Compose OK") }
    }
}
```
- [ ] Register it in the manifest (`exported=false`) and launch once via adb to prove Compose renders inside this app.
- [ ] Delete or keep hidden behind `debug` source set — recommend moving to `src/debug/java/...`.

### 0.6 Git & hygiene
- [ ] `.gitignore`: add `.kotlin/`, `build_kotlin/` artifacts if any appear.
- [ ] Create branch convention: `ui-rewrite/pNN-*` per phase; each phase ends with one merge to `main` + tag `pNN`.

### 0.7 Optional but recommended same-day
- [ ] Turn off `android.enableJetifier` and build — Picasso 2.71828 usually survives; if any support-lib
      reference breaks, re-enable until P11 (note result here).
- [ ] Enable `android.nonTransitiveRClass=true` (speeds builds; fix references if the compiler complains).

## Files touched
`gradle/libs.versions.toml` (new) · root+app `build.gradle` · `gradle.properties` · `AndroidManifest.xml` (smoke activity) · baselines noted here.

## Acceptance criteria
- [ ] `./gradlew assembleDebug assembleRelease` green.
- [ ] App behavior identical (quick smoke of browser + settings).
- [ ] A Compose activity renders (from debug source set).
- [ ] Baseline numbers recorded in this file.

## Rollback
Revert branch; no data/UI changes were made.

---

### Baseline results (fill in during step 0.1)
| Metric | Value | Device / date |
|---|---|---|
| APK release size | | |
| Cold start (median) | | |
| RAM steady | | |
