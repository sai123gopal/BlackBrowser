package com.saigopal.browser.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saigopal.browser.ui.anim.BBTransitions
import com.saigopal.browser.ui.screens.games.GamesHubScreen
import com.saigopal.browser.ui.screens.onboarding.OnboardingScreen
import com.saigopal.browser.ui.screens.settings.FeedbackScreen
import com.saigopal.browser.ui.screens.settings.LocalSettingsScaffoldScroll
import com.saigopal.browser.ui.screens.settings.SettingsHomeScreen
import com.saigopal.browser.ui.screens.settings.SettingsRouteScreen
import com.saigopal.browser.ui.shell.BrowserScreen
import com.saigopal.browser.ui.shell.ShellActivityHolder
import com.saigopal.browser.ui.di.rememberContainer

/*
 * AppNavHost (P05) — the whole navigation graph.
 *
 * No navigation-compose dependency: five destinations and a back-to-browser rule cover the app.
 * Browser is the root; settings keeps its plain-route stack; games/feedback are leaves;
 * Onboarding is the first-launch introduction and configuration flow.
 *
 * MOTION (BBTransitions): browser ↔ settings/games/feedback is a modal vertical rise + fade;
 * settings home ↔ inner screens use direction-aware horizontal push/pop, so "open" and "back"
 * read differently instead of a hard cut.
 */

private enum class Route {
    SPLASH,
    ONBOARDING,
    BROWSER,
    SETTINGS,
    GAMES,
    FEEDBACK,
}

@Composable
fun AppNavHost() {
    val container = rememberContainer()
    // SPLASH renders first (dark brand moment); the real launch route resolves beneath it.
    val startRoute = if (!container.settings.isOnboardingCompleted()) Route.ONBOARDING else Route.BROWSER
    var route by remember { mutableStateOf(Route.SPLASH) }

    val toBrowser: () -> Unit = { route = Route.BROWSER }
    val openSettings: () -> Unit = { route = Route.SETTINGS }
    val openGames: () -> Unit = { route = Route.GAMES }
    val openFeedback: () -> Unit = { route = Route.FEEDBACK }

    // Registered before the destinations' own handlers, so their back chains win first.
    BackHandler(enabled = route != Route.BROWSER && route != Route.ONBOARDING && route != Route.SPLASH) {
        toBrowser()
    }

    // Safe area: every destination starts BELOW the system status bar instead of under it.
    // ModalBottomSheets render in their own window layer and are unaffected by this padding.
    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                if (initialState == Route.SPLASH) {
                    // Splash → first screen: a quiet cross-fade, no panel motion.
                    fadeIn(androidx.compose.animation.core.tween(260)) togetherWith
                        fadeOut(androidx.compose.animation.core.tween(260))
                } else if (targetState == Route.BROWSER) {
                    // Returning home: the panel sinks back down toward the browser.
                    BBTransitions.enterModal() togetherWith BBTransitions.exitModalToBrowser()
                } else {
                    // Opening a panel: it rises into place.
                    BBTransitions.enterModal() togetherWith BBTransitions.exitModalForward()
                }
            },
            label = "rootRoutes",
        ) { current ->
            when (current) {
                Route.BROWSER -> BrowserScreen(
                    onOpenSettings = openSettings,
                    onOpenGames = openGames,
                    onOpenFeedback = openFeedback,
                )

                Route.SETTINGS -> SettingsDestination(onExit = toBrowser)

                Route.GAMES -> GamesHubScreen(
                    onBack = toBrowser,
                    onOpenExternally = { url ->
                        toBrowser()
                        ShellActivityHolder.post(
                            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                )

                Route.FEEDBACK -> FeedbackScreen(onBack = toBrowser)

                Route.ONBOARDING -> Unit

                Route.SPLASH ->
                    com.saigopal.browser.ui.screens.splash.SplashScreen(onDone = { route = startRoute })
            }
        }
    }
}

/**
 * Settings home + nested route stack as one destination (P03's private stack reused).
 *
 * FIX: the home screen's scroll position was lost on home → route → back, because the
 * rememberScrollState() inside SettingsHomeScreen died when the route replaced it in
 * composition. The home ScrollState is hoisted HERE so it survives the round trip; route
 * screens restore through the per-route hoisted states inside SettingsRouteScreen.
 *
 * MOTION: AnimatedContent keyed on the stack (not the current id) so both screens stay
 * composed during the transition and the direction is derived from stack depth.
 */
@Composable
private fun SettingsDestination(onExit: () -> Unit) {
    var stack by remember { mutableStateOf(listOf<String?>(null)) }
    val homeScroll = remember { ScrollState(0) }

    BackHandler(enabled = stack.last() != null) {
        stack = stack.dropLast(1)
    }

    AnimatedContent(
        targetState = stack,
        transitionSpec = {
            val forward = targetState.size > initialState.size
            if (forward) {
                BBTransitions.enterStackForward() togetherWith BBTransitions.exitStackForward()
            } else {
                BBTransitions.enterStackBackward() togetherWith BBTransitions.exitStackBackward()
            }
        },
        label = "settingsStack",
    ) { currentStack ->
        when (val current = currentStack.last()) {
            null -> CompositionLocalProvider(LocalSettingsScaffoldScroll provides homeScroll) {
                SettingsHomeScreen(
                    onExit = onExit,
                    onOpenRoute = { id -> stack = stack + id },
                )
            }

            else -> SettingsRouteScreen(
                routeId = current,
                onExit = { stack = stack.dropLast(1) },
            )
        }
    }
}
