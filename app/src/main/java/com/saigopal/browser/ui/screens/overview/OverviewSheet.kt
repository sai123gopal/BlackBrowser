@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.saigopal.browser.ui.screens.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saigopal.browser.R
import com.saigopal.browser.domain.BBRecord
import com.saigopal.browser.domain.SearchEngine
import com.saigopal.browser.ui.design.components.BBIconButton
import com.saigopal.browser.ui.design.components.BBListItem
import com.saigopal.browser.ui.design.components.BBTextFieldPill
import com.saigopal.browser.ui.design.theme.BBSpace
import com.saigopal.browser.ui.di.rememberContainer

/*
 * Overview sheet — replaces the legacy dialog_overview bottom sheet (P04).
 * The sheet is a presentation + intent surface over TabManager/RecordsRepository: it never
 * touches WebViews; loading and activation are reported through lambdas so the P05 shell
 * stays the single owner of browser state.
 */

enum class OverviewSegment { TABS, HOME, SAVED, HISTORY }

/** Chip labels for the segment switcher (legacy album titles preserved). */
private fun OverviewSegment.labelRes(): Int = when (this) {
    OverviewSegment.TABS -> R.string.album_title_tab
    OverviewSegment.HOME -> R.string.album_title_home
    OverviewSegment.SAVED -> R.string.album_title_bookmarks
    OverviewSegment.HISTORY -> R.string.album_title_history
}

/**
 * @param initialSegment pre-selected segment (start_tab pref / sc_* shortcut deep links).
 * @param onLoadUrl reports a URL to load: inNewTab=true → foreground new tab, else current tab.
 */
@Composable
fun OverviewSheet(
    initialSegment: OverviewSegment,
    onDismiss: () -> Unit,
    onLoadUrl: (url: String, inNewTab: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onNewTab: () -> Unit,
    onOpenGames: () -> Unit,
    onActivateTab: ((Long) -> Unit)? = null,
    onCloseTab: ((Long) -> Unit)? = null,
) {
    val container = rememberContainer()
    val settings by container.settings.state.collectAsState()
    val tabs by container.tabs.tabs.collectAsState()
    val activeId by container.tabs.activeId.collectAsState()

    var segment by remember { mutableStateOf(initialSegment) }
    var query by remember { mutableStateOf("") }
    // Bumped whenever an action mutates records/tabs so segment lists re-read from source.
    var refreshTick by remember { mutableStateOf(0) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
        ) {
            SearchBlock(
                query = query,
                onQueryChange = { query = it },
                engine = settings.searchEngine,
                customPrefix = settings.customSearchPrefix,
                onSubmit = { resolved ->
                    query = ""
                    onDismiss()
                    onLoadUrl(resolved, false)
                },
                onSuggestion = { url ->
                    query = ""
                    onDismiss()
                    onLoadUrl(url, false)
                },
            )

            // Horizontally scrolling filter chips — the four segments never squeeze into a fixed
            // segmented row; deep-linked segments (start_tab / sc_* shortcuts) scroll into view.
            val chipListState = rememberLazyListState()
            LaunchedEffect(segment) {
                // Scroll the selected chip into view safely.
                val total = chipListState.layoutInfo.totalItemsCount
                if (total > 0 && segment.ordinal in 0 until total) {
                    val visible = chipListState.layoutInfo.visibleItemsInfo
                    if (visible.isNotEmpty()) {
                        val firstVisible = chipListState.firstVisibleItemIndex
                        val lastVisible = visible.last().index
                        if (firstVisible > segment.ordinal || lastVisible < segment.ordinal) {
                            runCatching { chipListState.animateScrollToItem(segment.ordinal) }
                        }
                    }
                }
            }
            Row(
                Modifier.padding(horizontal = BBSpace.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LazyRow(
                    state = chipListState,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(BBSpace.xs),
                    contentPadding = PaddingValues(horizontal = BBSpace.xs),
                ) {
                    items(OverviewSegment.entries, key = { it.name }) { chip ->
                        FilterChip(
                            selected = segment == chip,
                            onClick = { segment = chip },
                            leadingIcon = {
                                val icon = when (chip) {
                                    OverviewSegment.TABS -> Icons.Filled.List
                                    OverviewSegment.HOME -> Icons.Filled.Home
                                    OverviewSegment.SAVED -> Icons.Filled.Star
                                    OverviewSegment.HISTORY -> Icons.Filled.Refresh
                                }
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                Text(
                                    stringResource(chip.labelRes()),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.padding(BBSpace.xs))
                BBIconButton(
                    icon = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.setting_title_start_control),
                    onClick = {
                        onDismiss()
                        onOpenSettings()
                    },
                )
            }

            // Bounded height so the lazy grids/columns inside segments can measure & scroll.
            // FIX: horizontal swipe anywhere on the content area moves between segments
            // (Tabs → Home → Saved → History), matching the chip order; chips stay clickable.
            // Each segment is keyed and KEEP-alive via rememberSaveable-less state hoisting:
            // segment lists live in the segment composables, so a horizontal page change simply
            // swaps content — vertical scroll inside a segment starts fresh per visit, which
            // matches the previous chip-tap behavior exactly.
            val pagerState = rememberPagerState(pageCount = { OverviewSegment.entries.size })
            LaunchedEffect(segment) {
                // Chip/deep-link driven changes animate the pager to the matching page.
                val target = OverviewSegment.entries.indexOf(segment)
                if (pagerState.currentPage != target) {
                    runCatching { pagerState.animateScrollToPage(target) }
                }
            }
            // Swipe-driven settle syncs back into `segment` so the chips follow the page.
            LaunchedEffect(pagerState.settledPage) {
                val seg = OverviewSegment.entries.getOrNull(pagerState.settledPage)
                if (seg != null && seg != segment) segment = seg
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                Box(Modifier.fillMaxSize()) {
                    when (OverviewSegment.entries[page]) {
                        OverviewSegment.TABS -> TabsSegment(
                            tabs = tabs,
                            activeId = activeId,
                            onActivate = { id ->
                                onActivateTab?.invoke(id) ?: container.tabs.activate(id)
                                onDismiss()
                            },
                            onClose = { id ->
                                onCloseTab?.invoke(id) ?: container.tabs.close(id)
                                refreshTick++
                            },
                            onNewTab = {
                                onDismiss()
                                onNewTab()
                            },
                        )
                        OverviewSegment.HOME -> HomeSegment(
                            refreshTick = refreshTick,
                            onLoadUrl = onLoadUrl,
                            onOpenGames = {
                                onDismiss()
                                onOpenGames()
                            },
                            onChanged = { refreshTick++ },
                        )
                        OverviewSegment.SAVED -> SavedSegment(
                            bookmarkSort = settings.bookmarkSort,
                            enabledColors = settings.enabledFilterColors,
                            refreshTick = refreshTick,
                            onLoadUrl = onLoadUrl,
                            onChanged = { refreshTick++ },
                        )
                        OverviewSegment.HISTORY -> HistorySegment(
                            refreshTick = refreshTick,
                            onLoadUrl = onLoadUrl,
                            onChanged = { refreshTick++ },
                        )
                    }
                }
            }

            // P08 banner slot
            val activeTab = tabs.firstOrNull { it.id == activeId }
            com.saigopal.browser.ui.ads.AdsController.Banner(
                placement = com.saigopal.browser.ui.ads.AdsController.Placement.OVERVIEW,
                enabled = activeTab?.isIncognito != true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SearchBlock(
    query: String,
    onQueryChange: (String) -> Unit,
    engine: SearchEngine,
    customPrefix: String,
    onSubmit: (resolvedUrl: String) -> Unit,
    onSuggestion: (url: String) -> Unit,
) {
    val container = rememberContainer()
    var suggestions by remember { mutableStateOf<List<BBRecord>>(emptyList()) }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            suggestions = emptyList()
        } else {
            val all = container.records.autocomplete()
            suggestions = all.filter {
                it.url.contains(query, ignoreCase = true) || it.title.contains(query, ignoreCase = true)
            }.take(4)
        }
    }

    Column(Modifier.padding(horizontal = BBSpace.md)) {
        BBTextFieldPill(
            value = query,
            onValueChange = onQueryChange,
            hint = stringResource(R.string.main_omnibox_input_hint),
            leadingIcon = Icons.Filled.Search,
            onSearch = {
                if (query.isNotBlank()) {
                    onSubmit(container.urlInterpreter.interpret(query, engine, customPrefix))
                }
            },
            trailing = {
                if (query.isNotEmpty()) {
                    BBIconButton(
                        icon = Icons.Filled.Close,
                        contentDescription = stringResource(android.R.string.cancel),
                        onClick = { onQueryChange("") },
                    )
                }
            },
        )

        // Autocomplete parity fix (bug B12): suggestions are typed list items, not hidden views.
        if (query.isNotBlank()) {
            suggestions.forEach { record ->
                BBListItem(
                    title = record.title.ifBlank { record.url },
                    subtitle = record.url,
                    onClick = { onSuggestion(record.url) },
                )
            }
            BBListItem(
                title = query,
                onClick = { onSubmit(container.urlInterpreter.interpret(query, engine, customPrefix)) },
                leading = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        }
    }
}
