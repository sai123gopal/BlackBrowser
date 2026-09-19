package com.saigopal.browser.ui.shell

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saigopal.browser.BrowserApplication
import com.saigopal.browser.ui.AppNavHost
import com.saigopal.browser.ui.design.theme.BBTheme
import com.saigopal.browser.ui.design.theme.BBThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/*
 * ComposeShellHost (P05 §5.9) — installs the Compose shell into MainActivity.
 *
 * MainActivity branches on BuildConfig.LEGACY_SHELL in onCreate: false → this installer runs and
 * the legacy view path is skipped entirely; true → the old layout path restores (documented
 * rollback, one release cycle). ShellActivityHolder is the only Java↔Kotlin bridge: launch and
 * onNewIntent intents land here for BrowserScreen to dispatch, and the hardware menu key routes
 * to whichever page-menu sheet is composed.
 */

object ShellActivityHolder {

    private val _intents = MutableStateFlow<Intent?>(null)
    val intents: StateFlow<Intent?> = _intents

    /** Set by BrowserScreen; invoked by MainActivity.onKeyDown for KEYCODE_MENU. */
    @Volatile
    var menuOpener: Runnable? = null

    /** Set by BrowserScreen; invoked by MainActivity.onTrimMemory. */
    @Volatile
    var memoryTrimmer: ((Int) -> Unit)? = null

    fun post(intent: Intent?) {
        _intents.value = intent
    }
}

object ComposeShellHost {

    /**
     * Call once from MainActivity.onCreate. File-chooser plumbing stays on
     * MainActivity.onActivityResult via ShellController.showFileChooser delegation.
     */
    fun install(activity: ComponentActivity) {
        // Post the launch intent so BrowserScreen's collector dispatches it once composed.
        ShellActivityHolder.post(activity.intent)

        val container = (activity.applicationContext as BrowserApplication).container

        activity.setContent {
            val settings by container.settings.state.collectAsState()

            BBTheme(
                mode = BBThemeMode.fromPref(settings.themePref),
                dynamicColor = settings.dynamicColor,
                reducedMotion = false,
            ) {
                AppNavHost()
            }
        }
    }
}
