package com.saigopal.browser.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saigopal.browser.ui.design.theme.BBMotion
import com.saigopal.browser.ui.design.theme.BBSpace
import com.saigopal.browser.ui.design.theme.LocalAurora
import com.saigopal.browser.ui.design.theme.LocalReducedMotion
import com.saigopal.browser.ui.design.theme.PillShape
import kotlinx.coroutines.delay

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

/*
 * The omnibox pill — the app's most-seen surface — carries the brand signature:
 * a 3dp "aurora edge" hairline across its top that doubles as the load progress bar.
 * Idle: hidden. Loading: gradient sweeps, sweep fraction IS the progress.
 */

/** Fraction of the pill's top edge painted by the aurora gradient while [progress] runs. */
@Composable
fun rememberAuroraEdgeFraction(progress: Int?, enabled: Boolean): Float {
    if (!enabled) return 0f
    val isLoading = progress != null && progress in 0..99
    val targetFraction = if (isLoading) {
        (progress!! / 100f).coerceIn(0.08f, 0.98f)
    } else {
        0f
    }
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = if (isLoading) {
            tween(durationMillis = 300, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 350, easing = FastOutLinearInEasing)
        },
        label = "auroraProgress",
    )
    return if (targetFraction == 0f && animatedFraction < 0.02f) 0f else animatedFraction
}

/**
 * Aurora hairline across the top of the current surface. Must be placed INSIDE a container
 * already clipped to its shape (M3 Surface does this), so the rounded ends come free.
 */
@Composable
fun AuroraEdge(
    fraction: Float,
    brush: Brush,
    modifier: Modifier = Modifier,
    hairlineHeight: Dp = BBSpace.edgeHairline,
) {
    if (fraction <= 0f) return
    Box(
        modifier
            .fillMaxWidth()
            .height(hairlineHeight),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(hairlineHeight)
                .background(brush),
        )
    }
}

val OmniboxPillHeight: Dp = 52.dp

/**
 * Collapsed omnibox pill: leading status icon, single-line title-or-URL, trailing actions.
 * Not editable — tapping enters edit mode upstream. [progress] in 0..100 drives the edge;
 * pass null/100 when idle.
 */
@Composable
fun BBOmniboxPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingTint: Color? = null,
    insecure: Boolean = false,
    progress: Int? = null,
    contentDescription: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val aurora = LocalAurora.current
    val edgeFraction = rememberAuroraEdgeFraction(progress, enabled = true)

    Surface(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription?.let { this.contentDescription = it } },
        shape = PillShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Column {
            AuroraEdge(
                fraction = edgeFraction,
                brush = aurora.edge,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(OmniboxPillHeight)
                    .padding(start = BBSpace.md, end = BBSpace.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = when {
                            insecure -> aurora.amber.strong
                            leadingTint != null -> leadingTint
                            else -> MaterialTheme.colorScheme.primary
                        },
                    )
                    Spacer(Modifier.width(BBSpace.sm))
                }
                Text(
                    text = text.ifEmpty { " " },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                trailing()
            }
        }
    }
}
