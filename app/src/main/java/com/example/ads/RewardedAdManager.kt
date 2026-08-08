package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object RewardedAdManager {
    private const val TAG = "RewardedAdManager"
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun loadAd(context: Context, adUnitId: String = AdMobConfig.getRewardedAdUnitId()) {
        if (rewardedAd != null || isLoading) return
        isLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                    Log.d(TAG, "Rewarded ad loaded successfully.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun showAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdClosedWithoutReward: () -> Unit = {}
    ) {
        val currentAd = rewardedAd
        if (currentAd != null) {
            var rewardGranted = false
            currentAd.show(activity, OnUserEarnedRewardListener { rewardItem ->
                rewardGranted = true
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onRewardEarned()
            })
            rewardedAd = null
            // Preload next ad
            loadAd(activity)
        } else {
            // If ad is not loaded or device is offline, gracefully allow feature access
            Log.d(TAG, "Rewarded ad unavailable. Executing fallback flow.")
            onRewardEarned()
            loadAd(activity)
        }
    }

    fun isAdReady(): Boolean = rewardedAd != null
}
