package com.saigopal.browser.domain

/*
 * GestureRouter — Kotlin port of MainActivity.performGesture's switch table.
 * Codes are the stored preference values; the host interface is what a future
 * BrowserViewModel (P05) implements. Pure JVM, unit-tested in GestureRouterTest.
 */

fun interface GestureHandler {
    fun perform(action: GestureAction)
}

object GestureRouter {

    /** Resolve which action a stored preference currently maps to. */
    fun actionFor(settings: GestureCodes, area: GestureArea, direction: GestureDirection): GestureAction =
        when (direction) {
            GestureDirection.UP -> settings.upCode(area)
            GestureDirection.DOWN -> settings.downCode(area)
            GestureDirection.LEFT -> settings.leftCode(area)
            GestureDirection.RIGHT -> settings.rightCode(area)
        }.let { GestureAction.fromCode(it) }

    /**
     * Dispatch a swipe. Returns false when the resolved action is NONE so callers can
     * fall back to normal click handling — matching legacy behaviour where "01" did nothing.
     */
    fun dispatch(handler: GestureHandler, action: GestureAction): Boolean {
        if (action == GestureAction.NONE) return false
        handler.perform(action)
        return true
    }
}

/** Read-side view of the eight gesture preferences; implemented by SettingsRepository state. */
interface GestureCodes {
    fun upCode(area: GestureArea): String?
    fun downCode(area: GestureArea): String?
    fun leftCode(area: GestureArea): String?
    fun rightCode(area: GestureArea): String?
}
