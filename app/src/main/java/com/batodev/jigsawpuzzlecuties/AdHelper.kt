package com.batodev.jigsawpuzzlecuties

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

const val AD_ID = "ca-app-pub-9667420067790140/7731686137"

object AdHelper {
    private var ad: InterstitialAd? = null

    fun showAdIfNeeded(activity: Activity) {
        val settings = SettingsHelper.load(activity)
        if (settings.addCounter >= settings.displayAddEveryXPicView) {
            settings.addCounter = 0
            SettingsHelper.save(activity, settings)
            showAd(activity)
        }
    }

    fun showAd(activity: Activity) {
        ad?.show(activity)
        loadAd(activity)
    }

    fun loadAd(activity: Activity) {
        val adRequest: AdRequest = AdRequest.Builder().build()

        InterstitialAd.load(activity, AD_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    // The mInterstitialAd reference will be null until
                    // an ad is loaded.
                    Log.i(AdHelper::class.simpleName, "onAdLoaded: $interstitialAd")
                    ad = interstitialAd
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error
                    Log.w(AdHelper::class.simpleName, "onAdFailedToLoad: $loadAdError")
                }
            })
    }
}
