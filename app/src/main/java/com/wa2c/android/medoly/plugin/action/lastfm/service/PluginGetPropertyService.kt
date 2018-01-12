package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.content.Intent
import com.wa2c.android.medoly.library.ExtraData
import com.wa2c.android.medoly.library.MediaProperty
import com.wa2c.android.medoly.library.PluginOperationCategory
import com.wa2c.android.medoly.library.PropertyData
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

        try {
            getProperties()
        } catch (e: Exception) {
            Logger.e(e)
            //AppUtils.showToast(this, R.string.error_app);
        }
    }

    /**
     * Get properties.
     */
    private fun getProperties() {
        var result = CommandResult.IGNORE
        var resultProperty: PropertyData? = null
        var resultExtra: ExtraData? = null
        try {
            val trackText = propertyData.getFirst(MediaProperty.TITLE)
            val artistText = propertyData.getFirst(MediaProperty.ARTIST)

            val track = if (session != null) {
                Track.getInfo(artistText, trackText, null, session?.username, session?.apiKey)
            } else {
                Track.getInfo(artistText, trackText, Token.getConsumerKey(context))
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
            result = CommandResult.SUCCEEDED
        } catch (e: Exception) {
            Logger.e(e)
            resultProperty = null
            resultExtra = null
            result = CommandResult.FAILED
        } finally {
            sendResult(resultProperty, resultExtra)
            if (result == CommandResult.SUCCEEDED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || prefs.getBoolean(R.string.prefkey_post_success_message_show))
                    AppUtils.showToast(context, R.string.message_get_data_success)
            } else if (result == CommandResult.FAILED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || prefs.getBoolean(R.string.prefkey_post_failure_message_show, true))
                    AppUtils.showToast(context, R.string.message_get_data_failure)
            }
        }
    }

}
