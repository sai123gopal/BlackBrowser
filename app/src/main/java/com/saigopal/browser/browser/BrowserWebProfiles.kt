package com.saigopal.browser.browser

import androidx.webkit.ProfileStore
import androidx.webkit.WebViewFeature

object BrowserWebProfiles {
    const val INCOGNITO_PROFILE = "blackbrowser-incognito"
    @Volatile private var startupCleanupDone = false

    fun supportsIncognitoProfiles(): Boolean =
        WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)

    fun deleteIncognitoProfile() {
        if (!supportsIncognitoProfiles()) return
        try {
            ProfileStore.getInstance().deleteProfile(INCOGNITO_PROFILE)
        } catch (_: IllegalStateException) {
            // A live WebView still owns the profile; its owner retries when the session closes.
        } catch (_: UnsupportedOperationException) {
            // The provider may withdraw profile support between the feature check and call.
        }
    }

    @Synchronized
    fun clearStaleProfileOnce() {
        if (startupCleanupDone) return
        deleteIncognitoProfile()
        startupCleanupDone = true
    }
}