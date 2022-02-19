package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.content.Intent
import com.softartdev.lastfm.Session
import com.softartdev.lastfm.Track
import com.softartdev.lastfm.scrobble.ScrobbleData
import com.softartdev.lastfm.scrobble.ScrobbleResult
import com.wa2c.android.medoly.library.MediaProperty
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.util.logD
import com.wa2c.android.medoly.plugin.action.lastfm.util.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.math.min


/**
 * Post plugin service.
 */
class PluginPostService : AbstractPluginService(PluginPostService::class.java.simpleName) {

    override fun runPlugin() {
        when (receivedClassName) {
            PluginReceivers.EventNowPlayingReceiver::class.java.name -> {
                // Now Playing
                runBlocking { updateNowPlaying(session) }
            }
            PluginReceivers.EventScrobbleReceiver::class.java.name -> {
                // Scrobble
                val result = runBlocking { scrobble(session) }
                val succeeded = getString(R.string.message_post_success, propertyData.getFirst(MediaProperty.TITLE))
                val failed = getString(R.string.message_post_failure)
                showMessage(result, succeeded, failed)
            }
            PluginReceivers.ExecuteLoveReceiver::class.java.name -> {
                // Love
                val result = runBlocking { love(session) }
                val succeeded = getString(R.string.message_love_success, propertyData.getFirst(MediaProperty.TITLE))
                val failed = getString(R.string.message_love_failure)
                showMessage(result, succeeded, failed)
            }
            PluginReceivers.ExecuteUnLoveReceiver::class.java.name -> {
                // Unlove
                val result = runBlocking { unlove(session) }
                val succeeded = getString(R.string.message_unlove_success, propertyData.getFirst(MediaProperty.TITLE))
                val failed = getString(R.string.message_unlove_failure)
                showMessage(result, succeeded, failed)
            }
        }
    }

    /**
     * Crate scrobble data.
     * @return The scrobble data.
     */
    private fun createScrobbleData(): ScrobbleData {
        val newData = ScrobbleData()
        newData.musicBrainzId = propertyData.getFirst(MediaProperty.MUSICBRAINZ_TRACK_ID)
        newData.track = propertyData.getFirst(MediaProperty.TITLE)
        newData.artist = propertyData.getFirst(MediaProperty.ARTIST)
        newData.albumArtist = propertyData.getFirst(MediaProperty.ALBUM_ARTIST)
        newData.album = propertyData.getFirst(MediaProperty.ALBUM)

        try {
            newData.duration = ((propertyData.getFirst(MediaProperty.DURATION)?.toLong() ?: 0) / 1000).toInt()
        } catch (ignore: NumberFormatException) {
        } catch (ignore: NullPointerException) {
        }

        try {
            newData.trackNumber = propertyData.getFirst(MediaProperty.TRACK)?.toInt() ?: 0
        } catch (ignore: NumberFormatException) {
        } catch (ignore: NullPointerException) {
        }

        newData.timestamp = (System.currentTimeMillis() / 1000).toInt()
        return newData
    }

    /**
     * Update now playing.
     * @param session The session.
     */
    private suspend fun updateNowPlaying(session: Session?): CommandResult {
        return withContext(Dispatchers.IO) {
            try {
                if (username.isNullOrEmpty()) {
                    return@withContext CommandResult.AUTH_FAILED

                } else if (session == null) {
                    return@withContext CommandResult.FAILED
                }

                val scrobbleData = createScrobbleData()
                val scrobbleResult = Track.updateNowPlaying(scrobbleData, session)
                return@withContext if (scrobbleResult.isSuccessful)
                    CommandResult.SUCCEEDED
                else
                    CommandResult.FAILED
            } catch (e: Exception) {
                logE(e)
                return@withContext CommandResult.FAILED
            }
        }
    }

    /**
     * Scrobble.
     * @param session The session.
     */
    private suspend fun scrobble(session: Session?): CommandResult {
        return withContext(Dispatchers.IO) {
            try {
                // create scrobble data
                val scrobbleData = createScrobbleData()

                // create scrobble list data
                var dataList: MutableList<ScrobbleData> = ArrayList()
                val dataArray =
                    prefs.getObjectOrNull<Array<ScrobbleData>>(R.string.prefkey_unsent_scrobble_data)
                if (dataArray != null)
                    dataList.addAll(listOf(*dataArray)) // load unsent data
                dataList.add(scrobbleData)

                // send if session is not null
                val result = if (session != null) {
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

                     if (dataList.size == 0)
                        CommandResult.SUCCEEDED
                    else
                        CommandResult.SAVED
                } else {
                     if (username.isNullOrEmpty())
                        CommandResult.AUTH_FAILED
                    else
                        CommandResult.SAVED
                }

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
                return@withContext result
            } catch (e: Exception) {
                logE(e)
                return@withContext CommandResult.FAILED
            } finally {
                // save previous media
                prefs.putString(PREFKEY_PREVIOUS_MEDIA_URI, propertyData.mediaUri.toString())
            }
        }
    }

    /**
     * Love.
     * @param session The session.
     */
    private suspend fun love(session: Session?): CommandResult {
        return withContext(Dispatchers.IO) {
            try {
                if (username.isNullOrEmpty()) {
                    return@withContext CommandResult.AUTH_FAILED
                } else if (session == null) {
                    return@withContext CommandResult.FAILED
                }

                val res = Track.love(
                    propertyData.getFirst(MediaProperty.ARTIST),
                    propertyData.getFirst(MediaProperty.TITLE),
                    session
                )
                return@withContext if (res.isSuccessful)
                    CommandResult.SUCCEEDED
                else
                    CommandResult.FAILED
            } catch (e: Exception) {
                logE(e)
                return@withContext CommandResult.FAILED
            }
        }
    }

    /**
     * UnLove.
     * @param session The session.
     */
    private suspend fun unlove(session: Session?): CommandResult {
        return withContext(Dispatchers.IO) {
            try {
                if (username.isNullOrEmpty()) {
                    return@withContext CommandResult.AUTH_FAILED
                } else if (session == null) {
                    return@withContext CommandResult.FAILED
                }

                val res = Track.unlove(
                    propertyData.getFirst(MediaProperty.ARTIST),
                    propertyData.getFirst(MediaProperty.TITLE),
                    session
                )
                return@withContext if (res.isSuccessful)
                    CommandResult.SUCCEEDED
                else
                    CommandResult.FAILED
            } catch (e: Exception) {
                logE(e)
                return@withContext CommandResult.FAILED
            }
        }
    }

}
