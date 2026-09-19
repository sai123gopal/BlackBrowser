package com.saigopal.browser.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saigopal.browser.ui.di.rememberContainer
import com.saigopal.browser.ui.screens.games.GamesHubScreen
import com.saigopal.browser.ui.screens.onboarding.OnboardingScreen
import com.saigopal.browser.ui.screens.settings.FeedbackScreen
import com.saigopal.browser.ui.screens.settings.SettingsHomeScreen
import com.saigopal.browser.ui.screens.settings.SettingsRouteScreen
import com.saigopal.browser.ui.shell.BrowserScreen
import com.saigopal.browser.ui.shell.ShellActivityHolder

/*
 * AppNavHost (P05) — the whole navigation graph, deliberately tiny.
 *
 * No navigation-compose dependency: five destinations and a back-to-browser rule cover the app.
 * Browser is the root; settings keeps its own nested route stack; games/feedback are leaves;
 * Onboarding is the first-launch introduction and configuration flow.
 * "Open externally" from the games hub posts an ACTION_VIEW through ShellActivityHolder so the
 * browser shell opens it as a normal tab (single command channel).
 */

private enum class Route {
    ONBOARDING,
    BROWSER,
    SETTINGS,
    GAMES,
    FEEDBACK,
}

@Composable
fun AppNavHost() {
    val container = rememberContainer()
    val startRoute = if (!container.settings.isOnboardingCompleted()) Route.ONBOARDING else Route.BROWSER
    var route by remember { mutableStateOf(startRoute) }
    val toBrowser: () -> Unit = { route = Route.BROWSER }

    // Registered before the destinations' own handlers, so their back chains win first.
    BackHandler(enabled = route != Route.BROWSER && route != Route.ONBOARDING) {
        toBrowser()
    }

    if (route == Route.ONBOARDING) {
        OnboardingScreen(
            onFinish = { prefs ->
                container.settings.update { s ->
                    s.copy(
                        adBlockEnabled = prefs.adBlockEnabled,
                        incognitoActive = prefs.incognitoByDefault,
                        remoteContentEnabled = !prefs.highSecurityGuard,
                        clearFlags = s.clearFlags.copy(
                            history = prefs.clearOnExit,
                            cache = prefs.clearOnExit,
                            cookies = prefs.clearOnExit,
                            onQuit = prefs.clearOnExit,
                        ),
                    )
                }
                container.settings.setOnboardingCompleted(true)
                route = Route.BROWSER
            },
        )
    } else {
        // Safe area: every destination starts BELOW the system status bar instead of under it.
        // ModalBottomSheets render in their own window layer and are unaffected by this padding.
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            when (route) {
                Route.BROWSER -> BrowserScreen(
                    onOpenSettings = { route = Route.SETTINGS },
                    onOpenGames = { route = Route.GAMES },
                    onOpenFeedback = { route = Route.FEEDBACK },
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
            }
        }
    }
}

/** Settings home + nested route stack as one destination (P03's private stack reused). */
@Composable
private fun SettingsDestination(onExit: () -> Unit) {
    var stack by remember { mutableStateOf(listOf<String?>(null)) }
    val current = stack.last()

    BackHandler(enabled = current != null) {
        stack = stack.dropLast(1)
    }

    if (current == null) {
        SettingsHomeScreen(
            onExit = onExit,
            onOpenRoute = { id -> stack = stack + id },
        )
    } else {
        SettingsRouteScreen(
            routeId = current,
            onExit = { stack = stack.dropLast(1) },
        )
    }
}
