package com.wa2c.android.medoly.plugin.action.lastfm

import android.app.Application
import com.wa2c.android.medoly.plugin.action.lastfm.service.AbstractPluginService
import timber.log.Timber


/**
 * App
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            //Timber.plant(CrashlyticsTree())
        } else {
            Timber.plant(CrashlyticsTree())
        }

        // Create channel
        AbstractPluginService.createChannel(this)
        // Migrator
        Migrator(this).versionUp()
    }

}
