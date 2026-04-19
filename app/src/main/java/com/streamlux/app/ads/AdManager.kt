package com.streamlux.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

private const val TAG = "StreamLuxAds"

object AdManager {
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-1281448884303417/6946857385"
    private const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-1281448884303417/3965470822"
    private const val REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-1281448884303417/1872613770"

    private var interstitialAd: InterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenAdShowing = false

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

    /** Show the interstitial if ready (e.g., before playing a video) */
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

    /** Show App Open Ad (call on app foregrounding) */
    fun showAppOpenAd(activity: Activity) {
        if (isAppOpenAdShowing || appOpenAd == null) return
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

    /** Pre-load Rewarded Interstitial */
    fun loadRewardedInterstitial(context: Context, onLoaded: (RewardedInterstitialAd) -> Unit) {
        RewardedInterstitialAd.load(
            context,
            REWARDED_INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    Log.d(TAG, "Rewarded Interstitial loaded.")
                    onLoaded(ad)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Rewarded Interstitial failed: ${error.message}")
                }
            }
        )
    }
}
