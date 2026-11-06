@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.saigopal.browser.ui.screens.menu

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saigopal.browser.R
import com.saigopal.browser.ui.design.theme.BBSpace
import com.saigopal.browser.ui.design.theme.LocalAurora
import com.saigopal.browser.ui.design.theme.PillShape
import com.saigopal.browser.ui.di.rememberContainer

/*
 * Modern Page Menu Bottom Sheet:
 * - Site Identity Header Card with live security status, tracker block badge, reload and share
 * - Quick Actions Row (New Tab, Incognito, Desktop Site, Find)
 * - Grouped Card Sections (Bookmarks & Home, Tools & Exports, System & Settings)
 */

@Composable
fun PageMenuSheet(
    page: PageInfo,
    actions: PageMenuActions,
    onDismiss: () -> Unit,
    onReload: () -> Unit = {},
    onIncognitoChanged: ((Boolean) -> Unit)? = null,
) {
    val aurora = LocalAurora.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BBSpace.md)
                .padding(bottom = BBSpace.xl),
            verticalArrangement = Arrangement.spacedBy(BBSpace.sm),
        ) {
            // ---- Site Identity / Home Card ---------------------------------------------------------
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(BBSpace.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (page.isStartPage) {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        } else {
                            Icon(
                                if (page.isSecure) Icons.Filled.Lock else Icons.Filled.Warning,
                                contentDescription = stringResource(
                                    if (page.isSecure) R.string.bb_secure_connection else R.string.bb_insecure_connection,
                                ),
                                tint = if (page.isSecure) MaterialTheme.colorScheme.primary else aurora.amber.strong,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Spacer(Modifier.size(BBSpace.xs))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (page.isStartPage) "BlackBrowser Home" else page.title.ifBlank { page.url.orEmpty() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (page.isStartPage) "Fast, Private & Secure" else page.url.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!page.isStartPage && page.blockedCount > 0) {
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = PillShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text = "${page.blockedCount} trackers blocked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    if (!page.isStartPage) {
                        IconButton(onClick = {
                            onDismiss()
                            onReload()
                        }) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.bb_menu_reload),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ---- Quick Action Grid ------------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BBSpace.xs),
            ) {
                QuickActionPill(
                    icon = Icons.Filled.Add,
                    label = stringResource(R.string.main_menu_new_tabOpen),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        actions.onNewTab()
                    },
                )
                if (page.isStartPage) {
                    QuickActionPill(
                        icon = Icons.Filled.ShoppingCart,
                        label = stringResource(R.string.menu_download),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            actions.onOpenDownloads()
                        },
                    )
                    QuickActionPill(
                        icon = Icons.Filled.Settings,
                        label = stringResource(R.string.setting_label),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            actions.onOpenSettings()
                        },
                    )
                } else {
                    QuickActionPill(
                        icon = Icons.Filled.Search,
                        label = stringResource(R.string.menu_other_searchSite),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            actions.onFindInPage()
                        },
                    )
                    if (actions.onToggleDesktop != null) {
                        QuickActionPill(
                            icon = Icons.Filled.Build,
                            label = stringResource(R.string.bb_menu_desktop_site),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onDismiss()
                                actions.onToggleDesktop.invoke()
                            },
                        )
                    }
                    QuickActionPill(
                        icon = Icons.Filled.Share,
                        label = stringResource(R.string.menu_share_link),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            actions.onShare()
                        },
                    )
                }
            }

            // ---- AdBlock & Privacy Shield Card (Webpages only) --------------------------------------
            if (!page.isStartPage) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AdBlockShieldRow(
                        isShieldActive = page.isShieldActive,
                        blockedCount = page.blockedCount,
                        onToggleShield = actions.onToggleShield,
                        onOpenWhitelist = actions.onOpenWhitelist,
                    )
                }
            }

            // ---- Incognito Card ---------------------------------------------------------------------
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IncognitoRow(page.isIncognito, onToggled = onIncognitoChanged)
            }

            // ---- Bookmarks & Home Card (Webpages only) ----------------------------------------------
            if (!page.isStartPage) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        MenuRowItem(
                            icon = Icons.Filled.Star,
                            label = stringResource(R.string.menu_fav),
                            onClick = { onDismiss(); actions.onSetFavorite() },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        MenuRowItem(
                            icon = Icons.Filled.Refresh,
                            label = "Reset Favorite",
                            onClick = { onDismiss(); actions.onResetFavorite() },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        MenuRowItem(
                            icon = Icons.Filled.FavoriteBorder,
                            label = stringResource(R.string.menu_save_bookmark),
                            onClick = { onDismiss(); actions.onAddBookmark() },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        MenuRowItem(
                            icon = Icons.Filled.AddCircle,
                            label = stringResource(R.string.menu_sc),
                            onClick = { onDismiss(); actions.onCreateShortcut() },
                        )
                    }
                }

                // ---- Tools & Downloads Card (Webpages only) -----------------------------------------
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        MenuRowItem(
                            icon = Icons.Filled.List,
                            label = stringResource(R.string.menu_shareClipboard),
                            onClick = { onDismiss(); actions.onCopyLink() },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        MenuRowItem(
                            icon = Icons.Filled.ExitToApp,
                            label = stringResource(R.string.menu_open_with),
                            onClick = { onDismiss(); actions.onOpenWith() },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        MenuRowItem(
                            icon = Icons.Filled.Done,
                            label = stringResource(R.string.menu_save_pdf),
                            onClick = { onDismiss(); actions.onPrintPdf() },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        MenuRowItem(
                            icon = Icons.Filled.ShoppingCart,
                            label = stringResource(R.string.menu_download),
                            onClick = { onDismiss(); actions.onOpenDownloads() },
                        )
                    }
                }
            }

            // ---- App & System Card ------------------------------------------------------------------
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    if (!page.isStartPage) {
                        MenuRowItem(
                            icon = Icons.Filled.Settings,
                            label = stringResource(R.string.setting_label),
                            onClick = { onDismiss(); actions.onOpenSettings() },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                    MenuRowItem(
                        icon = Icons.Filled.Close,
                        label = stringResource(R.string.menu_quit),
                        onClick = { onDismiss(); actions.onQuit() },
                        destructive = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .height(64.dp)
            .clickable(onClick = onClick),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AdBlockShieldRow(
    isShieldActive: Boolean,
    blockedCount: Int,
    onToggleShield: (() -> Unit)?,
    onOpenWhitelist: (() -> Unit)?,
) {
    var active by remember(isShieldActive) { mutableStateOf(isShieldActive) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BBSpace.md, vertical = BBSpace.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = if (active) androidx.compose.ui.graphics.Color(0xFF00C853).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Security,
                        contentDescription = null,
                        tint = if (active) androidx.compose.ui.graphics.Color(0xFF00C853) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.size(BBSpace.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "AdBlock & Privacy Shield",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (active) {
                        if (blockedCount > 0) "$blockedCount ads & trackers blocked" else "Shield active • Clean browsing"
                    } else {
                        "Shield paused on this site"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (active && blockedCount > 0) androidx.compose.ui.graphics.Color(0xFF00C853) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = active,
                onCheckedChange = { on ->
                    active = on
                    onToggleShield?.invoke()
                },
            )
        }

        if (onOpenWhitelist != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Manage Allowed Sites →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onOpenWhitelist)
                    .padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun IncognitoRow(initialActive: Boolean, onToggled: ((Boolean) -> Unit)? = null) {
    var active by remember(initialActive) { mutableStateOf(initialActive) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = BBSpace.md, vertical = BBSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(BBSpace.sm))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.bb_menu_incognito),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "History is not recorded. Supported WebView versions isolate cookies and site storage; older versions may share site storage.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = active,
            onCheckedChange = { on ->
                active = on
                onToggled?.invoke(on)
            },
        )
    }
}

@Composable
private fun MenuRowItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = BBSpace.md, vertical = BBSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (destructive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.padding(BBSpace.sm))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}
