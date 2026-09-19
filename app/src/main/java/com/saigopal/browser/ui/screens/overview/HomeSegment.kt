package com.saigopal.browser.ui.screens.overview

import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.saigopal.browser.R
import com.saigopal.browser.domain.BBRecord
import com.saigopal.browser.ui.design.components.BBCard
import com.saigopal.browser.ui.design.components.BBIconTile
import com.saigopal.browser.ui.design.components.BBTextFieldPill
import com.saigopal.browser.ui.design.theme.BBSpace
import com.saigopal.browser.ui.di.rememberContainer
import kotlinx.coroutines.launch

/*
 * Home segment: Pinned start-site tiles and quick navigation links.
 * Modernized with clean cards, direct click & long-press handling, and zero logo/letter overlap.
 */

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
        columns = GridCells.Adaptive(76.dp),
        contentPadding = PaddingValues(BBSpace.md),
        horizontalArrangement = Arrangement.spacedBy(BBSpace.xs),
        verticalArrangement = Arrangement.spacedBy(BBSpace.xs),
    ) {
        // Pinned Sites Section
        if (pinned.isNotEmpty()) {
            item(key = "pinned_header", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = BBSpace.xs, bottom = BBSpace.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Pinned Sites",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(20.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "${pinned.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            itemsIndexed(pinned, key = { index, tile -> "pinned_${tile.url}_$index" }) { _, tile ->
                BBIconTile(
                    label = tile.title,
                    url = tile.url,
                    onClick = { onLoadUrl(tile.url, false) },
                    onLongClick = { actionForTile = tile },
                )
            }
        }

        // Gamezop Banner (Below Pinned Sites)
        item(key = "gamezop_banner", span = { GridItemSpan(maxLineSpan) }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = BBSpace.xs)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenGames),
            ) {
                Image(
                    painter = painterResource(R.drawable.img_games),
                    contentDescription = "Gamezop - Play Games",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(960f / 368f),
                )
            }
        }

        // Quick Links Section
        item(key = "quick_header", span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Quick Links",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = BBSpace.sm, bottom = BBSpace.xxs),
            )
        }
        items(quickTiles, key = { "quick_${it.index}" }) { tile ->
            BBIconTile(
                label = tile.name,
                url = tile.url,
                onClick = { onLoadUrl(tile.url, false) },
                onLongClick = { editingQuick = tile },
            )
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
                prefs.edit().putString("${tile.index}_name", name).apply()
                prefs.edit().putString("${tile.index}_url", url).apply()
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
