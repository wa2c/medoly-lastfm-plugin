package com.wa2c.android.medoly.plugin.action.lastfm.plugin

import android.content.Context
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.softartdev.lastfm.Track
import com.wa2c.android.medoly.library.MediaProperty
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.util.*
import com.wa2c.android.prefs.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Unlove worker.
 */
class PluginPostUnloveWorker(private val context: Context, private val params: WorkerParameters) : Worker(context, params) {

    private val prefs: Prefs by lazy { Prefs(context) }

    override fun getForegroundInfoAsync(): ListenableFuture<ForegroundInfo> {
        return createForegroundFuture(context)
    }

    override fun doWork(): Result {
        val result = runBlocking {
            try {
                unlove()
                CommandResult.SUCCEEDED
            } catch (e: Exception) {
                logE(e)
                CommandResult.FAILED
            }
        }

        val succeeded = context.getString(R.string.message_unlove_success, params.inputData.getString(MediaProperty.TITLE.keyName))
        val failed = context.getString(R.string.message_unlove_failure)
        showMessage(prefs, result, succeeded, failed, params.isAutomaticallyAction)
        return Result.success()
    }

    /**
     * Unlove.
     */
    private suspend fun unlove(): CommandResult {
        return withContext(Dispatchers.IO) {
            val session = createSession(context, prefs).also {
                if (it?.username.isNullOrEmpty()) return@withContext CommandResult.AUTH_FAILED
            }

            val res = Track.unlove(params.mediaArtist, params.mediaTitle, session)
            return@withContext if (res.isSuccessful) {
                CommandResult.SUCCEEDED
            } else {
                CommandResult.FAILED
            }
        }
    }
}