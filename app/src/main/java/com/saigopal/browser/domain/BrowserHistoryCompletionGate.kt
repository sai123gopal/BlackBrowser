package com.saigopal.browser.domain

/** Ensures multiple WebView completion callbacks persist history only once per navigation. */
class BrowserHistoryCompletionGate {
    private var completed = false

    @Synchronized
    fun beginNavigation() {
        completed = false
    }

    @Synchronized
    fun completeOnce(): Boolean {
        if (completed) return false
        completed = true
        return true
    }
}