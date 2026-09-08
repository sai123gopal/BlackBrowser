package com.saigopal.browser.ui.shell

import android.content.Context
import com.saigopal.browser.browser.AlbumController
import com.saigopal.browser.view.SWebView

/*
 * WebViewPool (P05) — owns one SWebView per open tab, keyed by TabManager id.
 *
 * Interop decision record (plan §5.1): ONE persistent AndroidView slot re-parents the ACTIVE
 * instance; switching tabs moves the existing view between parents and never recreates it, so
 * scroll position, video and form state survive tab switches. Instances are created lazily on
 * first need (start-page tabs have none until they navigate away from about:startpage).
 */

class WebViewPool(
    private val context: Context,
    private val controller: com.saigopal.browser.browser.BrowserController,
) {

    private val webViews = LinkedHashMap<Long, SWebView>()

    private var activeTabId: Long? = null

    fun get(tabId: Long?): SWebView? = tabId?.let { webViews[it] }

    fun getOrCreate(tabId: Long): SWebView = webViews.getOrPut(tabId) {
        SWebView(context).apply { setBrowserController(controller) }
    }

    /** Reverse lookup for showAlbum/removeAlbum callbacks that arrive as AlbumController. */
    fun idOf(albumController: AlbumController): Long? =
        webViews.entries.firstOrNull { it.value === albumController }?.key

    /**
     * Marks [tabId] foreground: activates its webview (focus + progress reporting on) and
     * deactivates every other instance (their update() calls stop reaching the controller).
     */
    fun activate(tabId: Long) {
        if (activeTabId == tabId && webViews[tabId]?.isForeground == true) return
        webViews.forEach { (id, view) ->
            if (id == tabId) view.activate() else view.deactivate()
        }
        activeTabId = tabId
    }

    fun destroy(tabId: Long) {
        webViews.remove(tabId)?.destroy()
        if (activeTabId == tabId) activeTabId = null
    }

    fun destroyAll() {
        webViews.values.forEach { it.destroy() }
        webViews.clear()
        activeTabId = null
    }
}
