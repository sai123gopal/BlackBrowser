package com.saigopal.browser.ui.screens.overview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saigopal.browser.R
import com.saigopal.browser.domain.BBRecord
import com.saigopal.browser.domain.BookmarkSort
import com.saigopal.browser.ui.design.components.BBConfirmSheet
import com.saigopal.browser.ui.design.components.BBFormSheet
import com.saigopal.browser.ui.design.components.BBTextFieldPill
import com.saigopal.browser.ui.design.components.ColorDotPicker
import com.saigopal.browser.ui.design.components.EmptyState
import com.saigopal.browser.ui.design.theme.BBDotColor
import com.saigopal.browser.ui.design.theme.BBSpace
import com.saigopal.browser.ui.di.rememberContainer
import kotlinx.coroutines.launch

/*
 * Saved segment (P04 §4.5): bookmarks with legacy colour dots, sort (title/time via the
 * sort_bookmark pref), single-colour filter chips, per-row actions, clear-all.
 */

@Composable
fun SavedSegment(
    bookmarkSort: BookmarkSort,
    enabledColors: Set<Int>,
    refreshTick: Int,
    onLoadUrl: (url: String, inNewTab: Boolean) -> Unit,
    onChanged: () -> Unit,
) {
    val container = rememberContainer()
    val scope = rememberCoroutineScope()
    var bookmarks by remember { mutableStateOf<List<BBRecord>>(emptyList()) }
    var filterByCode by remember { mutableStateOf(0) } // 0 = off; else legacy colour code 1..11
    var actionFor by remember { mutableStateOf<BBRecord?>(null) }
    var editing by remember { mutableStateOf<BBRecord?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            bookmarks = container.records.listBookmarks(
                filter = filterByCode != 0,
                filterBy = filterByCode.toLong(),
            )
        }
    }

    LaunchedEffect(refreshTick, filterByCode) {
        bookmarks = container.records.listBookmarks(
            filter = filterByCode != 0,
            filterBy = filterByCode.toLong(),
        )
    }

    LazyColumn(Modifier.fillMaxWidth()) {
        item(key = "saved_controls") {
            Row(
                Modifier.padding(horizontal = BBSpace.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    when (bookmarkSort) {
                        BookmarkSort.TITLE -> stringResource(R.string.dialog_sortName)
                        BookmarkSort.TIME -> stringResource(R.string.dialog_sortDate)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        val next =
                            if (bookmarkSort == BookmarkSort.TITLE) BookmarkSort.TIME else BookmarkSort.TITLE
                        container.settings.update { it.copy(bookmarkSort = next) }
                    },
                )
                Spacer(Modifier.width(BBSpace.lg))
                Text(
                    stringResource(R.string.menu_delete),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable { confirmClear = true },
                )
            }
            if (enabledColors.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = BBSpace.md),
                    horizontalArrangement = Arrangement.spacedBy(BBSpace.xs),
                    modifier = Modifier.padding(top = BBSpace.xs),
                ) {
                    items(
                        BBDotColor.entries.filter { it.code in enabledColors },
                        key = { it.code },
                        contentType = { "bookmark_filter" },
                    ) { dot ->
                        val selected = dot.code == filterByCode
                        Box(
                            Modifier
                                .padding(vertical = BBSpace.xxs)
                                .size(if (selected) 28.dp else 22.dp)
                                .clip(CircleShape)
                                .background(dot.color)
                                .clickable {
                                    filterByCode = if (selected) 0 else dot.code
                                    reload()
                                },
                        )
                    }
                }
            }
        }

        if (bookmarks.isEmpty()) {
            item(key = "saved_empty") {
                EmptyState(
                    icon = Icons.Filled.PlayArrow,
                    title = stringResource(R.string.album_title_bookmarks),
                    hint = stringResource(R.string.bb_saved_empty_hint),
                )
            }
        } else {
            itemsIndexed(
                bookmarks,
                key = { index, record -> "bm_${record.time}_${record.url}_$index" },
                contentType = { _, _ -> "bookmark" },
            ) { _, record ->
                BookmarkRow(
                    record = record,
                    onClick = { onLoadUrl(record.url, false) },
                    onLongClick = { actionFor = record },
                )
            }
        }
    }

    actionFor?.let { record ->
        OverviewActionSheet(
            title = record.title.ifBlank { record.url },
            subtitle = record.url,
            onDismiss = { actionFor = null },
            actions = listOf(
                SheetAction(stringResource(R.string.main_menu_new_tabOpen), Icons.Filled.PlayArrow) {
                    onLoadUrl(record.url, true)
                },
                SheetAction(stringResource(R.string.main_menu_new_tab), Icons.Filled.ExitToApp) {
                    onLoadUrl(record.url, true)
                },
                SheetAction(stringResource(R.string.menu_edit), Icons.Filled.Edit) {
                    editing = record
                },
                SheetAction(stringResource(R.string.menu_delete), Icons.Filled.Delete) {
                    scope.launch {
                        container.records.deleteBookmark(record.url)
                        onChanged()
                    }
                },
            ),
        )
    }

    editing?.let { record ->
        BookmarkEditDialog(
            initialTitle = record.title,
            onDismiss = { editing = null },
            onSave = { newTitle, colorCode ->
                editing = null
                scope.launch {
                    container.records.editBookmark(record.url, newTitle, colorCode)
                    onChanged()
                }
            },
        )
    }

    if (confirmClear) {
        BBConfirmSheet(
            title = stringResource(R.string.album_title_bookmarks),
            body = stringResource(R.string.bb_clear_bookmarks_confirm),
            confirmLabel = stringResource(R.string.menu_delete),
            destructive = true,
            onConfirm = {
                scope.launch {
                    container.records.clearBookmarks()
                    onChanged()
                }
            },
            onDismiss = { confirmClear = false },
        )
    }
}

/** Colour dot + title + url row; whole row long-pressable for the context sheet. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkRow(
    record: BBRecord,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = BBSpace.md, vertical = BBSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(dotColor(record.colorCode)),
        )
        Spacer(Modifier.width(BBSpace.md))
        Column(Modifier.weight(1f)) {
            Text(
                record.title.ifBlank { record.url },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                record.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Composable colour resolution: legacy code → dot colour; uncoloured rows get a faint outline. */
@Composable
private fun dotColor(code: Int): Color =
    if (code in 1..11) {
        BBDotColor.byCode(code.toLong()).color
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }

/** Legacy edit-bookmark semantics: new title + colour (TIME-column encoding lives in the repo).
 * UI migrated from AlertDialog to the standard form bottom sheet (app design language). */
@Composable
private fun BookmarkEditDialog(
    initialTitle: String,
    onSave: (newTitle: String, colorCode: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var colorCode by remember { mutableStateOf(1) } // GREY default so saves always carry a colour

    BBFormSheet(
        title = stringResource(R.string.menu_edit),
        icon = Icons.Filled.Edit,
        onDismiss = onDismiss,
        confirmEnabled = title.isNotBlank(),
        onConfirm = { onSave(title.trim(), colorCode) },
    ) {
        BBTextFieldPill(
            value = title,
            onValueChange = { title = it },
            hint = stringResource(R.string.album_title_bookmarks),
        )
        Spacer(Modifier.height(BBSpace.sm))
        Text(
            text = stringResource(R.string.setting_filter),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(BBSpace.xxs))
        ColorDotPicker(selectedCode = colorCode, onSelect = { colorCode = it })
    }
}
