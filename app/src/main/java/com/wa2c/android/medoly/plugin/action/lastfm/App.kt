package com.wa2c.android.medoly.plugin.action.lastfm

import android.app.Application
import android.content.Context
import com.wa2c.android.medoly.library.PluginOperationCategory
import com.wa2c.android.prefs.Prefs
import timber.log.Timber


/**
 * App
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG)
            Timber.plant(Timber.DebugTree())

        Migrator(this).versionUp()
    }

}
