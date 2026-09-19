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

class BrowserApplication : Application(), coil.ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    private var imageLoaderRef: coil.ImageLoader? = null

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // P08 8.1: MobileAds.initialize exactly once, process-wide (was double-initialized in two
        // activities). Async by nature; nothing blocks first frame.
        com.saigopal.browser.ui.ads.AdsController.warmUp(this)
    }

    override fun newImageLoader(): coil.ImageLoader {
        return imageLoaderRef ?: coil.ImageLoader.Builder(this)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    // Cap at 15% heap on low-end 4GB devices instead of 25% default
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(25L * 1024 * 1024)
                    .build()
            }
            .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
            .crossfade(true)
            .respectCacheHeaders(false)
            .build().also { imageLoaderRef = it }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        imageLoaderRef?.memoryCache?.trimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        imageLoaderRef?.memoryCache?.clear()
    }
}

