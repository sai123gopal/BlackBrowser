@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.saigopal.browser.ui.screens.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.saigopal.browser.R
import com.saigopal.browser.ui.design.theme.BBSpace

/*
 * Context menus (§6.2, §6.3): long-press targets on web content and on list rows.
 * Same row-sheet shape as the overview action sheet so every "what can I do here" surface
 * in the app behaves identically.
 */

@Composable
private fun ContextMenuSheet(
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit,
    actions: List<Triple<String, ImageVector?, () -> Unit>>,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = BBSpace.lg),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = BBSpace.lg),
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = BBSpace.lg),
                )
            }
            actions.forEach { (label, icon, onClick) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = BBSpace.touchTarget)
                        .clickable {
                            onDismiss()
                            onClick()
                        }
                        .padding(horizontal = BBSpace.lg, vertical = BBSpace.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (icon != null) {
                        androidx.compose.material3.Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.padding(BBSpace.md))
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** Port of show_contextMenu_link; title shows the target URL (spec §6.2). */
@Composable
fun LinkContextMenu(
    url: String,
    actions: LinkContextActions,
    onDismiss: () -> Unit,
) {
    ContextMenuSheet(
        title = url,
        subtitle = null,
        onDismiss = onDismiss,
        actions = listOf(
            Triple(stringResource(R.string.main_menu_new_tabOpen), Icons.Filled.Add, actions.onNewTabForeground),
            Triple(stringResource(R.string.main_menu_new_tab), Icons.Filled.PlayArrow, actions.onNewTabBackground),
            Triple(stringResource(R.string.menu_share_link), Icons.Filled.Share, actions.onShare),
            Triple(stringResource(R.string.menu_open_with), Icons.Filled.ExitToApp, actions.onOpenWith),
            Triple(stringResource(R.string.menu_save_as), Icons.Filled.Edit, actions.onSaveAs),
            Triple(stringResource(R.string.menu_save_home), Icons.Filled.Home, actions.onAddToHome),
            Triple(stringResource(R.string.menu_shareClipboard), null, actions.onCopyLink),
        ),
    )
}

/**
 * Shared record menu for bookmarks/history/home rows (§6.3). Delete/edit entries appear only
 * when their lambdas are provided (edit is bookmark-only).
 */
@Composable
fun RecordContextMenu(
    title: String,
    url: String,
    isBookmark: Boolean,
    actions: RecordContextActions,
    onDismiss: () -> Unit,
) {
    val rows = mutableListOf<Triple<String, ImageVector?, () -> Unit>>(
        Triple(stringResource(R.string.main_menu_new_tabOpen), Icons.Filled.Add, actions.onNewTabForeground),
        Triple(stringResource(R.string.main_menu_new_tab), Icons.Filled.PlayArrow, actions.onNewTabBackground),
    )
    actions.onEdit?.let {
        if (isBookmark) rows.add(Triple(stringResource(R.string.menu_edit), Icons.Filled.Edit, it))
    }
    actions.onDelete?.let { rows.add(Triple(stringResource(R.string.menu_delete), Icons.Filled.Delete, it)) }

    ContextMenuSheet(
        title = title.ifBlank { url },
        subtitle = url,
        onDismiss = onDismiss,
        actions = rows,
    )
}
