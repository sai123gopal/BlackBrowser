@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.saigopal.browser.ui.screens.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saigopal.browser.R
import com.saigopal.browser.ui.design.theme.BBSpace

/*
 * Row-based action sheets for overview items (ports of the legacy show_contextMenu_list sheets).
 * One shape for every list: title, optional url line, then full-width action rows.
 */

data class SheetAction(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
)

@Composable
fun OverviewActionSheet(
    title: String,
    onDismiss: () -> Unit,
    actions: List<SheetAction>,
    subtitle: String? = null,
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
                Spacer(Modifier.height(BBSpace.xxs))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = BBSpace.lg),
                )
            }
            Spacer(Modifier.height(BBSpace.sm))
            actions.forEach { action ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = BBSpace.touchTarget)
                        .clickable {
                            onDismiss()
                            action.onClick()
                        }
                        .padding(horizontal = BBSpace.lg, vertical = BBSpace.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (action.icon != null) {
                        Icon(
                            action.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.padding(BBSpace.sm))
                    }
                    Text(
                        action.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** Tile long-press sheet (Pinned tiles): open here / background / delete. */
@Composable
fun TileActionsSheet(
    title: String,
    onDismiss: () -> Unit,
    onOpenForeground: () -> Unit,
    onOpenBackground: () -> Unit,
    onDelete: () -> Unit,
) {
    OverviewActionSheet(
        title = title,
        onDismiss = onDismiss,
        actions = listOf(
            SheetAction("Open", Icons.Filled.PlayArrow, onOpenForeground),
            SheetAction(
                stringResource(R.string.main_menu_new_tab),
                Icons.Filled.ExitToApp,
                onOpenBackground,
            ),
            SheetAction(stringResource(R.string.menu_delete), Icons.Filled.Delete, onDelete),
        ),
    )
}
