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
import androidx.compose.foundation.layout.width
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
        items(tabs, key = { it.id }, contentType = { "tab_card" }) { tab ->
            DismissibleTabCard(
                tab = tab,
                isActive = tab.id == activeId,
                onActivate = { onActivate(tab.id) },
                onClose = { onClose(tab.id) },
            )
        }
        item(key = "new_tab", contentType = "new_tab") {
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
            // Header: favicon + site title + incognito badge + close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = BBSpace.sm, top = BBSpace.xs, end = BBSpace.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {                // Favicon in header
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    com.saigopal.browser.ui.design.components.LetterTile(
                        label = tab.title,
                        url = tab.url,
                        tileSize = 18.dp,
                    )
                    if (!tab.url.isNullOrEmpty()) {
                        // PERF: AsyncImage over the LetterTile instead of SubcomposeAsyncImage —
                        // identical look (letter until loaded, bitmap on success) with no
                        // per-cell subcomposition in the tab-switcher grid.
                        coil.compose.AsyncImage(
                            model = "https://www.google.com/s2/favicons?sz=64&domain_url=${tab.url}",
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))

                Text(
                    text = tab.title.ifBlank { tab.url.orEmpty().ifBlank { stringResource(R.string.album_title_tab) } },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (tab.isIncognito) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(18.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = "Incognito",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }

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

            // Body: Rich website viewport preview
            val isStart = com.saigopal.browser.domain.UrlInterpreter.isStartPage(tab.url)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = BBSpace.xs)
                    .padding(bottom = BBSpace.xs)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentAlignment = Alignment.Center,
            ) {
                if (isStart) {
                    // Start page miniature mockup preview
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(BBSpace.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Start Page",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    // Website preview viewport
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(BBSpace.xs),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Mini URL bar
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (tab.url.orEmpty().startsWith("https")) {
                                        androidx.compose.material.icons.Icons.Filled.Lock
                                    } else {
                                        androidx.compose.material.icons.Icons.Filled.Info
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(10.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                val host = com.saigopal.browser.domain.UrlInterpreter.domainOf(tab.url.orEmpty())
                                Text(
                                    text = host.ifBlank { tab.url.orEmpty() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                                )
                            }
                        }

                        // Central website preview graphic / favicon
                        Spacer(Modifier.weight(1f))
                        com.saigopal.browser.ui.design.components.BBFavicon(
                            label = tab.title,
                            url = tab.url,
                            size = 52.dp,
                            shape = RoundedCornerShape(14.dp),
                        )

                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = tab.title.ifBlank { tab.url.orEmpty() },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(Modifier.weight(1f))

                        if (tab.isDesktopMode) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = "Desktop",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp),
                                )
                            }
                        }
                    }
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
