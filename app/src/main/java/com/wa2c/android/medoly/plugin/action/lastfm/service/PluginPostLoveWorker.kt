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
import com.wa2c.android.medoly.plugin.action.lastfm.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Love worker.
 */
class PluginPostLoveWorker(private val context: Context, private val params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val result = runBlocking {
            try {
                love()
                CommandResult.SUCCEEDED
            } catch (e: Exception) {
                logE(e)
                CommandResult.FAILED
            }
        }

        val succeeded = context.getString(R.string.message_love_success, params.mediaTitle)
        val failed = context.getString(R.string.message_love_failure)
        context.showMessage(result, succeeded, failed, params.isAutomaticallyAction)
        return Result.success()
    }

    /**
     * Love.
     */
    private suspend fun love(): CommandResult {
        return withContext(Dispatchers.IO) {
            val session = createSession(context).also {
                if (it?.username.isNullOrEmpty()) return@withContext CommandResult.AUTH_FAILED
            }

            val res = Track.love(params.mediaArtist, params.mediaTitle, session)
            return@withContext if (res.isSuccessful) {
                CommandResult.SUCCEEDED
            } else {
                CommandResult.FAILED
            }
        }
    }
}