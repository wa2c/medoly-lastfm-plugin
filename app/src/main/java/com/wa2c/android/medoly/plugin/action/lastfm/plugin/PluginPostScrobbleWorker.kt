package com.wa2c.android.medoly.plugin.action.lastfm.plugin

import android.content.Context
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
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
 * Scrobble worker.
 */
class PluginPostScrobbleWorker(private val context: Context, private val params: WorkerParameters) : Worker(context, params) {

    private val prefs: Prefs by lazy { Prefs(context) }

    override fun getForegroundInfoAsync(): ListenableFuture<ForegroundInfo> {
        return createForegroundFuture(context)
    }

    override fun doWork(): Result {
        val result = runBlocking {
            try {
                scrobble()
                CommandResult.SUCCEEDED
            } catch (e: Exception) {
                logE(e)
                CommandResult.FAILED
            }
        }

        val succeeded = context.getString(R.string.message_post_success, params.mediaTitle)
        val failed = context.getString(R.string.message_post_failure)
        showMessage(prefs, result, succeeded, failed, params.isAutomaticallyAction)
        return Result.success()
    }

    /**
     * Scrobble.
     */
    private suspend fun scrobble(): CommandResult {
        return withContext(Dispatchers.IO) {
            val session = createSession(context, prefs).also {
                if (it?.username.isNullOrEmpty()) return@withContext CommandResult.AUTH_FAILED
            }
            val scrobbleData = params.inputData.toScrobbleData()

            // create scrobble list data
            var dataList: MutableList<ScrobbleData> = ArrayList()
            val dataArray =
                prefs.getObjectOrNull<Array<ScrobbleData>>(R.string.prefkey_unsent_scrobble_data)
            if (dataArray != null)
                dataList.addAll(listOf(*dataArray)) // load unsent data
            dataList.add(scrobbleData)

            // send if session is not null

            val resultList = ArrayList<ScrobbleResult>(dataList.size)
            val maxSize = 50
            var from = 0
            var cancelSending = true
            while (from < dataList.size) {
                val to = min(from + maxSize, dataList.size)
                val subDataList = dataList.subList(from, to)
                resultList.addAll(Track.scrobble(subDataList, session))
                for (r in resultList) {
                    if (r.isSuccessful) {
                        cancelSending = false
                        break
                    }
                }
                if (cancelSending) break // cancel follow process if failed
                from += maxSize
            }

            // delete succeeded data
            for (i in resultList.indices.reversed()) {
                val scrobbleResult = resultList[i]
                if (scrobbleResult.isSuccessful) {
                    dataList.removeAt(i)
                }
            }

            val result = if (dataList.size == 0)
                CommandResult.SUCCEEDED
            else
                CommandResult.SAVED

            val notSave = prefs.getBoolean(R.string.prefkey_unsent_scrobble_not_save)

            // not save (leave exists data)
            if (notSave) {
                dataList.remove(scrobbleData)
            }

            // truncate to limit
            val maxDefault = context.resources.getInteger(R.integer.pref_default_unsent_max)
            val unsentMaxString =
                prefs.getString(R.string.prefkey_unsent_max, maxDefault.toString())
            val unsentMax = try {
                Integer.parseInt(unsentMaxString)
            } catch (e: Exception) {
                maxDefault
            }
            if (unsentMax > 0 && dataList.size > unsentMax) {
                dataList = dataList.subList(dataList.size - unsentMax, dataList.size)
            }

            // save unsent data
            prefs.putObject(R.string.prefkey_unsent_scrobble_data, dataList.toTypedArray())
            prefs.putString(PREFKEY_PREVIOUS_MEDIA_URI, params.inputData.getString(MediaProperty.DATA_URI.keyName))
            result
        }
    }

    companion object {
        /** Previous data key.  */
        const val PREFKEY_PREVIOUS_MEDIA_URI = "previous_media_uri"
    }
}