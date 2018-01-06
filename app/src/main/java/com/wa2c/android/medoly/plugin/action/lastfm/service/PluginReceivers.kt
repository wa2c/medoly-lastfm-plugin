package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.MediaProperty
import com.wa2c.android.medoly.library.PluginOperationCategory
import com.wa2c.android.medoly.library.PluginTypeCategory
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger

/**
 * Plugin receiver classes.
 */
class PluginReceivers {

    abstract class AbstractPluginReceiver : BroadcastReceiver() {
        @Synchronized override fun onReceive(context: Context, intent: Intent) {
            Logger.d("onReceive: " + this.javaClass.simpleName)

            val serviceIntent = MediaPluginIntent(intent)
            val c = this.javaClass
            serviceIntent.putExtra(AbstractPluginService.RECEIVED_CLASS_NAME, c.name)

            val propertyData = serviceIntent.propertyData ?: return
            val pref = PreferenceManager.getDefaultSharedPreferences(context)

            if (this is EventScrobbleReceiver ||
                this is EventNowPlayingReceiver ||
                this is ExecuteLoveReceiver ||
                this is ExecuteUnLoveReceiver) {
                // checks
                if (!serviceIntent.hasCategory(PluginTypeCategory.TYPE_POST_MESSAGE)) {
                    return
                }
                if (this is EventNowPlayingReceiver && !pref.getBoolean(context.getString(R.string.prefkey_now_playing_enabled), true)) {
                    return
                }
                if (this is EventScrobbleReceiver && !pref.getBoolean(context.getString(R.string.prefkey_scrobble_enabled), true)) {
                    return
                }
                if (propertyData.isMediaEmpty) {
                    AppUtils.showToast(context, R.string.message_no_media)
                    return
                }
                if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                    return
                }
                if (pref.getString(context.getString(R.string.prefkey_auth_username), "").isNullOrEmpty()) {
                    AppUtils.showToast(context, R.string.message_account_not_auth)
                    return
                }

                // service
                serviceIntent.setClass(context, PluginPostService::class.java)
            } else if (this is EventGetAlbumArtReceiver || this is ExecuteGetAlbumArtReceiver) {
                // check
                if (!serviceIntent.hasCategory(PluginTypeCategory.TYPE_GET_ALBUM_ART)) {
                    AppUtils.sendResult(context, serviceIntent)
                    return
                }
                if (propertyData.isMediaEmpty) {
                    AppUtils.showToast(context, R.string.message_no_media)
                    AppUtils.sendResult(context, serviceIntent)
                    return
                }
                if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                    AppUtils.sendResult(context, serviceIntent)
                    return
                }
                val operation = try { PluginOperationCategory.valueOf(pref.getString(context.getString(R.string.prefkey_event_get_album_art_operation), "")) } catch (ignore : Exception) { null }
                if (!serviceIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) && !serviceIntent.hasCategory(operation)) {
                    AppUtils.sendResult(context, serviceIntent)
                    return
                }

                // service
                serviceIntent.setClass(context, PluginGetAlbumArtService::class.java)
            } else if (this is EventGetPropertyReceiver || this is ExecuteGetPropertyReceiver) {
                if (!serviceIntent.hasCategory(PluginTypeCategory.TYPE_GET_PROPERTY)) {
                    AppUtils.sendResult(context, serviceIntent)
                    return
                }
                if (propertyData.isMediaEmpty) {
                    AppUtils.showToast(context, R.string.message_no_media)
                    AppUtils.sendResult(context, serviceIntent)
                    return
                }
                if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                    AppUtils.sendResult(context, serviceIntent)
                    return
                }
                val operation = pref.getString(context.getString(R.string.prefkey_event_get_property_operation), "")
                if (!serviceIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) && !serviceIntent.hasCategory(operation)) {
                    AppUtils.sendResult(context, serviceIntent)
                    return
                }

                // service
                serviceIntent.setClass(context, PluginGetPropertyService::class.java)
            } else if (this is ExecuteTrackPageReceiver || this is ExecuteLastfmSiteReceiver) {
                // check
                if (!serviceIntent.hasCategory(PluginTypeCategory.TYPE_RUN)) {
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
                serviceIntent.setClass(context, PluginRunService::class.java)
            }

            context.stopService(serviceIntent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
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
