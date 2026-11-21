package uz.appclone

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/** Reklama. Pro foydalanuvchiga ko'rsatilmaydi. */
object Ads {
    // SINOV reklama birligi. Haqiqiy AdMob ID bilan almashtiriladi.
    private const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private var inited = false

    fun init(activity: Activity) {
        if (!inited) {
            inited = true
            MobileAds.initialize(activity.applicationContext) {}
        }
    }

    /** Reklama tugagach (yoki yuklanmasa) onDone chaqiriladi. Hech qachon osilib qolmaydi. */
    fun showInterstitial(activity: Activity, onDone: () -> Unit) {
        if (Prefs.isPro(activity)) {
            onDone()
            return
        }
        init(activity)

        var finished = false
        val handler = Handler(Looper.getMainLooper())
        val finish: () -> Unit = {
            if (!finished) {
                finished = true
                handler.removeCallbacksAndMessages(null)
                onDone()
            }
        }
        handler.postDelayed({ finish() }, 6000)   // reklama kelmasa 6 soniyada davom etamiz

        InterstitialAd.load(
            activity, INTERSTITIAL_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    if (finished || activity.isFinishing) return
                    handler.removeCallbacksAndMessages(null)   // ko'rsatilmoqda: kutish taymerini o'chiramiz
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() = finish()
                        override fun onAdFailedToShowFullScreenContent(e: AdError) = finish()
                    }
                    ad.show(activity)
                }

                override fun onAdFailedToLoad(e: LoadAdError) = finish()
            }
        )
    }
}
