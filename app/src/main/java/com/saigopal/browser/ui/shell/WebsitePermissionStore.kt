package com.saigopal.browser.ui.shell

/*
 * WebsitePermissionStore (P09 9.1) — session-scoped memory of per-origin web-permission grants.
 *
 * Deliberately NOT persisted: every app start starts from a clean slate, so a site must re-ask
 * after the process dies. "Reset website permissions" in Settings → Privacy wipes this map and
 * clears the WebView-side geolocation state.
 */
object WebsitePermissionStore {

    private val geoGrants = LinkedHashMap<String, Boolean>()

    /** True when this origin was already answered this session (grant or deny). */
    fun hasDecision(origin: String): Boolean = synchronized(geoGrants) { geoGrants.containsKey(origin) }

    fun decisionFor(origin: String): Boolean = synchronized(geoGrants) { geoGrants[origin] ?: false }

    fun remember(origin: String, granted: Boolean) {
        synchronized(geoGrants) { geoGrants[origin] = granted }
    }

    /** Settings → Privacy "Reset website permissions": forget every per-origin decision. */
    fun reset() {
        synchronized(geoGrants) { geoGrants.clear() }
    }
}
