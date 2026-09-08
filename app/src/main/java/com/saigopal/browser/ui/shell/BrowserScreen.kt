package com.saigopal.browser.ui.shell

import android.Manifest
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.saigopal.browser.R
import com.saigopal.browser.data.repo.BBSettings
import com.saigopal.browser.domain.GestureArea
import com.saigopal.browser.domain.GestureDirection
import com.saigopal.browser.domain.GestureRouter
import com.saigopal.browser.domain.NavPosition
import com.saigopal.browser.domain.UrlInterpreter
import com.saigopal.browser.unit.HelperUnit
import com.saigopal.browser.ui.design.components.BBConfirmSheet
import com.saigopal.browser.ui.design.components.BBOmniboxPill
import com.saigopal.browser.ui.design.components.BBTextFieldPill
import com.saigopal.browser.ui.design.components.TopSneakbarHost
import com.saigopal.browser.ui.design.components.rememberTopSneakbarState
import com.saigopal.browser.ui.design.theme.BBSpace
import com.saigopal.browser.ui.design.theme.PillShape
import com.saigopal.browser.ui.di.rememberContainer
import com.saigopal.browser.ui.screens.menu.FastTogglesSheet
import com.saigopal.browser.ui.screens.menu.LinkContextMenu
import com.saigopal.browser.ui.screens.menu.LinkContextActions
import com.saigopal.browser.ui.screens.menu.PageInfo
import com.saigopal.browser.ui.screens.menu.PageMenuActions
import com.saigopal.browser.ui.screens.menu.PageMenuSheet
import com.saigopal.browser.ui.screens.overview.OverviewSegment
import com.saigopal.browser.ui.screens.overview.OverviewSheet
import com.saigopal.browser.ui.screens.start.StartPageScreen
import com.saigopal.browser.view.SWebView
import kotlinx.coroutines.launch

/*
 * BrowserScreen (P05 §5.2) — the Compose browsing shell:
 *
 *   Box {
 *     TabContentArea (start page OR one re-parented pooled SWebView)
 *     chrome: bottom omnibox pill + menu/tabs buttons (+ home button by navPosition), scroll-hidden
 *     FindBar overlay · omnibox edit overlay
 *   }
 *   sheets: overview / page menu / fast toggles / link menu / close & quit confirms
 *
 * Back-chain order (plan §5.4): fullscreen → find → overview → address-edit → toolbar-show →
 * webview canGoBack → close-tab(confirm pref) → quit-confirm / quit.
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BrowserScreen(
    onOpenSettings: () -> Unit,
    onOpenGames: () -> Unit,
    onOpenFeedback: () -> Unit,
) {
    val container = rememberContainer()
    val activity = LocalContext.current as AppCompatActivity
    val scope = rememberCoroutineScope()

    val settings by container.settings.state.collectAsState()
    val tabs by container.tabs.tabs.collectAsState()
    val activeId by container.tabs.activeId.collectAsState()

    // The pool lives in BrowserShellStore (keyed to the Activity), NOT in composition: route
    // switches to settings/games/feedback must not destroy tab WebViews — that was the
    // "everything reloads" bug. Cleanup happens in applyQuit / MainActivity.onDestroy.
    val controller = remember { BrowserShellStore.obtain(activity, container) }
    val pool = controller.pool

    // ---- bootstrap + intent dispatch ---------------------------------------------------------

    LaunchedEffect(Unit) {
        val pendingAction = activity.intent?.action
        val intentHasTarget = !pendingAction.isNullOrEmpty() && pendingAction != Intent.ACTION_MAIN
        controller.bootstrapIfNeeded(
            overviewOnStart = settings.overviewOnStart,
            intentHasTarget = intentHasTarget,
        )
        IntentDispatcher.consume(activity.intent)
    }

    val pendingIntent by ShellActivityHolder.intents.collectAsState()
    LaunchedEffect(pendingIntent) {
        if (pendingIntent == null) return@LaunchedEffect
        when (val cmd = IntentDispatcher.dispatch(pendingIntent)) {
            is IntentCommand.ResumeOnly -> Unit
            is IntentCommand.OpenUrl ->
                controller.newTab(cmd.url?.takeIf { it.isNotBlank() }, foreground = true)
            is IntentCommand.WebSearch ->
                controller.newTab(
                    container.urlInterpreter.interpret(
                        cmd.query.orEmpty().trim(),
                        settings.searchEngine,
                        settings.customSearchPrefix,
                    ),
                    foreground = true,
                )
            is IntentCommand.ShortcutOverview -> {
                controller.openStartTab()
                controller.openOverview(cmd.segment)
            }
            is IntentCommand.ColdStart ->
                controller.bootstrapIfNeeded(overviewOnStart = settings.overviewOnStart, intentHasTarget = false)
        }
        IntentDispatcher.consume(pendingIntent)
        ShellActivityHolder.post(null)
    }

    // ---- local UI state ----------------------------------------------------------------------

    var editQuery by remember { mutableStateOf("") }
    var findQuery by remember { mutableStateOf("") }
    var findCount by remember { mutableIntStateOf(0) }
    var fastTogglesVisible by remember { mutableStateOf(false) }
    var confirmCloseRequested by remember { mutableStateOf(false) }
    val sneakbarState = rememberTopSneakbarState()

    // P09 (9.1): OS-level location permission follows the user's web-level Allow decision.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* result ignored: the WebView enforces the OS grant itself */ }

    val activeTab = tabs.firstOrNull { it.id == activeId }
    val activeOnStartPage = activeTab == null || UrlInterpreter.isStartPage(activeTab.url)

    // Hardware menu key → page menu (invoked from MainActivity's flipped branch).
    DisposableEffect(Unit) {
        ShellActivityHolder.menuOpener = Runnable { controller.pageMenuVisible.value = true }
        onDispose { ShellActivityHolder.menuOpener = null }
    }

    // ---- find-in-page wiring ------------------------------------------------------------------

    LaunchedEffect(controller.findActive.value, activeId) {
        val wv = pool.get(activeId)
        if (controller.findActive.value) {
            wv?.setFindListener { _, matchCount, _ -> findCount = matchCount }
        } else {
            wv?.setFindListener(null)
            wv?.clearMatches()
            findQuery = ""
            findCount = 0
        }
    }

    // ---- scroll-hide (port of legacy updateProgress hide-on-scroll-down, show-on-scroll-up) ----

    val activeWv = activeId?.let { pool.get(it) }
    DisposableEffect(activeWv, settings.hideToolbarOnScroll) {
        val listener = SWebView.OnScrollChangeListener { scrollY, oldScrollY ->
            if (!settings.hideToolbarOnScroll ||
                controller.findActive.value ||
                controller.fullscreenActive.value ||
                controller.omniboxEditing.value
            ) {
                return@OnScrollChangeListener
            }
            // Hide once scrolled well past the top moving down; any upward scroll reveals.
            controller.chromeVisible.value = scrollY < oldScrollY || scrollY < 200
        }
        activeWv?.setOnScrollChangeListener(listener)
        // Explicit type: SWebView's setter overloads against View's, so a bare null is ambiguous.
        onDispose {
            activeWv?.setOnScrollChangeListener(null as SWebView.OnScrollChangeListener?)
        }
    }

    // ---- back chain -----------------------------------------------------------------------------

    var lastBackPressTime by remember { mutableStateOf(0L) }

    fun closeActiveWithPolicy() {
        if (settings.confirmTabClose) confirmCloseRequested = true
        else activeTab?.let { controller.closeTab(it.id) }
    }

    BackHandler(enabled = true) {
        when {
            controller.fullscreenActive.value -> controller.onHideCustomView()
            controller.findActive.value -> controller.findActive.value = false
            controller.pageMenuVisible.value -> controller.pageMenuVisible.value = false
            fastTogglesVisible -> fastTogglesVisible = false
            controller.overviewVisible.value -> controller.overviewVisible.value = false
            controller.omniboxEditing.value -> controller.omniboxEditing.value = false
            !controller.chromeVisible.value && settings.showToolbarOnBack -> controller.chromeVisible.value = true
            !activeOnStartPage && pool.get(activeId)?.canGoBack() == true -> controller.webGoBack()
            !activeOnStartPage -> controller.loadInActive(controller.startPageUrl())
            tabs.size > 1 -> closeActiveWithPolicy()
            settings.confirmExit -> controller.requestQuit()
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000L) {
                    controller.applyQuit()
                } else {
                    lastBackPressTime = now
                    sneakbarState.show("Press back again to exit", Icons.Filled.ExitToApp)
                }
            }
        }
    }

    // ---- tab content ----------------------------------------------------------------------------

    Box(Modifier.fillMaxSize()) {
        if (!activeOnStartPage && activeTab != null) {
            key(activeTab.id) {
                AndroidView(
                    factory = { pool.getOrCreate(activeTab.id).also { v ->
                        (v.parent as? android.view.ViewGroup)?.removeView(v)
                    } },
                    onRelease = { v -> (v.parent as? android.view.ViewGroup)?.removeView(v) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            StartPageScreen(
                onSubmitQuery = { q ->
                    controller.loadInActive(
                        container.urlInterpreter.interpret(q, settings.searchEngine, settings.customSearchPrefix),
                    )
                },
                onOpenTile = { url -> controller.loadInActive(url) },
                onEditTiles = { controller.openOverview(OverviewSegment.HOME) },
                onOpenGames = onOpenGames,
                onOpenFeedback = onOpenFeedback,
            )
        }

        // ---- find bar -------------------------------------------------------------------------------

        AnimatedVisibility(
            visible = controller.findActive.value,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            FindBar(
                query = findQuery,
                onQueryChange = {
                    findQuery = it
                    if (it.isNotBlank()) pool.get(activeId)?.findAllAsync(it) else pool.get(activeId)?.clearMatches()
                },
                matchCount = findCount,
                onNext = { pool.get(activeId)?.findNext(true) },
                onPrevious = { pool.get(activeId)?.findNext(false) },
                onClose = { controller.findActive.value = false },
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            )
        }

        // ---- bottom chrome: expanded floating dock --------------------------------------------------

        AnimatedVisibility(
            visible = controller.chromeVisible.value && !controller.fullscreenActive.value,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            Surface(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = BBSpace.sm, vertical = BBSpace.xs)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = BBSpace.xs, vertical = BBSpace.xs)
                        .tabSwipeGestures(
                            controller = controller,
                            onSwipeLeft = {
                                controller.nextTab()
                                sneakbarState.show("Next Tab", Icons.Filled.List)
                            },
                            onSwipeRight = {
                                controller.previousTab()
                                sneakbarState.show("Previous Tab", Icons.Filled.List)
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BBSpace.xs),
                ) {
                    val navVisible = settings.navPosition != NavPosition.HIDDEN
                    val navLeft = settings.navPosition != NavPosition.RIGHT
                    if (navVisible && navLeft) {
                        NavButton(controller, settings)
                    }
                    val url = activeTab?.url.orEmpty()
                    BBOmniboxPill(
                        text = activeTab?.title?.takeIf { it.isNotBlank() } ?: url,
                        onClick = {
                            editQuery = url
                            controller.omniboxEditing.value = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .chromeGestures(controller, settings),
                        leadingIcon = if (url.startsWith("https")) Icons.Filled.Lock else Icons.Filled.Warning,
                        insecure = url.startsWith("http://"),
                        progress = controller.progress.intValue.takeIf { it < 100 },
                        trailing = if (activeTab?.isDesktopMode == true) {
                            {
                                Icon(
                                    Icons.Filled.Build,
                                    contentDescription = stringResource(R.string.bb_menu_desktop_site),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable(onClick = controller::toggleDesktopActive),
                                )
                            }
                        } else {
                            {}
                        },
                    )
                    IconButton(
                        onClick = { controller.pageMenuVisible.value = true },
                        modifier = Modifier
                            .chromeGestures(controller, settings)
                            .combinedClickable(
                                onClick = { controller.pageMenuVisible.value = true },
                                onLongClick = { fastTogglesVisible = true },
                            ),
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.bb_menu_more),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    TabBadgeButton(
                        count = tabs.size,
                        onClick = { controller.openOverview(OverviewSegment.TABS) },
                    )
                    if (navVisible && !navLeft) {
                        NavButton(controller, settings)
                    }
                }
            }
        }

        // ---- bottom chrome: collapsed compact chip (floating pill on scroll down) -------------------

        AnimatedVisibility(
            visible = !controller.chromeVisible.value && !controller.fullscreenActive.value,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            Surface(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = BBSpace.sm)
                    .height(42.dp)
                    .tabSwipeGestures(
                        controller = controller,
                        onSwipeLeft = {
                            controller.nextTab()
                            sneakbarState.show("Next Tab", Icons.Filled.List)
                        },
                        onSwipeRight = {
                            controller.previousTab()
                            sneakbarState.show("Previous Tab", Icons.Filled.List)
                        },
                    )
                    .clickable { controller.chromeVisible.value = true },
                shape = PillShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = BBSpace.md, vertical = BBSpace.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BBSpace.xs),
                ) {
                    val url = activeTab?.url.orEmpty()
                    Icon(
                        if (url.startsWith("https")) Icons.Filled.Lock else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (url.startsWith("https")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    val domain = container.urlInterpreter.domainOf(url).ifBlank {
                        activeTab?.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.app_name)
                    }
                    Text(
                        text = domain,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.widthIn(max = 140.dp),
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(22.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${tabs.size.coerceAtMost(99)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Expand bar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // ---- omnibox edit mode ------------------------------------------------------------------------

        if (controller.omniboxEditing.value) {
            OmniboxEditOverlay(
                initial = editQuery,
                tick = controller.autocompleteTick.intValue,
                onSubmit = { raw ->
                    controller.omniboxEditing.value = false
                    if (raw.isBlank()) {
                        android.widget.Toast.makeText(
                            activity,
                            R.string.toast_input_empty,
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        controller.chromeVisible.value = true
                        controller.loadInActive(
                            container.urlInterpreter.interpret(raw.trim(), settings.searchEngine, settings.customSearchPrefix),
                        )
                    }
                },
                onCancel = { controller.omniboxEditing.value = false },
                onPickUrl = { picked ->
                    controller.omniboxEditing.value = false
                    controller.chromeVisible.value = true
                    controller.loadInActive(picked)
                },
            )
        }

        TopSneakbarHost(
            state = sneakbarState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    // ---- sheets -------------------------------------------------------------------------------------

    if (controller.overviewVisible.value) {
        OverviewSheet(
            initialSegment = controller.overviewInitialSegment.value ?: OverviewSegment.TABS,
            onDismiss = { controller.overviewVisible.value = false },
            onLoadUrl = { url, inNewTab ->
                controller.overviewVisible.value = false
                if (inNewTab) controller.newTab(url, foreground = true)
                else controller.loadInActive(url)
            },
            onOpenSettings = {
                controller.overviewVisible.value = false
                onOpenSettings()
            },
            onNewTab = {
                controller.overviewVisible.value = false
                controller.newTab(controller.startPageUrl(), foreground = true)
            },
            onOpenGames = {
                controller.overviewVisible.value = false
                onOpenGames()
            },
        )
    }

    // Bound reference: K2 parses `else { … }` as a block, never a lambda, so a brace literal
    // can't sit in an if-expression branch.
    val toggleDesktopAction: (() -> Unit)? =
        if (activeOnStartPage) null else controller::toggleDesktopActive

    if (controller.pageMenuVisible.value) {
        PageMenuSheet(
            page = PageInfo(
                title = activeTab?.title.orEmpty(),
                url = activeTab?.url.orEmpty(),
                isSecure = (activeTab?.url ?: "").startsWith("https"),
                blockedCount = 0,
                // P09: the header reflects the ACTIVE TAB's mode, not the new-tab default.
                isIncognito = activeTab?.isIncognito == true,
                isDesktop = activeTab?.isDesktopMode == true,
            ),
            actions = PageMenuActions(
                onNewTab = {
                    controller.newTab(controller.startPageUrl(), foreground = true)
                    sneakbarState.show("New tab opened", Icons.Filled.Add)
                },
                onFindInPage = {
                    controller.findActive.value = true
                    sneakbarState.show("Find in page", Icons.Filled.Search)
                },
                onSetFavorite = {
                    activeTab?.url?.let { u ->
                        container.settings.update { s -> s.copy(favoriteUrl = u) }
                        sneakbarState.show("Saved as favorite", Icons.Filled.Star)
                    }
                },
                onAddBookmark = {
                    activeTab?.let { t ->
                        scope.launch {
                            container.records.addBookmark(t.title.ifBlank { t.url.orEmpty() }, t.url.orEmpty())
                            sneakbarState.show("Bookmark added", Icons.Filled.FavoriteBorder)
                        }
                    }
                },
                onAddToHome = {
                    activeTab?.let { t ->
                        scope.launch {
                            container.records.addTile(t.title.ifBlank { t.url.orEmpty() }, t.url.orEmpty(), tabs.size)
                            sneakbarState.show("Added to home screen", Icons.Filled.Home)
                        }
                    }
                },
                onCreateShortcut = {
                    activeTab?.let { t ->
                        HelperUnit.createShortcut(activity, t.title.ifBlank { t.url.orEmpty() }, t.url.orEmpty())
                        sneakbarState.show("Shortcut created", Icons.Filled.AddCircle)
                    }
                },
                onShare = {
                    shareText(activity, activeTab?.url.orEmpty())
                    sneakbarState.show("Sharing link...", Icons.Filled.Share)
                },
                onCopyLink = {
                    copyToClipboard(activity, activeTab?.url.orEmpty())
                    sneakbarState.show("Link copied to clipboard", Icons.Filled.List)
                },
                onOpenWith = {
                    openExternally(activity, activeTab?.url.orEmpty())
                    sneakbarState.show("Opening in external app...", Icons.Filled.ExitToApp)
                },
                onSaveAs = {
                    activeTab?.url?.let { u ->
                        HelperUnit.save_as(activity, u)
                        sneakbarState.show("Downloading page...", Icons.Filled.Create)
                    }
                },
                onPrintPdf = {
                    pool.get(activeId)?.let { wv ->
                        val name = HelperUnit.fileName(wv.url)
                        val pm = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                        pm.print(name, wv.createPrintDocumentAdapter(name), PrintAttributes.Builder().build())
                        sneakbarState.show("Preparing PDF print...", Icons.Filled.Done)
                    }
                },
                onOpenDownloads = {
                    activity.startActivity(
                        Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    sneakbarState.show("Opening downloads...", Icons.Filled.ShoppingCart)
                },
                onOpenSettings = onOpenSettings,
                onQuit = { controller.requestQuit() },
                // Hidden for start-page tabs — there is no engine page to re-render as desktop.
                onToggleDesktop = if (toggleDesktopAction == null) null else {
                    {
                        toggleDesktopAction.invoke()
                        sneakbarState.show("Desktop mode toggled", Icons.Filled.Build)
                    }
                },
            ),
            onDismiss = { controller.pageMenuVisible.value = false },
            onReload = {
                controller.reload()
                sneakbarState.show("Page reloaded", Icons.Filled.Refresh)
            },
            onIncognitoChanged = { on ->
                sneakbarState.show(
                    if (on) "Incognito mode on for new tabs" else "Incognito mode off",
                    Icons.Filled.Info,
                )
            },
        )
    }

    if (fastTogglesVisible) {
        FastTogglesSheet(
            currentDomain = container.urlInterpreter.domainOf(activeTab?.url),
            onApplyReload = { controller.reload() },
            onDismiss = { fastTogglesVisible = false },
        )
    }

    controller.longPressUrl.value?.let { link ->
        LinkContextMenu(
            url = link,
            actions = LinkContextActions(
                onNewTabForeground = { controller.newTab(link, foreground = true) },
                onNewTabBackground = { controller.newTab(link, foreground = false) },
                onShare = { shareText(activity, link) },
                onOpenWith = { openExternally(activity, link) },
                onSaveAs = { HelperUnit.save_as(activity, link) },
                onAddToHome = { scope.launch { container.records.addTile(link, link, 0) } },
                onCopyLink = { copyToClipboard(activity, link) },
            ),
            onDismiss = { controller.longPressUrl.value = null },
        )
    }

    if (confirmCloseRequested) {
        BBConfirmSheet(
            title = stringResource(R.string.menu_closeTab),
            body = stringResource(R.string.setting_summary_confirm_tab_close),
            confirmLabel = stringResource(R.string.app_ok),
            onConfirm = {
                confirmCloseRequested = false
                activeTab?.let { controller.closeTab(it.id) }
            },
            onDismiss = { confirmCloseRequested = false },
        )
    }

    if (controller.quitRequested.value) {
        BBConfirmSheet(
            title = stringResource(R.string.menu_quit),
            body = stringResource(R.string.toast_quit),
            confirmLabel = stringResource(R.string.app_ok),
            onConfirm = {
                controller.quitRequested.value = false
                controller.applyQuit()
            },
            onDismiss = { controller.quitRequested.value = false },
        )
    }

    // ---- P09 dialogs ---------------------------------------------------------------------------

    controller.geoPrompt.value?.let { request ->
        GeoPermissionDialog(
            origin = request.origin,
            onAllow = {
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
                controller.answerGeolocation(granted = true)
            },
            onDeny = { controller.answerGeolocation(granted = false) },
        )
    }

    if (controller.incognitoExplainerVisible.value) {
        AlertDialog(
            onDismissRequest = { controller.incognitoExplainerVisible.value = false },
            title = { Text(stringResource(R.string.bb_incognito_title)) },
            text = { Text(stringResource(R.string.bb_incognito_body)) },
            confirmButton = {
                TextButton(onClick = { controller.incognitoExplainerVisible.value = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }
}

/** P09 (9.1): the per-origin web-permission prompt the legacy build never showed (defect B2). */
@Composable
private fun GeoPermissionDialog(origin: String, onAllow: () -> Unit, onDeny: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDeny,
        title = { Text(stringResource(R.string.bb_geo_title)) },
        text = { Text(stringResource(R.string.bb_geo_message, origin)) },
        confirmButton = {
            TextButton(onClick = onAllow) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDeny) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

// ---------------------------------------------------------------- helpers

private fun shareText(context: Context, text: String) {
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            },
            null,
        ),
    )
}

private fun openExternally(context: Context, url: String) {
    if (url.isBlank()) return
    context.startActivity(
        Intent.createChooser(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)), null),
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("url", text))
}

/** Home/start-page button carrying NAV_BUTTON-area gestures; placement follows navPosition. */
@Composable
private fun RowScope.NavButton(
    controller: ShellController,
    settings: BBSettings,
) {
    IconButton(
        onClick = { controller.loadInActive(controller.startPageUrl()) },
        modifier = Modifier.chromeGestures(controller, settings),
    ) {
        Icon(
            Icons.Filled.Home,
            contentDescription = stringResource(R.string.app_name),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Swipe gestures on a chrome control routed through GestureRouter with TOOLBAR-area codes.
 * One gesture per drag: after firing, remaining movement is consumed until drag end.
 */
@Composable
private fun Modifier.chromeGestures(
    controller: ShellController,
    settings: BBSettings,
): Modifier {
    var fired by remember { mutableStateOf(false) }
    var accumulated by remember { mutableStateOf(Offset.Zero) }
    return this.then(
        Modifier.pointerInput(controller) {
            val slopPx = viewConfiguration.touchSlop * 2f
            detectDragGestures(
                onDragEnd = { fired = false; accumulated = Offset.Zero },
                onDragCancel = { fired = false; accumulated = Offset.Zero },
            ) { change, dragAmount ->
                if (fired) {
                    change.consume()
                    return@detectDragGestures
                }
                val next = accumulated + dragAmount
                accumulated = next
                if (kotlin.math.abs(next.x) > slopPx || kotlin.math.abs(next.y) > slopPx) {
                    val direction = if (kotlin.math.abs(next.x) > kotlin.math.abs(next.y)) {
                        if (next.x > 0) GestureDirection.RIGHT else GestureDirection.LEFT
                    } else {
                        if (next.y > 0) GestureDirection.DOWN else GestureDirection.UP
                    }
                    val action = GestureRouter.actionFor(settings, GestureArea.TOOLBAR, direction)
                    GestureRouter.dispatch({ controller.performGesture(it) }, action)
                    fired = true
                    change.consume()
                }
            }
        },
    )
}

@Composable
private fun TabBadgeButton(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(7.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (count > 99) ":D" else "$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Swipe gestures directly on the bottom bar or chip to quickly switch tabs.
 * Dragging left triggers nextTab(), dragging right triggers previousTab().
 */
@Composable
private fun Modifier.tabSwipeGestures(
    controller: ShellController,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
): Modifier {
    var fired by remember { mutableStateOf(false) }
    var totalDrag by remember { mutableFloatStateOf(0f) }
    return this.then(
        Modifier.pointerInput(controller) {
            val thresholdPx = 50.dp.toPx()
            detectDragGestures(
                onDragStart = {
                    fired = false
                    totalDrag = 0f
                },
                onDragEnd = {
                    fired = false
                    totalDrag = 0f
                },
                onDragCancel = {
                    fired = false
                    totalDrag = 0f
                },
            ) { change, dragAmount ->
                if (fired) {
                    change.consume()
                    return@detectDragGestures
                }
                totalDrag += dragAmount.x
                if (kotlin.math.abs(totalDrag) > thresholdPx) {
                    fired = true
                    change.consume()
                    if (totalDrag < 0) {
                        onSwipeLeft()
                    } else {
                        onSwipeRight()
                    }
                }
            }
        },
    )
}

/**
 * Address-edit mode: full-screen modern overlay with backdrop scrim,
 * search engine indicator, auto-focus input with clear and paste buttons,
 * and filtered autocomplete suggestions with icons (Search, History, Bookmark).
 */
@Composable
private fun OmniboxEditOverlay(
    initial: String,
    tick: Int,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
    onPickUrl: (String) -> Unit,
) {
    val container = rememberContainer()
    val context = LocalContext.current
    var query by remember { mutableStateOf(initial) }
    val allRecords by produceState<List<com.saigopal.browser.domain.BBRecord>>(
        initialValue = emptyList(),
        key1 = tick,
    ) {
        value = runCatching { container.records.autocomplete() }.getOrDefault(emptyList())
    }

    val filteredSuggestions = remember(query, allRecords) {
        if (query.isBlank()) {
            allRecords.take(6)
        } else {
            val q = query.trim().lowercase()
            allRecords.filter {
                it.url.lowercase().contains(q) || it.title.lowercase().contains(q)
            }.take(8)
        }
    }

    val clipboardText = remember {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.primaryClip?.getItemAt(0)?.text?.toString()?.takeIf { it.isNotBlank() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f))
            .clickable(onClick = onCancel),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .clickable(enabled = false) {}
                .windowInsetsPadding(WindowInsets.statusBars),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BBSpace.md, vertical = BBSpace.sm),
            ) {
                // Search Bar Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BBSpace.xs),
                ) {
                    BBTextFieldPill(
                        value = query,
                        onValueChange = { query = it },
                        hint = stringResource(R.string.main_omnibox_input_hint),
                        autoFocus = true,
                        onSearch = { onSubmit(query) },
                        modifier = Modifier.weight(1f),
                        trailing = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                    )

                    IconButton(
                        onClick = { onSubmit(query) },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }

                // Quick Paste button if clipboard contains text and query is blank
                if (clipboardText != null && query.isBlank()) {
                    Spacer(Modifier.height(BBSpace.xs))
                    Surface(
                        shape = PillShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                query = clipboardText
                                onSubmit(clipboardText)
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = BBSpace.md, vertical = BBSpace.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(BBSpace.sm),
                        ) {
                            Icon(
                                Icons.Filled.List,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Paste & Go: $clipboardText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // Suggestion List
                if (filteredSuggestions.isNotEmpty()) {
                    Spacer(Modifier.height(BBSpace.xs))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        filteredSuggestions.forEach { record ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPickUrl(record.url) }
                                    .padding(vertical = BBSpace.xs, horizontal = BBSpace.xs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(BBSpace.sm),
                            ) {
                                val isBookmark = record.colorCode > 0
                                Icon(
                                    imageVector = if (isBookmark) Icons.Filled.Star else Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = if (isBookmark) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(20.dp),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = record.title.ifBlank { record.url },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (record.title.isNotBlank()) {
                                        Text(
                                            text = record.url,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { query = record.url },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Create,
                                        contentDescription = "Fill query",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Cancel button row
                Spacer(Modifier.height(BBSpace.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onCancel) {
                        Text(
                            text = stringResource(android.R.string.cancel),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                // Bottom indicator handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = BBSpace.xs, bottom = BBSpace.xxs)
                        .width(36.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}
