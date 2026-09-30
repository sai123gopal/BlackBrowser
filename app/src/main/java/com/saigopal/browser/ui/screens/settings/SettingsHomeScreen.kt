package com.saigopal.browser.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saigopal.browser.R
import com.saigopal.browser.data.repo.BBSettings
import com.saigopal.browser.domain.NavPosition
import com.saigopal.browser.domain.SearchEngine
import com.saigopal.browser.domain.StartTab
import com.saigopal.browser.ui.design.components.BBIconButton
import com.saigopal.browser.ui.design.components.BBTextFieldPill
import com.saigopal.browser.ui.design.components.TopSneakbarHost
import com.saigopal.browser.ui.design.components.rememberTopSneakbarState
import com.saigopal.browser.ui.design.theme.BBSpace
import com.saigopal.browser.ui.di.rememberContainer
import com.saigopal.browser.ui.screens.settings.LocalSettingsScaffoldScroll

/*
 * Modern Jetpack Compose Settings — Card-grouped Material 3 design with vibrant icon badges,
 * instant live updates via SettingsRepository, and factory defaults restoration.
 */

/** Dialogs opened from the root screen's rows. */
private sealed interface HomeDialog {
    data object None : HomeDialog
    data object Favorite : HomeDialog
    data object Engine : HomeDialog
    data object CustomPrefix : HomeDialog
    data object UserAgent : HomeDialog
    data object Theme : HomeDialog
    data object FontSize : HomeDialog
    data object NavPosition : HomeDialog
    data object StartTab : HomeDialog
}

internal val FONT_SNAP_POINTS = listOf(75, 90, 100, 110, 125, 150, 175)
private val THEME_VALUES = listOf("0", "1", "2", "3")

@Composable
fun SettingsHomeScreen(
    onExit: () -> Unit,
    onOpenRoute: (String) -> Unit,
) {
    val container = rememberContainer()
    val settings by container.settings.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var dialog by remember { mutableStateOf<HomeDialog>(HomeDialog.None) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    val sneakbarState = rememberTopSneakbarState()

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                // FIX: consume the hoisted home ScrollState (provided by SettingsDestination) so
                // scroll position survives home → sub-route → back instead of resetting to top.
                .verticalScroll(LocalSettingsScaffoldScroll.current),
        ) {
            // Modern Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BBIconButton(
                    icon = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(android.R.string.cancel),
                    onClick = onExit,
                )
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.bb_settings_title),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Search pill
            BBTextFieldPill(
                value = query,
                onValueChange = { query = it },
                hint = stringResource(R.string.bb_settings_search_hint),
                leadingIcon = Icons.Filled.Search,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )

            if (query.isBlank()) {
                SettingsSections(
                    settings = settings,
                    showDialog = { dialog = it },
                    onOpenRoute = onOpenRoute,
                    onResetDefaults = { showResetConfirmDialog = true },
                )
            } else {
                SettingsSearchResults(query = query, onOpenRoute = onOpenRoute)
            }

            Spacer(Modifier.height(48.dp))
        }

        TopSneakbarHost(
            state = sneakbarState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            icon = {
                SettingsIconBadge(
                    icon = Icons.Filled.RestartAlt,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    iconColor = MaterialTheme.colorScheme.error,
                )
            },
            title = {
                Text(
                    text = "Reset all settings?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Text(
                    text = "All search engine, appearance, gestures, toolbar, and browser preferences will be restored to their factory defaults.\n\nYour bookmarks, history, start tiles, and tabs will remain untouched.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        container.settings.resetToDefaults()
                        showResetConfirmDialog = false
                        sneakbarState.show(
                            message = "Settings restored to defaults",
                            icon = Icons.Filled.RestartAlt,
                        )
                    },
                ) {
                    Text(
                        text = "Reset",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    when (dialog) {
        HomeDialog.None -> Unit
        HomeDialog.Favorite -> SettingsTextDialog(
            title = stringResource(R.string.setting_title_fav),
            initial = settings.favoriteUrl,
            hint = "https://",
            onConfirm = {
                container.settings.update { s -> s.copy(favoriteUrl = it) }
                dialog = HomeDialog.None
            },
            onDismiss = { dialog = HomeDialog.None },
        )
        HomeDialog.Engine -> {
            val engineNames = SearchEngine.entries.map { engineLabel(it) }
            SettingsChoiceDialog(
                title = stringResource(R.string.setting_title_search_engine),
                options = engineNames,
                selectedIndex = SearchEngine.entries.indexOf(settings.searchEngine),
                label = { it },
                onSelect = { index ->
                    container.settings.update { s -> s.copy(searchEngine = SearchEngine.entries[index]) }
                    dialog = HomeDialog.None
                },
                onDismiss = { dialog = HomeDialog.None },
            )
        }
        HomeDialog.CustomPrefix -> SettingsTextDialog(
            title = stringResource(R.string.setting_title_searchEngine),
            initial = settings.customSearchPrefix,
            hint = "https://…/?q=",
            onConfirm = {
                container.settings.update { s -> s.copy(customSearchPrefix = it) }
                dialog = HomeDialog.None
            },
            onDismiss = { dialog = HomeDialog.None },
        )
        HomeDialog.UserAgent -> SettingsTextDialog(
            title = stringResource(R.string.setting_title_userAgent),
            initial = settings.userAgent,
            hint = stringResource(R.string.bb_user_agent_hint),
            onConfirm = {
                container.settings.update { s -> s.copy(userAgent = it) }
                dialog = HomeDialog.None
            },
            onDismiss = { dialog = HomeDialog.None },
        )
        HomeDialog.Theme -> {
            val themeNames = THEME_VALUES.map { themeLabel(it) }
            SettingsChoiceDialog(
                title = stringResource(R.string.setting_theme),
                options = themeNames,
                selectedIndex = THEME_VALUES.indexOf(settings.themePref.ifBlank { "0" }).coerceAtLeast(0),
                label = { it },
                onSelect = { index ->
                    container.settings.update { s -> s.copy(themePref = THEME_VALUES[index]) }
                    dialog = HomeDialog.None
                },
                onDismiss = { dialog = HomeDialog.None },
            )
        }
        HomeDialog.FontSize -> SettingsChoiceDialog(
            title = stringResource(R.string.setting_title_font),
            options = FONT_SNAP_POINTS,
            selectedIndex = FONT_SNAP_POINTS.indexOf(settings.fontSizePercent).coerceAtLeast(0),
            label = { "$it %" },
            onSelect = { index ->
                container.settings.update { s -> s.copy(fontSizePercent = FONT_SNAP_POINTS[index]) }
                dialog = HomeDialog.None
            },
            onDismiss = { dialog = HomeDialog.None },
        )
        HomeDialog.NavPosition -> {
            val positionNames = NavPosition.entries.map { navPositionLabel(it) }
            SettingsChoiceDialog(
                title = "New Tab Button Position",
                options = positionNames,
                selectedIndex = NavPosition.entries.indexOf(settings.navPosition),
                label = { it },
                onSelect = { index ->
                    container.settings.update { s -> s.copy(navPosition = NavPosition.entries[index]) }
                    dialog = HomeDialog.None
                },
                onDismiss = { dialog = HomeDialog.None },
            )
        }
        HomeDialog.StartTab -> {
            val tabNames = StartTab.entries.map { startTabLabel(it) }
            SettingsChoiceDialog(
                title = stringResource(R.string.setting_overView),
                options = tabNames,
                selectedIndex = StartTab.entries.indexOf(settings.startTab),
                label = { it },
                onSelect = { index ->
                    container.settings.update { s -> s.copy(startTab = StartTab.entries[index]) }
                    dialog = HomeDialog.None
                },
                onDismiss = { dialog = HomeDialog.None },
            )
        }
    }
}

@Composable
private fun SettingsSections(
    settings: BBSettings,
    showDialog: (HomeDialog) -> Unit,
    onOpenRoute: (String) -> Unit,
    onResetDefaults: () -> Unit,
) {
    val container = rememberContainer()

    fun update(transform: (BBSettings) -> BBSettings) {
        container.settings.update(transform)
    }

    // 1. Search & Browser Group
    SettingsCardGroup(title = stringResource(R.string.setting_title_browser)) {
        ModernSettingsRow(
            title = stringResource(R.string.setting_title_fav),
            subtitle = settings.favoriteUrl,
            icon = Icons.Filled.Star,
            iconContainerColor = Color(0xFFFFF3E0),
            iconColor = Color(0xFFF57C00),
            onClick = { showDialog(HomeDialog.Favorite) },
        )
        SettingsDivider()
        ModernSettingsRow(
            title = stringResource(R.string.setting_title_search_engine),
            trailingText = engineLabel(settings.searchEngine),
            icon = Icons.Filled.Search,
            iconContainerColor = Color(0xFFE3F2FD),
            iconColor = Color(0xFF1976D2),
            onClick = { showDialog(HomeDialog.Engine) },
        )
        if (settings.searchEngine == SearchEngine.CUSTOM) {
            SettingsDivider()
            ModernSettingsRow(
                title = stringResource(R.string.setting_title_searchEngine),
                subtitle = settings.customSearchPrefix,
                icon = Icons.Filled.Edit,
                iconContainerColor = Color(0xFFEDE7F6),
                iconColor = Color(0xFF673AB7),
                onClick = { showDialog(HomeDialog.CustomPrefix) },
            )
        }
        SettingsDivider()
        ModernSettingsRow(
            title = stringResource(R.string.setting_title_userAgent),
            subtitle = settings.userAgent.ifBlank { stringResource(R.string.bb_user_agent_default) },
            icon = Icons.Filled.Devices,
            iconContainerColor = Color(0xFFE0F2F1),
            iconColor = Color(0xFF00897B),
            onClick = { showDialog(HomeDialog.UserAgent) },
        )
        SettingsDivider()
        ModernSettingsSwitchRow(
            title = "Desktop site (Beta)",
            subtitle = "View all websites in desktop mode",
            icon = Icons.Filled.Laptop,
            iconContainerColor = Color(0xFFE8EAF6),
            iconColor = Color(0xFF3F51B5),
            checked = settings.desktopMode,
            onCheckedChange = { v -> update { s -> s.copy(desktopMode = v) } },
        )
    }

    // 2. Appearance & Display Group
    SettingsCardGroup(title = stringResource(R.string.bb_section_appearance)) {
        ModernSettingsRow(
            title = stringResource(R.string.setting_theme),
            trailingText = themeLabel(settings.themePref.ifBlank { "0" }),
            icon = Icons.Filled.Palette,
            iconContainerColor = Color(0xFFF3E5F5),
            iconColor = Color(0xFF8E24AA),
            onClick = { showDialog(HomeDialog.Theme) },
        )
        SettingsDivider()
        ModernSettingsSwitchRow(
            title = stringResource(R.string.bb_dynamic_color_title),
            subtitle = stringResource(R.string.bb_dynamic_color_summary),
            icon = Icons.Filled.ColorLens,
            iconContainerColor = Color(0xFFFCE4EC),
            iconColor = Color(0xFFD81B60),
            checked = settings.dynamicColor,
            onCheckedChange = { v -> update { s -> s.copy(dynamicColor = v) } },
        )
        SettingsDivider()
        ModernSettingsRow(
            title = stringResource(R.string.setting_title_font),
            trailingText = "${settings.fontSizePercent} %",
            icon = Icons.Filled.FormatSize,
            iconContainerColor = Color(0xFFFFF8E1),
            iconColor = Color(0xFFFFA000),
            onClick = { showDialog(HomeDialog.FontSize) },
        )
        SettingsDivider()
        ModernSettingsSwitchRow(
            title = stringResource(R.string.setting_title_inverted),
            subtitle = stringResource(R.string.setting_summary_inverted),
            icon = Icons.Filled.DarkMode,
            iconContainerColor = Color(0xFFEDE7F6),
            iconColor = Color(0xFF5E35B1),
            checked = settings.invertWebContent,
            onCheckedChange = { v -> update { s -> s.copy(invertWebContent = v) } },
        )
    }

    // 3. Toolbar & Navigation Group
    SettingsCardGroup(title = "Toolbar & Navigation") {
        ModernSettingsSwitchRow(
            title = stringResource(R.string.setting_title_hideToolbar),
            subtitle = stringResource(R.string.setting_summary_hide),
            icon = Icons.Filled.VisibilityOff,
            iconContainerColor = Color(0xFFE8F5E9),
            iconColor = Color(0xFF43A047),
            checked = settings.hideToolbarOnScroll,
            onCheckedChange = { v -> update { s -> s.copy(hideToolbarOnScroll = v) } },
        )
        SettingsDivider()
        ModernSettingsSwitchRow(
            title = stringResource(R.string.setting_title_toolbarShow),
            subtitle = stringResource(R.string.setting_summary_toolbarShow),
            icon = Icons.Filled.ViewHeadline,
            iconContainerColor = Color(0xFFF1F8E9),
            iconColor = Color(0xFF689F38),
            checked = settings.showToolbarOnBack,
            onCheckedChange = { v -> update { s -> s.copy(showToolbarOnBack = v) } },
        )
        SettingsDivider()
        ModernSettingsRow(
            title = "New Tab Position",
            subtitle = "Choose placement of the New Tab (+) button on bottom bar",
            trailingText = navPositionLabel(settings.navPosition),
            icon = Icons.Filled.Add,
            iconContainerColor = Color(0xFFE8F5E9),
            iconColor = Color(0xFF2E7D32),
            onClick = { showDialog(HomeDialog.NavPosition) },
        )
        SettingsDivider()
        ModernSettingsSwitchRow(
            title = stringResource(R.string.setting_overView_onStart_title),
            subtitle = stringResource(R.string.setting_overView_onStart_summary),
            icon = Icons.Filled.OpenInBrowser,
            iconContainerColor = Color(0xFFFFF3E0),
            iconColor = Color(0xFFFB8C00),
            checked = settings.overviewOnStart,
            onCheckedChange = { v -> update { s -> s.copy(overviewOnStart = v) } },
        )
        SettingsDivider()
        ModernSettingsRow(
            title = stringResource(R.string.setting_overView),
            trailingText = startTabLabel(settings.startTab),
            icon = Icons.Filled.Tab,
            iconContainerColor = Color(0xFFEFEBE9),
            iconColor = Color(0xFF6D4C41),
            onClick = { showDialog(HomeDialog.StartTab) },
        )
    }

    // 4. Privacy & Filter Group
    SettingsCardGroup(title = "Privacy & Filtering") {
        val context = androidx.compose.ui.platform.LocalContext.current
        val allTimeBlocked = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            .getInt("stats_ads_blocked", 0)

        ModernSettingsSwitchRow(
            title = "AdBlock & Privacy Shield",
            subtitle = if (settings.adBlockEnabled) {
                if (allTimeBlocked > 0) "$allTimeBlocked ads & trackers blocked all-time" else "Active • Ads & trackers blocked"
            } else {
                "Shield is disabled globally"
            },
            icon = Icons.Filled.Security,
            iconContainerColor = Color(0xFFE8F5E9),
            iconColor = Color(0xFF00C853),
            checked = settings.adBlockEnabled,
            onCheckedChange = { v -> update { s -> s.copy(adBlockEnabled = v) } },
        )
        SettingsDivider()
        ModernSettingsRow(
            title = "Exempt Sites / Whitelist",
            subtitle = "Websites allowed to show ads and run scripts",
            icon = Icons.Filled.List,
            iconContainerColor = Color(0xFFE0F2F1),
            iconColor = Color(0xFF00897B),
            onClick = { onOpenRoute(SettingsRoute.WHITELIST_ADBLOCK.id) },
        )
        SettingsDivider()
        ModernSettingsRow(
            title = stringResource(R.string.setting_title_start_control),
            subtitle = stringResource(R.string.setting_summary_start_control),
            icon = Icons.Filled.Shield,
            iconContainerColor = Color(0xFFE0F2F1),
            iconColor = Color(0xFF00897B),
            onClick = { onOpenRoute(SettingsRoute.PRIVACY.id) },
        )
        SettingsDivider()
        ModernSettingsRow(
            title = stringResource(R.string.setting_filter),
            subtitle = stringResource(R.string.setting_filter_summary),
            icon = Icons.Filled.Security,
            iconContainerColor = Color(0xFFFFEBEE),
            iconColor = Color(0xFFE53935),
            onClick = { onOpenRoute(SettingsRoute.FILTER.id) },
        )
    }

    // 5. Behavior Group
    SettingsCardGroup(title = stringResource(R.string.setting_title_behavior)) {
        ModernSettingsSwitchRow(
            title = stringResource(R.string.setting_title_confirm_exit),
            subtitle = stringResource(R.string.setting_summary_confirm_exit),
            icon = Icons.Filled.ExitToApp,
            iconContainerColor = Color(0xFFECEFF1),
            iconColor = Color(0xFF607D8B),
            checked = settings.confirmExit,
            onCheckedChange = { v -> update { s -> s.copy(confirmExit = v) } },
        )
        SettingsDivider()
        ModernSettingsSwitchRow(
            title = stringResource(R.string.setting_title_confirm_tab_close),
            subtitle = stringResource(R.string.setting_summary_confirm_tab_close),
            icon = Icons.Filled.Close,
            iconContainerColor = Color(0xFFFFEBEE),
            iconColor = Color(0xFFE53935),
            checked = settings.confirmTabClose,
            onCheckedChange = { v -> update { s -> s.copy(confirmTabClose = v) } },
        )
        SettingsDivider()
        ModernSettingsSwitchRow(
            title = stringResource(R.string.setting_title_reopenLastTab),
            subtitle = stringResource(R.string.setting_summary_reopenLastTab),
            icon = Icons.Filled.Restore,
            iconContainerColor = Color(0xFFE3F2FD),
            iconColor = Color(0xFF1E88E5),
            checked = settings.reopenLastTab,
            onCheckedChange = { v -> update { s -> s.copy(reopenLastTab = v) } },
        )
    }

    // 6. Gestures Group
    SettingsCardGroup(title = stringResource(R.string.setting_gestures)) {
        ModernSettingsRow(
            title = stringResource(R.string.setting_gestures),
            subtitle = stringResource(R.string.setting_gestures_use_summary),
            icon = Icons.Filled.TouchApp,
            iconContainerColor = Color(0xFFF3E5F5),
            iconColor = Color(0xFF9C27B0),
            onClick = { onOpenRoute(SettingsRoute.GESTURES.id) },
        )
    }

    // 7. Data & Storage Group
    SettingsCardGroup(title = stringResource(R.string.setting_title_data)) {
        ModernSettingsRow(
            title = stringResource(R.string.setting_title_clear_control),
            subtitle = stringResource(R.string.setting_summary_clear_control),
            icon = Icons.Filled.DeleteSweep,
            iconContainerColor = Color(0xFFFBE9E7),
            iconColor = Color(0xFFFF5722),
            onClick = { onOpenRoute(SettingsRoute.CLEAR.id) },
        )
        SettingsDivider()
        ModernSettingsRow(
            title = stringResource(R.string.setting_title_data),
            subtitle = stringResource(R.string.setting_summary_data),
            icon = Icons.Filled.Storage,
            iconContainerColor = Color(0xFFE1F5FE),
            iconColor = Color(0xFF0288D1),
            onClick = { onOpenRoute(SettingsRoute.DATA.id) },
        )
    }

    // 8. Help & Feedback Group
    SettingsCardGroup(title = stringResource(R.string.setting_title_others)) {
        ModernSettingsRow(
            title = "Feedback",
            subtitle = "Send thoughts, bugs or feature requests",
            icon = Icons.Filled.Feedback,
            iconContainerColor = Color(0xFFE0F7FA),
            iconColor = Color(0xFF00ACC1),
            onClick = { onOpenRoute(SettingsRoute.FEEDBACK.id) },
        )
        SettingsDivider()
        ModernSettingsRow(
            title = stringResource(R.string.menu_other_help),
            subtitle = "Help, tips and user manual",
            icon = Icons.Filled.Info,
            iconContainerColor = Color(0xFFE8EAF6),
            iconColor = Color(0xFF3949AB),
            onClick = { onOpenRoute(SettingsRoute.ABOUT.id) },
        )
    }

    // 9. Reset to Default Settings Card
    SettingsCardGroup(title = "Reset") {
        ModernSettingsRow(
            title = "Reset to Default Settings",
            subtitle = "Restore all settings to initial factory defaults",
            icon = Icons.Filled.RestartAlt,
            iconContainerColor = Color(0xFFFFEBEE),
            iconColor = Color(0xFFD32F2F),
            destructive = true,
            onClick = onResetDefaults,
        )
    }
}

@Composable
private fun SettingsSearchResults(
    query: String,
    onOpenRoute: (String) -> Unit,
) {
    val index = settingsIndex()
    val results = remember(query) { SettingsIndex.filter(index, query) }

    SettingsCardGroup(title = "Search Results") {
        results.forEachIndexed { idx, entry ->
            val route = entry.route ?: return@forEachIndexed
            if (idx > 0) SettingsDivider()
            ModernSettingsRow(
                title = entry.title,
                subtitle = entry.keywords,
                icon = Icons.Filled.Search,
                iconContainerColor = Color(0xFFE3F2FD),
                iconColor = Color(0xFF1976D2),
                onClick = { onOpenRoute(route.id) },
            )
        }
        if (results.isEmpty()) {
            Text(
                stringResource(R.string.bb_settings_search_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(BBSpace.md),
            )
        }
    }
}

/* ---- label helpers ----------------------------------------------------------------------- */

@Composable
internal fun engineLabel(engine: SearchEngine): String = when (engine) {
    SearchEngine.DUCKDUCKGO -> stringResource(R.string.setting_summary_search_engine_duckduckgo)
    SearchEngine.GOOGLE -> stringResource(R.string.setting_summary_search_engine_google)
    SearchEngine.BING -> stringResource(R.string.setting_summary_search_engine_bing)
    SearchEngine.BAIDU -> stringResource(R.string.setting_summary_search_engine_baidu)
    SearchEngine.SEARX -> stringResource(R.string.setting_summary_search_engine_searx)
    SearchEngine.QWANT -> stringResource(R.string.setting_summary_search_engine_qwant)
    SearchEngine.CUSTOM -> stringResource(R.string.setting_title_searchEngine)
}

@Composable
internal fun themeLabel(prefValue: String): String = when (prefValue) {
    "1" -> stringResource(R.string.setting_theme_light)
    "2" -> stringResource(R.string.setting_theme_dark)
    "3" -> "Amoled"
    else -> stringResource(R.string.setting_theme_system)
}

@Composable
internal fun navPositionLabel(position: NavPosition): String = when (position) {
    NavPosition.LEFT -> stringResource(R.string.setting_summary_nav_pos_left)
    NavPosition.CENTER -> stringResource(R.string.setting_summary_nav_pos_center)
    NavPosition.HIDDEN -> stringResource(R.string.setting_summary_nav_pos_null)
    NavPosition.RIGHT -> stringResource(R.string.setting_summary_nav_pos_right)
}

@Composable
internal fun startTabLabel(tab: StartTab): String = when (tab) {
    StartTab.TABS -> stringResource(R.string.album_title_tab)
    StartTab.BOOKMARKS -> stringResource(R.string.album_title_bookmarks)
    StartTab.HISTORY -> stringResource(R.string.album_title_history)
    StartTab.HOME -> stringResource(R.string.album_title_home)
}

/** Flat, searchable index of everything reachable from the root screen. */
@Composable
fun settingsIndex(): List<SettingsEntry> = listOf(
    SettingsEntry(SettingsRoute.PRIVACY, stringResource(R.string.setting_title_start_control),
        "privacy adblock javascript cookies remote whitelist location images savedata"),
    SettingsEntry(SettingsRoute.CLEAR, stringResource(R.string.setting_title_clear_control),
        "clear cache history cookies indexeddb delete database quit"),
    SettingsEntry(SettingsRoute.FILTER, stringResource(R.string.setting_filter),
        "bookmark color filter colours dots"),
    SettingsEntry(SettingsRoute.GESTURES, stringResource(R.string.setting_gestures),
        "swipe gestures toolbar navigation button"),
    SettingsEntry(SettingsRoute.DATA, stringResource(R.string.setting_title_data),
        "backup export import bookmarks app data"),
    SettingsEntry(SettingsRoute.FEEDBACK, "Feedback", "feedback report bug suggestion issue"),
    SettingsEntry(SettingsRoute.ABOUT, stringResource(R.string.menu_other_help), "help tips"),
    SettingsEntry(SettingsRoute.ABOUT, stringResource(R.string.menu_other_info), "about license info version"),
)
