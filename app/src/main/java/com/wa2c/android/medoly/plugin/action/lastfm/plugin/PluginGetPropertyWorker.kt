package com.wa2c.android.medoly.plugin.action.lastfm.plugin

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
import com.wa2c.android.medoly.plugin.action.lastfm.util.*
import com.wa2c.android.prefs.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Get property worker.
 */
class PluginGetPropertyWorker(private val context: Context, private val params: WorkerParameters) : Worker(context, params) {

    private val prefs: Prefs by lazy { Prefs(context) }

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
        showMessage(prefs, result, succeeded, failed, params.isAutomaticallyAction)
        return Result.success()
    }

    /**
     * Get properties.
     */
    private suspend fun getProperties(): MediaPluginIntent {
        return withContext(Dispatchers.IO) {
            val track = Track.getInfo(params.mediaArtist, params.mediaTitle, Token.getConsumerKey())
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