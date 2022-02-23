package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.softartdev.lastfm.Track
import com.softartdev.lastfm.scrobble.ScrobbleData
import com.softartdev.lastfm.scrobble.ScrobbleResult
import com.wa2c.android.medoly.library.MediaProperty
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.util.*
import com.wa2c.android.prefs.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * NowPlaying worker.
 */
class PluginPostNowPlayingWorker(private val context: Context, private val params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val result = runBlocking {
            try {
                updateNowPlaying()
                CommandResult.SUCCEEDED
            } catch (e: Exception) {
                logE(e)
                CommandResult.FAILED
            }
        }

        val succeeded = context.getString(R.string.message_post_success, params.mediaTitle)
        val failed = context.getString(R.string.message_post_failure)
        context.showMessage(result, succeeded, failed, params.isAutomaticallyAction)
        return Result.success()
    }

    /**
     * Update now playing.
     */
    private suspend fun updateNowPlaying(): CommandResult {
        return withContext(Dispatchers.IO) {
            val session = createSession(context).also {
                if (it?.username.isNullOrEmpty()) return@withContext CommandResult.AUTH_FAILED
            }
            val scrobbleData = params.inputData.toScrobbleData()
            val scrobbleResult = Track.updateNowPlaying(scrobbleData, session)
            if (scrobbleResult.isSuccessful) {
                CommandResult.SUCCEEDED
            } else {
                CommandResult.FAILED
            }
        }
    }

}