package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.content.Intent
import android.text.TextUtils
import com.wa2c.android.medoly.library.MediaProperty
import com.wa2c.android.medoly.library.PluginOperationCategory
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger
import de.umass.lastfm.Session
import de.umass.lastfm.Track
import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult
import java.util.*


/**
 * Intent service.
 */
class PluginPostService : AbstractPluginService(PluginPostService::class.java.simpleName) {

    override fun onHandleIntent(intent: Intent?) {
        super.onHandleIntent(intent)

        try {
            if (receivedClassName == PluginReceivers.EventNowPlayingReceiver::class.java.name) {
                // Now Playing
                updateNowPlaying(session)
            } else if (receivedClassName == PluginReceivers.EventScrobbleReceiver::class.java.name) {
                // Scrobble
                scrobble(session)
            } else if (receivedClassName == PluginReceivers.ExecuteLoveReceiver::class.java.name) {
                // Love
                love(session)
            } else if (receivedClassName == PluginReceivers.ExecuteUnLoveReceiver::class.java.name) {
                // Unlove
                unlove(session)
            }
        } catch (e: Exception) {
            Logger.e(e)
            //AppUtils.showToast(this, R.string.error_app);
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
    private fun updateNowPlaying(session: Session?) {
        var result = CommandResult.IGNORE
        try {
            if (session == null) {
                result = CommandResult.AUTH_FAILED
                return
            }

            val scrobbleData = createScrobbleData()
            val scrobbleResult = Track.updateNowPlaying(scrobbleData, session)
            result = if (scrobbleResult.isSuccessful)
                CommandResult.SUCCEEDED
            else
                CommandResult.FAILED
        } catch (e: Exception) {
            Logger.e(e)
            result = CommandResult.FAILED
        } finally {
            if (result == CommandResult.AUTH_FAILED) {
//                AppUtils.showToast(context, R.string.message_account_not_auth)
            } else if (result == CommandResult.SUCCEEDED) {
//                if (preferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), true))
//                    AppUtils.showToast(context, R.string.message_post_success)
            } else if (result == CommandResult.FAILED) {
//                if (preferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
//                    AppUtils.showToast(context, R.string.message_post_failure)
            }
        }
    }

    /**
     * Scrobble.
     * @param session The session.
     */
    private fun scrobble(session: Session?) {
        var result = CommandResult.IGNORE
        try {
            // Check previous media
            val mediaUriText = propertyData.mediaUri.toString()
            val previousMediaUri = preferences.getString(AbstractPluginService.PREFKEY_PREVIOUS_MEDIA_URI, "")
            val previousMediaEnabled = preferences.getBoolean(context.getString(R.string.prefkey_previous_media_enabled), false)
            if (!previousMediaEnabled && !TextUtils.isEmpty(mediaUriText) && !TextUtils.isEmpty(previousMediaUri) && mediaUriText == previousMediaUri) {
                return
            }
            preferences.edit().putString(AbstractPluginService.PREFKEY_PREVIOUS_MEDIA_URI, mediaUriText).apply()

            // create scrobble data
            val scrobbleData = createScrobbleData()

            // create scrobble list data
            var dataList: MutableList<ScrobbleData> = ArrayList()
            val dataArray = AppUtils.loadObject<Array<ScrobbleData>>(context, context.getString(R.string.prefkey_unsent_scrobble_data))
            if (dataArray != null) dataList.addAll(Arrays.asList(*dataArray)) // load unsent data
            dataList.add(scrobbleData)

            // send if session is not null
            if (session != null) {
                val resultList = ArrayList<ScrobbleResult>(dataList.size)
                val maxSize = 50
                var from = 0
                var cancelSending = true
                while (from < dataList.size) {
                    val to = Math.min(from + maxSize, dataList.size)
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

                result = if (dataList.size == 0)
                    CommandResult.SUCCEEDED
                else
                    CommandResult.FAILED
            } else {
                result = CommandResult.AUTH_FAILED
            }

            val notSave = preferences.getBoolean(context.getString(R.string.prefkey_unsent_scrobble_not_save), false)

            // not save (leave exists data)
            if (notSave) {
                dataList.remove(scrobbleData)
            }

            // truncate to limit
            val maxDefault = context.resources.getInteger(R.integer.pref_default_unsent_max)
            val unsentMaxString = preferences.getString(context.getString(R.string.prefkey_unsent_max), maxDefault.toString())
            val unsentMax = try { Integer.parseInt(unsentMaxString) } catch (e: Exception) { maxDefault }
            if (unsentMax > 0 && dataList.size > unsentMax) {
                dataList = dataList.subList(dataList.size - unsentMax, dataList.size)
            }

            // save unsent data
            AppUtils.saveObject(context, context.getString(R.string.prefkey_unsent_scrobble_data), dataList.toTypedArray())
        } catch (e: Exception) {
            Logger.e(e)
            result = CommandResult.FAILED
        } finally {
            if (result == CommandResult.AUTH_FAILED) {
                AppUtils.showToast(context, R.string.message_account_not_auth)
            } else if (result == CommandResult.SUCCEEDED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || preferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), false))
                    AppUtils.showToast(context, getString(R.string.message_post_success, propertyData.getFirst(MediaProperty.TITLE)))
            } else if (result == CommandResult.FAILED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || preferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
                    AppUtils.showToast(context, R.string.message_post_failure)
            }
        }
    }

    /**
     * Love.
     * @param session The session.
     */
    private fun love(session: Session?) {
        var result = CommandResult.IGNORE
        try {
            if (session == null) {
                result = CommandResult.AUTH_FAILED
                return
            }

            val res = Track.love(propertyData.getFirst(MediaProperty.ARTIST), propertyData.getFirst(MediaProperty.TITLE), session)
            result = if (res.isSuccessful)
                CommandResult.SUCCEEDED
            else
                CommandResult.FAILED
        } catch (e: Exception) {
            Logger.e(e)
            result = CommandResult.FAILED
        } finally {
            if (result == CommandResult.AUTH_FAILED) {
                AppUtils.showToast(context, R.string.message_account_not_auth)
            } else if (result == CommandResult.SUCCEEDED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || preferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), false))
                    AppUtils.showToast(context, context.getString(R.string.message_love_success, propertyData.getFirst(MediaProperty.TITLE)))
            } else if (result == CommandResult.FAILED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || preferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
                    AppUtils.showToast(context, R.string.message_love_failure)
            }
        }
    }

    /**
     * UnLove.
     * @param session The session.
     */
    private fun unlove(session: Session?) {
        var result = CommandResult.IGNORE
        try {
            if (session == null) {
                result = CommandResult.AUTH_FAILED
                return
            }

            val res = Track.unlove(propertyData.getFirst(MediaProperty.ARTIST), propertyData.getFirst(MediaProperty.TITLE), session)
            result = if (res.isSuccessful)
                CommandResult.SUCCEEDED
            else
                CommandResult.FAILED
        } catch (e: Exception) {
            Logger.e(e)
            result = CommandResult.FAILED
        } finally {
            if (result == CommandResult.AUTH_FAILED) {
                AppUtils.showToast(context, R.string.message_account_not_auth)
            } else if (result == CommandResult.SUCCEEDED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || preferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), false))
                    AppUtils.showToast(context, context.getString(R.string.message_unlove_success, propertyData.getFirst(MediaProperty.TITLE)))
            } else if (result == CommandResult.FAILED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || preferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
                    AppUtils.showToast(context, R.string.message_unlove_failure)
            }
        }
    }

}
