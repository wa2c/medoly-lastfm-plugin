package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.MediaProperty
import com.wa2c.android.medoly.library.PluginOperationCategory
import com.wa2c.android.medoly.library.PluginTypeCategory
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger
import com.wa2c.android.medoly.plugin.action.lastfm.util.Prefs

/**
 * Plugin receiver.
 */
class PluginReceivers {

    abstract class AbstractPluginReceiver : BroadcastReceiver() {
        @Synchronized override fun onReceive(context: Context, intent: Intent) {
            Logger.d("onReceive: " + this.javaClass.simpleName)

            val pluginIntent = MediaPluginIntent(intent)
            val propertyData = pluginIntent.propertyData ?: return
            val prefs = Prefs(context)

            if (this is EventScrobbleReceiver ||
                this is EventNowPlayingReceiver ||
                this is ExecuteLoveReceiver ||
                this is ExecuteUnLoveReceiver) {
                // checks
                if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_POST_MESSAGE)) {
                    return
                }
                if (this is EventNowPlayingReceiver && !prefs.getBoolean(R.string.prefkey_now_playing_enabled)) {
                    return
                }
                if (this is EventScrobbleReceiver && !prefs.getBoolean(R.string.prefkey_scrobble_enabled)) {
                    return
                }
                if (propertyData.isMediaEmpty) {
                    AppUtils.showToast(context, R.string.message_no_media)
                    return
                }
                if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                    return
                }

                // service
                pluginIntent.setClass(context, PluginPostService::class.java)
            } else if (this is EventGetAlbumArtReceiver || this is ExecuteGetAlbumArtReceiver) {
                // check
                if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_GET_ALBUM_ART)) {
                    AppUtils.sendResult(context, pluginIntent)
                    return
                }
                if (propertyData.isMediaEmpty) {
                    AppUtils.showToast(context, R.string.message_no_media)
                    AppUtils.sendResult(context, pluginIntent)
                    return
                }
                if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                    AppUtils.sendResult(context, pluginIntent)
                    return
                }
                val operation = try { PluginOperationCategory.valueOf(prefs.getString(R.string.prefkey_event_get_album_art_operation)!!) } catch (ignore : Exception) { null }
                if (!pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) && !pluginIntent.hasCategory(operation)) {
                    AppUtils.sendResult(context, pluginIntent)
                    return
                }

                // service
                pluginIntent.setClass(context, PluginGetAlbumArtService::class.java)
            } else if (this is EventGetPropertyReceiver || this is ExecuteGetPropertyReceiver) {
                if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_GET_PROPERTY)) {
                    AppUtils.sendResult(context, pluginIntent)
                    return
                }
                if (propertyData.isMediaEmpty) {
                    AppUtils.showToast(context, R.string.message_no_media)
                    AppUtils.sendResult(context, pluginIntent)
                    return
                }
                if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                    AppUtils.sendResult(context, pluginIntent)
                    return
                }
                val operation = prefs.getString(R.string.prefkey_event_get_property_operation)
                if (!pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) && !pluginIntent.hasCategory(operation)) {
                    AppUtils.sendResult(context, pluginIntent)
                    return
                }

                // service
                pluginIntent.setClass(context, PluginGetPropertyService::class.java)
            } else if (this is ExecuteTrackPageReceiver || this is ExecuteLastfmSiteReceiver) {
                // check
                if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_RUN)) {
                    return
                }
                if (this is ExecuteTrackPageReceiver) {
                    if (propertyData.isMediaEmpty) {
                        AppUtils.showToast(context, R.string.message_no_media)
                        return
                    }
                    if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                        return
                    }
                }

                // service
                pluginIntent.setClass(context, PluginRunService::class.java)
            }

            pluginIntent.putExtra(AbstractPluginService.RECEIVED_CLASS_NAME, this.javaClass.name)
            context.stopService(pluginIntent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(pluginIntent)
            } else {
                context.startService(pluginIntent)
            }
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
