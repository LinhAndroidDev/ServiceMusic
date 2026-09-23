package com.example.serviceandroid.ads

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads one app-open ad for the splash screen and shows it a single time.
 * Debug builds use Google's test ad unit so clicks are not counted as real traffic.
 */
@Singleton
class AppOpenAdController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var appOpenAd: AppOpenAd? = null
    private var isLoading = false
    private var isShowing = false
    private var hasShown = false
    private var loadFailed = false
    private val pendingResults = mutableListOf<(Boolean) -> Unit>()

    fun load(onResult: (loaded: Boolean) -> Unit) {
        if (hasShown || loadFailed) {
            onResult(false)
            return
        }
        if (appOpenAd != null) {
            onResult(true)
            return
        }
        pendingResults.add(onResult)
        if (isLoading) return
        isLoading = true
        AppOpenAd.load(
            context,
            adUnitId(),
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    isLoading = false
                    appOpenAd = ad
                    dispatch(loaded = true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    appOpenAd = null
                    loadFailed = true
                    dispatch(loaded = false)
                }
            },
        )
    }

    fun showIfAvailable(activity: Activity, onFinished: () -> Unit) {
        val ad = appOpenAd
        if (hasShown || isShowing || ad == null) {
            onFinished()
            return
        }
        hasShown = true
        isShowing = true
        var finished = false
        fun finishOnce() {
            if (finished) return
            finished = true
            isShowing = false
            appOpenAd = null
            onFinished()
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                finishOnce()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                finishOnce()
            }
        }
        ad.show(activity)
    }

    private fun dispatch(loaded: Boolean) {
        val results = pendingResults.toList()
        pendingResults.clear()
        results.forEach { it(loaded) }
    }

    private fun adUnitId(): String {
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        return if (debuggable) TEST_AD_UNIT_ID else PROD_AD_UNIT_ID
    }

    private companion object {
        const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
        const val PROD_AD_UNIT_ID = "ca-app-pub-1586151845780441/2856451328"
    }
}
