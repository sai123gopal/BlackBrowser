package com.saigopal.browser.ui.screens.overview

import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.saigopal.browser.R
import com.saigopal.browser.domain.BBRecord
import com.saigopal.browser.ui.design.components.BBCard
import com.saigopal.browser.ui.design.components.BBIconTile
import com.saigopal.browser.ui.design.components.BBTextFieldPill
import com.saigopal.browser.ui.design.components.SectionHeader
import com.saigopal.browser.ui.design.theme.BBSpace
import com.saigopal.browser.ui.di.rememberContainer
import kotlinx.coroutines.launch

/*
 * Home segment (P04 §4.4): DB start-site tiles ("Pinned") + the 8 SharedPreferences quick tiles,
 * visually grouped until P07 unifies them. Quick-tile keys {i}_name/{i}_url (i = 0..7) are legacy
 * stored data — written with commit() parity so old code reading them never sees a gap.
 */

/** One quick tile read from prefs; defaults mirror CustomGridViewAdapter exactly. */
internal data class QuickTile(val index: Int, val name: String, val url: String)

internal fun readQuickTiles(prefs: SharedPreferences): List<QuickTile> {
    val defaultNames = listOf(
        "Google", "Facebook", "Instagram", "Twitter",
        "Amazon", "Wikipedia", "DuckDuckGo", "BlackBrowser",
    )
    val defaultUrls = listOf(
        "https://www.google.com/",
        "https://www.facebook.com",
        "https://www.instagram.com/",
        "https://twitter.com",
        "https://www.amazon.com/",
        "https://www.wikipedia.org/",
        "https://www.duckduckgo.com",
        "https://blackbrowser-androidapp.web.app/",
    )
    return (0..7).map { i ->
        QuickTile(
            index = i,
            name = prefs.getString("${i}_name", defaultNames[i]) ?: defaultNames[i],
            url = prefs.getString("${i}_url", defaultUrls[i]) ?: defaultUrls[i],
        )
    }
}

/**
 * Transparent click surface over a tile so both tap and long-press work even though the
 * underlying component only exposes onClick.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LongPressTile(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        content()
    }
}

@Composable
fun HomeSegment(
    refreshTick: Int,
    onLoadUrl: (url: String, inNewTab: Boolean) -> Unit,
    onOpenGames: () -> Unit,
    onChanged: () -> Unit,
) {
    val container = rememberContainer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pinned by remember { mutableStateOf<List<BBRecord>>(emptyList()) }
    var editingQuick by remember { mutableStateOf<QuickTile?>(null) }
    var actionForTile by remember { mutableStateOf<BBRecord?>(null) }

    LaunchedEffect(refreshTick) {
        pinned = container.records.listTiles()
    }
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val quickTiles = remember(refreshTick) { readQuickTiles(prefs) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(88.dp),
        contentPadding = PaddingValues(BBSpace.md),
    ) {
        item(key = "games_card", span = { GridItemSpan(maxLineSpan) }) {
            BBCard(Modifier.padding(bottom = BBSpace.sm), onClick = onOpenGames) {
                Column(Modifier.padding(BBSpace.md)) {
                    Text(
                        "Games",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        stringResource(R.string.bb_overview_games_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (pinned.isNotEmpty()) {
            item(key = "pinned_header", span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader("Pinned")
            }
            itemsIndexed(pinned, key = { index, tile -> "pinned_${tile.url}_$index" }) { _, tile ->
                LongPressTile(
                    onClick = { onLoadUrl(tile.url, false) },
                    onLongClick = { actionForTile = tile },
                ) {
                    BBIconTile(label = tile.title, url = tile.url, onClick = {})
                }
            }
        }
        item(key = "quick_header", span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader("Quick links")
        }
        items(quickTiles, key = { "quick_${it.index}" }) { tile ->
            LongPressTile(
                onClick = { onLoadUrl(tile.url, false) },
                onLongClick = {
                    // Legacy parity: only quick tiles get the edit dialog (same keys, same toasts).
                    editingQuick = tile
                },
            ) {
                BBIconTile(label = tile.name, url = tile.url, onClick = {})
            }
        }
    }

    actionForTile?.let { tile ->
        TileActionsSheet(
            title = tile.title.ifBlank { tile.url },
            onDismiss = { actionForTile = null },
            onOpenForeground = {
                actionForTile = null
                onLoadUrl(tile.url, false)
            },
            onOpenBackground = {
                actionForTile = null
                onLoadUrl(tile.url, true)
            },
            onDelete = {
                actionForTile = null
                scope.launch {
                    container.records.deleteTile(tile.url)
                    onChanged()
                }
            },
        )
    }

    editingQuick?.let { tile ->
        QuickTileEditDialog(
            initialName = tile.name,
            initialUrl = tile.url,
            onSave = { name, url ->
                prefs.edit().putString("${tile.index}_name", name).commit()
                prefs.edit().putString("${tile.index}_url", url).commit()
                Toast.makeText(context, "saved", Toast.LENGTH_SHORT).show()
                editingQuick = null
                onChanged()
            },
            onDismiss = { editingQuick = null },
        )
    }
}

/** Port of legacy updateHomeGridView / dialog_edit_homescreen semantics, same keys and toasts. */
@Composable
private fun QuickTileEditDialog(
    initialName: String,
    initialUrl: String,
    onSave: (name: String, url: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var url by remember { mutableStateOf(initialUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.menu_edit)) },
        text = {
            Column {
                BBTextFieldPill(value = name, onValueChange = { name = it }, hint = "Name")
                BBTextFieldPill(
                    value = url,
                    onValueChange = { url = it },
                    hint = "URL",
                    modifier = Modifier.padding(top = BBSpace.sm),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank() || url.isBlank()) {
                    // Verbatim legacy toast.
                    Toast.makeText(context, "Enter all details", Toast.LENGTH_SHORT).show()
                } else {
                    onSave(name.trim(), url.trim())
                }
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
