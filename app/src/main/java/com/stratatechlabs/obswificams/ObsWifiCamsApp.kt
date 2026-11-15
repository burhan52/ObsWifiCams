package com.stratatechlabs.obswificams

import android.app.Application
import com.google.firebase.analytics.FirebaseAnalytics
import com.stratatechlabs.obswificams.ads.AdManager

/**
 * Base Application class that initializes global services including AdMob advertising and Firebase Analytics.
 */
class ObsWifiCamsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseAnalytics.getInstance(this)
        AdManager.initialize(this)
    }
}
