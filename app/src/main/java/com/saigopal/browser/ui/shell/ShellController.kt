package com.saigopal.browser.ui.shell

import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.saigopal.browser.AppContainer
import com.saigopal.browser.browser.AlbumController
import com.saigopal.browser.browser.BrowserController
import com.saigopal.browser.domain.ClearFlags
import com.saigopal.browser.domain.GestureAction
import com.saigopal.browser.domain.TabState
import com.saigopal.browser.ui.screens.overview.OverviewSegment
import com.saigopal.browser.view.SWebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** One pending geolocation decision, held until BrowserScreen's dialog answers it. */
data class GeoPromptRequest(
    val origin: String,
    val callback: android.webkit.GeolocationPermissions.Callback,
)

/*
 * ShellController (P05) — the Compose-side BrowserController every pooled SWebView talks to.
 *
 * Ported responsibilities from legacy MainActivity: progress reporting (harvesting title/url off
 * the active instance exactly when onProgressChanged fires, since the engine only pushes titles
 * into AlbumItem), album switch/close, file-chooser hand-off, custom-view (fullscreen video)
 * hosting on the decor view (parity port of MainActivity.onShowCustomView — lower-risk than a
 * Compose-owned host and pixel-identical), link long-press and overview control.
 *
 * P09 additions: real geolocation prompts (B2), per-tab incognito cookie isolation (B4), and a
 * per-tab desktop-site mode replacing the global forced UA (B5).
 */

class ShellController(
    private val activity: AppCompatActivity,
    val container: AppContainer,
) : BrowserController {

    lateinit var pool: WebViewPool

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** P09 (B5): the device's own UA for normal tabs, a stable-Chrome desktop UA when toggled. */
    private val defaultUa: String =
        android.webkit.WebSettings.getDefaultUserAgent(activity.applicationContext)

    // ---------------------------------------------------------------- observable shell state

    val progress: androidx.compose.runtime.MutableIntState = mutableIntStateOf(100)
    val autocompleteTick: androidx.compose.runtime.MutableIntState = mutableIntStateOf(0)
    val overviewVisible: MutableState<Boolean> = mutableStateOf(false)
    val overviewInitialSegment: MutableState<OverviewSegment?> = mutableStateOf(null)
    val omniboxEditing: MutableState<Boolean> = mutableStateOf(false)
    val findActive: MutableState<Boolean> = mutableStateOf(false)
    val chromeVisible: MutableState<Boolean> = mutableStateOf(true)
    val longPressUrl: MutableState<String?> = mutableStateOf(null)
    val pageMenuVisible: MutableState<Boolean> = mutableStateOf(false)
    val quitRequested: MutableState<Boolean> = mutableStateOf(false)
    val fullscreenActive: MutableState<Boolean> = mutableStateOf(false)

    // P09 state
    val geoPrompt: MutableState<GeoPromptRequest?> = mutableStateOf(null)
    val incognitoExplainerVisible: MutableState<Boolean> = mutableStateOf(false)

    // ---------------------------------------------------------------- BrowserController

    override fun updateAutoComplete() {
        autocompleteTick.intValue++
        // Load-finished parity: also record history through the repository layer.
        val wv = activeWebView()
        val url = wv?.url ?: return
        if (activeTab()?.isIncognito == true) return // P09 (B4): incognito tabs leave no history
        if (container.urlInterpreter.isInternalScheme(url)) return
        if (!container.settings.state.value.saveHistory) return
        // WebView getters must run on the caller (main) thread — only the DB write hops to IO.
        val title = wv.title?.ifBlank { url } ?: url
        scope.launch {
            container.records.addHistory(
                com.saigopal.browser.domain.BBRecord(
                    title = title,
                    url = url,
                    time = System.currentTimeMillis(),
                ),
            )
        }
    }

    /** Last per-tab metadata snapshot; prevents a full tab-list emit on every progress tick. */
    private var lastMetaTitle: String? = null
    private var lastMetaUrl: String? = null
    private var lastMetaProgress = -1
    private var lastMetaTabId: Long? = null

    override fun updateProgress(progress: Int) {
        this.progress.intValue = progress
        val id = container.tabs.activeId.value
        val wv = pool.get(id)
        if (id != null && wv != null) {
            // PERF: TabManager.updateMeta copies the whole tab list and emits a new StateFlow
            // value each call; onProgressChanged fires dozens of times per page. Nothing renders
            // TabState.progress (the bar reads ShellController.progress), so only forward
            // changes that actually matter: title/url immediately, progress coalesced to ≥10
            // steps or completion. Visible behavior is identical; recomposition churn and the
            // per-tick session-save debounce churn are gone.
            val title = wv.title?.takeIf { it.isNotBlank() }
            val url = wv.url
            val titleChanged = title != lastMetaTitle || id != lastMetaTabId
            val urlChanged = url != lastMetaUrl || id != lastMetaTabId
            val progressChanged =
                progress == 100 ||
                    progress - lastMetaProgress >= 10 ||
                    progress < lastMetaProgress // new navigation restarted the bar
            if (titleChanged || urlChanged || progressChanged) {
                container.tabs.updateMeta(
                    id,
                    title = title,
                    url = url,
                    progress = progress,
                )
                lastMetaTitle = title
                lastMetaUrl = url
                lastMetaProgress = progress
                lastMetaTabId = id
            }
        }
    }

    override fun showAlbum(albumController: AlbumController) {
        val id = pool.idOf(albumController) ?: return
        container.tabs.activate(id)
        pool.activate(id)
        applyIncognitoCookiePolicy()
        overviewVisible.value = false
    }

    override fun removeAlbum(albumController: AlbumController) {
        pool.idOf(albumController)?.let(::closeTab)
    }

    /** Delegates to MainActivity.showFileChooser; onActivityResult plumbing stays intact. */
    override fun showFileChooser(filePathCallback: android.webkit.ValueCallback<Array<android.net.Uri>>?) {
        (activity as? com.saigopal.browser.activity.MainActivity)?.showFileChooser(filePathCallback)
    }

    override fun hideOverview() {
        overviewVisible.value = false
    }

    override fun onLongPress(url: String?) {
        longPressUrl.value = url
    }

    // ---------------------------------------------------------------- P09 geolocation (B2)

    /**
     * Replaces the legacy silent auto-grant: master switch off denies outright, a remembered
     * per-origin decision replays silently, anything else parks a prompt for BrowserScreen.
     */
    override fun onGeolocationPrompt(origin: String?, callback: android.webkit.GeolocationPermissions.Callback?) {
        if (origin == null || callback == null) return
        if (!container.settings.state.value.locationAllowed) {
            callback.invoke(origin, false, false)
            return
        }
        if (WebsitePermissionStore.hasDecision(origin)) {
            callback.invoke(origin, WebsitePermissionStore.decisionFor(origin), false)
            return
        }
        geoPrompt.value = GeoPromptRequest(origin, callback)
    }

    fun answerGeolocation(granted: Boolean) {
        val request = geoPrompt.value ?: return
        geoPrompt.value = null
        WebsitePermissionStore.remember(request.origin, granted)
        request.callback.invoke(request.origin, granted, false)
    }

    private fun activeTab(): TabState? = container.tabs.active

    // ---------------------------------------------------------------- fullscreen (decor-view port)

    private var customView: View? = null
    private var originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private var fullscreenHolder: FrameLayout? = null
    private var videoView: VideoView? = null

    override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
        if (view == null) return
        if (customView != null) {
            callback?.onCustomViewHidden()
            return
        }
        customView = view
        originalOrientation = activity.requestedOrientation

        val holder = FrameLayout(activity).apply {
            addView(view, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
        fullscreenHolder = holder
        (activity.window.decorView as FrameLayout).addView(
            holder,
            FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT),
        )

        view.keepScreenOn = true
        fullscreenActive.value = true
        chromeVisible.value = false

        val video = (view as? FrameLayout)?.focusedChild as? VideoView
        if (video != null) {
            videoView = video
            val completion = android.media.MediaPlayer.OnCompletionListener {
                onHideCustomView()
            }
            val error = android.media.MediaPlayer.OnErrorListener { _, _, _ -> false }
            video.setOnCompletionListener(completion)
            video.setOnErrorListener(error)
        }
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun onHideCustomView() {
        val holder = fullscreenHolder ?: return
        (activity.window.decorView as FrameLayout).removeView(holder)

        customView?.keepScreenOn = false
        fullscreenHolder = null
        videoView?.setOnErrorListener(null)
        videoView?.setOnCompletionListener(null)
        videoView = null
        customView = null
        fullscreenActive.value = false
        chromeVisible.value = true
        activity.requestedOrientation = originalOrientation
    }

    // ---------------------------------------------------------------- actions

    fun activeWebView(): SWebView? = pool.get(container.tabs.activeId.value)

    fun activeTabId(): Long? = container.tabs.activeId.value

    /** Legacy favoriteURL default was the Newtab asset; the shell renders about:startpage natively. */
    fun startPageUrl(): String {
        val fav = container.settings.state.value.favoriteUrl
        return if (fav.isBlank() || fav.contains("Newtab.html", ignoreCase = true)) {
            com.saigopal.browser.domain.UrlInterpreter.URL_START_PAGE
        } else {
            fav
        }
    }

    fun openStartTab(foreground: Boolean = true) {
        newTab(startPageUrl(), foreground)
    }

    /**
     * IsIncognito (settings.incognitoActive) is the DEFAULT for new tabs, not a global mode —
     * P09 (B4). First-ever incognito use raises the one-time explainer.
     */
    fun newTab(url: String?, foreground: Boolean) {
        val settings = container.settings.state.value
        val incognito = settings.incognitoActive
        if (incognito && !incognitoExplainerSeen()) {
            markIncognitoExplainerSeen()
            incognitoExplainerVisible.value = true
        }
        lastMetaTabId = null
        lastMetaProgress = -1
        lastMetaTitle = null
        lastMetaUrl = null
        val id = container.tabs.newTab(
            title = activity.getString(com.saigopal.browser.R.string.app_name),
            url = url,
            foreground = foreground,
            incognito = incognito,
        )
        if (foreground) {
            pool.activate(id)
            applyIncognitoCookiePolicy()
            url?.let(::loadIntoActiveIfReal)
        }
    }

    /**
     * Routes a URL into the active tab. about:startpage never reaches the engine — the shell
     * renders StartPageScreen for that tab until a real navigation replaces it.
     */
    fun loadInActive(rawUrl: String) {
        val id = activeTabId() ?: return
        container.tabs.updateMeta(id, url = rawUrl, title = null)
        lastMetaUrl = rawUrl
        lastMetaTitle = null
        lastMetaProgress = 0
        loadIntoActiveIfReal(rawUrl)
        progress.intValue = 0
    }

    private fun loadIntoActiveIfReal(url: String) {
        if (com.saigopal.browser.domain.UrlInterpreter.isStartPage(url)) return
        val wv = pool.getOrCreate(activeTabId() ?: return)
        applyDesktopMode(wv)
        wv.loadUrl(url)
    }

    /** P09 (9.3): per-tab desktop-site toggle; UA applies to the pooled instance immediately. */
    fun toggleDesktopActive() {
        val id = activeTabId() ?: return
        val now = !(activeTab()?.isDesktopMode ?: false)
        container.tabs.setDesktop(id, now)
        activeWebView()?.let(::applyDesktopMode)
        activeTabId()?.let { reload() }
    }

    private fun applyDesktopMode(wv: SWebView) {
        val on = activeTab()?.isDesktopMode ?: false
        wv.settings.userAgentString = if (on) MODERN_DESKTOP_UA else defaultUa
        wv.settings.useWideViewPort = on
        wv.settings.loadWithOverviewMode = on
    }

    /**
     * Toggles incognito mode on the active tab dynamically.
     * When toggled ON, the active tab continues in incognito mode:
     * - Future browsing leaves no history.
     * - Cookies are isolated/disabled for this tab.
     * - Also updates settings.incognitoActive so subsequent new tabs follow user intent.
     */
    fun toggleActiveTabIncognito(on: Boolean) {
        val id = activeTabId() ?: return
        container.tabs.setIncognito(id, on)
        container.settings.update { it.copy(incognitoActive = on) }
        applyIncognitoCookiePolicy()
        if (on && !incognitoExplainerSeen()) {
            markIncognitoExplainerSeen()
            incognitoExplainerVisible.value = true
        }
    }

    /**
     * P09 (B4): while an incognito tab is foregrounded the engine accepts no cookies; leaving it
     * restores the user's cookie preference. Called on every active-tab switch.
     */
    private fun applyIncognitoCookiePolicy() {
        val incognitoForeground = activeTab()?.isIncognito == true
        val accept = !incognitoForeground && container.settings.state.value.cookiesEnabled
        android.webkit.CookieManager.getInstance().setAcceptCookie(accept)
    }

    fun closeTab(id: Long) {
        if (fullscreenActive.value) onHideCustomView()
        val closing = container.tabs.tabs.value.firstOrNull { it.id == id }
        val lastIncognito =
            closing?.isIncognito == true &&
                container.tabs.tabs.value.count { it.isIncognito } == 1
        pool.destroy(id)
        container.tabs.close(id)
        if (lastIncognito) wipeIncognitoArtifacts()
        if (container.tabs.tabs.value.isEmpty()) {
            // Reopen-last-tab pref collapses to "fresh start page" for now — the session store
            // does not yet remember closed tabs (P10 moves closing into Room).
            openStartTab()
        } else {
            container.tabs.activeId.value?.let { pool.activate(it) }
            applyIncognitoCookiePolicy()
        }
    }

    /** Best-effort sweep when the LAST incognito tab closes (9.2). */
    private fun wipeIncognitoArtifacts() {
        CookieManager.getInstance().run {
            removeAllCookies(null)
            flush()
        }
        activeWebView()?.clearCache(true)
        WebStorage.getInstance().deleteAllData()
    }

    fun activateTab(id: Long) {
        container.tabs.activate(id)
        pool.activate(id)
        applyIncognitoCookiePolicy()
        lastMetaTabId = id
        lastMetaProgress = -1
        lastMetaTitle = null
        lastMetaUrl = null
        val tab = container.tabs.tabs.value.firstOrNull { it.id == id }
        if (tab != null && !com.saigopal.browser.domain.UrlInterpreter.isStartPage(tab.url.orEmpty())) {
            val wv = pool.get(id)
            if (wv == null || wv.url.isNullOrBlank() || wv.url == "about:blank") {
                tab.url?.let(::loadIntoActiveIfReal)
            } else {
                try {
                    wv.onResume()
                } catch (ignored: Exception) {}
                wv.postInvalidate()
            }
        }
    }

    fun reload() {
        activeWebView()?.reload()
        progress.intValue = 0
    }

    fun webGoBack() {
        val wv = activeWebView()
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            val id = activeTabId()
            if (id != null) {
                val current = container.tabs.tabs.value.firstOrNull { it.id == id }
                if (current != null && !com.saigopal.browser.domain.UrlInterpreter.isStartPage(current.url)) {
                    loadInActive(startPageUrl())
                } else if (container.tabs.tabs.value.size > 1) {
                    closeTab(id)
                }
            }
        }
    }

    fun webForward() {
        activeWebView()?.goForward()
    }

    fun openOverview(segment: OverviewSegment? = null) {
        overviewInitialSegment.value = segment
        overviewVisible.value = true
        omniboxEditing.value = false
        pageMenuVisible.value = false
        // P08 8.5: screen_view parity for the overview surface.
        com.google.firebase.analytics.FirebaseAnalytics.getInstance(activity).logEvent(
            "screen_view",
            android.os.Bundle().apply { putString("screen_name", "overview") },
        )
    }

    fun nextTab(): Long? {
        val nextId = container.tabs.cycle(1) ?: return null
        pool.activate(nextId)
        applyIncognitoCookiePolicy()
        return nextId
    }

    fun previousTab(): Long? {
        val prevId = container.tabs.cycle(-1) ?: return null
        pool.activate(prevId)
        applyIncognitoCookiePolicy()
        return prevId
    }

    fun performGesture(action: GestureAction) {
        when (action) {
            GestureAction.FORWARD -> webForward()
            GestureAction.BACK -> webGoBack()
            GestureAction.PAGE_UP -> activeWebView()?.pageUp(true)
            GestureAction.PAGE_DOWN -> activeWebView()?.pageDown(true)
            GestureAction.NEXT_TAB -> { nextTab() }
            GestureAction.PREVIOUS_TAB -> { previousTab() }
            GestureAction.OPEN_START_OVERVIEW -> openOverview()
            GestureAction.NEW_TAB -> newTab(startPageUrl(), foreground = true)
            GestureAction.CLOSE_TAB -> activeTabId()?.let(::closeTab)
            GestureAction.NONE -> Unit
        }
    }

    fun requestQuit() {
        quitRequested.value = true
    }

    fun applyQuit() {
        runClearOnExit(container.settings.state.value.clearFlags.copy(onQuit = true))
        // saveSessionNow is mutex-guarded; flush it on IO before the activity tears down.
        scope.launch { container.tabs.saveSessionNow() }
        BrowserShellStore.release(activity) // pooled engines die with a real exit, not route switches
        activity.finishAffinity()
    }

    fun runClearOnExit(flags: ClearFlags) {
        if (flags.cache) activeWebView()?.clearCache(true)
        if (flags.cookies) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
        if (flags.history) {
            scope.launch { container.records.clearHistory() }
        }
        if (flags.indexedDb) {
            WebStorage.getInstance().deleteAllData()
        }
    }

    /**
     * Cold/warm-start tab bootstrap (dispatchIntent's empty branch): restore session rows when
     * present, else one start tab; optionally pop the overview when the user wants it on launch.
     */
    fun bootstrapIfNeeded(overviewOnStart: Boolean, intentHasTarget: Boolean) {
        if (container.tabs.tabs.value.isNotEmpty()) return
        val restored = container.tabs.restoreSession()
        if (restored.isNotEmpty()) {
            restored.forEachIndexed { index, (_, url) ->
                if (index == 0) {
                    newTab(url.ifBlank { startPageUrl() }, foreground = true)
                } else {
                    newTab(url, foreground = false)
                }
            }
        } else {
            openStartTab()
        }
        if (overviewOnStart && !intentHasTarget) openOverview()
    }

    private fun incognitoExplainerSeen(): Boolean =
        androidx.preference.PreferenceManager
            .getDefaultSharedPreferences(activity)
            .getBoolean(PREF_INCOGNITO_EXPLAINER, false)

    private fun markIncognitoExplainerSeen() {
        androidx.preference.PreferenceManager
            .getDefaultSharedPreferences(activity)
            .edit()
            .putBoolean(PREF_INCOGNITO_EXPLAINER, true)
            .apply()
    }

    private companion object {
        const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT

        /** One-time incognito explainer flag (9.2); session-scoped prefs stay legacy-owned. */
        const val PREF_INCOGNITO_EXPLAINER = "bb_incognito_explainer_seen"

        /** P09 (B5): a current stable-Chrome desktop UA replaces the stale Chrome/85 string. */
        const val MODERN_DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }
}
