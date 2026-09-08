package com.saigopal.browser.ui.design.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/*
 * BBTheme — the whole design system entry point.
 *
 * Type personality comes from treatment, not exotic fonts (system faces keep the APK lean and
 * legibility at any text-zoom intact): heavy tight displays, airy eyebrow labels with wide
 * tracking that encode real section structure, tabular figures for counters.
 */

// ---------------------------------------------------------------- typography

private val Display = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 30.sp,
    lineHeight = 36.sp,
    letterSpacing = (-0.2).sp,
)

val BBTypography = Typography(
    displaySmall = Display,
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.1).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    // Eyebrow device: uppercase micro-labels that mark real sections/segments only.
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.6.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = (+1.4).sp, // wide tracking = "eyebrow" voice
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 13.sp,
        letterSpacing = (+0.8).sp,
        fontFeatureSettings = "tnum", // tabular counters (tab chip, blocked count)
    ),
)

// ---------------------------------------------------------------- shape & spacing

val BBShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** The pill — omnibox, search fields, chips. Radius high enough to read as a capsule at any height. */
val PillShape = RoundedCornerShape(percent = 50)

object BBSpace {
    val xxs = 4.dp; val xs = 8.dp; val sm = 12.dp; val md = 16.dp
    val lg = 24.dp; val xl = 32.dp; val xxl = 48.dp

    /** Standard touch target per Material accessibility guidance. */
    val touchTarget = 48.dp

    /** Height of the aurora-edge hairline (the signature progress element). */
    val edgeHairline = 3.dp
}

// ---------------------------------------------------------------- motion

object BBMotion {
    const val fastMs = 140
    const val standardMs = 200
    const val sheetMs = 260
    const val auroraLoopMs = 1200
}

// ---------------------------------------------------------------- theme modes

/**
 * Maps the legacy `sp_theme` preference values verbatim so old installs migrate with no code:
 * 0 system · 1 light · 2 dark · 3 amoled.
 */
enum class BBThemeMode(val prefValue: String) {
    SYSTEM("0"), LIGHT("1"), DARK("2"), AMOLED("3");

    companion object {
        fun fromPref(value: String?): BBThemeMode =
            entries.firstOrNull { it.prefValue == value } ?: DARK
    }
}

data class BBAurora(val dark: Boolean) {
    val edge get() = AuroraBrand.edgeBrush(dark)
    val amber: SignalAmber get() = if (dark) darkAmber else lightAmber
}

val LocalAurora = staticCompositionLocalOf { BBAurora(dark = true) }
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun rememberBBDynamicScheme(enabled: Boolean, dark: Boolean): ColorScheme? {
    if (!enabled || Build.VERSION.SDK_INT < 31) return null
    val context = LocalContext.current
    return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

@Composable
fun BBTheme(
    mode: BBThemeMode,
    dynamicColor: Boolean = false,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Default to dark and ignore system light; only explicit user override to LIGHT uses light theme.
    val dark = when (mode) {
        BBThemeMode.LIGHT -> false
        BBThemeMode.DARK, BBThemeMode.AMOLED, BBThemeMode.SYSTEM -> true
    }

    val scheme = rememberBBDynamicScheme(dynamicColor, dark)
        ?: when (mode) {
            BBThemeMode.AMOLED -> auroraAmoled()
            else -> if (dark) AuroraDark else AuroraLight
        }

    val view = LocalView.current
    SideEffect {
        if (!view.isInEditMode) {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(
        LocalAurora provides BBAurora(dark),
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = BBTypography,
            shapes = BBShapes,
            content = content,
        )
    }
}

/** Convenience accessors so features never import raw palette objects. */
object BBThemeValues {
    @Composable fun dot(code: Long): Color = BBDotColor.byCode(code).color
}
