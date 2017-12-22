package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.content.Intent
import android.text.TextUtils
import com.wa2c.android.medoly.library.*
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger
import de.umass.lastfm.Track


/**
 * Intent service.
 */
/**
 * Constructor.
 */
class PluginGetPropertyService : AbstractPluginService(PluginGetPropertyService::class.java.simpleName) {

    override fun onHandleIntent(intent: Intent?) {
        super.onHandleIntent(intent)

        if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_GET_PROPERTY)) {
            sendResult(null)
            return
        }

        try {
            val operation = sharedPreferences.getString(getString(R.string.prefkey_event_get_property_operation), "")
            if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) ||
                    pluginIntent.hasCategory(PluginOperationCategory.OPERATION_MEDIA_OPEN) && PluginOperationCategory.OPERATION_MEDIA_OPEN.name == operation ||
                    pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_START) && PluginOperationCategory.OPERATION_PLAY_START.name == operation) {
                getProperties()
            } else {
                sendResult(null)
            }
        } catch (e: Exception) {
            Logger.e(e)
            //AppUtils.showToast(this, R.string.error_app);
        }

    }

    /**
     * Get properties.
     */
    private fun getProperties() {
        var result: AbstractPluginService.CommandResult = AbstractPluginService.CommandResult.IGNORE
        var resultProperty: PropertyData? = null
        var resultExtra: ExtraData? = null
        try {
            if (propertyData.isMediaEmpty) {
                result = AbstractPluginService.CommandResult.NO_MEDIA
                return
            }

            val trackText = propertyData.getFirst(MediaProperty.TITLE)
            val artistText = propertyData.getFirst(MediaProperty.ARTIST)
            if (TextUtils.isEmpty(trackText) || TextUtils.isEmpty(artistText))
                return

            // Get info
            val track: Track
            if (session != null) {
                track = Track.getInfo(artistText, trackText, null, session?.username, session?.apiKey)
            } else {
                track = Track.getInfo(artistText, trackText, Token.getConsumerKey(context))
            }

            // Property data
            resultProperty = PropertyData()
            resultProperty.put(MediaProperty.TITLE, track.name)
            resultProperty.put(MediaProperty.ARTIST, track.artist)
            resultProperty.put(MediaProperty.ALBUM, track.album)
            resultProperty.put(MediaProperty.MUSICBRAINZ_TRACK_ID, track.mbid)
            resultProperty.put(MediaProperty.MUSICBRAINZ_ARTIST_ID, track.artistMbid)
            resultProperty.put(MediaProperty.MUSICBRAINZ_RELEASE_ID, track.albumMbid)

            // Extra data
            resultExtra = ExtraData()
            if (track.userPlaycount > 0)
                resultExtra.put(getString(R.string.label_extra_data_user_play_count), track.userPlaycount.toString())
            if (track.userPlaycount > 0)
                resultExtra.put(getString(R.string.label_extra_data_play_count), track.playcount.toString())
            if (track.listeners > 0)
                resultExtra.put(getString(R.string.label_extra_data_listener_count), track.listeners.toString())
            resultExtra.put(getString(R.string.label_extra_data_lastfm_track_url), track.url)
            result = AbstractPluginService.CommandResult.SUCCEEDED
        } catch (e: Exception) {
            Logger.e(e)
            resultProperty = null
            resultExtra = null
            result = AbstractPluginService.CommandResult.FAILED
        } finally {
            sendResult(resultProperty, resultExtra)
            if (result == AbstractPluginService.CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media)
            } else if (result == AbstractPluginService.CommandResult.SUCCEEDED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), false))
                    AppUtils.showToast(context, R.string.message_get_data_success)
            } else if (result == AbstractPluginService.CommandResult.FAILED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
                    AppUtils.showToast(context, R.string.message_get_data_failure)
            }
        }
    }

}
