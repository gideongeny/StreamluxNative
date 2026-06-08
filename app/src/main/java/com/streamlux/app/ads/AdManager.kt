package com.streamlux.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

private const val TAG = "StreamLuxAds"

object AdManager {
    // ── Your live ad unit IDs ──────────────────────────────────────────────
    private const val INTERSTITIAL_AD_UNIT_ID        = "ca-app-pub-1281448884303417/3383670834"
    private const val APP_OPEN_AD_UNIT_ID             = "ca-app-pub-1281448884303417/3800649401"
    private const val REWARDED_AD_UNIT_ID_1           = "ca-app-pub-1281448884303417/6220147770"
    private const val REWARDED_AD_UNIT_ID_2           = "ca-app-pub-1281448884303417/5113731075"

    private var interstitialAd: InterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenAdShowing = false
    private var rewardedAd: RewardedAd? = null

    // ── Interstitial ──────────────────────────────────────────────────────

    /** Call once on app start to pre-load an Interstitial Ad */
    fun loadInterstitial(context: Context) {
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial ad loaded.")
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            loadInterstitial(context) // Reload for next time
                        }
                        override fun onAdFailedToShowFullScreenContent(e: AdError) {
                            interstitialAd = null
                        }
                    }
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Interstitial failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Show the interstitial if ready, then invoke [onClose].
     * If no ad is cached, [onClose] is invoked immediately so the user
     * is never blocked.
     */
    fun showInterstitial(activity: Activity, onClose: () -> Unit = {}) {
        if (interstitialAd != null) {
            interstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onClose()
                }
                override fun onAdFailedToShowFullScreenContent(e: AdError) {
                    interstitialAd = null
                    onClose()
                }
            }
            interstitialAd!!.show(activity)
        } else {
            onClose()
        }
    }

    // ── App Open ──────────────────────────────────────────────────────────

    /** Load App Open Ad */
    fun loadAppOpenAd(context: Context) {
        AppOpenAd.load(
            context,
            APP_OPEN_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    Log.d(TAG, "App Open Ad loaded.")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "App Open Ad failed to load: ${error.message}")
                }
            }
        )
    }

    /** Show App Open Ad (called on app foregrounding from MainActivity.onResume) */
    var isVideoPlaying: Boolean = false

    fun showAppOpenAd(activity: Activity) {
        if (isAppOpenAdShowing || appOpenAd == null || isVideoPlaying) return
        appOpenAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isAppOpenAdShowing = false
                appOpenAd = null
                loadAppOpenAd(activity)
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                isAppOpenAdShowing = false
                appOpenAd = null
            }
            override fun onAdShowedFullScreenContent() {
                isAppOpenAdShowing = true
            }
        }
        appOpenAd!!.show(activity)
    }

    // ── Rewarded ──────────────────────────────────────────────────────────

    /**
     * Pre-load a Rewarded Ad.
     * Uses REWARDED_AD_UNIT_ID_1 by default, falling back to REWARDED_AD_UNIT_ID_2.
     * Call this early (e.g., after SDK init or after the previous ad was shown)
     * so the ad is ready when the user taps "Download".
     */
    fun loadRewardedAd(context: Context, useSecondary: Boolean = false) {
        val unitId = if (useSecondary) REWARDED_AD_UNIT_ID_2 else REWARDED_AD_UNIT_ID_1
        RewardedAd.load(
            context,
            unitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded Ad loaded ($unitId).")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Rewarded Ad failed to load: ${error.message}")
                    // Try the secondary unit if the primary failed
                    if (!useSecondary) loadRewardedAd(context, useSecondary = true)
                }
            }
        )
    }

    /**
     * Show the rewarded ad, then invoke [onRewarded] when the user earns the reward.
     * If no ad is cached, [onRewarded] is invoked immediately so the download
     * still proceeds without blocking the user.
     */
    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewardedAd(activity) // Pre-load next ad
                }
                override fun onAdFailedToShowFullScreenContent(e: AdError) {
                    rewardedAd = null
                    onRewarded() // Graceful fallback — don't block the user
                }
            }
            ad.show(activity, OnUserEarnedRewardListener { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onRewarded()
            })
        } else {
            // No ad cached — let the download proceed immediately
            onRewarded()
        }
    }
}
