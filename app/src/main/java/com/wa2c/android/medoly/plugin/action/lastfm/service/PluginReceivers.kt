package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.wa2c.android.medoly.library.*
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.util.logD
import com.wa2c.android.medoly.plugin.action.lastfm.util.toWorkParams
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

        lateinit var prefs: Prefs

        override fun onReceive(context: Context, intent: Intent) {
            logD("onReceive: %s", this.javaClass.simpleName)
            prefs = Prefs(context)

            val pluginIntent = MediaPluginIntent(intent)
            val result = if( this is EventScrobbleReceiver) {
                scrobble(context, pluginIntent)
            } else if (this is EventNowPlayingReceiver) {
                updateNowPlaying(context, pluginIntent)
            } else if( this is ExecuteLoveReceiver) {
                love(context, pluginIntent)
            } else if( this is ExecuteUnLoveReceiver) {
                unlove(context, pluginIntent)
            } else if (this is EventGetAlbumArtReceiver || this is ExecuteGetAlbumArtReceiver) {
                getAlbumArt(context, pluginIntent)
            } else if (this is EventGetPropertyReceiver || this is ExecuteGetPropertyReceiver) {
                getProperty(context, pluginIntent)
            } else {
                return
            }

            setResult(result.resultCode, null, null)
        }

//        /**
//         * Receive data.
//         */
//        private fun receive(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult  {
//            var result =  PluginBroadcastResult.CANCEL
//
//            val propertyData = pluginIntent.propertyData ?: return result
//
//            if (this is EventScrobbleReceiver ||
//                    this is EventNowPlayingReceiver ) {
//                // category
//                if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_POST_MESSAGE)) {
//                    return result
//                }
//                // media
//                if (propertyData.isMediaEmpty) {
//                    context.toast(R.string.message_no_media)
//                    return result
//                }
//                // property
//                if (propertyData.getFirst(MediaProperty.TITLE)
//                        .isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST)
//                        .isNullOrEmpty()
//                ) {
//                    return result
//                }
//                if (this is EventNowPlayingReceiver) {
//                    // enabled
//                    if (!prefs.getBoolean(
//                            R.string.prefkey_now_playing_enabled,
//                            defRes = R.bool.pref_default_now_playing_enabled
//                        )
//                    ) {
//                        return result
//                    }
//                } else if (this is EventScrobbleReceiver) {
//                    // enabled
//                    if (!prefs.getBoolean(
//                            R.string.prefkey_scrobble_enabled,
//                            defRes = R.bool.pref_default_scrobble_enabled
//                        )
//                    ) {
//                        return result
//                    }
//                    // previous media
//                    val mediaUriText = propertyData.mediaUri.toString()
//                    val previousMediaUri = prefs.getString(AbstractPluginService.PREFKEY_PREVIOUS_MEDIA_URI)
//                    val previousMediaEnabled = prefs.getBoolean(R.string.prefkey_previous_media_enabled, defRes = R.bool.pref_default_previous_media_enabled)
//                    if (!previousMediaEnabled && mediaUriText.isNotEmpty() && previousMediaUri.isNotEmpty() && mediaUriText == previousMediaUri) {
//                        return result
//                    }
//                }
//
//                // service
//                pluginIntent.setClass(context, PluginPostService::class.java)
//                result = PluginBroadcastResult.COMPLETE
//            } else if( this is EventScrobbleReceiver) {
//                return scrobble(context, pluginIntent)
//            } else if( this is ExecuteLoveReceiver) {
//                return love(context, pluginIntent)
//            } else if( this is ExecuteUnLoveReceiver) {
//                return unlove(context, pluginIntent)
//            } else if (this is EventGetAlbumArtReceiver || this is ExecuteGetAlbumArtReceiver) {
//                return getAlbumArt(context, pluginIntent)
//            } else if (this is EventGetPropertyReceiver || this is ExecuteGetPropertyReceiver) {
//                return getProperty(context, pluginIntent)
//            }
//
//            pluginIntent.putExtra(AbstractPluginService.RECEIVED_CLASS_NAME, this.javaClass.name)
//            ContextCompat.startForegroundService(context, pluginIntent)
//            return result
//        }

        /**
         * Scrobble.
         */
        private fun scrobble(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult {
            val propertyData = pluginIntent.propertyData ?: return PluginBroadcastResult.CANCEL
            if (!existsMedia(context, propertyData)) return PluginBroadcastResult.CANCEL

            // enabled
            if (!prefs.getBoolean(R.string.prefkey_scrobble_enabled, defRes = R.bool.pref_default_scrobble_enabled)) {
                return PluginBroadcastResult.CANCEL
            }

            // previous media
            val mediaUriText = propertyData.mediaUri.toString()
            val previousMediaUri = prefs.getString(PluginPostScrobbleWorker.PREFKEY_PREVIOUS_MEDIA_URI)
            val previousMediaEnabled = prefs.getBoolean(R.string.prefkey_previous_media_enabled, defRes = R.bool.pref_default_previous_media_enabled)
            if (!previousMediaEnabled && mediaUriText.isNotEmpty() && previousMediaUri.isNotEmpty() && mediaUriText == previousMediaUri) {
                return PluginBroadcastResult.CANCEL
            }

            launchWorker<PluginPostScrobbleWorker>(context.applicationContext, pluginIntent.toWorkParams())
            return PluginBroadcastResult.COMPLETE
        }

        /**
         * Now Playing.
         */
        private fun updateNowPlaying(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult {
            val propertyData = pluginIntent.propertyData ?: return PluginBroadcastResult.CANCEL
            if (!existsMedia(context, propertyData)) return PluginBroadcastResult.CANCEL

            // enabled
            if (!prefs.getBoolean(R.string.prefkey_now_playing_enabled, defRes = R.bool.pref_default_now_playing_enabled)) {
                return PluginBroadcastResult.CANCEL
            }

            launchWorker<PluginPostNowPlayingWorker>(context.applicationContext, pluginIntent.toWorkParams())
            return PluginBroadcastResult.COMPLETE
        }

        /**
         * Love
         */
        private fun love(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult {
            val propertyData = pluginIntent.propertyData ?: return PluginBroadcastResult.CANCEL
            if (!existsMedia(context, propertyData)) return PluginBroadcastResult.CANCEL

            launchWorker<PluginPostLoveWorker>(context.applicationContext, pluginIntent.toWorkParams())
            return PluginBroadcastResult.COMPLETE
        }

        /**
         * Unlove
         */
        private fun unlove(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult {
            val propertyData = pluginIntent.propertyData ?: return PluginBroadcastResult.CANCEL
            if (!existsMedia(context, propertyData)) return PluginBroadcastResult.CANCEL

            launchWorker<PluginPostUnloveWorker>(context.applicationContext, pluginIntent.toWorkParams())
            return PluginBroadcastResult.COMPLETE
        }

        /**
         * Get album art.
         */
        private fun getAlbumArt(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult {
            val propertyData = pluginIntent.propertyData ?: return PluginBroadcastResult.CANCEL
            if (!existsMedia(context, propertyData)) return PluginBroadcastResult.CANCEL

            // operation
            val operation = prefs.getString(R.string.prefkey_event_get_album_art_operation, defRes = R.string.pref_default_event_get_album_art_operation)
            if (!pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) && !pluginIntent.hasCategory(operation)) {
                return PluginBroadcastResult.CANCEL
            }

            launchWorker<PluginGetAlbumArtWorker>(context.applicationContext, pluginIntent.toWorkParams())
            return PluginBroadcastResult.PROCESSING
        }

        /**
         * Get property.
         */
        private fun getProperty(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult {
            val propertyData = pluginIntent.propertyData ?: return PluginBroadcastResult.CANCEL
            if (!existsMedia(context, propertyData)) return PluginBroadcastResult.CANCEL

            // operation
            val operation = prefs.getString(R.string.prefkey_event_get_property_operation, defRes = R.string.pref_default_event_get_property_operation)
            if (!pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) && !pluginIntent.hasCategory(operation)) {
                return PluginBroadcastResult.CANCEL
            }

            launchWorker<PluginGetPropertyWorker>(context.applicationContext, pluginIntent.toWorkParams())
            return PluginBroadcastResult.PROCESSING
        }

        /**
         * True if exists media.
         */
        private fun existsMedia(context: Context, propertyData: PropertyData): Boolean {
            // media
            if (propertyData.isMediaEmpty) {
                context.toast(R.string.message_no_media)
                return false
            }
            // property
            if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                return false
            }
            return true
        }

        /**
         * Launch worker.
         */
        private inline fun <reified T : Worker> launchWorker(context: Context, params: Data) {
            val workManager = WorkManager.getInstance(context.applicationContext)
            val request = OneTimeWorkRequestBuilder<T>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(params)
                .build()
            workManager.enqueue(request)
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
