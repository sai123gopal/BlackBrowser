package com.saigopal.browser.ui.screens.games

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.firebase.analytics.FirebaseAnalytics
import com.saigopal.browser.R
import com.saigopal.browser.ui.design.theme.BBSpace
import com.saigopal.browser.view.SWebView

/*
 * Gamezop games hub (P07) — the legacy WebViewActivity flow collapsed into one screen.
 * Hosts its own SWebView (fully self-initializing) instead of a tab, so game sessions never
 * pollute browser history or the tab strip.
 *
 * TODO(P05/P09): per-tab ad-block exemption declared via TabState — plug-in point.
 * The hub intentionally keeps SWebView's default engine settings; do NOT mutate global
 * ad-block/JS preferences from here (that would leak into normal browsing).
 */

/** Legacy Gamezop portal entry point. */
const val GAMES_URL = "https://10788.play.gamezop.com/"

/**
 * Keeps ONE hub WebView per Activity instance so re-entering the hub doesn't re-download the
 * portal every time (the "games page reloads again and again" bug). A new Activity instance
 * (configuration change) drops the stale engine, since it is bound to the dead window context.
 * Released from MainActivity.onDestroy via [GamesHubStore.release].
 */
object GamesHubStore {
    // Keyed by the owning Activity instance at runtime (LocalContext.current inside setContent
    // IS the Activity), so a configuration change drops engines bound to the dead window.
    private var holder: Pair<android.content.Context, SWebView>? = null

    fun obtain(context: android.content.Context): SWebView {
        holder?.let { (owned, view) -> if (owned === context) return view }
        holder?.second?.destroy()
        return SWebView(context).apply { loadUrl(GAMES_URL) }.also { holder = context to it }
    }

    fun release(context: android.content.Context) {
        val current = holder ?: return
        if (current.first === context) {
            current.second.destroy()
            holder = null
        }
    }
}

/** Analytics parity with the legacy portal activity, namespaced under games_*. */
object GamesAnalytics {
    fun logOpen(context: Context) {
        FirebaseAnalytics.getInstance(context).logEvent("games_open", null)
    }

    fun logSessionStart(context: Context) {
        FirebaseAnalytics.getInstance(context).logEvent("games_session_start", Bundle().apply {
            putString("hub", "gamezop")
        })
    }

    fun logSessionEnd(context: Context) {
        FirebaseAnalytics.getInstance(context).logEvent("games_session_end", Bundle().apply {
            putString("hub", "gamezop")
        })
    }
}

/**
 * @param onBack leaves the hub; shell decides where to return to.
 * @param onOpenExternally opens a URL outside the hub (system browser / normal tab).
 */
@Composable
fun GamesHubScreen(
    onBack: () -> Unit,
    onOpenExternally: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // One WebView per Activity instance, reused across hub visits (never destroyed on dispose —
    // that was the repeated-reload bug); lifecycle pause/resume still applies below.
    val webView = remember { GamesHubStore.obtain(context) }

    LaunchedEffect(Unit) {
        GamesAnalytics.logOpen(context)
        GamesAnalytics.logSessionStart(context)
    }

    // In-page back first, then leave the hub — same rule as the legacy WebView activity.
    BackHandler {
        if (webView.canGoBack()) webView.goBack() else onBack()
    }

    DisposableEffect(lifecycleOwner, webView) {
        // LifecycleRegistry replays up to the current state on addObserver, so this also
        // delivers ON_RESUME for an already-resumed host.
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                Lifecycle.Event.ON_PAUSE -> webView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            GamesAnalytics.logSessionEnd(context)
            // The engine itself survives in GamesHubStore; only the lifecycle hook detaches.
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BBSpace.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.abc_action_bar_up_description),
                )
            }
            Text(
                text = stringResource(R.string.bb_start_games_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onOpenExternally(GAMES_URL) }) {
                Icon(
                    Icons.Filled.ExitToApp,
                    contentDescription = stringResource(R.string.menu_open_with),
                )
            }
        }
        AndroidView(
            factory = { webView },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        Spacer(Modifier.padding(bottom = BBSpace.xxs))
    }
}
