package com.saigopal.browser.ui.shell

import androidx.appcompat.app.AppCompatActivity
import com.saigopal.browser.AppContainer
import com.saigopal.browser.browser.BrowserWebProfiles

/*
 * BrowserShellStore — keeps the browser's WebView pool alive across in-app route switches.
 *
 * Bug fix: AppNavHost swaps composables with a plain `when`, so opening Settings/Games/Feedback
 * left BrowserScreen's composition and its DisposableEffect destroyed every pooled WebView;
 * coming back recreated blank engines (manual reload needed) and the games hub re-downloaded
 * Gamezop on every visit. The pool now lives HERE, keyed to the Activity instance:
 *
 *   - same Activity, any number of route switches → one controller + one pool, zero reloads;
 *   - new Activity instance (rotation/theme change) → stale WebViews are bound to a dead
 *     window context, so they are destroyed and rebuilt (same safety as the old design);
 *   - real exit → MainActivity.onDestroy / ShellController.applyQuit call release().
 */
object BrowserShellStore {

    private class Holder(val activity: AppCompatActivity, val controller: ShellController)

    @Volatile
    private var holder: Holder? = null

    /** Returns the live controller for [activity], creating (or rebuilding) it when needed. */
    fun obtain(activity: AppCompatActivity, container: AppContainer): ShellController {
        BrowserWebProfiles.clearStaleProfileOnce()
        holder?.let { existing ->
            if (existing.activity === activity) return existing.controller
            // Configuration change: WebViews bound to the old window must not survive.
            existing.controller.pool.destroyAll()
        }
        return ShellController(activity, container).also { controller ->
            controller.pool = WebViewPool(activity, controller)
            holder = Holder(activity, controller)
        }
    }

    /** Tears down the pool if it belongs to [activity]; no-op for a newer instance. */
    fun release(activity: AppCompatActivity) {
        val current = holder ?: return
        if (current.activity === activity) {
            current.controller.pool.destroyAll()
            holder = null
        }
    }
}
