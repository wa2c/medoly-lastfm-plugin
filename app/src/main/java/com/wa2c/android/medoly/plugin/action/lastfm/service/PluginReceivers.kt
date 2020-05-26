package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.wa2c.android.medoly.library.*
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.util.logD
import com.wa2c.android.medoly.plugin.action.lastfm.util.toast
import com.wa2c.android.prefs.Prefs

/**
 * Plugin receiver.
 */
class PluginReceivers {

    /**
     * Plugin request receiver
     */
    abstract class AbstractPluginReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            logD("onReceive: %s", this.javaClass.simpleName)
            val result = receive(context, MediaPluginIntent(intent))
            setResult(result.resultCode, null, null)
        }

        /**
         * Receive data.
         */
        private fun receive(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult  {
            var result =  PluginBroadcastResult.CANCEL

            val propertyData = pluginIntent.propertyData ?: return result
            val prefs = Prefs(context)

            if (this is EventScrobbleReceiver ||
                    this is EventNowPlayingReceiver ||
                    this is ExecuteLoveReceiver ||
                    this is ExecuteUnLoveReceiver) {
                // category
                if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_POST_MESSAGE)) {
                    return result
                }
                // media
                if (propertyData.isMediaEmpty) {
                    context.toast(R.string.message_no_media)
                    return result
                }
                // property
                if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                    return result
                }
                if (this is EventNowPlayingReceiver) {
                    // enabled
                    if ( !prefs.getBoolean(R.string.prefkey_now_playing_enabled, defRes = R.bool.pref_default_now_playing_enabled) ) {
                        return result
                    }
                } else  if (this is EventScrobbleReceiver) {
                    // enabled
                    if (!prefs.getBoolean(R.string.prefkey_scrobble_enabled, defRes = R.bool.pref_default_scrobble_enabled)) {
                        return result
                    }
                    // previous media
                    val mediaUriText = propertyData.mediaUri.toString()
                    val previousMediaUri = prefs.getString(AbstractPluginService.PREFKEY_PREVIOUS_MEDIA_URI)
                    val previousMediaEnabled = prefs.getBoolean(R.string.prefkey_previous_media_enabled, defRes = R.bool.pref_default_previous_media_enabled)
                    if (!previousMediaEnabled && mediaUriText.isNotEmpty() && previousMediaUri.isNotEmpty() && mediaUriText == previousMediaUri) {
                        return result
                    }
                }

                // service
                pluginIntent.setClass(context, PluginPostService::class.java)
                result = PluginBroadcastResult.COMPLETE
            } else if (this is EventGetAlbumArtReceiver || this is ExecuteGetAlbumArtReceiver) {
                // category
                if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_GET_ALBUM_ART)) {
                    return result
                }
                // media
                if (propertyData.isMediaEmpty) {
                    context.toast(R.string.message_no_media)
                    return result
                }
                // property
                if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                    return result
                }
                // operation
                val operation = prefs.getString(R.string.prefkey_event_get_album_art_operation, defRes = R.string.pref_default_event_get_album_art_operation)
                if (!pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) && !pluginIntent.hasCategory(operation)) {
                    return result
                }

                // service
                pluginIntent.setClass(context, PluginGetAlbumArtService::class.java)
                result = PluginBroadcastResult.PROCESSING
            } else if (this is EventGetPropertyReceiver || this is ExecuteGetPropertyReceiver) {
                // category
                if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_GET_PROPERTY)) {
                    return result
                }
                // media
                if (propertyData.isMediaEmpty) {
                    context.toast(R.string.message_no_media)
                    return result
                }
                // property
                if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                    return result
                }
                // operation
                val operation = prefs.getString(R.string.prefkey_event_get_property_operation, defRes = R.string.pref_default_event_get_property_operation)
                if (!pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) && !pluginIntent.hasCategory(operation)) {
                    return result
                }

                // service
                pluginIntent.setClass(context, PluginGetPropertyService::class.java)
                result = PluginBroadcastResult.PROCESSING
            }

            pluginIntent.putExtra(AbstractPluginService.RECEIVED_CLASS_NAME, this.javaClass.name)
            ContextCompat.startForegroundService(context, pluginIntent)
            return result
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

}
