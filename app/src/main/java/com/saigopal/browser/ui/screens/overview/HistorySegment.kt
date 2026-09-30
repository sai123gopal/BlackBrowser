package com.saigopal.browser.ui.screens.overview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saigopal.browser.R
import com.saigopal.browser.ui.design.components.BBConfirmSheet
import com.saigopal.browser.ui.design.components.EmptyState
import com.saigopal.browser.ui.design.components.LetterTile
import com.saigopal.browser.ui.design.components.SectionHeader
import com.saigopal.browser.ui.design.theme.BBSpace
import com.saigopal.browser.ui.di.rememberContainer
import com.saigopal.browser.unit.RecordUnit
import kotlinx.coroutines.launch

/*
 * History segment (P04 §4.6): day-grouped list, tap opens in current tab, long-press for
 * open-in-new-tab / delete, header action clears everything.
 */

@Composable
fun HistorySegment(
    refreshTick: Int,
    onLoadUrl: (url: String, inNewTab: Boolean) -> Unit,
    onChanged: () -> Unit,
) {
    val container = rememberContainer()
    val scope = rememberCoroutineScope()
    var days by remember { mutableStateOf<List<HistoryDay>>(emptyList()) }
    var confirmClear by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTick) {
        val records = container.records.listHistory()
        days = groupHistoryByDay(
            entries = records.map { HistoryEntry(it.title, it.url, it.time) },
        )
    }

    LazyColumn(Modifier.fillMaxWidth()) {
        item(key = "history_clear") {
            Text(
                stringResource(R.string.clear_title_history),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .clickable { confirmClear = true }
                    .padding(horizontal = BBSpace.md, vertical = BBSpace.xs),
            )
        }
        if (days.isEmpty()) {
            item(key = "history_empty") {
                EmptyState(
                    icon = Icons.Filled.PlayArrow,
                    title = stringResource(R.string.album_title_history),
                    hint = stringResource(R.string.bb_history_empty_hint),
                )
            }
        } else {
            days.forEach { day ->
                item(key = "day_${day.label}") {
                    SectionHeader(day.label)
                }
                itemsIndexed(day.items, key = { index, entry -> "h_${day.label}_${entry.url}_${entry.time}_$index" }) { _, entry ->
                    HistoryRow(
                        entry = entry,
                        onClick = { onLoadUrl(entry.url, false) },
                        onDelete = {
                            scope.launch {
                                container.records.deleteUrl(entry.url, RecordUnit.TABLE_HISTORY)
                                onChanged()
                            }
                        },
                        onOpenNewTab = { onLoadUrl(entry.url, true) },
                    )
                }
            }
        }
    }

    if (confirmClear) {
        BBConfirmSheet(
            title = stringResource(R.string.clear_title_history),
            body = stringResource(R.string.bb_clear_history_confirm),
            confirmLabel = stringResource(R.string.menu_delete),
            destructive = true,
            onConfirm = {
                scope.launch {
                    container.records.clearHistory()
                    onChanged()
                }
            },
            onDismiss = { confirmClear = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onClick: () -> Unit,
    onOpenNewTab: () -> Unit,
    onDelete: () -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = { showSheet = true })
            .padding(horizontal = BBSpace.md, vertical = BBSpace.xs),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        // Website image / favicon tile
        com.saigopal.browser.ui.design.components.BBFavicon(
            label = entry.title,
            url = entry.url,
            size = 40.dp,
            shape = RoundedCornerShape(10.dp),
        )

        Spacer(Modifier.width(BBSpace.sm))

        Column(Modifier.weight(1f)) {
            Text(
                text = entry.title.ifBlank { entry.url },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.url.removePrefix("https://").removePrefix("http://").removePrefix("www."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (entry.time > 0) {
                    Text(
                        text = " • ${android.text.format.DateUtils.formatDateTime(LocalContext.current, entry.time, android.text.format.DateUtils.FORMAT_SHOW_TIME)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                    )
                }
            }
        }
    }

    if (showSheet) {
        OverviewActionSheet(
            title = entry.title.ifBlank { entry.url },
            subtitle = entry.url,
            onDismiss = { showSheet = false },
            actions = listOf(
                SheetAction(stringResource(R.string.main_menu_new_tabOpen), Icons.Filled.PlayArrow, onOpenNewTab),
                SheetAction(stringResource(R.string.menu_delete), Icons.Filled.Delete, onDelete),
            ),
        )
    }
}
