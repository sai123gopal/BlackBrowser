package com.saigopal.browser

import android.app.Application
import com.saigopal.browser.data.repo.RecordsRepository
import com.saigopal.browser.data.repo.SettingsRepository
import com.saigopal.browser.data.repo.WhitelistRepository
import com.saigopal.browser.domain.TabManager
import com.saigopal.browser.domain.UrlInterpreter

/*
 * BrowserApplication + AppContainer — the manual DI graph (P02).
 *
 * Deliberately NOT Hilt: this app stays single-module and small; a hand-built container is
 * ~40 lines, has no codegen cost, and every consumer is constructor-injected.
 */

class AppContainer(app: Application) {
    val settings = SettingsRepository(app)
    val records = RecordsRepository(app)
    val whitelists = WhitelistRepository(app)
    val tabs = TabManager(app)
    val urlInterpreter = UrlInterpreter
    val gestureCodes: com.saigopal.browser.domain.GestureCodes get() = settings.state.value

    init {
        settings.ensureFirstRunDefaults()
    }
}

class BrowserApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // P08 8.1: MobileAds.initialize exactly once, process-wide (was double-initialized in two
        // activities). Async by nature; nothing blocks first frame.
        com.saigopal.browser.ui.ads.AdsController.warmUp(this)
    }
}
