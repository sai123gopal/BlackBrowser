package com.saigopal.browser.ui.design.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/*
 * BlackBrowser "Obsidian & Aurora" palette.
 *
 * Concept: the app is named Black, so the surfaces behave like stone — obsidian in dark mode,
 * cool porcelain in light mode. Life comes from two accent temperatures:
 *   · Aurora mint (primary)   — the "secure connection" colour; drives interactive chrome.
 *   · Dusk violet (tertiary)  — playful surfaces only (games, highlights).
 * The legacy brand orange survives exactly once, demoted to a semantic warning colour
 * (insecure HTTP / destructive confirmations) so upgraders still recognise "caution".
 */

// ---- Brand ramp (hand-tuned, committed as literals — never computed at runtime) ----
object AuroraBrand {
    // Primary ramp — aurora mint / teal
    val Mint10 = Color(0xFF00201B)
    val Mint20 = Color(0xFF00382F)
    val Mint30 = Color(0xFF005143)
    val Mint40 = Color(0xFF00685A) // light-scheme primary
    val Mint70 = Color(0xFF7FDCC7) // dark-scheme primary
    val Mint80 = Color(0xFF9BF1DE)

    // Secondary ramp — cool slate (quiet supporting chrome)
    val Slate10 = Color(0xFF072019)
    val Slate20 = Color(0xFF1C3530)
    val Slate30 = Color(0xFF324B45)
    val Slate40 = Color(0xFF4A635D)
    val Slate80 = Color(0xFFB0CCC3)
    val Slate90 = Color(0xFFCCE8E0)

    // Tertiary ramp — dusk violet, reserved for playful surfaces
    val Violet10 = Color(0xFF211A47)
    val Violet20 = Color(0xFF322A60)
    val Violet30 = Color(0xFF4A4178)
    val Violet40 = Color(0xFF685BA6)
    val Violet80 = Color(0xFFC9BFF2)
    val Violet90 = Color(0xFFE5DEFF)

    // Semantic amber — insecure page / destructive emphasis ONLY (legacy orange's last job)
    val Amber10 = Color(0xFF271900)
    val Amber20 = Color(0xFF43340A)
    val Amber30 = Color(0xFF5C4200)
    val Amber40 = Color(0xFF825500)
    val Amber80 = Color(0xFFFFBE4C)
    val Amber90 = Color(0xFFFFDDB4)

    // Error ramp (standard M3 red)
    val Red10 = Color(0xFF410002)
    val Red20 = Color(0xFF690005)
    val Red30 = Color(0xFF93000A)
    val Red40 = Color(0xFFBA1A1A)
    val Red80 = Color(0xFFFFB4AB)
    val Red90 = Color(0xFFFFDAD6)
    val Red100 = Color(0xFFFFFFFF)

    /** The signature element: gradient used by the omnibox progress hairline. */
    val EdgeDark = listOf(Color(0xFF7FDCC7), Color(0xFF62C6E8), Color(0xFFA99BF0))
    val EdgeLight = listOf(Color(0xFF00A98F), Color(0xFF2196C9), Color(0xFF7A66D9))

    fun edgeBrush(dark: Boolean): Brush =
        Brush.horizontalGradient(if (dark) EdgeDark else EdgeLight)
}

// Bookmark colour dots — codes are DATA (stored 1..11); do not renumber. Mirrors HelperUnit.setFilterIcons.
enum class BBDotColor(val code: Int, val color: Color) {
    GREY(1, Color(0xFF9E9E9E)),
    BROWN(2, Color(0xFF795548)),
    ORANGE(3, Color(0xFFFF9800)),
    YELLOW(4, Color(0xFFFFEB3B)),
    LIME(5, Color(0xffCDDC39)),
    GREEN(6, Color(0xFF4CAF50)),
    TEAL(7, Color(0xFF009688)),
    BLUE(8, Color(0xFF2196F3)),
    PURPLE(9, Color(0xFF9C27B0)),
    PINK(10, Color(0xFFE91E63)),
    RED(11, Color(0xFFF44336));

    companion object {
        fun byCode(code: Long): BBDotColor = entries.firstOrNull { it.code.toLong() == code } ?: GREY
    }
}

/** Light theme — cool porcelain with basalt-grey structure. */
val AuroraLight: ColorScheme = lightColorScheme(
    primary = AuroraBrand.Mint40,
    onPrimary = Color.White,
    primaryContainer = AuroraBrand.Mint80,
    onPrimaryContainer = AuroraBrand.Mint10,
    inversePrimary = AuroraBrand.Mint70,

    secondary = AuroraBrand.Slate40,
    onSecondary = Color.White,
    secondaryContainer = AuroraBrand.Slate90,
    onSecondaryContainer = AuroraBrand.Slate10,

    tertiary = AuroraBrand.Violet40,
    onTertiary = Color.White,
    tertiaryContainer = AuroraBrand.Violet90,
    onTertiaryContainer = AuroraBrand.Violet10,

    error = AuroraBrand.Red40,
    onError = AuroraBrand.Red100,
    errorContainer = AuroraBrand.Red90,
    onErrorContainer = AuroraBrand.Red10,

    background = Color(0xFFF5FAF8),
    onBackground = Color(0xFF161D1B),
    surface = Color(0xFFF5FAF8),
    onSurface = Color(0xFF161D1B),
    surfaceVariant = Color(0xFFDAE5E1),
    onSurfaceVariant = Color(0xFF3F4946),

    surfaceDim = Color(0xFFD5DBD9),
    surfaceBright = Color(0xFFF5FAF8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEFF5F3),
    surfaceContainer = Color(0xFFE9EFED),
    surfaceContainerHigh = Color(0xFFE3EAE8),
    surfaceContainerHighest = Color(0xFFDDE4E2),

    outline = Color(0xFF6F7976),
    outlineVariant = Color(0xFFBFC9C5),

    inverseSurface = Color(0xFF2B3230),
    inverseOnSurface = Color(0xFFECF2EF),
    scrim = Color.Black,
)

/** Dark theme — polished obsidian, faintly cool, never pure black (that is AMOLED's job). */
val AuroraDark: ColorScheme = darkColorScheme(
    primary = AuroraBrand.Mint70,
    onPrimary = AuroraBrand.Mint20,
    primaryContainer = AuroraBrand.Mint30,
    onPrimaryContainer = AuroraBrand.Mint80,
    inversePrimary = AuroraBrand.Mint40,

    secondary = AuroraBrand.Slate80,
    onSecondary = AuroraBrand.Slate20,
    secondaryContainer = AuroraBrand.Slate30,
    onSecondaryContainer = AuroraBrand.Slate90,

    tertiary = AuroraBrand.Violet80,
    onTertiary = AuroraBrand.Violet20,
    tertiaryContainer = AuroraBrand.Violet30,
    onTertiaryContainer = AuroraBrand.Violet90,

    error = AuroraBrand.Red80,
    onError = AuroraBrand.Red20,
    errorContainer = AuroraBrand.Red30,
    onErrorContainer = AuroraBrand.Red90,

    background = Color(0xFF0D1312),
    onBackground = Color(0xFFDCE3E0),
    surface = Color(0xFF0D1312),
    onSurface = Color(0xFFDCE3E0),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBEC9C4),

    surfaceDim = Color(0xFF0D1312),
    surfaceBright = Color(0xFF333A38),
    surfaceContainerLowest = Color(0xFF070C0B),
    surfaceContainerLow = Color(0xFF141A19),
    surfaceContainer = Color(0xFF181F1D),
    surfaceContainerHigh = Color(0xFF212827),
    surfaceContainerHighest = Color(0xFF2B3331),

    outline = Color(0xFF88938E),
    outlineVariant = Color(0xFF3F4946),

    inverseSurface = Color(0xFFDCE3E0),
    inverseOnSurface = Color(0xFF2B3230),
    scrim = Color.Black,
)

/** AMOLED — same accents on absolute black; containers stay hairline-thin above zero. */
fun auroraAmoled(): ColorScheme = AuroraDark.copy(
    background = Color.Black,
    onBackground = Color(0xFFE0E6E3),
    surface = Color.Black,
    onSurface = Color(0xFFE0E6E3),
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF333A38),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A100F),
    surfaceContainer = Color(0xFF0E1514),
    surfaceContainerHigh = Color(0xFF161D1C),
    surfaceContainerHighest = Color(0xFF202726),
    outline = Color(0xFF6C7672),
    outlineVariant = Color(0xFF232B29),
    inverseSurface = Color(0xFFE0E6E3),
    inverseOnSurface = Color(0xFF2B3230),
)

/** Semantic warning pair (insecure HTTP etc.) resolved per theme. */
data class SignalAmber(val strong: Color, val container: Color, val onContainer: Color)

val lightAmber = SignalAmber(AuroraBrand.Amber40, AuroraBrand.Amber90, AuroraBrand.Amber10)
val darkAmber = SignalAmber(AuroraBrand.Amber80, AuroraBrand.Amber30, AuroraBrand.Amber90)
