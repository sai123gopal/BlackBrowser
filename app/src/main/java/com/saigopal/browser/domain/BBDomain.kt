package com.saigopal.browser.domain

/*
 * Domain models shared by repositories, view models and (later) Compose screens.
 * Pure Kotlin — no Android imports — so everything here is unit-testable on the JVM.
 */

/** One row of HISTORY / BOOKMARK / GRID. [colorCode] carries the legacy 1..11 bookmark colour when set. */
data class BBRecord(
    val title: String,
    val url: String,
    val time: Long,
    val colorCode: Int = 0,
)

/** Metadata for one open tab. WebView instances are NOT owned here until P05 flips ownership. */
data class TabState(
    val id: Long,
    val title: String,
    val url: String?,
    val position: Int,
    val isForeground: Boolean = false,
    val isIncognito: Boolean = false,
    val isDesktopMode: Boolean = false,
    val progress: Int = 100,
)

// ---------------------------------------------------------------- search engines
// Values mirror the legacy `SP_SEARCH_ENGINE_9` strings exactly.

enum class SearchEngine(val prefValue: String) {
    DUCKDUCKGO("0"),
    GOOGLE("1"),
    BING("2"),
    BAIDU("3"),
    SEARX("4"),
    QWANT("5"),
    CUSTOM("6");

    companion object {
        fun fromPref(value: String?): SearchEngine =
            entries.firstOrNull { it.prefValue == value } ?: DUCKDUCKGO
    }
}

enum class NavPosition(val prefValue: String) {
    RIGHT("0"), LEFT("1"), CENTER("2"), HIDDEN("3");

    companion object {
        fun fromPref(value: String?): NavPosition =
            entries.firstOrNull { it.prefValue == value } ?: RIGHT
    }
}

/** Legacy `start_tab` values: 0 home · 3 bookmarks · 4 history · 5 tabs. */
enum class StartTab(val prefValue: String) {
    HOME("0"), BOOKMARKS("3"), HISTORY("4"), TABS("5");

    companion object {
        fun fromPref(value: String?): StartTab =
            entries.firstOrNull { it.prefValue == value } ?: HOME
    }
}

// ---------------------------------------------------------------- gestures
// Codes are stored preferences ("01".."10") — never renumber.

enum class GestureAction(val code: String) {
    NONE("01"),
    FORWARD("02"),
    BACK("03"),
    PAGE_UP("04"),
    PAGE_DOWN("05"),
    NEXT_TAB("06"),
    PREVIOUS_TAB("07"),
    OPEN_START_OVERVIEW("08"),
    NEW_TAB("09"),
    CLOSE_TAB("10");

    companion object {
        fun fromCode(code: String?): GestureAction =
            entries.firstOrNull { it.code == code } ?: NONE
    }
}

enum class GestureDirection {
    UP, DOWN, LEFT, RIGHT;

    companion object {
        fun fromName(raw: String?): GestureDirection =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UP
    }
}

enum class GestureArea(val keyPrefix: String) {
    TOOLBAR("setting_gesture_tb_"),
    NAV_BUTTON("setting_gesture_nav_");
}

enum class TileSort(val prefValue: String) {
    ORDINAL("ordinal"), TITLE("title"), TIME("TIME");

    companion object {
        fun fromPref(value: String?): TileSort =
            entries.firstOrNull { it.prefValue == value } ?: ORDINAL
    }
}

enum class BookmarkSort(val prefValue: String) {
    TITLE("title"), TIME("time");

    companion object {
        fun fromPref(value: String?): BookmarkSort =
            entries.firstOrNull { it.prefValue == value } ?: TITLE
    }
}

/** Clear-on-exit switches (legacy keys in SettingsRepository). */
data class ClearFlags(
    val cache: Boolean = false,
    val history: Boolean = false,
    val cookies: Boolean = false,
    val indexedDb: Boolean = false,
    val onQuit: Boolean = false,
)
