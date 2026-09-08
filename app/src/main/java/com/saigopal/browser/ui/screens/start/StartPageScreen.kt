package com.saigopal.browser.ui.screens.start

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import kotlinx.coroutines.launch
import com.saigopal.browser.ui.ads.AdsController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.saigopal.browser.R
import com.saigopal.browser.domain.BBRecord
import com.saigopal.browser.ui.design.components.BBIconTile
import com.saigopal.browser.ui.design.components.BBTextFieldPill
import com.saigopal.browser.ui.design.theme.BBSpace
import com.saigopal.browser.ui.design.theme.LocalAurora
import com.saigopal.browser.ui.design.theme.PillShape
import com.saigopal.browser.ui.di.rememberContainer

/*
 * Modernized Native Start Page:
 * - Aurora hero title and branding
 * - Elevated search bar with instant query execution
 * - Squircle Quick-Tile grid for top sites
 * - Quick action hub cards (Games, Feedback, Edit shortcuts)
 * - 100% backward-compatible migration logic for legacy prefs
 */

/** One migrated quick-tile slot. */
data class TileSpec(val title: String, val url: String)

object StartPageMigration {

    /** Legacy defaults verbatim from MainActivity.CustomGridViewAdapter (slots 0..7). */
    private val DEFAULTS = listOf(
        "Google" to "https://www.google.com/",
        "Facebook" to "https://www.facebook.com",
        "Instagram" to "https://www.instagram.com/",
        "Twitter" to "https://twitter.com",
        "Amazon" to "https://www.amazon.com/",
        "Wikipedia" to "https://www.wikipedia.org/",
        "DuckDuckGo" to "https://www.duckduckgo.com",
        "BlackBrowser" to "https://blackbrowser-androidapp.web.app/",
    )

    /**
     * Idempotent bridge from the legacy "{i}_name"/"{i}_url" prefs to GRID rows.
     * Returns only specs whose URL is missing from [existing]; slot order preserved;
     * duplicate/blank URLs collapse away. [prefs] is a flat String dump of
     * SharedPreferences.all — pure and JVM-testable.
     */
    fun migrateQuickTiles(existing: List<BBRecord>, prefs: Map<String, String>): List<TileSpec> {
        val knownUrls = existing.mapTo(HashSet()) { it.url.lowercase() }
        val seen = HashSet<String>()
        return buildList {
            for (slot in DEFAULTS.indices) {
                val name = prefs["${slot}_name"]?.takeIf { it.isNotBlank() } ?: DEFAULTS[slot].first
                val url = prefs["${slot}_url"]?.takeIf { it.isNotBlank() } ?: DEFAULTS[slot].second
                if (!seen.add(url.lowercase())) continue
                if (url.lowercase() in knownUrls) continue
                add(TileSpec(name, url))
            }
        }
    }
}

@Composable
fun StartPageScreen(
    onSubmitQuery: (String) -> Unit,
    onOpenTile: (String) -> Unit,
    onEditTiles: () -> Unit,
    onOpenGames: () -> Unit,
    onOpenFeedback: () -> Unit,
) {
    val container = rememberContainer()
    val context = LocalContext.current
    var tiles by remember { mutableStateOf<List<BBRecord>>(emptyList()) }
    var editingTile by remember { mutableStateOf<BBRecord?>(null) }
    var showAddTileDialog by remember { mutableStateOf(false) }

    // One-shot legacy quick-tile migration into GRID (idempotent — safe across launches).
    LaunchedEffect(Unit) {
        val existing = container.records.listTiles()
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val dump = sp.all.mapValues { (_, v) -> v as? String ?: "" }
        val missing = StartPageMigration.migrateQuickTiles(existing, dump)
        missing.forEachIndexed { slot, spec ->
            container.records.addTile(spec.title, spec.url, slot)
        }
        tiles = if (missing.isNotEmpty()) container.records.listTiles() else existing
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 88.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = BBSpace.lg, vertical = BBSpace.md),
        horizontalArrangement = Arrangement.spacedBy(BBSpace.sm),
        verticalArrangement = Arrangement.spacedBy(BBSpace.sm),
    ) {
        // ---- Hero Title ----
        item(key = "brand", span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = BBSpace.xl, bottom = BBSpace.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Spacer(Modifier.height(BBSpace.sm))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(BBSpace.xs))
                AuroraRule(Modifier.width(80.dp))
            }
        }

        // ---- Search Bar ----
        item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
            var query by remember { mutableStateOf("") }
            Surface(
                shape = PillShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = BBSpace.xs),
            ) {
                BBTextFieldPill(
                    value = query,
                    onValueChange = { query = it },
                    hint = stringResource(R.string.main_omnibox_input_hint),
                    leadingIcon = Icons.Filled.Search,
                    onSearch = {
                        val q = query.trim()
                        if (q.isNotEmpty()) {
                            query = ""
                            onSubmitQuery(q)
                        }
                    },
                )
            }
        }

        // ---- Pinned Quick Tiles Header ----
        item(key = "pinned_header", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = BBSpace.md, bottom = BBSpace.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.bb_start_pinned),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showAddTileDialog = true }) {
                    Text(
                        text = stringResource(R.string.menu_edit),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // ---- Quick Tiles Grid ----
        if (tiles.isEmpty()) {
            item(key = "tiles_empty", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.bb_start_tiles_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = BBSpace.lg),
                )
            }
        } else {
            items(items = tiles, key = { "tile_" + it.url }) { tile ->
                BBIconTile(
                    label = tile.title,
                    url = tile.url,
                    onClick = { onOpenTile(tile.url) },
                    onLongClick = { editingTile = tile },
                )
            }
        }

        // ---- Google AdMob Banner Ad (Native support & AdMob guidelines compliant) ----
        item(key = "admob_banner", span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = BBSpace.xs),
                contentAlignment = Alignment.Center,
            ) {
                AdsController.Banner(
                    placement = AdsController.Placement.START_PAGE,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // ---- Quick Feature Cards (Games Hub & Feedback) ----
        item(key = "games_card", span = { GridItemSpan(maxLineSpan) }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = BBSpace.md)
                    .clickable(onClick = onOpenGames),
            ) {
                Row(
                    modifier = Modifier.padding(BBSpace.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(BBSpace.sm))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.bb_start_games_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.bb_overview_games_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // ---- Footer Feedback Link ----
        item(key = "footer", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = BBSpace.sm, bottom = BBSpace.lg),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = onOpenFeedback) {
                    Icon(
                        Icons.Filled.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(BBSpace.xs))
                    Text(
                        text = stringResource(R.string.bb_start_feedback),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    // ---- Edit Tile Dialog (onLongClick of any pinned tile) --------------------------------------
    editingTile?.let { target ->
        var editTitle by remember(target) { mutableStateOf(target.title) }
        var editUrl by remember(target) { mutableStateOf(target.url) }
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { editingTile = null },
            title = { Text(text = "Edit Pinned Site", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(BBSpace.sm)) {
                    BBTextFieldPill(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        hint = "Title",
                    )
                    BBTextFieldPill(
                        value = editUrl,
                        onValueChange = { editUrl = it },
                        hint = "URL (e.g. https://...)",
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTitle = editTitle.trim()
                        val newUrl = editUrl.trim()
                        if (newUrl.isNotEmpty()) {
                            scope.launch {
                                container.records.deleteTile(target.url)
                                container.records.addTile(
                                    title = if (newTitle.isNotEmpty()) newTitle else newUrl,
                                    url = newUrl,
                                    ordinal = tiles.indexOf(target).coerceAtLeast(0),
                                )
                                tiles = container.records.listTiles()
                                editingTile = null
                            }
                        }
                    },
                    enabled = editUrl.isNotBlank(),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            scope.launch {
                                container.records.deleteTile(target.url)
                                tiles = container.records.listTiles()
                                editingTile = null
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("Remove")
                    }
                    TextButton(onClick = { editingTile = null }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            },
        )
    }

    // ---- Add Pinned Site / Manage Dialog --------------------------------------------------------
    if (showAddTileDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newUrl by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { showAddTileDialog = false },
            title = { Text(text = "Add Pinned Site", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(BBSpace.sm)) {
                    BBTextFieldPill(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        hint = "Title (optional)",
                    )
                    BBTextFieldPill(
                        value = newUrl,
                        onValueChange = { newUrl = it },
                        hint = "URL (e.g. https://...)",
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val url = newUrl.trim()
                        val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            "https://$url"
                        } else url
                        val title = newTitle.trim().ifEmpty { formattedUrl }
                        scope.launch {
                            container.records.addTile(title, formattedUrl, tiles.size)
                            tiles = container.records.listTiles()
                            showAddTileDialog = false
                        }
                    },
                    enabled = newUrl.isNotBlank(),
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showAddTileDialog = false
                            onEditTiles()
                        },
                    ) {
                        Text("Manage All")
                    }
                    TextButton(onClick = { showAddTileDialog = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            },
        )
    }
}

/** Small brand hairline using the active aurora edge brush. */
@Composable
private fun AuroraRule(modifier: Modifier = Modifier) {
    val aurora = LocalAurora.current
    Box(
        modifier
            .height(3.dp)
            .clip(PillShape)
            .background(brush = aurora.edge),
    )
}
