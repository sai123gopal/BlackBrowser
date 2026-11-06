package com.saigopal.browser.data.repo

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.saigopal.browser.domain.BookmarkSort
import com.saigopal.browser.domain.ClearFlags
import com.saigopal.browser.domain.GestureAction
import com.saigopal.browser.domain.GestureArea
import com.saigopal.browser.domain.GestureCodes
import com.saigopal.browser.domain.GestureDirection
import com.saigopal.browser.domain.NavPosition
import com.saigopal.browser.domain.SearchEngine
import com.saigopal.browser.domain.StartTab
import com.saigopal.browser.domain.TileSort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/*
 * Typed, observable view over the legacy default SharedPreferences file.
 *
 * Key literals match arrays.xml / preference_*.xml EXACTLY — old installs keep working and
 * legacy screens (still alive during P03–P05) see every change this repository writes.
 * DataStore migration swaps only the storage behind this class in P10.
 */

/** Every user-facing setting, typed. Defaults mirror the preference XMLs. */
data class BBSettings(
    // Appearance
    val themePref: String = "2",                 // 0 system · 1 light · 2 dark · 3 amoled
    val fontSizePercent: Int = 100,
    val invertWebContent: Boolean = true,        // legacy force-dark flag; semantics reworked P09
    val dynamicColor: Boolean = false,           // NEW (Material You) — no legacy key
    // Toolbar behaviour
    val hideToolbarOnScroll: Boolean = true,
    val showToolbarOnBack: Boolean = false,
    val navPosition: NavPosition = NavPosition.RIGHT,
    // Overview
    val overviewOnStart: Boolean = true,
    val startTab: StartTab = StartTab.HOME,
    // Confirmations & tabs
    val confirmExit: Boolean = true,
    val confirmTabClose: Boolean = false,
    val reopenLastTab: Boolean = false,
    // Browser core
    val favoriteUrl: String = "file:///android_asset/Newtab.html",
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val customSearchPrefix: String = "https://duckduckgo.com/search?q=",
    val userAgent: String = "",
    val desktopMode: Boolean = false,
    val incognitoActive: Boolean = false,        // global toggle today; becomes per-tab default P09
    // Privacy toggles
    val saveHistory: Boolean = true,
    val images: Boolean = true,
    val saveDataHeader: Boolean = true,
    val locationAllowed: Boolean = false,
    val adBlockEnabled: Boolean = true,
    val javaScriptEnabled: Boolean = true,
    val cookiesEnabled: Boolean = true,
    val remoteContentEnabled: Boolean = false,
    // Gestures
    val gesturesEnabled: Boolean = true,
    val gestureToolbarUp: GestureAction = GestureAction.OPEN_START_OVERVIEW,   // "08"
    val gestureToolbarDown: GestureAction = GestureAction.NONE,                // "01"
    val gestureToolbarLeft: GestureAction = GestureAction.PREVIOUS_TAB,        // "07"
    val gestureToolbarRight: GestureAction = GestureAction.NEXT_TAB,           // "06"
    val gestureNavUp: GestureAction = GestureAction.PAGE_UP,                   // "04"
    val gestureNavDown: GestureAction = GestureAction.PAGE_DOWN,               // "05"
    val gestureNavLeft: GestureAction = GestureAction.BACK,                    // "03"
    val gestureNavRight: GestureAction = GestureAction.FORWARD,                // "02"
    // Clear on exit
    val clearFlags: ClearFlags = ClearFlags(),
    // Sorting
    val tileSort: TileSort = TileSort.ORDINAL,
    val bookmarkSort: BookmarkSort = BookmarkSort.TITLE,
    // Bookmark colour filters (codes 1..11 all enabled by default)
    val enabledFilterColors: Set<Int> = DEFAULT_FILTER_COLORS,
) : GestureCodes {
    override fun upCode(area: GestureArea): String? = when (area) {
        GestureArea.TOOLBAR -> gestureToolbarUp.code
        GestureArea.NAV_BUTTON -> gestureNavUp.code
    }

    override fun downCode(area: GestureArea): String? = when (area) {
        GestureArea.TOOLBAR -> gestureToolbarDown.code
        GestureArea.NAV_BUTTON -> gestureNavDown.code
    }

    override fun leftCode(area: GestureArea): String? = when (area) {
        GestureArea.TOOLBAR -> gestureToolbarLeft.code
        GestureArea.NAV_BUTTON -> gestureNavLeft.code
    }

    override fun rightCode(area: GestureArea): String? = when (area) {
        GestureArea.TOOLBAR -> gestureToolbarRight.code
        GestureArea.NAV_BUTTON -> gestureNavRight.code
    }

    companion object {
        val DEFAULT_FILTER_COLORS: Set<Int> = (1..11).toSet()
    }
}

class SettingsRepository(app: Application) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<BBSettings> = _state

    init {
        prefs.registerOnSharedPreferenceChangeListener { _, _ ->
            scope.launch { _state.value = read() }
        }
    }

    /** Apply a transform and write only the changed fields back to legacy keys. */
    fun update(transform: (BBSettings) -> BBSettings) {
        val current = _state.value
        val next = transform(current)
        _state.value = next
        writeDiffs(current, next)
    }

    /** Reset all user preferences to factory defaults. */
    fun resetToDefaults() {
        val current = _state.value
        val defaults = BBSettings()
        _state.value = defaults
        writeDiffs(current, defaults)
    }

    // ------------------------------------------------------------------ read

    @Suppress("ApplySharedPref")
    private fun read(): BBSettings {
        val s = prefs
        return BBSettings(
            themePref = s.getString(K.THEME, null) ?: "2",
            fontSizePercent = s.getString(K.FONT_SIZE, null)?.toIntOrNull() ?: 100,
            invertWebContent = s.getBoolean(K.INVERT_WEB_CONTENT, true),
            dynamicColor = s.getBoolean(K.DYNAMIC_COLOR, false),
            hideToolbarOnScroll = s.getBoolean(K.HIDE_TOOLBAR, true),
            showToolbarOnBack = s.getBoolean(K.SHOW_TOOLBAR_ON_BACK, false),
            navPosition = NavPosition.fromPref(s.getString(K.NAV_POSITION, null)),
            overviewOnStart = s.getBoolean(K.OVERVIEW_ON_START, true),
            startTab = StartTab.fromPref(s.getString(K.START_TAB, null)),
            confirmExit = s.getBoolean(K.CONFIRM_EXIT, true),
            confirmTabClose = s.getBoolean(K.CONFIRM_TAB_CLOSE, false),
            reopenLastTab = s.getBoolean(K.REOPEN_LAST_TAB, false),
            favoriteUrl = s.getString(K.FAVORITE_URL, null) ?: "file:///android_asset/Newtab.html",
            searchEngine = SearchEngine.fromPref(s.getString(K.SEARCH_ENGINE, null)),
            customSearchPrefix = s.getString(K.CUSTOM_SEARCH_PREFIX, null) ?: "https://duckduckgo.com/search?q=",
            userAgent = s.getString(K.USER_AGENT, null) ?: "",
            desktopMode = s.getBoolean(K.DESKTOP_MODE, false),
            incognitoActive = s.getBoolean(K.INCOGNITO_ACTIVE, false),
            saveHistory = s.getBoolean(K.SAVE_HISTORY, true),
            images = s.getBoolean(K.IMAGES, true),
            saveDataHeader = s.getBoolean(K.SAVE_DATA_HEADER, true),
            locationAllowed = s.getBoolean(K.LOCATION_ALLOWED, false),
            adBlockEnabled = s.getBoolean(K.AD_BLOCK_ENABLED, true),
            javaScriptEnabled = s.getBoolean(K.JAVASCRIPT_ENABLED, true),
            cookiesEnabled = s.getBoolean(K.COOKIES_ENABLED, true),
            remoteContentEnabled = s.getBoolean(K.REMOTE_CONTENT_ENABLED, false),
            gesturesEnabled = s.getBoolean(K.GESTURES_ENABLED, true),
            gestureToolbarUp = GestureAction.fromCode(s.getString(K.GESTURE_TB_UP, null)),
            gestureToolbarDown = GestureAction.fromCode(s.getString(K.GESTURE_TB_DOWN, null)),
            gestureToolbarLeft = GestureAction.fromCode(s.getString(K.GESTURE_TB_LEFT, null)),
            gestureToolbarRight = GestureAction.fromCode(s.getString(K.GESTURE_TB_RIGHT, null)),
            gestureNavUp = GestureAction.fromCode(s.getString(K.GESTURE_NAV_UP, null)),
            gestureNavDown = GestureAction.fromCode(s.getString(K.GESTURE_NAV_DOWN, null)),
            gestureNavLeft = GestureAction.fromCode(s.getString(K.GESTURE_NAV_LEFT, null)),
            gestureNavRight = GestureAction.fromCode(s.getString(K.GESTURE_NAV_RIGHT, null)),
            clearFlags = ClearFlags(
                cache = s.getBoolean(K.CLEAR_CACHE, false),
                history = s.getBoolean(K.CLEAR_HISTORY, false),
                cookies = s.getBoolean(K.CLEAR_COOKIES, false),
                indexedDb = s.getBoolean(K.CLEAR_INDEXED_DB, false),
                onQuit = s.getBoolean(K.CLEAR_ON_QUIT, false),
            ),
            tileSort = TileSort.fromPref(s.getString(K.SORT_START_SITE, null)),
            bookmarkSort = BookmarkSort.fromPref(s.getString(K.SORT_BOOKMARK, null)),
            enabledFilterColors = (1..11).filter { s.getBoolean("filter_%02d".format(it), true) }.toSet(),
        )
    }

    // ------------------------------------------------------------------ write

    private fun writeDiffs(old: BBSettings, new: BBSettings) {
        val e = prefs.edit()
        if (old.themePref != new.themePref) e.putString(K.THEME, new.themePref)
        if (old.fontSizePercent != new.fontSizePercent) e.putString(K.FONT_SIZE, new.fontSizePercent.toString())
        if (old.invertWebContent != new.invertWebContent) e.putBoolean(K.INVERT_WEB_CONTENT, new.invertWebContent)
        if (old.dynamicColor != new.dynamicColor) e.putBoolean(K.DYNAMIC_COLOR, new.dynamicColor)
        if (old.hideToolbarOnScroll != new.hideToolbarOnScroll) e.putBoolean(K.HIDE_TOOLBAR, new.hideToolbarOnScroll)
        if (old.showToolbarOnBack != new.showToolbarOnBack) e.putBoolean(K.SHOW_TOOLBAR_ON_BACK, new.showToolbarOnBack)
        if (old.navPosition != new.navPosition) e.putString(K.NAV_POSITION, new.navPosition.prefValue)
        if (old.overviewOnStart != new.overviewOnStart) e.putBoolean(K.OVERVIEW_ON_START, new.overviewOnStart)
        if (old.startTab != new.startTab) e.putString(K.START_TAB, new.startTab.prefValue)
        if (old.confirmExit != new.confirmExit) e.putBoolean(K.CONFIRM_EXIT, new.confirmExit)
        if (old.confirmTabClose != new.confirmTabClose) e.putBoolean(K.CONFIRM_TAB_CLOSE, new.confirmTabClose)
        if (old.reopenLastTab != new.reopenLastTab) e.putBoolean(K.REOPEN_LAST_TAB, new.reopenLastTab)
        if (old.favoriteUrl != new.favoriteUrl) e.putString(K.FAVORITE_URL, new.favoriteUrl)
        if (old.searchEngine != new.searchEngine) e.putString(K.SEARCH_ENGINE, new.searchEngine.prefValue)
        if (old.customSearchPrefix != new.customSearchPrefix) e.putString(K.CUSTOM_SEARCH_PREFIX, new.customSearchPrefix)
        if (old.userAgent != new.userAgent) e.putString(K.USER_AGENT, new.userAgent)
        if (old.desktopMode != new.desktopMode) e.putBoolean(K.DESKTOP_MODE, new.desktopMode)
        if (old.incognitoActive != new.incognitoActive) e.putBoolean(K.INCOGNITO_ACTIVE, new.incognitoActive)
        if (old.saveHistory != new.saveHistory) e.putBoolean(K.SAVE_HISTORY, new.saveHistory)
        if (old.images != new.images) e.putBoolean(K.IMAGES, new.images)
        if (old.saveDataHeader != new.saveDataHeader) e.putBoolean(K.SAVE_DATA_HEADER, new.saveDataHeader)
        if (old.locationAllowed != new.locationAllowed) e.putBoolean(K.LOCATION_ALLOWED, new.locationAllowed)
        if (old.adBlockEnabled != new.adBlockEnabled) e.putBoolean(K.AD_BLOCK_ENABLED, new.adBlockEnabled)
        if (old.javaScriptEnabled != new.javaScriptEnabled) e.putBoolean(K.JAVASCRIPT_ENABLED, new.javaScriptEnabled)
        if (old.cookiesEnabled != new.cookiesEnabled) e.putBoolean(K.COOKIES_ENABLED, new.cookiesEnabled)
        if (old.remoteContentEnabled != new.remoteContentEnabled) e.putBoolean(K.REMOTE_CONTENT_ENABLED, new.remoteContentEnabled)
        if (old.gesturesEnabled != new.gesturesEnabled) e.putBoolean(K.GESTURES_ENABLED, new.gesturesEnabled)

        fun putGesture(key: String, oldA: GestureAction, newA: GestureAction) {
            if (oldA != newA) e.putString(key, newA.code)
        }
        putGesture(K.GESTURE_TB_UP, old.gestureToolbarUp, new.gestureToolbarUp)
        putGesture(K.GESTURE_TB_DOWN, old.gestureToolbarDown, new.gestureToolbarDown)
        putGesture(K.GESTURE_TB_LEFT, old.gestureToolbarLeft, new.gestureToolbarLeft)
        putGesture(K.GESTURE_TB_RIGHT, old.gestureToolbarRight, new.gestureToolbarRight)
        putGesture(K.GESTURE_NAV_UP, old.gestureNavUp, new.gestureNavUp)
        putGesture(K.GESTURE_NAV_DOWN, old.gestureNavDown, new.gestureNavDown)
        putGesture(K.GESTURE_NAV_LEFT, old.gestureNavLeft, new.gestureNavLeft)
        putGesture(K.GESTURE_NAV_RIGHT, old.gestureNavRight, new.gestureNavRight)

        if (old.clearFlags != new.clearFlags) {
            e.putBoolean(K.CLEAR_CACHE, new.clearFlags.cache)
            e.putBoolean(K.CLEAR_HISTORY, new.clearFlags.history)
            e.putBoolean(K.CLEAR_COOKIES, new.clearFlags.cookies)
            e.putBoolean(K.CLEAR_INDEXED_DB, new.clearFlags.indexedDb)
            e.putBoolean(K.CLEAR_ON_QUIT, new.clearFlags.onQuit)
        }
        if (old.tileSort != new.tileSort) e.putString(K.SORT_START_SITE, new.tileSort.prefValue)
        if (old.bookmarkSort != new.bookmarkSort) e.putString(K.SORT_BOOKMARK, new.bookmarkSort.prefValue)

        val changedColors = old.enabledFilterColors != new.enabledFilterColors
        if (changedColors) {
            (1..11).forEach { code -> e.putBoolean(filterKey(code), code in new.enabledFilterColors) }
        }
        e.apply()
    }

    /**
     * First-run defaults — idempotent port of MainActivity.onCreate's `saved_key_ok` block so
     * both entry points can call it safely.
     */
    fun ensureFirstRunDefaults() {
        if (prefs.getString(K.FIRST_RUN_OK, null) == "yes") return
        prefs.edit()
            .putString("setting_gesture_tb_up", "08")
            .putString("setting_gesture_tb_down", "01")
            .putString("setting_gesture_tb_left", "07")
            .putString("setting_gesture_tb_right", "06")
            .putString("setting_gesture_nav_up", "04")
            .putString("setting_gesture_nav_down", "05")
            .putString("setting_gesture_nav_left", "03")
            .putString("setting_gesture_nav_right", "02")
            .putBoolean(K.LOCATION_ALLOWED, false)
            .putString(K.FIRST_RUN_OK, "yes")
            .apply()
        _state.value = read()
    }

    fun isOnboardingCompleted(): Boolean = prefs.getBoolean(K.ONBOARDING_COMPLETED, false)

    fun setOnboardingCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean(K.ONBOARDING_COMPLETED, completed).apply()
    }

    fun isTutorialCompleted(): Boolean = prefs.getBoolean(K.TUTORIAL_COMPLETED, false)

    fun setTutorialCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean(K.TUTORIAL_COMPLETED, completed).apply()
    }

    companion object K {
        const val FIRST_RUN_OK = "saved_key_ok"
        const val ONBOARDING_COMPLETED = "bb_onboarding_completed"
        const val TUTORIAL_COMPLETED = "bb_tutorial_completed"

        // Literal key names — identical to arrays.xml string resources / preference XMLs.
        const val THEME = "sp_theme"                       // sp_theme
        const val FONT_SIZE = "sp_fontSize"
        const val INVERT_WEB_CONTENT = "sp_invert"         // sp_invert
        const val DYNAMIC_COLOR = "bb_dynamic_color_v2"     // NEW — no legacy equivalent
        const val HIDE_TOOLBAR = "hideToolbar"
        const val SHOW_TOOLBAR_ON_BACK = "sp_toolbarShow"
        const val NAV_POSITION = "nav_position"
        const val OVERVIEW_ON_START = "start_tabStart"
        const val START_TAB = "start_tab"
        const val CONFIRM_EXIT = "sp_close_browser_confirm"
        const val CONFIRM_TAB_CLOSE = "sp_close_tab_confirm"
        const val REOPEN_LAST_TAB = "sp_reopenLastTab"
        const val FAVORITE_URL = "favoriteURL"
        const val SEARCH_ENGINE = "SP_SEARCH_ENGINE_9"      // @string/sp_search_engine
        const val CUSTOM_SEARCH_PREFIX = "sp_search_engine_custom"
        const val USER_AGENT = "userAgent"
        const val DESKTOP_MODE = "desktop_mode"
        const val INCOGNITO_ACTIVE = "IsIncognito"
        const val SAVE_HISTORY = "saveHistory"
        const val IMAGES = "SP_IMAGES_9"                    // @string/sp_images
        const val SAVE_DATA_HEADER = "SP_SAVE_DATA_9"       // @string/sp_savedata
        const val LOCATION_ALLOWED = "SP_LOCATION_9"        // @string/sp_location
        const val AD_BLOCK_ENABLED = "SP_AD_BLOCK_9"        // @string/sp_ad_block
        const val JAVASCRIPT_ENABLED = "SP_JAVASCRIPT_9"    // @string/sp_javascript
        const val COOKIES_ENABLED = "SP_COOKIES_9"          // @string/sp_cookies
        const val REMOTE_CONTENT_ENABLED = "sp_remote"
        const val GESTURES_ENABLED = "sp_gestures_use"
        const val GESTURE_TB_UP = "setting_gesture_tb_up"
        const val GESTURE_TB_DOWN = "setting_gesture_tb_down"
        const val GESTURE_TB_LEFT = "setting_gesture_tb_left"
        const val GESTURE_TB_RIGHT = "setting_gesture_tb_right"
        const val GESTURE_NAV_UP = "setting_gesture_nav_up"
        const val GESTURE_NAV_DOWN = "setting_gesture_nav_down"
        const val GESTURE_NAV_LEFT = "setting_gesture_nav_left"
        const val GESTURE_NAV_RIGHT = "setting_gesture_nav_right"
        const val CLEAR_CACHE = "SP_CLEAR_CACHE_9"          // @string/sp_clear_cache
        const val CLEAR_HISTORY = "SP_CLEAR_HISTORY_9"      // @string/sp_clear_history
        const val CLEAR_COOKIES = "SP_CLEAR_COOKIE_9"       // @string/sp_clear_cookie
        const val CLEAR_INDEXED_DB = "sp_clearIndexedDB"
        const val CLEAR_ON_QUIT = "SP_CLEAR_QUIT_9"         // @string/sp_clear_quit
        const val SORT_START_SITE = "sort_startSite"
        const val SORT_BOOKMARK = "sort_bookmark"

        fun filterKey(code: Int): String = "filter_%02d".format(code)
    }
}
