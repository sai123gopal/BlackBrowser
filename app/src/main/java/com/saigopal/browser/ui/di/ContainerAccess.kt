package com.saigopal.browser.ui.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.saigopal.browser.AppContainer
import com.saigopal.browser.BrowserApplication

/*
 * Manual-DI access from composables. The app is single-module with one Application-scoped
 * container (see BrowserApplication); screens reach it through this helper so no constructor
 * plumbing has to cross the legacy/Compose boundary during the strangler migration.
 */

@Composable
fun rememberContainer(): AppContainer {
    val context = LocalContext.current
    val app = context.applicationContext as BrowserApplication
    return app.container
}
