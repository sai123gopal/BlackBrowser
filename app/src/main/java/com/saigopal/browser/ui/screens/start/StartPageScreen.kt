package com.saigopal.browser.ui.screens.start

import android.content.ClipboardManager
import android.content.Context
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.saigopal.browser.R
import com.saigopal.browser.domain.BBRecord
import com.saigopal.browser.domain.SearchEngine
import com.saigopal.browser.domain.UrlInterpreter
import com.saigopal.browser.ui.ads.AdsController
import com.saigopal.browser.ui.design.components.BBIconTile
import com.saigopal.browser.ui.design.components.BBTextFieldPill
import com.saigopal.browser.ui.design.theme.BBSpace
import com.saigopal.browser.ui.design.theme.LocalAurora
import com.saigopal.browser.ui.design.theme.PillShape
import com.saigopal.browser.ui.di.rememberContainer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/*
 * Modern Native Start / Home Page Experience:
 * - Ambient greeting & dynamic time/status header (Shield & Incognito live pills)
 * - Elevated hero search bar with interactive search engine picker
 * - Smart clipboard "Paste & Go" quick-action pill
 * - Squircle Quick-Tiles grid for top sites with in-grid "+ Add Site" and long-press edit
 * - "Jump Back In" recent sessions carousel
 * - Modern browser utilities hub (Web games, Bookmarks, History, Feedback)
 * - Google AdMob native banner compliant with AdMob policy
 * - Responsive design adapting seamlessly to phones, foldables, tablets & landscape
 * - 100% backward-compatible migration logic for legacy preferences
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
    onOpenBookmarks: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenTabs: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val container = rememberContainer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by container.settings.state.collectAsState()

    var tiles by remember { mutableStateOf<List<BBRecord>>(emptyList()) }
    var recentHistory by remember { mutableStateOf<List<BBRecord>>(emptyList()) }
    var editingTile by remember { mutableStateOf<BBRecord?>(null) }
    var showAddTileDialog by remember { mutableStateOf(false) }
    var showEnginePicker by remember { mutableStateOf(false) }

    var clipboardSnippet by remember { mutableStateOf<String?>(null) }
    var clipboardDismissed by remember { mutableStateOf(false) }

    // Dynamic greeting based on current hour
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }
    val formattedDate = remember {
        val format = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        format.format(Date())
    }

    // One-shot legacy quick-tile migration into GRID (idempotent — safe across launches)
    // and load recent history for "Jump Back In".
    LaunchedEffect(Unit) {
        val existing = container.records.listTiles()
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val dump = sp.all.mapValues { (_, v) -> v as? String ?: "" }
        val missing = StartPageMigration.migrateQuickTiles(existing, dump)
        missing.forEachIndexed { slot, spec ->
            container.records.addTile(spec.title, spec.url, slot)
        }
        tiles = if (missing.isNotEmpty()) container.records.listTiles() else existing

        // Fetch recent history
        runCatching {
            recentHistory = container.records.listHistory()
                .filter { it.url.isNotBlank() && !UrlInterpreter.isInternalScheme(it.url) }
                .distinctBy { it.url }
                .take(4)
        }

        // Check clipboard for instant Paste & Go
        runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = cm?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
            if (!clip.isNullOrBlank() && clip.length < 300 && (clip.startsWith("http://") || clip.startsWith("https://") || clip.contains("."))) {
                clipboardSnippet = clip
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = BBSpace.md, vertical = BBSpace.md),
            horizontalArrangement = Arrangement.spacedBy(BBSpace.sm),
            verticalArrangement = Arrangement.spacedBy(BBSpace.sm),
        ) {
            // ---- Hero Header & Status Section ----
            item(key = "brand_header", span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = BBSpace.lg, bottom = BBSpace.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Logo Icon with subtle glow
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.size(54.dp),
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

                    // Dynamic Greeting & Title
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(BBSpace.xs))
                    AuroraRule(Modifier.width(96.dp))

                    Spacer(Modifier.height(BBSpace.sm))

                    // Status badges row: Shield status, Incognito status, Search engine
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = BBSpace.xxs),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Shield Status Pill
                        Surface(
                            shape = PillShape,
                            color = if (settings.adBlockEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                1.dp,
                                if (settings.adBlockEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(
                                            if (settings.adBlockEnabled) Color(0xFF4CAF50)
                                            else MaterialTheme.colorScheme.outline,
                                            CircleShape,
                                        ),
                                )
                                Text(
                                    text = if (settings.adBlockEnabled) "Shield Active" else "Shield Paused",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        Spacer(Modifier.width(BBSpace.xs))

                        // Incognito badge if active
                        if (settings.incognitoActive) {
                            Surface(
                                shape = PillShape,
                                color = Color(0xFF4A148C).copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.5f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.VisibilityOff,
                                        contentDescription = null,
                                        tint = Color(0xFFBA68C8),
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = "Incognito",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE1BEE7),
                                    )
                                }
                            }
                            Spacer(Modifier.width(BBSpace.xs))
                        }

                        // Search Engine Switcher Badge
                        Surface(
                            shape = PillShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable { showEnginePicker = true },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = settings.searchEngine.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            // ---- Elevated Hero Search Bar ----
            item(key = "search_bar", span = { GridItemSpan(maxLineSpan) }) {
                var query by remember { mutableStateOf("") }

                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
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
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                if (query.isNotEmpty()) {
                                    IconButton(
                                        onClick = { query = "" },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val q = query.trim()
                                        if (q.isNotEmpty()) {
                                            query = ""
                                            onSubmitQuery(q)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                ) {
                                    Icon(
                                        Icons.Filled.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        },
                    )
                }
            }

            // ---- Smart Clipboard Quick Action ("Paste & Go") ----
            if (clipboardSnippet != null && !clipboardDismissed) {
                item(key = "clipboard_pill", span = { GridItemSpan(maxLineSpan) }) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = BBSpace.xxs),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = BBSpace.md, vertical = BBSpace.xs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(BBSpace.sm),
                            ) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Copied link found",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = clipboardSnippet ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        val target = clipboardSnippet
                                        if (!target.isNullOrBlank()) {
                                            onSubmitQuery(target)
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                ) {
                                    Text("Open", fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = { clipboardDismissed = true },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---- Pinned Quick Tiles Header ----
            item(key = "pinned_header", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = BBSpace.sm, bottom = BBSpace.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.bb_start_pinned),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(20.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${tiles.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showAddTileDialog = true }) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    TextButton(onClick = onEditTiles) {
                        Text(
                            text = stringResource(R.string.menu_edit),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ---- Quick Tiles Grid Items ----
            items(items = tiles, key = { "tile_" + it.url }) { tile ->
                BBIconTile(
                    label = tile.title,
                    url = tile.url,
                    onClick = { onOpenTile(tile.url) },
                    onLongClick = { editingTile = tile },
                )
            }

            // In-grid "+ Add Site" Tile Card
            item(key = "tile_add_button") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddTileDialog = true }
                        .padding(BBSpace.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add site",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Text(
                        text = "Add Site",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = BBSpace.xxs),
                    )
                }
            }

            // ---- "Jump Back In" / Recent Sessions ----
            if (recentHistory.isNotEmpty()) {
                item(key = "recent_header", span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = BBSpace.md, bottom = BBSpace.xxs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Jump Back In",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onOpenHistory) {
                            Text(
                                text = "History →",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                recentHistory.forEach { item ->
                    item(key = "recent_" + item.url, span = { GridItemSpan(maxLineSpan) }) {
                        val relativeTime = remember(item.time) {
                            if (item.time > 0L) {
                                DateUtils.getRelativeTimeSpanString(
                                    item.time,
                                    System.currentTimeMillis(),
                                    DateUtils.MINUTE_IN_MILLIS,
                                ).toString()
                            } else {
                                ""
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenTile(item.url) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = BBSpace.md, vertical = BBSpace.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(BBSpace.sm),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(34.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = (item.title.firstOrNull() ?: item.url.firstOrNull() ?: '?').uppercase(),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title.ifBlank { item.url },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = UrlInterpreter.domainOf(item.url),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (relativeTime.isNotEmpty()) {
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline,
                                            )
                                            Text(
                                                text = relativeTime,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---- Browser Hub / Utilities Shelf ----
            item(key = "hub_header", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Browser Hub",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = BBSpace.md, bottom = BBSpace.xxs),
                )
            }

            // Web Games Hub Card
            item(key = "games_hub_card", span = { GridItemSpan(maxLineSpan) }) {
                HubCard(
                    title = stringResource(R.string.bb_start_games_title),
                    subtitle = stringResource(R.string.bb_overview_games_hint),
                    icon = Icons.Filled.PlayArrow,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    onClick = onOpenGames,
                )
            }

            // Bookmarks & History Shortcuts Row
            item(key = "shortcuts_row", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(BBSpace.sm),
                ) {
                    // Bookmarks shortcut card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenBookmarks),
                    ) {
                        Row(
                            modifier = Modifier.padding(BBSpace.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(BBSpace.sm),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFFF3E0),
                                modifier = Modifier.size(36.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFF57C00),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Bookmarks",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Saved pages",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // History shortcut card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenHistory),
                    ) {
                        Row(
                            modifier = Modifier.padding(BBSpace.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(BBSpace.sm),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.size(36.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF388E3C),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "History",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Past visits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // ---- Google AdMob Banner Ad (Native support & AdMob guidelines compliant) ----
            item(key = "admob_banner", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = BBSpace.sm),
                    contentAlignment = Alignment.Center,
                ) {
                    AdsController.Banner(
                        placement = AdsController.Placement.START_PAGE,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ---- Footer Feedback Link ----
            item(key = "footer", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = BBSpace.xs, bottom = BBSpace.xl),
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
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }

    // ---- Edit Tile Dialog (onLongClick of any pinned tile) --------------------------------------
    editingTile?.let { target ->
        var editTitle by remember(target) { mutableStateOf(target.title) }
        var editUrl by remember(target) { mutableStateOf(target.url) }

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
                        hint = "URL (e.g. google.com)",
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val rawUrl = newUrl.trim()
                        val formattedUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                            "https://$rawUrl"
                        } else rawUrl
                        val title = newTitle.trim().ifEmpty { UrlInterpreter.domainOf(formattedUrl).ifBlank { formattedUrl } }
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

    // ---- Search Engine Selector Dialog ---------------------------------------------------------
    if (showEnginePicker) {
        AlertDialog(
            onDismissRequest = { showEnginePicker = false },
            title = { Text(text = "Choose Search Engine", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SearchEngine.entries.forEach { engine ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    container.settings.update { it.copy(searchEngine = engine) }
                                    showEnginePicker = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = settings.searchEngine == engine,
                                onClick = {
                                    container.settings.update { it.copy(searchEngine = engine) }
                                    showEnginePicker = false
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = engine.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (settings.searchEngine == engine) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEnginePicker = false }) {
                    Text("Done")
                }
            },
        )
    }
}

/** Reusable Hub Card for games, feedback, etc. */
@Composable
private fun HubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(BBSpace.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = iconBg,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.width(BBSpace.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
