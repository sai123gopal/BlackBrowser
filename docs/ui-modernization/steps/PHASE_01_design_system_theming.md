# Phase 01 — Design System & Theming (Material 3)

> Goal: a single Compose design system (`BBTheme`) that encodes BlackBrowser’s brand — including the four
> existing themes (System / Light / Dark / AMOLED) — plus the shared component kit every later phase reuses.
> Effort: 3–4 days · Depends on: P00 · Ships: theme demo screen (debug), no user-visible change yet

## Design tokens

### Color
- Brand seed = current accent **`#FF793F`** (`values/colors.xml` `colorAccent`). Generate M3 schemes from it
  (Studio “Color tool” or materialkolor) and commit the resulting literals — do not compute at runtime.
- Four schemes: `LightColors`, `DarkColors`, `AmoledColors` (pure black surfaces, hairline borders),
  and dynamic-color variants for Android 12+ (used only when user enables Material You — decision D2).
- Semantic aliases used by all screens (never raw colors in features):
  `omniboxSurface`, `blockedBadge` (ad-block count), `incognitoTint`, `gamesCardGradient`.

### Typography & shape
- Default Roboto ramp; `titleMedium` for omnibox text; tabular nums for tab-count chip.
- Shapes: pill=999dp, card=16dp, sheet=M3 default 28dp top.

### Motion
- Durations: 200ms standard, 250ms sheet; easing `FastOutSlowIn`; spring for dismiss gestures.

## Steps

### 1.1 Theme module
- [ ] Create `ui/theme/Color.kt`, `Type.kt`, `Shapes.kt`, `Theme.kt`.
- [ ] `BBTheme` composable:

```kotlin
enum class BBThemeMode(val prefValue: String) {
    SYSTEM("0"), LIGHT("1"), DARK("2"), AMOLED("3");

    companion object {
        fun fromPref(v: String?) = entries.firstOrNull { it.prefValue == v } ?: SYSTEM
    }
}

@Composable
fun BBTheme(
    mode: BBThemeMode,
    dynamicColor: Boolean = false,     // decision D2
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        SYSTEM -> isSystemInDarkTheme()
        LIGHT -> false
        DARK, AMOLED -> true
    }
    val base = if (mode == AMOLED) AmoledColors else if (dark) DarkColors else LightColors
    val scheme = if (dynamicColor && Build.VERSION.SDK_INT >= 31)
        if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
    else base

    SideEffect {                       // status/nav bar scrim handling lives in P05 edge-to-edge step
        val view = LocalView.current
        if (!view.isInEditMode) {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }
    MaterialTheme(colorScheme = scheme, typography = BBTypography, shapes = BBShapes, content = content)
}
```
- [ ] Map legacy pref `sp_theme` values exactly (`0` system, `1` light, `2` dark, `3` amoled — see
      `HelperUnit.applyTheme`). Until P10 reads DataStore, read via `SharedPreferences` through a tiny
      `SettingsRepository.observeTheme(): Flow<BBThemeMode>` stub created now (P02 formalizes it).

### 1.2 WebView rendering policy hook (foundation only; behavior flips in P09)
- [ ] Add `engine/WebViewTheming.kt` with:
```kotlin
fun WebSettings.applyContentDarkening(dark: Boolean) {
    // Replaces deprecated Force-Dark path (HelperUnit.initRendering). Actual switch happens P09.
}
```
- [ ] Leave old behavior untouched this phase.

### 1.3 Component kit — `ui/components/`
Build + preview each (Preview parameters for light/dark/AMOLED):
- [ ] `BBIconTile(url, label, size)` — favicon via Coil w/ letter-tile fallback (deterministic hue from host);
      replaces Picasso usage pattern later.
- [ ] `BBListItem(icon, title, subtitle, onClick, trailing)` — used by menus/settings/lists.
- [ ] `SwitchRow(title, subtitle, checked, onChange, icon)` — settings workhorse.
- [ ] `BBSegmentedRow(options, selected, onSelect)` — overview rail (Tabs/Home/Saved/History).
- [ ] `ConfirmSheet(title, body, confirmLabel, destructive, onConfirm, onDismiss)` — replaces all
      `dialog_action` bottom-sheets.
- [ ] `BBTextFieldPill(value, onValueChange, hint, leading, trailing)` — omnibox/search inputs.
- [ ] `SectionHeader(text)`, `EmptyState(icon, title, hint)`.
- [ ] `ColorDotPicker(selected, onSelect)` — preserves the 11 bookmark color circles
      (`circle_red_big` … `circle_grey_big` semantics; codes 1–11 kept for data compat until P10).
- [ ] Demo route (debug-only): `ui/demo/ComponentGalleryScreen.kt` listing every component in all themes.

### 1.4 Iconography pass
- [ ] Audit `res/drawable/icon_*`: keep vector sources; where missing, add Material Symbols outlines as
      new drawables (do **not** delete old ones yet — legacy XML still references them until P11).
- [ ] New screens reference Compose `Icons` (material-icons-extended) primarily; drawables only for brand art.

### 1.5 Instant-theme plumbing (prep for later phases)
- [ ] `SettingsRepository.observeTheme()` must emit on change so future screens re-theme without restart
      (kills the old `restart_changed` flow for theming).

## Acceptance criteria
- [ ] Gallery screen renders all components in System/Light/Dark/AMOLED without visual defects.
- [ ] `sp_theme=3` yields pure-black surfaces; contrast of body text ≥ 4.5:1 in all schemes (spot-check).
- [ ] No changes to shipped screens; release build still byte-comparable behavior.

## Rollback
Delete `ui/theme`, `ui/components`, `ui/demo` — nothing else depends on them yet.

## Backlog (not this phase)
Custom font option · true-black vs soft-dark toggle · icon-pack variants.
