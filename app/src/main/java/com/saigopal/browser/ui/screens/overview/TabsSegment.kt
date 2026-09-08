package com.saigopal.browser.ui.screens.overview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saigopal.browser.R
import com.saigopal.browser.domain.TabState
import com.saigopal.browser.ui.design.theme.BBSpace

/*
 * Modern Tab Switcher Grid:
 * - Card view with website title, domain preview, close button, and active status glow
 * - Incognito indicator badge
 * - Dismiss with horizontal swipe
 * - "+ New Tab" hero action card
 */

@Composable
fun TabsSegment(
    tabs: List<TabState>,
    activeId: Long?,
    onActivate: (Long) -> Unit,
    onClose: (Long) -> Unit,
    onNewTab: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(156.dp),
        contentPadding = PaddingValues(BBSpace.md),
        horizontalArrangement = Arrangement.spacedBy(BBSpace.sm),
        verticalArrangement = Arrangement.spacedBy(BBSpace.sm),
    ) {
        items(tabs, key = { it.id }) { tab ->
            DismissibleTabCard(
                tab = tab,
                isActive = tab.id == activeId,
                onActivate = { onActivate(tab.id) },
                onClose = { onClose(tab.id) },
            )
        }
        item(key = "new_tab") {
            NewTabCard(onClick = onNewTab)
        }
    }
}

@Composable
private fun DismissibleTabCard(
    tab: TabState,
    isActive: Boolean,
    onActivate: () -> Unit,
    onClose: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onClose()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {},
    ) {
        TabCard(tab, isActive, onActivate, onClose)
    }
}

@Composable
private fun TabCard(
    tab: TabState,
    isActive: Boolean,
    onActivate: () -> Unit,
    onClose: () -> Unit,
) {
    val cardShape = RoundedCornerShape(16.dp)
    val borderStroke = if (isActive) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    Surface(
        shape = cardShape,
        color = if (isActive) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        tonalElevation = if (isActive) 6.dp else 2.dp,
        border = borderStroke,
        modifier = Modifier
            .aspectRatio(0.82f)
            .clip(cardShape)
            .clickable(onClick = onActivate),
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header: site title + close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = BBSpace.sm, top = BBSpace.xs, end = BBSpace.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (tab.isIncognito) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = "Incognito",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 2.dp),
                    )
                } else if (tab.url.orEmpty().startsWith("https")) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Secure",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 2.dp),
                    )
                }
                Text(
                    text = tab.title.ifBlank { tab.url.orEmpty().ifBlank { stringResource(R.string.album_title_tab) } },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.menu_closeTab),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Body: Webview miniature viewport preview placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = BBSpace.xs)
                    .padding(bottom = BBSpace.xs)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(BBSpace.sm),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (tab.title.firstOrNull() ?: tab.url?.firstOrNull() ?: 'W').uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(Modifier.height(BBSpace.xs))
                    val urlSnippet = tab.url?.removePrefix("https://")?.removePrefix("http://")?.removePrefix("www.")
                    Text(
                        text = urlSnippet ?: stringResource(R.string.app_name),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun NewTabCard(onClick: () -> Unit) {
    val cardShape = RoundedCornerShape(16.dp)
    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = Modifier
            .aspectRatio(0.82f)
            .clip(cardShape)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.main_menu_new_tabOpen),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(Modifier.height(BBSpace.sm))
            Text(
                text = stringResource(R.string.main_menu_new_tabOpen),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
