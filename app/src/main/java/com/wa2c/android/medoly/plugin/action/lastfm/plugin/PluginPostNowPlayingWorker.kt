package com.wa2c.android.medoly.plugin.action.lastfm.plugin

import android.content.Context
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.softartdev.lastfm.Track
import com.wa2c.android.medoly.plugin.action.lastfm.util.createForegroundFuture
import com.wa2c.android.medoly.plugin.action.lastfm.util.createSession
import com.wa2c.android.medoly.plugin.action.lastfm.util.logE
import com.wa2c.android.medoly.plugin.action.lastfm.util.toScrobbleData
import com.wa2c.android.prefs.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


/**
 * NowPlaying worker.
 */
class PluginPostNowPlayingWorker(private val context: Context, private val params: WorkerParameters) : Worker(context, params) {

    private val prefs: Prefs by lazy { Prefs(context) }

    override fun getForegroundInfoAsync(): ListenableFuture<ForegroundInfo> {
        return createForegroundFuture(context)
     }

    override fun doWork(): Result {
        runBlocking {
            try {
                updateNowPlaying()
                CommandResult.SUCCEEDED
            } catch (e: Exception) {
                logE(e)
                CommandResult.FAILED
            }
        }
        return Result.success()
    }

    /**
     * Update now playing.
     */
    private suspend fun updateNowPlaying(): CommandResult {
        return withContext(Dispatchers.IO) {
            val session = createSession(context, prefs).also {
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