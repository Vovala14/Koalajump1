package com.lavrik.koalajump.utils

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Manages AdMob rewarded ads for the game
 */
class AdManager(private val context: Context) {
    companion object {
        private const val TAG = "AdManager"

        // Use your actual Rewarded Ad unit ID from AdMob dashboard
        private const val REWARDED_AD_UNIT_ID = "ca-app-pub-2052671084682074/1751264807"

        // Test ad unit ID for development testing
        private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }

    // Track if ads are loaded and ready
    val isAdReady = mutableStateOf(false)

    // Track if network is available
    val isNetworkAvailable = mutableStateOf(false)

    // Hold the loaded ad
    private var rewardedAd: RewardedAd? = null

    // Flag to track if initialization has completed
    private var isInitialized = false

    /**
     * Initialize AdMob
     */
    fun initialize() {
        try {
            Log.d(TAG, "Starting AdMob initialization")

            // Update network status first
            updateNetworkStatus()

            // Initialize the Mobile Ads SDK
            MobileAds.initialize(context) { initializationStatus ->
                val statusMap = initializationStatus.adapterStatusMap
                for (adapterClass in statusMap.keys) {
                    val status = statusMap[adapterClass]
                    Log.d(TAG, "Adapter name: ${adapterClass}, Description: ${status?.description}, " +
                            "Latency: ${status?.latency}")
                }

                isInitialized = true
                Log.d(TAG, "AdMob initialization complete, loading ads now")

                // Load an ad right away after initialization
                loadRewardedAd()
            }

            // Try loading an ad even before initialization completes
            // This might help have an ad ready sooner
            loadRewardedAd()

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AdMob: ${e.message}")
        }
    }

    /**
     * Load a rewarded ad
     */
    fun loadRewardedAd() {
        updateNetworkStatus()

        Log.d(TAG, "Loading rewarded ad. Network available: ${isNetworkAvailable.value}, Initialized: $isInitialized")

        if (!isNetworkAvailable.value) {
            Log.d(TAG, "Skipping ad load - no network available")
            isAdReady.value = false
            return
        }

        try {
            // Create an ad request
            val adRequest = AdRequest.Builder().build()

            val adUnitId = REWARDED_AD_UNIT_ID
            Log.d(TAG, "Requesting ad with unit ID: $adUnitId")

            // Load the rewarded ad
            RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Ad failed to load: ${adError.message}, code: ${adError.code}")
                    rewardedAd = null
                    isAdReady.value = false

                    // Try loading a test ad as fallback for development - removing BuildConfig check
                    if (adUnitId != TEST_REWARDED_AD_UNIT_ID) {
                        Log.d(TAG, "Trying to load test ad instead")
                        loadTestAd()
                    }
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Ad loaded successfully")
                    rewardedAd = ad
                    isAdReady.value = true
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Exception while loading ad: ${e.message}")
            isAdReady.value = false
        }
    }

    /**
     * Load a test ad for development purposes
     */
    private fun loadTestAd() {
        try {
            val adRequest = AdRequest.Builder().build()

            // Load using Google's test ad unit ID
            RewardedAd.load(context, TEST_REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Test ad failed to load: ${adError.message}")
                    rewardedAd = null
                    isAdReady.value = false
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Test ad loaded successfully")
                    rewardedAd = ad
                    isAdReady.value = true
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Exception while loading test ad: ${e.message}")
        }
    }

    /**
     * Show the rewarded ad and provide callback for earning reward
     */
    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit, onAdClosed: () -> Unit) {
        val ad = rewardedAd

        if (ad == null) {
            Log.d(TAG, "Rewarded ad is not ready yet")
            onAdClosed()
            loadRewardedAd() // Try to load for next time
            return
        }

        // Set callback for ad events
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad was dismissed")
                // Reset and prepare for the next ad
                rewardedAd = null
                isAdReady.value = false
                loadRewardedAd()
                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Ad failed to show: ${adError.message}")
                rewardedAd = null
                isAdReady.value = false
                loadRewardedAd()
                onAdClosed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content")
            }
        }

        // Show the ad
        ad.show(activity, OnUserEarnedRewardListener { rewardItem ->
            // Handle the reward
            val rewardAmount = rewardItem.amount
            val rewardType = rewardItem.type
            Log.d(TAG, "User earned reward: $rewardAmount $rewardType")
            onRewarded()
        })
    }

    /**
     * Check if network is available and update state
     */
    fun updateNetworkStatus() {
        isNetworkAvailable.value = isNetworkConnected()
        Log.d(TAG, "Network available: ${isNetworkAvailable.value}")
    }

    /**
     * Check if device has active network connection
     */
    private fun isNetworkConnected(): Boolean {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val networkCapabilities = connectivityManager.activeNetwork ?: return false
                val activeNetwork = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

                return when {
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                return networkInfo != null && networkInfo.isConnected
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network status: ${e.message}")
            return false
        }
    }
}