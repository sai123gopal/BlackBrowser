package com.saigopal.browser.ui.screens.settings

/*
 * Route identifiers for the Compose settings graph. The string ids are stable contract values:
 * an external nav host (P05 shell) routes by id, so they must never be renamed casually.
 * The four whitelist ids intentionally mirror the legacy table kinds they manage.
 */
enum class SettingsRoute(val id: String) {
    APPEARANCE("appearance"),
    PRIVACY("privacy"),
    GESTURES("gestures"),
    DATA("data"),
    CLEAR("clear"),
    FILTER("filter"),
    WHITELIST_ADBLOCK("whitelist_adblock"),
    WHITELIST_JAVASCRIPT("whitelist_javascript"),
    WHITELIST_COOKIE("whitelist_cookie"),
    WHITELIST_REMOTE("whitelist_remote"),
    FEEDBACK("feedback"),
    ABOUT("about");

    companion object {
        fun fromId(id: String?): SettingsRoute? = entries.firstOrNull { it.id == id }
    }
}

/** One searchable row of the settings graph. [route] null = inline toggle handled by the caller. */
data class SettingsEntry(
    val route: SettingsRoute?,
    val title: String,
    val keywords: String,
)

object SettingsIndex {

    /**
     * Pure filter for the root settings search: case-insensitive match against title and keyword
     * synonyms; blank query matches everything so callers can render the plain section list.
     */
    fun filter(entries: List<SettingsEntry>, query: String): List<SettingsEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return entries
        return entries.filter { entry ->
            entry.title.lowercase().contains(q) ||
                entry.keywords.lowercase().split("\\s+".toRegex()).any { it.startsWith(q) }
        }
    }
}
