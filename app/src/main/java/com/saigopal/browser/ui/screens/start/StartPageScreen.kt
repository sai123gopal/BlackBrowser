package com.saigopal.browser.ui.screens.start

import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.saigopal.browser.ui.design.components.BBFavicon
import com.saigopal.browser.ui.design.components.BBFormSheet
import com.saigopal.browser.ui.design.components.BBIconTile
import com.saigopal.browser.ui.design.components.BBTextFieldPill
import com.saigopal.browser.ui.design.theme.BBSpace
import com.saigopal.browser.ui.di.rememberContainer
import kotlinx.coroutines.launch
import java.util.Calendar

/*
 * Modern Native Start / Home Page Experience:
 * - Ambient aurora background graphics
 * - Minimal clean top bar (BlackBrowser + BROWSE FREELY, Settings icon)
 * - Hero greeting & tracked-out tagline (FASTER SAFER YOURS)
 * - Omnibox search pill with Voice Search and Lens/Scan triggers
 * - 4 Quick Navigation Cards: Bookmarks, History, Downloads, Incognito
 * - 5-column Pinned Sites grid with + Add Site
 * - Continue Browsing (History) with non-overlapping favicons and 3-dot menus
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
     * Idempotent bridge from legacy prefs to GRID rows.
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

// Built-in starter pinned sites matching the clean 5-column design
private val DEFAULT_PINNED_SITES = listOf(
    TileSpec("Google", "https://www.google.com/"),
    TileSpec("Facebook", "https://www.facebook.com/"),
    TileSpec("Instagram", "https://www.instagram.com/"),
    TileSpec("Twitter", "https://twitter.com/"),
    TileSpec("Amazon", "https://www.amazon.com/"),
    TileSpec("Wikipedia", "https://www.wikipedia.org/"),
    TileSpec("DuckDuckGo", "https://www.duckduckgo.com/"),
    TileSpec("Reddit", "https://www.reddit.com/"),
    TileSpec("YouTube", "https://www.youtube.com/"),
)

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
    onOpenDownloads: () -> Unit = {},
    onToggleIncognito: () -> Unit = {},
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

    var searchQuery by remember { mutableStateOf("") }
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

    // Voice Search Launcher
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                onSubmitQuery(spoken)
            }
        }
    }

    // Load tiles and recent history
    LaunchedEffect(Unit) {
        val existing = container.records.listTiles()
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val dump = sp.all.mapValues { (_, v) -> v as? String ?: "" }
        val missing = StartPageMigration.migrateQuickTiles(existing, dump)
        missing.forEachIndexed { slot, spec ->
            container.records.addTile(spec.title, spec.url, slot)
        }
        var loadedTiles = if (missing.isNotEmpty()) container.records.listTiles() else existing

        // If user has zero tiles, seed the default 9 starter sites
        if (loadedTiles.isEmpty()) {
            DEFAULT_PINNED_SITES.forEachIndexed { index, spec ->
                container.records.addTile(spec.title, spec.url, index)
            }
            loadedTiles = container.records.listTiles()
        }
        tiles = loadedTiles

        // Fetch recent history
        runCatching {
            // PERF: bounded newest-first window instead of materializing the whole history
            // table on every start-page composition. The filter → distinctBy → take chain is
            // unchanged, so the displayed strip is identical.
            recentHistory = container.records.listRecentHistory(48)
                .filter { it.url.isNotBlank() && !UrlInterpreter.isInternalScheme(it.url) }
                .distinctBy { it.url }
                .take(6)
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
            .background(Color(0xFF090D0F)),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Atmospheric Emerald Aurora Curve in Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-right emerald ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x3820E398),
                        Color(0x1500897B),
                        Color.Transparent,
                    ),
                    center = Offset(w * 0.92f, h * 0.12f),
                    radius = w * 0.75f,
                )
            )

            // Dynamic swooshing aurora curved lines
            val path1 = Path().apply {
                moveTo(w * 0.40f, 0f)
                cubicTo(
                    w * 0.58f, h * 0.07f,
                    w * 0.90f, h * 0.11f,
                    w, h * 0.26f,
                )
            }
            drawPath(
                path = path1,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0x1220E398), Color(0x6020E398), Color(0x0020E398)),
                    start = Offset(w * 0.40f, 0f),
                    end = Offset(w, h * 0.26f),
                ),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round),
            )

            val path2 = Path().apply {
                moveTo(w * 0.52f, 0f)
                cubicTo(
                    w * 0.72f, h * 0.08f,
                    w * 0.82f, h * 0.19f,
                    w, h * 0.32f,
                )
            }
            drawPath(
                path = path2,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0x0820E398), Color(0x3820E398), Color(0x0020E398)),
                    start = Offset(w * 0.52f, 0f),
                    end = Offset(w, h * 0.32f),
                ),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round),
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // ---- Top Bar (Brand + Settings only, No profile icon) ----
            item(key = "top_bar", span = { GridItemSpan(5) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Black",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text(
                                text = " Browser",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF20E398),
                            )
                        }
                        Text(
                            text = "BROWSE FREELY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 2.5.sp,
                                fontSize = 10.sp,
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF7E8B92),
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFFB0BAC0),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            // ---- Hero Greeting & Tagline ----
            item(key = "hero_greeting", span = { GridItemSpan(5) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$greeting,",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9EAAB1),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Let's explore\nthe web",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 28.sp,
                                lineHeight = 34.sp,
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(top = 10.dp),
                    ) {
                    }
                }
            }

            // ---- Omnibox Search Pill ----
            item(key = "search_pill", span = { GridItemSpan(5) }) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF13191C),
                    border = BorderStroke(1.dp, Color(0xFF222B30)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF7E8B92),
                            modifier = Modifier
                                .size(22.dp)
                                .clickable {
                                    val q = searchQuery.trim()
                                    if (q.isNotEmpty()) {
                                        onSubmitQuery(q)
                                    }
                                },
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.main_omnibox_input_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF7E8B92),
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                cursorBrush = SolidColor(Color(0xFF20E398)),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    val q = searchQuery.trim()
                                    if (q.isNotEmpty()) {
                                        onSubmitQuery(q)
                                    }
                                }),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF7E8B92),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }

                        // Voice Search Button
                        IconButton(
                            onClick = {
                                runCatching {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Search or speak URL")
                                    }
                                    voiceSearchLauncher.launch(intent)
                                }.onFailure {
                                    Toast.makeText(context, "Voice search unavailable", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Filled.Mic,
                                contentDescription = "Voice Search",
                                tint = Color(0xFFB0BAC0),
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        // Lens / QR Scanner Button
                        IconButton(
                            onClick = {
                                runCatching {
                                    val intent = Intent("android.intent.action.VIEW").apply {
                                        setPackage("com.google.ar.lens")
                                    }
                                    context.startActivity(intent)
                                }.onFailure {
                                    showEnginePicker = true
                                }
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Filled.CropFree,
                                contentDescription = "Scan / Search Engine",
                                tint = Color(0xFFB0BAC0),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // ---- Smart Clipboard Quick Action ("Paste & Go") ----
            if (clipboardSnippet != null && !clipboardDismissed) {
                item(key = "clipboard_pill", span = { GridItemSpan(5) }) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF13191C),
                            border = BorderStroke(1.dp, Color(0xFF20E398).copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Copied link found",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF20E398),
                                    )
                                    Text(
                                        text = clipboardSnippet ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
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
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF20E398)),
                                ) {
                                    Text("Open", fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = { clipboardDismissed = true },
                                    modifier = Modifier.size(26.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color(0xFF7E8B92),
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---- Quick Navigation Hub (4 rounded cards in a row) ----
            item(key = "quick_hub_row", span = { GridItemSpan(5) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickHubCard(
                        icon = Icons.Filled.Star,
                        label = "Bookmarks",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenBookmarks,
                    )
                    QuickHubCard(
                        icon = Icons.Filled.AccessTime,
                        label = "History",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenHistory,
                    )
                    QuickHubCard(
                        icon = Icons.Filled.ArrowDownward,
                        label = "Downloads",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenDownloads,
                    )
                    QuickHubCard(
                        icon = Icons.Filled.VisibilityOff,
                        label = "Incognito",
                        modifier = Modifier.weight(1f),
                        onClick = onToggleIncognito,
                    )
                }
            }

            // ---- Pinned Sites Header ----
            item(key = "pinned_header", span = { GridItemSpan(5) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Pinned Sites",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = "See all →",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF20E398),
                        modifier = Modifier.clickable(onClick = onEditTiles),
                    )
                }
            }

            // ---- Pinned Sites Grid Items (5 Columns) ----
            items(items = tiles, key = { "tile_" + it.url }, contentType = { "start_tile" }) { tile ->
                BBIconTile(
                    label = tile.title,
                    url = tile.url,
                    onClick = { onOpenTile(tile.url) },
                    onLongClick = { editingTile = tile },
                    tileSize = 50.dp,
                )
            }

            // In-grid "+ Add Site" Tile Card
            item(key = "tile_add_button") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddTileDialog = true }
                        .padding(vertical = 6.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF13191C),
                        border = BorderStroke(1.dp, Color(0xFF222B30)),
                        modifier = Modifier.size(50.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add site",
                                tint = Color(0xFF20E398),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Text(
                        text = "Add Site",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF7E8B92),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            // ---- Gamezop Games Banner (Below Pinned Sites) ----
            item(key = "gamezop_banner", span = { GridItemSpan(5) }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF13191C),
                    border = BorderStroke(1.dp, Color(0xFF222B30)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 4.dp)
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

            // ---- Continue Browsing (History) Header ----
            if (recentHistory.isNotEmpty()) {
                item(key = "recent_header", span = { GridItemSpan(5) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 22.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Continue Browsing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = "History →",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF20E398),
                            modifier = Modifier.clickable(onClick = onOpenHistory),
                        )
                    }
                }

                // History Items with BBFavicon and 3-dot menu
                recentHistory.forEach { item ->
                    item(key = "recent_" + item.url, span = { GridItemSpan(5) }) {
                        val relativeTime = remember(item.time) {
                            if (item.time > 0L) {
                                DateUtils.getRelativeTimeSpanString(
                                    item.time,
                                    System.currentTimeMillis(),
                                    DateUtils.MINUTE_IN_MILLIS,
                                ).toString()
                            } else ""
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF13191C),
                            border = BorderStroke(1.dp, Color(0xFF20282C)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onOpenTile(item.url) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BBFavicon(
                                    label = item.title,
                                    url = item.url,
                                    size = 40.dp,
                                    shape = CircleShape,
                                )

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title.ifBlank { item.url },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = UrlInterpreter.domainOf(item.url),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF7E8B92),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (relativeTime.isNotEmpty()) {
                                            Text(
                                                text = " • $relativeTime",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF7E8B92),
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }

                                // Three-dot options menu
                                var menuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.MoreVert,
                                            contentDescription = "Options",
                                            tint = Color(0xFF7E8B92),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Open in new tab") },
                                            onClick = {
                                                menuExpanded = false
                                                onOpenTabs()
                                                onSubmitQuery(item.url)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Copy link") },
                                            onClick = {
                                                menuExpanded = false
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                cm?.setPrimaryClip(ClipData.newPlainText("URL", item.url))
                                                Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Remove from history") },
                                            onClick = {
                                                menuExpanded = false
                                                scope.launch {
                                                    container.records.deleteUrl(item.url, com.saigopal.browser.unit.RecordUnit.TABLE_HISTORY)
                                                    recentHistory = container.records.listRecentHistory(48)
                                                        .filter { it.url.isNotBlank() && !UrlInterpreter.isInternalScheme(it.url) }
                                                        .distinctBy { it.url }
                                                        .take(6)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---- Google AdMob Banner ----
            item(key = "admob_banner", span = { GridItemSpan(5) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AdsController.Banner(AdsController.Placement.START_PAGE)
                }
            }
        }
    }

    // ---- Dialogs ----

    // Edit Pinned Site — form bottom sheet (design-language migration from AlertDialog)
    editingTile?.let { tile ->
        var editTitle by remember(tile) { mutableStateOf(tile.title) }
        var editUrl by remember(tile) { mutableStateOf(tile.url) }

        BBFormSheet(
            title = "Edit Pinned Site",
            icon = Icons.Filled.Edit,
            onDismiss = { editingTile = null },
            confirmEnabled = editUrl.isNotBlank(),
            onConfirm = {
                val t = editTitle.trim()
                val u = editUrl.trim()
                if (u.isNotEmpty()) {
                    scope.launch {
                        container.records.deleteTile(tile.url)
                        container.records.addTile(t.ifEmpty { u }, u, 0)
                        tiles = container.records.listTiles()
                    }
                }
                editingTile = null
            },
        ) {
            BBTextFieldPill(
                value = editTitle,
                onValueChange = { editTitle = it },
                hint = "Title",
            )
            BBTextFieldPill(
                value = editUrl,
                onValueChange = { editUrl = it },
                hint = "https://example.com",
                modifier = Modifier.padding(top = BBSpace.xs),
            )
            TextButton(
                onClick = {
                    scope.launch {
                        container.records.deleteTile(tile.url)
                        tiles = container.records.listTiles()
                    }
                    editingTile = null
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.padding(top = BBSpace.xs),
            ) {
                Text("Remove this tile")
            }
        }
    }

    // Add Pinned Site — form bottom sheet (design-language migration from AlertDialog)
    if (showAddTileDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newUrl by remember { mutableStateOf("") }

        BBFormSheet(
            title = "Add Pinned Site",
            icon = Icons.Filled.Add,
            onDismiss = { showAddTileDialog = false },
            confirmLabel = "Add",
            confirmEnabled = newUrl.isNotBlank(),
            onConfirm = {
                val t = newTitle.trim()
                val u = newUrl.trim()
                if (u.isNotEmpty()) {
                    val canonicalUrl = if (!u.startsWith("http://") && !u.startsWith("https://")) "https://$u" else u
                    scope.launch {
                        container.records.addTile(t.ifEmpty { canonicalUrl }, canonicalUrl, tiles.size)
                        tiles = container.records.listTiles()
                    }
                }
                showAddTileDialog = false
            },
        ) {
            BBTextFieldPill(
                value = newTitle,
                onValueChange = { newTitle = it },
                hint = "Site Title (e.g. GitHub)",
            )
            BBTextFieldPill(
                value = newUrl,
                onValueChange = { newUrl = it },
                hint = "https://github.com",
                modifier = Modifier.padding(top = BBSpace.xs),
            )
        }
    }

    // Search Engine Selector Dialog
    if (showEnginePicker) {
        AlertDialog(
            onDismissRequest = { showEnginePicker = false },
            title = { Text("Select Search Engine") },
            text = {
                Column {
                    SearchEngine.values().forEach { engine ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        container.settings.update { it.copy(searchEngine = engine) }
                                    }
                                    showEnginePicker = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = (settings.searchEngine == engine),
                                onClick = {
                                    scope.launch {
                                        container.settings.update { it.copy(searchEngine = engine) }
                                    }
                                    showEnginePicker = false
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = engine.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEnginePicker = false }) {
                    Text("Close")
                }
            },
        )
    }
}

@Composable
private fun QuickHubCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF13191C),
        border = BorderStroke(1.dp, Color(0xFF222B30)),
        modifier = modifier
            .height(78.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF20E398),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Medium,
                color = Color(0xFFB0BAC0),
            )
        }
    }
}
