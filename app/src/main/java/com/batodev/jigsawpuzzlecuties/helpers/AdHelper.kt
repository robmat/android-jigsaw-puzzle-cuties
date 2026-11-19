package com.batodev.jigsawpuzzlecuties.helpers

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

const val AD_ID = "ca-app-pub-9667420067790140/7731686137"

/**
 * A helper object for displaying ads.
 */
object AdHelper {
    private var ad: InterstitialAd? = null

    /**
     * Play's Families policy: an app whose target audience includes children may only show ads
     * suitable for its content rating. Without this every request was unrestricted, so the app
     * could be served mature ads - the reason 3.5 was rejected. Applies to all later requests.
     */
    private val familySafeAdConfiguration: RequestConfiguration =
        RequestConfiguration
            .Builder()
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
            // Replaces the deprecated child-directed / under-age-of-consent tags in SDK 25.
            .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
            .build()

    /**
     * Shows an interstitial ad if the ad counter reaches the display threshold.
     * The ad counter is reset after showing the ad.
     * @param activity The {@link Activity} context used to show the ad.
     * @see SettingsHelper
     * @see #showAd(Activity)
     */
    fun showAdIfNeeded(activity: Activity) {
        val settings = SettingsHelper.load(activity)
        if (settings.addCounter >= settings.displayAddEveryXPicView) {
            settings.addCounter = 0
            SettingsHelper.save(activity, settings)
            showAd(activity)
        }
    }

    /**
     * Displays the loaded interstitial ad and then reloads a new ad.
     * @param activity The {@link Activity} context used to show the ad.
     * @see #loadAd(Activity)
     */
    fun showAd(activity: Activity) {
        FirebaseHelper.logEvent(activity, "show_ad_attempt")
        // An interstitial can be shown only once: drop it, then load the next one.
        ad?.show(activity)
        ad = null
        loadAd(activity)
    }

    /**
     * Loads an interstitial ad. The loaded ad is stored in a private variable.
     * @param activity The {@link Activity} context used to load the ad.
     */
    fun loadAd(activity: Activity) {
        MobileAds.setRequestConfiguration(familySafeAdConfiguration)
        val adRequest: AdRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            activity,
            AD_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    // The mInterstitialAd reference will be null until
                    // an ad is loaded.
                    FirebaseHelper.logEvent(activity, "ad_loaded")
                    Log.i(AdHelper::class.simpleName, "onAdLoaded: $interstitialAd")
                    ad = interstitialAd
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error
                    FirebaseHelper.logException(activity, "ad_failed_to_load", loadAdError.message)
                    Log.w(AdHelper::class.simpleName, "onAdFailedToLoad: $loadAdError")
                }
            },
        )
    }
}
