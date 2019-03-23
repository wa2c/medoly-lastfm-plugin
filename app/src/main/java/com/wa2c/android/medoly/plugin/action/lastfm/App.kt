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
        }

        // Create channel
        AbstractPluginService.createChannel(this)
        // Migrator
        Migrator(this).versionUp()
    }


}
