package com.saigopal.browser.domain

/*
 * ClearPlanner (P09 9.8) — the pure decision core of exit-time clearing.
 *
 * Legacy ClearService read four sp_* flags and ran side effects inline; the decision logic was
 * untestable. ClearService now consults this planner so the flag combinations are covered by JVM
 * tests; the Compose shell's exit-clear performs the same ops in the same order.
 */
object ClearPlanner {

    data class Flags(
        val cache: Boolean = false,
        val cookies: Boolean = false,
        val history: Boolean = false,
        val indexedDb: Boolean = false,
    )

    sealed interface Op {
        /** WebView disk/in-memory cache; arg mirrors WebWebView.clearCache(includeDiskFiles). */
        data object ClearCache : Op

        data object ClearCookies : Op
        data object ClearHistory : Op
        data object ClearIndexedDb : Op
    }

    /** Deterministic order: cheap volatile stores first, history last (it hits the DB). */
    fun plan(flags: Flags): List<Op> = buildList {
        if (flags.cache) add(Op.ClearCache)
        if (flags.cookies) add(Op.ClearCookies)
        if (flags.indexedDb) add(Op.ClearIndexedDb)
        if (flags.history) add(Op.ClearHistory)
    }
}
