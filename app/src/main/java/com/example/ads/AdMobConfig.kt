package com.example.ads

object AdMobConfig {
    const val ADMOB_APP_ID = "ca-app-pub-8155064094205693~2372542694"

    const val PROD_NATIVE_AD_UNIT_ID = "ca-app-pub-8155064094205693/3724886882"
    const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-8155064094205693/9508796681"

    // Official Google Test Ad Unit IDs for safe testing during development
    const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Set to true for development/testing, false for production release
    var USE_TEST_ADS = true

    fun getNativeAdUnitId(): String {
        return if (USE_TEST_ADS) TEST_NATIVE_AD_UNIT_ID else PROD_NATIVE_AD_UNIT_ID
    }

    fun getRewardedAdUnitId(): String {
        return if (USE_TEST_ADS) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID
    }
}
