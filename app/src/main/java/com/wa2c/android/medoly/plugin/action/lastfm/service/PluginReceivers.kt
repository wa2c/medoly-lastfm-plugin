package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Plugin receiver classes.
 */
class PluginReceivers {

    abstract class AbstractPluginReceiver : BroadcastReceiver() {
        @Synchronized override fun onReceive(context: Context, intent: Intent) {
            val serviceIntent = Intent(intent)
            val c = this.javaClass
            serviceIntent.putExtra(AbstractPluginService.RECEIVED_CLASS_NAME, c.getName())

            if (c == EventScrobbleReceiver::class.java ||
                    c == EventNowPlayingReceiver::class.java ||
                    c == ExecuteLoveReceiver::class.java ||
                    c == ExecuteUnLoveReceiver::class.java) {
                serviceIntent.setClass(context, PluginPostService::class.java!!)
            } else if (c == EventGetAlbumArtReceiver::class.java || c == ExecuteGetAlbumArtReceiver::class.java) {
                serviceIntent.setClass(context, PluginGetAlbumArtService::class.java!!)
            } else if (c == EventGetPropertyReceiver::class.java || c == ExecuteGetPropertyReceiver::class.java) {
                serviceIntent.setClass(context, PluginGetPropertyService::class.java!!)
            } else {
                serviceIntent.setClass(context, PluginRunService::class.java!!)
            }

            context.stopService(serviceIntent)
            context.startService(serviceIntent)
        }
    }

    // Event

    class EventScrobbleReceiver : AbstractPluginReceiver()

    class EventNowPlayingReceiver : AbstractPluginReceiver()

    class EventGetAlbumArtReceiver : AbstractPluginReceiver()

    class EventGetPropertyReceiver : AbstractPluginReceiver()

    // Execution

    class ExecuteLoveReceiver : AbstractPluginReceiver()

    class ExecuteUnLoveReceiver : AbstractPluginReceiver()

    class ExecuteGetAlbumArtReceiver : AbstractPluginReceiver()

    class ExecuteGetPropertyReceiver : AbstractPluginReceiver()

    class ExecuteTrackPageReceiver : AbstractPluginReceiver()

    class ExecuteLastfmSiteReceiver : AbstractPluginReceiver()

}
