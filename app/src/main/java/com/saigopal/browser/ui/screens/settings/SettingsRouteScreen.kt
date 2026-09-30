package com.saigopal.browser.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saigopal.browser.data.repo.WhitelistKind
import com.saigopal.browser.ui.ads.AdsController
import com.saigopal.browser.ui.screens.settings.LocalSettingsScaffoldScroll

/**
 * Renders one settings route by id, with a private back-stack for nested links
 * (Privacy → Whitelist) so the P05 shell can host settings as a single destination
 * without a navigation library. Unknown ids degrade to About rather than crashing.
 *
 * P08 8.3: SETTINGS banner placement — footer under the section content (parity with the legacy
 * settings screen's single banner).
 *
 * FIX: one ScrollState per route id, hoisted ABOVE the `when` so each sub-screen's scroll
 * survives push (leaving composition) and pop (re-entering composition). Without this every
 * scroll-to-bottom → open route → back reset the screen to the top.
 */
@Composable
fun SettingsRouteScreen(
    routeId: String,
    onExit: () -> Unit,
) {
    var stack by remember { mutableStateOf(listOf(routeId)) }
    val current = stack.last()

    val push: (String) -> Unit = { stack = stack + it }
    val pop: () -> Unit = {
        if (stack.size > 1) stack = stack.dropLast(1) else onExit()
    }

    BackHandler(enabled = true) {
        pop()
    }

    val scrollStates = remember { mutableMapOf<String, ScrollState>() }
    val currentScroll = scrollStates.getOrPut(current) { ScrollState(0) }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            CompositionLocalProvider(LocalSettingsScaffoldScroll provides currentScroll) {
                when (SettingsRoute.fromId(current)) {
                SettingsRoute.PRIVACY -> PrivacySection(onBack = pop, onOpenRoute = push)
                SettingsRoute.GESTURES -> GesturesSection(onBack = pop)
                SettingsRoute.DATA -> DataSection(onBack = pop)
                SettingsRoute.CLEAR -> ClearSection(onBack = pop)
                SettingsRoute.FILTER -> FilterSection(onBack = pop)
                SettingsRoute.WHITELIST_ADBLOCK ->
                    WhitelistScreen(WhitelistKind.AD_BLOCK, onBack = pop)
                SettingsRoute.WHITELIST_JAVASCRIPT ->
                    WhitelistScreen(WhitelistKind.JAVASCRIPT, onBack = pop)
                SettingsRoute.WHITELIST_COOKIE ->
                    WhitelistScreen(WhitelistKind.COOKIE, onBack = pop)
                SettingsRoute.WHITELIST_REMOTE ->
                    WhitelistScreen(WhitelistKind.REMOTE_CONTENT, onBack = pop)
                SettingsRoute.FEEDBACK -> FeedbackScreen(onBack = pop)
                // APPEARANCE lives inline on the settings home; landing here shows About.
                SettingsRoute.APPEARANCE, SettingsRoute.ABOUT, null -> AboutSheet(onBack = pop)
                }
            }
        }
        AdsController.Banner(
            placement = AdsController.Placement.SETTINGS,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
