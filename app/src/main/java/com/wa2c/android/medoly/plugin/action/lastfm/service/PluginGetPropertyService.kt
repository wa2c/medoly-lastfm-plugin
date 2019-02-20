package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.content.Intent
import com.wa2c.android.medoly.library.ExtraData
import com.wa2c.android.medoly.library.MediaProperty
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import de.umass.lastfm.Track
import timber.log.Timber


/**
 * Get album art plugin service.
 */
class PluginGetPropertyService : AbstractPluginService(PluginGetPropertyService::class.java.simpleName) {

    override fun onHandleIntent(intent: Intent?) {
        super.onHandleIntent(intent)

        try {
            getProperties()
        } catch (e: Exception) {
            Timber.e(e)
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
            resultProperty[MediaProperty.TITLE] = track.name
            resultProperty[MediaProperty.ARTIST] = track.artist
            resultProperty[MediaProperty.ALBUM] = track.album
            resultProperty[MediaProperty.MUSICBRAINZ_TRACK_ID] = track.mbid
            resultProperty[MediaProperty.MUSICBRAINZ_ARTIST_ID] = track.artistMbid
            resultProperty[MediaProperty.MUSICBRAINZ_RELEASE_ID] = track.albumMbid

            // Extra data
            resultExtra = ExtraData()
            if (track.userPlaycount > 0)
                resultExtra[getString(R.string.label_extra_data_user_play_count)] = track.userPlaycount.toString()
            if (track.playcount > 0)
                resultExtra[getString(R.string.label_extra_data_play_count)] = track.playcount.toString()
            if (track.listeners > 0)
                resultExtra[getString(R.string.label_extra_data_listener_count)] = track.listeners.toString()
            resultExtra[getString(R.string.label_extra_data_lastfm_track_url)] = track.url
            result = CommandResult.SUCCEEDED
        } catch (e: Exception) {
            Timber.e(e)
            resultProperty = null
            resultExtra = null
            result = CommandResult.FAILED
        } finally {
            sendResult(resultProperty, resultExtra)

            // show message
            val succeeded = getString(R.string.message_get_property_success)
            val failed = getString(R.string.message_get_property_failure)
            showMessage(result, succeeded, failed)
        }
    }

}
