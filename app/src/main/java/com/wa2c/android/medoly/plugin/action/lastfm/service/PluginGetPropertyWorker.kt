package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.softartdev.lastfm.Track
import com.wa2c.android.medoly.library.ExtraData
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.MediaProperty
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import com.wa2c.android.medoly.plugin.action.lastfm.util.isAutomaticallyAction
import com.wa2c.android.medoly.plugin.action.lastfm.util.logE
import com.wa2c.android.medoly.plugin.action.lastfm.util.showMessage
import com.wa2c.android.medoly.plugin.action.lastfm.util.toPluginIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Get property worker.
 */
class PluginGetPropertyWorker(private val context: Context, private val params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val result = runBlocking {
            try {
                getProperties().let { context.sendBroadcast(it) }
                CommandResult.SUCCEEDED
            } catch (e: Exception) {
                logE(e)
                CommandResult.FAILED
            }
        }

        val succeeded = context.getString(R.string.message_get_property_success)
        val failed = context.getString(R.string.message_get_property_failure)
        context.showMessage(result, succeeded, failed, params.isAutomaticallyAction)
        return Result.success()
    }

    /**
     * Get properties.
     */
    private suspend fun getProperties(): MediaPluginIntent {
        return withContext(Dispatchers.IO) {
            val trackText = params.inputData.getString(MediaProperty.TITLE.keyName)
            val artistText = params.inputData.getString(MediaProperty.ARTIST.keyName)
            val track = Track.getInfo(artistText, trackText, Token.getConsumerKey())
            // Track.getInfo(artistText, trackText, null, session?.username, session?.apiKey)

            // Property data
            val resultProperty = PropertyData().apply {
                this[MediaProperty.TITLE] = track.name
                this[MediaProperty.ARTIST] = track.artist
                this[MediaProperty.ALBUM] = track.album
                this[MediaProperty.MUSICBRAINZ_TRACK_ID] = track.mbid
                this[MediaProperty.MUSICBRAINZ_ARTIST_ID] = track.artistMbid
                this[MediaProperty.MUSICBRAINZ_RELEASE_ID] = track.albumMbid
            }

            // Extra data
            val resultExtra = ExtraData().apply {
                // if (track.userPlaycount > 0)
                //     resultExtra[getString(R.string.label_extra_data_user_play_count)] = track.userPlaycount.toString()
                if (track.playcount > 0) {
                    this[context.getString(R.string.label_extra_data_play_count)] = track.playcount.toString()
                }
                if (track.listeners > 0) {
                    this[context.getString(R.string.label_extra_data_listener_count)] = track.listeners.toString()
                }
                this[context.getString(R.string.label_extra_data_lastfm_track_url)] = track.url
            }

            params.inputData.toPluginIntent(resultProperty, resultExtra)
        }
    }
}