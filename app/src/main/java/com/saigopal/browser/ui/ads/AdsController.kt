package com.saigopal.browser.ui.ads

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.analytics.FirebaseAnalytics

/*
 * AdsController (P08) — every ad touchpoint lives here.
 *
 * Lifecycle bug B1 is made impossible by construction: the AdView is created once per Banner
 * placement lifetime, loadAd runs inside DisposableEffect, and destroy() runs on dispose — there
 * is no code path that loads into a destroyed view.
 *
 * Deviation from the plan sketch (recorded): a Kotlin `object` instead of an AdsController(appContainer)
 * class — the manual DI container carries no ad state, and UMP consent info is process-global anyway.
 *
 * Policy notes (8.3/8.6): no banners on browser content, start page, or games hub; overview banner
 * suppressed while the foreground tab is incognito; frequency guard caps reload requests.
 */
object AdsController {

    enum class Placement(val tag: String) {
        OVERVIEW("overview"),
        SETTINGS("settings"),
        START_PAGE("start_page"),
    }

    /** 8.3: overview banner re-loads at most once per interval — cuts request waste on rapid open/close. */
    const val MIN_RELOAD_INTERVAL_MS = 5L * 60L * 1000L

    private var warmedUp = false
    private val lastLoadAt = mutableMapOf<Placement, Long>()

    // ---- 8.1 init -----------------------------------------------------------------------------

    /** Called once from BrowserApplication.onCreate; MobileAds.initialize is async by nature. */
    fun warmUp(context: Context) {
        if (warmedUp || !com.saigopal.browser.BuildConfig.ADS_ENABLED) return
        warmedUp = true
        MobileAds.initialize(context) {}
    }

    // ---- 8.4 UMP consent ------------------------------------------------------------------------

    fun consentInformation(context: Context): ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    /**
     * First-launch flow before any loadAd: gather consent info, then show the form if required.
     * SDK caches the outcome; safe to call on every cold start. Always calls onDone exactly once.
     */
    @JvmOverloads // MainActivity.java calls this without the onDone callback
    fun maybeShowConsent(activity: Activity, onDone: () -> Unit = {}) {
        if (!com.saigopal.browser.BuildConfig.ADS_ENABLED) {
            onDone()
            return
        }
        val info = consentInformation(activity)
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ -> onDone() }
            },
            { _ -> onDone() }, // network failure etc.: proceed without blocking the app
        )
    }

    fun canRequestAds(context: Context): Boolean =
        com.saigopal.browser.BuildConfig.ADS_ENABLED && consentInformation(context).canRequestAds()

    /** Google policy requires a persistent way back into the privacy options form (About sheet row). */
    fun privacyOptionsRequired(context: Context): Boolean =
        consentInformation(context).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptionsForm(activity: Activity, onDismissed: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> onDismissed() }
    }

    // ---- 8.2/8.5 compose banner ------------------------------------------------------------------

    @Composable
    fun Banner(placement: Placement, modifier: Modifier = Modifier, enabled: Boolean = true) {
        if (!com.saigopal.browser.BuildConfig.ADS_ENABLED || !enabled) return
        val context = LocalContext.current
        if (!canRequestAds(context)) return

        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val adSize = remember(configuration) {
            runCatching {
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, configuration.screenWidthDp)
            }.getOrDefault(AdSize.BANNER)
        }

        var collapsed by remember { mutableStateOf(false) }
        if (collapsed) return

        val analytics = remember { FirebaseAnalytics.getInstance(context) }
        val adView = remember(placement, adSize) {
            AdView(context).apply {
                adUnitId = adUnitIdFor(placement)
                setAdSize(adSize)
            }
        }

        DisposableEffect(adView) {
            adView.adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    // No-fill: collapse entirely instead of reserving dead space.
                    collapsed = true
                }

                override fun onAdImpression() {
                    val payload = Bundle().apply { putString("placement", placement.tag) }
                    analytics.logEvent("ad_impression", payload)
                }
            }
            val now = SystemClock.elapsedRealtime()
            val intervalElapsed = now - (lastLoadAt[placement] ?: 0L) >= MIN_RELOAD_INTERVAL_MS
            if (intervalElapsed && !adView.isLoading) {
                adView.loadAd(AdRequest.Builder().build())
                lastLoadAt[placement] = now
            }
            onDispose {
                // setAdListener's Kotlin type is non-null: swap in a no-op instead of null.
                adView.adListener = object : AdListener() {}
                adView.destroy()
            }
        }

        AndroidView(
            factory = {
                (adView.parent as? android.view.ViewGroup)?.removeView(adView)
                adView
            },
            onRelease = { v ->
                (v.parent as? android.view.ViewGroup)?.removeView(v)
            },
            modifier = modifier.height(with(density) { adSize.getHeightInPixels(context).toDp() }),
        )
    }

    /**
     * Both placements reuse the legacy banner unit until dedicated units are created in the
     * AdMob console; debug builds always point at the Google test unit so QA never violates policy.
     */
    private fun adUnitIdFor(placement: Placement): String = when (placement) {
        Placement.OVERVIEW, Placement.START_PAGE -> com.saigopal.browser.BuildConfig.AD_UNIT_OVERVIEW
        Placement.SETTINGS -> com.saigopal.browser.BuildConfig.AD_UNIT_SETTINGS
    }
}
