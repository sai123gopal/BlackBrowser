package com.saigopal.browser.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saigopal.browser.ui.design.theme.BBSpace

/*
 * Small structural pieces used by every later screen. Quiet by design — the aurora edge on the
 * omnibox is the one loud element in the system; everything here stays disciplined M3.
 */

/** Eyebrow section header — uppercase micro-label; use ONLY to mark real groupings. */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            start = BBSpace.md,
            end = BBSpace.md,
            top = BBSpace.xl, // clear air above each section separates groups while scrolling
            bottom = BBSpace.xs,
        ),
    )
}

/** Filled-tonal circular icon button — the standard chrome control (48dp target). */
@Composable
fun BBIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = Color.Transparent,
    size: Dp = BBSpace.touchTarget,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

/** Elevated card with the system's medium radius. */
@Composable
fun BBCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit,
) {
    val m = modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .background(containerColor)
    if (onClick != null) {
        Box(m.clickable(onClick = onClick)) { content() }
    } else {
        Box(m) { content() }
    }
}

/** Empty state: explains what will live here and how to create it. No mood copy. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    hint: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BBSpace.xl, vertical = BBSpace.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(BBSpace.sm))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hint != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/**
 * Standard list row: leading slot (icon or dot), title + optional subtitle, trailing slot.
 * Enforces the 56dp minimum row height so lists feel identical across screens.
 */
@Composable
fun BBListItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    maxTitleLines: Int = 1,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = BBSpace.md, vertical = BBSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { leading() }
            Spacer(Modifier.width(BBSpace.md))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = maxTitleLines,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(BBSpace.sm))
            trailing()
        }
    }
}
