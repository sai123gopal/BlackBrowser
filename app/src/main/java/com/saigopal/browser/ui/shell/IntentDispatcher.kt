package com.saigopal.browser.ui.shell

import android.content.Intent
import android.app.SearchManager
import com.saigopal.browser.ui.screens.overview.OverviewSegment

/*
 * IntentDispatcher (P05 §5.7) — Kotlin port of MainActivity.dispatchIntent's decision table.
 *
 * Pure classification: returns a command; the shell executes it against TabManager/pool and
 * consumes the intent (legacy `getIntent().setAction("")` → [consume]). Regression matrix from
 * the plan: Gmail link · share-into-app · Quick Findbox WEB_SEARCH · sc_* shortcuts · cold start
 * · second VIEW while running (singleTask onNewIntent).
 */

sealed interface IntentCommand {
    /** Nothing to do — app resumed with an empty action (warm start). */
    data object ResumeOnly : IntentCommand

    /** Open [url] in a new foreground tab; null url opens the start page. */
    data class OpenUrl(val url: String?) : IntentCommand

    /** ACTION_WEB_SEARCH — run [query] through the interpreter like typed omnibox input. */
    data class WebSearch(val query: String?) : IntentCommand

    /** Launcher shortcuts: open a start tab, then the overview pinned at [segment]. */
    data class ShortcutOverview(val segment: OverviewSegment) : IntentCommand

    /** No recognized action and no tabs yet — legacy cold-start branch (favoriteURL + overview). */
    data object ColdStart : IntentCommand
}

object IntentDispatcher {

    fun dispatch(intent: Intent?): IntentCommand {
        if (intent == null) return IntentCommand.ResumeOnly
        val action = intent.action
        return when {
            action == null || action.isEmpty() -> IntentCommand.ResumeOnly
            action == Intent.ACTION_WEB_SEARCH ->
                IntentCommand.WebSearch(intent.getStringExtra(SearchManager.QUERY))
            action == "sc_history" -> IntentCommand.ShortcutOverview(OverviewSegment.HISTORY)
            action == "sc_bookmark" -> IntentCommand.ShortcutOverview(OverviewSegment.SAVED)
            action == "sc_startPage" -> IntentCommand.ShortcutOverview(OverviewSegment.HOME)
            action == Intent.ACTION_SEND ->
                IntentCommand.OpenUrl(intent.getStringExtra(Intent.EXTRA_TEXT))
            action == Intent.ACTION_VIEW ->
                IntentCommand.OpenUrl(intent.data?.toString())
            else -> IntentCommand.ColdStart
        }
    }

    /** Legacy parity: consume so onResume doesn't re-dispatch the same intent forever. */
    fun consume(intent: Intent?) {
        intent?.setData(null)
        // setAction("") is what legacy used; an empty action short-circuits dispatch above.
        intent?.action = ""
    }
}
