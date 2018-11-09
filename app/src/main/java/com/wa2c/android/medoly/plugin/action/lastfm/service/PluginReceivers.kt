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
import com.wa2c.android.prefs.Prefs
import timber.log.Timber

/**
 * Plugin receiver.
 */
class PluginReceivers {

    abstract class AbstractPluginReceiver : BroadcastReceiver() {
        @Synchronized override fun onReceive(context: Context, intent: Intent) {
            Timber.d("onReceive: " + this.javaClass.simpleName)

            val pluginIntent = MediaPluginIntent(intent)
            val propertyData = pluginIntent.propertyData ?: return
            val prefs = Prefs(context)

            if (this is EventScrobbleReceiver ||
                this is EventNowPlayingReceiver ||
                this is ExecuteLoveReceiver ||
                this is ExecuteUnLoveReceiver) {
                // category
                if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_POST_MESSAGE)) {
                    return
                }
                // media
                if (propertyData.isMediaEmpty) {
                    AppUtils.showToast(context, R.string.message_no_media)
                    return
                }
                // property
                if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                    return
                }
                if (this is EventNowPlayingReceiver) {
                    // enabled
                    if ( !prefs.getBoolean(R.string.prefkey_now_playing_enabled)) {
                        return
                    }
                }
                if (this is EventScrobbleReceiver) {
                    // enabled
                    if (!prefs.getBoolean(R.string.prefkey_scrobble_enabled)) {
                        return
                    }
                    // previous media
                    val mediaUriText = propertyData.mediaUri.toString()
                    val previousMediaUri = prefs.getString(AbstractPluginService.PREFKEY_PREVIOUS_MEDIA_URI)
                    val previousMediaEnabled = prefs.getBoolean(R.string.prefkey_previous_media_enabled)
                    if (!previousMediaEnabled && !mediaUriText.isEmpty() && !previousMediaUri.isEmpty() && mediaUriText == previousMediaUri) {
                        return
                    }
                }

                // service
                pluginIntent.setClass(context, PluginPostService::class.java)
            } else if (this is EventGetAlbumArtReceiver || this is ExecuteGetAlbumArtReceiver) {
                // category
                if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_GET_ALBUM_ART)) {
                    AppUtils.sendResult(context, pluginIntent)
                    return
                }
                // media
                if (propertyData.isMediaEmpty) {
                    AppUtils.showToast(context, R.string.message_no_media)
                    AppUtils.sendResult(context, pluginIntent)
                    return
                }
                // property
                if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                    AppUtils.sendResult(context, pluginIntent)
                    return
                }
                // operation
                val operation = try { PluginOperationCategory.valueOf(prefs.getString(R.string.prefkey_event_get_album_art_operation)) } catch (ignore : Exception) { null }
                if (!pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) && !pluginIntent.hasCategory(operation)) {
                    AppUtils.sendResult(context, pluginIntent)
                    return
                }

                // service
                pluginIntent.setClass(context, PluginGetAlbumArtService::class.java)
            } else if (this is EventGetPropertyReceiver || this is ExecuteGetPropertyReceiver) {
                // category
                if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_GET_PROPERTY)) {
                    AppUtils.sendResult(context, pluginIntent)
                    return
                }
                // media
                if (propertyData.isMediaEmpty) {
                    AppUtils.showToast(context, R.string.message_no_media)
                    AppUtils.sendResult(context, pluginIntent)
                    return
                }
                // property
                if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                    AppUtils.sendResult(context, pluginIntent)
                    return
                }
                // operation
                val operation = prefs.getString(R.string.prefkey_event_get_property_operation)
                if (!pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) && !pluginIntent.hasCategory(operation)) {
                    AppUtils.sendResult(context, pluginIntent)
                    return
                }

                // service
                pluginIntent.setClass(context, PluginGetPropertyService::class.java)
            } else if (this is ExecuteTrackPageReceiver || this is ExecuteLastfmSiteReceiver) {
                // category
                if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_RUN)) {
                    return
                }
                if (this is ExecuteTrackPageReceiver) {
                    // media
                    if (propertyData.isMediaEmpty) {
                        AppUtils.showToast(context, R.string.message_no_media)
                        return
                    }
                    // property
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
