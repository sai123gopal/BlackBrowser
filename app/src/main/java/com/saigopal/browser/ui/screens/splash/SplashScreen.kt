package com.saigopal.browser.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.saigopal.browser.ui.design.theme.AuroraBrand
import com.saigopal.browser.ui.design.theme.PillShape
import kotlinx.coroutines.delay

/*
 * SplashScreen — the brand moment on cold start.
 *
 * Dark (obsidian) canvas from frame one — matching the DARK/AMOLED identity — with:
 *  · the aurora ring logo scaling in on a spring, its highlight arc slowly rotating;
 *  · the wordmark fading/sliding up right after;
 *  · a breathing "BROWSE FREELY" eyebrow;
 *  · two faint aurora swooshes echoing the start page background.
 *
 * Purely additive motion: it dismisses itself via [onDone]; if the transition misses the
 * signal the parent LaunchedEffect hard-exits after a safety timeout.
 */

@Composable
fun SplashScreen(onDone: () -> Unit, modifier: Modifier = Modifier) {
    // ---- choreography -----------------------------------------------------------------------
    val ringScale = remember { Animatable(0.6f) }
    val ringAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffset = remember { Animatable(24f) }

    LaunchedEffect(Unit) {
        ringAlpha.animateTo(1f, tween(180))
        ringScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 220f),
        )
    }
    LaunchedEffect(Unit) {
        delay(240)
        textAlpha.animateTo(1f, tween(260))
        textOffset.animateTo(0f, tween(260))
    }

    // Slow rotation of the ring's bright highlight arc.
    val infinite = rememberInfiniteTransition(label = "splash")
    val sweepAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    val breathe by infinite.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathe",
    )

    // Self-dismiss after the intro (long enough to read, short enough to not annoy).
    LaunchedEffect(Unit) {
        delay(1250)
        onDone()
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Color(0xFF090D0F)), // obsidian — same as start page canvas base
        contentAlignment = Alignment.Center,
    ) {
        // Faint aurora swooshes (same design language as the start page).
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.35f, 0f)
                cubicTo(w * 0.55f, h * 0.08f, w * 0.85f, h * 0.14f, w, h * 0.30f)
            }
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0x1420E398), Color(0x3320E398), Color(0x0020E398)),
                    start = Offset(w * 0.35f, 0f),
                    end = Offset(w, h * 0.30f),
                ),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // Outer soft glow
                Canvas(Modifier.size(148.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x4020E398),
                                Color.Transparent,
                            ),
                        ),
                        radius = size.minDimension / 2f,
                    )
                }
                // The brand ring (launcher-logo echo) with rotating highlight.
                Canvas(
                    Modifier
                        .size(116.dp)
                        .scale(ringScale.value)
                        .alpha(ringAlpha.value),
                ) {
                    drawCircle(
                        color = AuroraBrand.Mint70,
                        radius = size.minDimension / 2f - 6f,
                        style = Stroke(width = 10f, cap = StrokeCap.Round),
                        alpha = 0.9f * breathe,
                    )
                    drawArc(
                        color = Color.White,
                        startAngle = sweepAngle,
                        sweepAngle = 70f,
                        useCenter = false,
                        style = Stroke(width = 10f, cap = StrokeCap.Round),
                        alpha = 0.55f,
                    )
                }
                // Wordmark dot — the aurora mint "browser" half of the brand.
                Canvas(Modifier.size(30.dp)) {
                    drawCircle(color = AuroraBrand.Mint70, radius = size.minDimension / 2f)
                }
            }

            Spacer(Modifier.height(22.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .graphicsLayer { translationY = textOffset.value },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Black",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = " Browser",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AuroraBrand.Mint70,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "BROWSE FREELY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.5.sp,
                        fontSize = 10.sp,
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF7E8B92),
                )
            }
        }
    }
}
