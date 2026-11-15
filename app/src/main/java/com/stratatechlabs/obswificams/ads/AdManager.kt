package com.stratatechlabs.obswificams.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.stratatechlabs.obswificams.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Unified AdMob manager for App Open, Back Button Interstitial, and Reward Tea Ads.
 * Reads ad unit IDs injected from the separate .env file via BuildConfig.
 */
object AdManager {

    private const val TAG = "AdManager"

    private val isInitialized = AtomicBoolean(false)

    // -------------------------------------------------------------------------
    // App Open Ad Manager
    // -------------------------------------------------------------------------
    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenAdLoading = false
    private var isShowingAppOpenAd = false
    private var appOpenLoadTime: Long = 0L

    // -------------------------------------------------------------------------
    // Back Button Interstitial Ad Manager
    // -------------------------------------------------------------------------
    private var backInterstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    // -------------------------------------------------------------------------
    // Rewarded Tea Ad Manager
    // -------------------------------------------------------------------------
    private var rewardedTeaAd: RewardedAd? = null
    private var isRewardedTeaLoading = false

    fun initialize(application: Application) {
        if (isInitialized.getAndSet(true)) return

        MobileAds.initialize(application) { initStatus ->
            Log.d(TAG, "AdMob initialized: $initStatus")
            loadAppOpenAd(application)
            loadBackInterstitialAd(application)
            loadRewardedTeaAd(application)
        }
    }

    // =========================================================================
    // 1. App Open Ad
    // =========================================================================

    fun loadAppOpenAd(context: Context) {
        val adUnitId = BuildConfig.ADMOB_OPEN_APP_ID
        if (adUnitId.isBlank() || isAppOpenAdLoading || isAppOpenAdAvailable()) {
            return
        }

        isAppOpenAdLoading = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isAppOpenAdLoading = false
                    appOpenLoadTime = SystemClock.elapsedRealtime()
                    Log.d(TAG, "App Open Ad loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenAd = null
                    isAppOpenAdLoading = false
                    Log.w(TAG, "App Open Ad failed to load: ${loadAdError.message}")
                }
            },
        )
    }

    internal fun isAppOpenAdAvailable(): Boolean {
        val fourHoursMs = 4 * 3600 * 1000L
        return (appOpenAd != null) && ((SystemClock.elapsedRealtime() - appOpenLoadTime) < fourHoursMs)
    }

    fun isBackInterstitialAvailable(): Boolean = backInterstitialAd != null

    fun isRewardedTeaAvailable(): Boolean = rewardedTeaAd != null

    fun showAppOpenAdIfAvailable(activity: Activity, onDismissedOrFailed: () -> Unit = {}) {
        if (isShowingAppOpenAd) {
            onDismissedOrFailed()
            return
        }

        if (!isAppOpenAdAvailable()) {
            loadAppOpenAd(activity.applicationContext)
            onDismissedOrFailed()
            return
        }

        val ad = appOpenAd ?: run {
            onDismissedOrFailed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAppOpenAd = false
                loadAppOpenAd(activity.applicationContext)
                onDismissedOrFailed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAppOpenAd = false
                loadAppOpenAd(activity.applicationContext)
                onDismissedOrFailed()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAppOpenAd = true
            }
        }

        isShowingAppOpenAd = true
        ad.show(activity)
    }

    // =========================================================================
    // 2. Back Button Interstitial Ad
    // =========================================================================

    fun loadBackInterstitialAd(context: Context) {
        val adUnitId = BuildConfig.ADMOB_BACK_INTERSTITIAL_ID
        if (adUnitId.isBlank() || isInterstitialLoading || (backInterstitialAd != null)) {
            return
        }

        isInterstitialLoading = true
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    backInterstitialAd = interstitialAd
                    isInterstitialLoading = false
                    Log.d(TAG, "Back Interstitial Ad loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    backInterstitialAd = null
                    isInterstitialLoading = false
                    Log.w(TAG, "Back Interstitial Ad failed to load: ${loadAdError.message}")
                }
            },
        )
    }

    /**
     * Shows the interstitial ad if available when the user presses back.
     * Returns true if ad was shown (and invokes [onDismissed] when closed),
     * or false if not available so caller can proceed immediately.
     */
    fun showBackInterstitial(activity: Activity, onDismissed: () -> Unit): Boolean {
        val ad = backInterstitialAd
        if (ad == null) {
            loadBackInterstitialAd(activity.applicationContext)
            return false
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                backInterstitialAd = null
                loadBackInterstitialAd(activity.applicationContext)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                backInterstitialAd = null
                loadBackInterstitialAd(activity.applicationContext)
                onDismissed()
            }
        }

        ad.show(activity)
        return true
    }

    // =========================================================================
    // 3. Rewarded Tea Ad
    // =========================================================================

    fun loadRewardedTeaAd(context: Context) {
        val adUnitId = BuildConfig.ADMOB_REWARD_TEA_ID
        if (adUnitId.isBlank() || isRewardedTeaLoading || (rewardedTeaAd != null)) {
            return
        }

        isRewardedTeaLoading = true
        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            adUnitId,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedTeaAd = ad
                    isRewardedTeaLoading = false
                    Log.d(TAG, "Rewarded Tea Ad loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedTeaAd = null
                    isRewardedTeaLoading = false
                    Log.w(TAG, "Rewarded Tea Ad failed to load: ${loadAdError.message}")
                }
            },
        )
    }

    /**
     * Shows the Rewarded Ad for the "Tea" reward.
     * If loaded, shows the ad and triggers [onRewardEarned] on success.
     * If not loaded, invokes [onNotReady] and starts loading.
     */
    fun showRewardedTea(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onDismissed: () -> Unit = {},
        onNotReady: () -> Unit = {},
    ) {
        val ad = rewardedTeaAd
        if (ad == null) {
            loadRewardedTeaAd(activity.applicationContext)
            onNotReady()
            return
        }

        var rewardGranted = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedTeaAd = null
                loadRewardedTeaAd(activity.applicationContext)
                if (rewardGranted) {
                    onRewardEarned()
                } else {
                    onDismissed()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedTeaAd = null
                loadRewardedTeaAd(activity.applicationContext)
                onDismissed()
            }
        }

        ad.show(activity) { _ ->
            rewardGranted = true
        }
    }

    @androidx.annotation.VisibleForTesting
    @Suppress("unused")
    internal fun setTestAppOpenAd(ad: AppOpenAd?, loadTime: Long = SystemClock.elapsedRealtime()) {
        appOpenAd = ad
        appOpenLoadTime = loadTime
    }

    @androidx.annotation.VisibleForTesting
    @Suppress("unused")
    internal fun setTestBackInterstitialAd(ad: InterstitialAd?) {
        backInterstitialAd = ad
    }

    @androidx.annotation.VisibleForTesting
    @Suppress("unused")
    internal fun setTestRewardedTeaAd(ad: RewardedAd?) {
        rewardedTeaAd = ad
    }

    @androidx.annotation.VisibleForTesting
    internal fun resetForTesting() {
        appOpenAd = null
        backInterstitialAd = null
        rewardedTeaAd = null
        isShowingAppOpenAd = false
        isAppOpenAdLoading = false
        isInterstitialLoading = false
        isRewardedTeaLoading = false
        appOpenLoadTime = 0L
    }
}
