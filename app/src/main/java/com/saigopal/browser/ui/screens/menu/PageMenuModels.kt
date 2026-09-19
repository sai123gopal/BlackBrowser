package com.saigopal.browser.ui.screens.menu

/*
 * Contracts for the P06 menu/dialog kit. Pure data + lambdas only — the shell (P05) owns
 * every side effect; these composables never touch WebViews or repositories directly
 * (except the explicitly-noted incognito/whitelist writes).
 */

/** Everything the page menu header needs to know about the current tab. */
data class PageInfo(
    val title: String,
    val url: String?,
    val isSecure: Boolean,
    val blockedCount: Int,
    val isIncognito: Boolean,
    /** P09 (9.3): per-tab desktop-site state; drives the toggle row when onToggleDesktop != null. */
    val isDesktop: Boolean = false,
    val isStartPage: Boolean = false,
    val isShieldActive: Boolean = true,
)

/**
 * One callback per item of the legacy overflow menu, in spec §6.1 order:
 * New tab · Incognito · Find · Favorite · Bookmark · Home · Shortcut · Share · Copy ·
 * Open-with · Save-as · Print · Downloads · Settings · Quit.
 */
class PageMenuActions(
    val onNewTab: () -> Unit,
    val onFindInPage: () -> Unit,
    val onSetFavorite: () -> Unit,
    val onResetFavorite: () -> Unit = {},
    val onAddBookmark: () -> Unit,
    val onAddToHome: () -> Unit = {},
    val onCreateShortcut: () -> Unit,
    val onShare: () -> Unit,
    val onCopyLink: () -> Unit,
    val onOpenWith: () -> Unit,
    val onSaveAs: () -> Unit = {},
    val onPrintPdf: () -> Unit,
    val onOpenDownloads: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onQuit: () -> Unit,
    /** P09 (9.3): per-tab desktop toggle. Null hides the row (e.g. start-page tabs). */
    val onToggleDesktop: (() -> Unit)? = null,
    val onToggleShield: (() -> Unit)? = null,
    val onOpenWhitelist: (() -> Unit)? = null,
)

/** Port of show_contextMenu_link targets. */
class LinkContextActions(
    val onNewTabForeground: () -> Unit,
    val onNewTabBackground: () -> Unit,
    val onShare: () -> Unit,
    val onOpenWith: () -> Unit,
    val onSaveAs: () -> Unit,
    val onAddToHome: () -> Unit,
    val onCopyLink: () -> Unit,
)

/** Shared row menu for bookmarks/history/home records (§6.3). */
class RecordContextActions(
    val onNewTabForeground: () -> Unit,
    val onNewTabBackground: () -> Unit,
    val onDelete: (() -> Unit)? = null,
    /** Bookmarks only: opens the title + colour editor. Null hides the entry. */
    val onEdit: (() -> Unit)? = null,
)
