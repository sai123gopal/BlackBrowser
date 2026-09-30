package com.saigopal.browser.ui.shell

import android.app.ActivityManager
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
 *
 * PERF (low-end memory package): every pooled instance is a full Chromium renderer process.
 * Uncapped, 10+ background tabs can push a 2-3GB device into whole-process kill — losing ALL
 * tabs and session state. The pool now evicts the least-recently-used BACKGROUND instance
 * beyond a device-class cap. Eviction is behavior-safe because activateTab() already supports
 * rehydrating an evicted tab (missing/blank engine ⇒ loads the tab's URL again); the only loss
 * is background scroll/form state on the evicted tab, the same trade the OS makes when it
 * kills the process — except here the active tab is never the victim.
 */

class WebViewPool(
    private val context: Context,
    private val controller: com.saigopal.browser.browser.BrowserController,
) {

    private val webViews = LinkedHashMap<Long, SWebView>()

    private var activeTabId: Long? = null

    /** Device-class cap computed once; 0 disables the cap (high-RAM devices). */
    private val poolCap: Int = computePoolCap(context)

    fun get(tabId: Long?): SWebView? = tabId?.let { webViews[it] }

    fun getOrCreate(tabId: Long): SWebView = webViews.getOrPut(tabId) {
        SWebView(context).apply { setBrowserController(controller) }
    }.also {
        // Re-insert at the MRU end: a just-reused (rehydrated) tab must not be the next victim.
        if (poolCap > 0) {
            webViews.remove(tabId)
            webViews[tabId] = it
            evictIfNeeded(protectedId = tabId)
        }
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
        if (poolCap > 0) {
            // True LRU: re-insert at the MRU end so eviction scans genuinely oldest-used first
            // (plain insertion order would evict early tabs the user revisits often).
            webViews.remove(tabId)?.let { webViews[tabId] = it }
            evictIfNeeded(protectedId = tabId)
        }
    }

    fun trimMemory(level: Int) {
        webViews.forEach { (id, view) ->
            if (id != activeTabId) {
                view.trimMemory(level)
            }
        }
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

    // ---------------------------------------------------------------- LRU eviction

    /**
     * Evicts the least-recently-used BACKGROUND WebViews while over the cap. The active tab is
     * never a victim; neither is the tab currently being (re)created ([protectedId]).
     * Destroyed-tab metadata stays in TabManager (title/url/position untouched) — closing the
     * shell's expectation that a pooled instance may not exist is what activateTab() rehydrates.
     */
    private fun evictIfNeeded(protectedId: Long?) {
        if (poolCap <= 0) return
        val iterator = webViews.entries.iterator()
        while (webViews.size > poolCap && iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key == activeTabId || entry.key == protectedId) continue
            entry.value.destroy()
            iterator.remove()
        }
    }

    private companion object {
        /**
         * Cap sizing: memory-class driven with a low-RAM floor. Modern 8GB+ flagships report
         * memoryClass ≥ 256 → no cap (original uncapped behavior preserved exactly). Mid devices
         * 128-255 → 6 engines. lowRamDevice (Android Go / 2GB class) → 3 engines.
         */
        fun computePoolCap(context: Context): Int {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return 0
            val memoryClass = am.memoryClass
            return when {
                am.isLowRamDevice -> 3
                memoryClass >= 256 -> 0 // uncapped — identical to pre-optimization behavior
                memoryClass >= 128 -> 6
                else -> 3
            }
        }
    }
}
