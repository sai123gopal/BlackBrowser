package com.saigopal.browser.ui.design.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saigopal.browser.ui.design.theme.BBSpace
import kotlinx.coroutines.delay

/**
 * Payload describing a top sneakbar notification message.
 */
data class TopSneakbarData(
    val message: String,
    val icon: ImageVector? = null,
    val iconTint: Color? = null,
    val durationMs: Long = 2500L,
    val id: Long = System.currentTimeMillis(),
)

@Stable
class TopSneakbarState {
    var currentData by mutableStateOf<TopSneakbarData?>(null)
        private set

    fun show(
        message: String,
        icon: ImageVector? = null,
        iconTint: Color? = null,
        durationMs: Long = 2500L,
    ) {
        currentData = TopSneakbarData(
            message = message,
            icon = icon,
            iconTint = iconTint,
            durationMs = durationMs,
            id = System.currentTimeMillis(),
        )
    }

    fun dismiss() {
        currentData = null
    }
}

@Composable
fun rememberTopSneakbarState(): TopSneakbarState = remember { TopSneakbarState() }

/**
 * Top-anchored Sneakbar Host — displays sleek, pill-shaped HUD notifications at the top of the
 * viewport with smooth spring animations. Designed for instant visual confirmation on menu actions.
 */
@Composable
fun TopSneakbarHost(
    state: TopSneakbarState,
    modifier: Modifier = Modifier,
) {
    val data = state.currentData

    LaunchedEffect(data?.id) {
        if (data != null) {
            delay(data.durationMs)
            if (state.currentData?.id == data.id) {
                state.dismiss()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = BBSpace.sm, start = BBSpace.md, end = BBSpace.md),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = data != null,
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                initialOffsetY = { -it - 40 },
            ) + fadeIn(),
            exit = slideOutVertically(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                targetOffsetY = { -it - 40 },
            ) + fadeOut(),
        ) {
            if (data != null) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .wrapContentWidth()
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(24.dp),
                        ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (data.icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = (data.iconTint ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.15f),
                                        shape = CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = data.icon,
                                    contentDescription = null,
                                    tint = data.iconTint ?: MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            text = data.message,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
