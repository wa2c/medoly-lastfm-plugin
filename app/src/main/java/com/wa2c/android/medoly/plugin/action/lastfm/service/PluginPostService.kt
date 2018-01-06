package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.content.Intent
import android.text.TextUtils
import com.wa2c.android.medoly.library.MediaProperty
import com.wa2c.android.medoly.library.PluginOperationCategory
import com.wa2c.android.medoly.library.PluginTypeCategory
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
/**
 * Constructor.
 */
class PluginPostService : AbstractPluginService(PluginPostService::class.java.simpleName) {

    override fun onHandleIntent(intent: Intent?) {
        super.onHandleIntent(intent)

        if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_POST_MESSAGE)) {
            return
        }

        try {
            if (receivedClassName == PluginReceivers.EventNowPlayingReceiver::class.java.name) {
                // Update Now Playing (event: OPERATION_PLAY_START)
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_START) && sharedPreferences.getBoolean(getString(R.string.prefkey_now_playing_enabled), true)) {
                    updateNowPlaying(session)
                }
            } else if (receivedClassName == PluginReceivers.EventScrobbleReceiver::class.java.name) {
                // Scrobble (event: OPERATION_PLAY_NOW)
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_NOW) && sharedPreferences.getBoolean(getString(R.string.prefkey_scrobble_enabled), true)) {
                    scrobble(session)
                }
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
            newData.duration = (propertyData.getFirst(MediaProperty.DURATION)?.toLong() ?: 0 / 1000).toInt()
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
        var result: AbstractPluginService.CommandResult = AbstractPluginService.CommandResult.IGNORE
        try {
            if (session == null) {
                result = AbstractPluginService.CommandResult.AUTH_FAILED
                return
            }

            if (propertyData.isMediaEmpty) {
                result = AbstractPluginService.CommandResult.NO_MEDIA
                return
            }

            // create scrobble data
            val scrobbleData = createScrobbleData()
            if (TextUtils.isEmpty(scrobbleData.track) || TextUtils.isEmpty(scrobbleData.artist))
                return

            val scrobbleResult = Track.updateNowPlaying(scrobbleData, session)
            result = if (scrobbleResult.isSuccessful)
                AbstractPluginService.CommandResult.SUCCEEDED
            else
                AbstractPluginService.CommandResult.FAILED
        } catch (e: Exception) {
            Logger.e(e)
            result = AbstractPluginService.CommandResult.FAILED
        } finally {
            if (result == AbstractPluginService.CommandResult.AUTH_FAILED) {
                AppUtils.showToast(context, R.string.message_account_not_auth)
            } else if (result == AbstractPluginService.CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media)
                //            } else if (result == CommandResult.SUCCEEDED) {
                //                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), true))
                //                    AppUtils.showToast(context, R.string.message_post_success);
                //            } else if (result == CommandResult.FAILED) {
                //                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
                //                    AppUtils.showToast(context, R.string.message_post_failure);
            }
        }
    }

    /**
     * Scrobble.
     * @param session The session.
     */
    private fun scrobble(session: Session?) {
        var result: AbstractPluginService.CommandResult = AbstractPluginService.CommandResult.IGNORE
        try {
            if (propertyData.isMediaEmpty) {
                result = AbstractPluginService.CommandResult.NO_MEDIA
                return
            }

            // Check previous media
            val mediaUriText = propertyData.mediaUri.toString()
            val previousMediaUri = sharedPreferences.getString(AbstractPluginService.PREFKEY_PREVIOUS_MEDIA_URI, "")
            val previousMediaEnabled = sharedPreferences.getBoolean(context.getString(R.string.prefkey_previous_media_enabled), false)
            if (!previousMediaEnabled && !TextUtils.isEmpty(mediaUriText) && !TextUtils.isEmpty(previousMediaUri) && mediaUriText == previousMediaUri) {
                return
            }
            sharedPreferences.edit().putString(AbstractPluginService.PREFKEY_PREVIOUS_MEDIA_URI, mediaUriText).apply()

            if (session == null) {
                result = AbstractPluginService.CommandResult.AUTH_FAILED
                return
            }

            // create scrobble data
            val scrobbleData = createScrobbleData()
            if (TextUtils.isEmpty(scrobbleData.track) || TextUtils.isEmpty(scrobbleData.artist))
                return

            // create scrobble list data
            var dataList: MutableList<ScrobbleData> = ArrayList()
            val dataArray = AppUtils.loadObject<Array<ScrobbleData>>(context, context.getString(R.string.prefkey_unsent_scrobble_data))
            if (dataArray != null) dataList.addAll(Arrays.asList(*dataArray)) // load unsent data
            dataList.add(scrobbleData)

            // send if session is not null
            if (!session.isSubscriber) {
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
                    AbstractPluginService.CommandResult.SUCCEEDED
                else
                    AbstractPluginService.CommandResult.FAILED
            }

            val notSave = sharedPreferences.getBoolean(context.getString(R.string.prefkey_unsent_scrobble_not_save), false)

            // not save (leave exists data)
            if (notSave) {
                dataList.remove(scrobbleData)
            }

            // truncate to limit
            val maxDefault = context.resources.getInteger(R.integer.pref_default_unsent_max)
            val unsentMaxString = sharedPreferences.getString(context.getString(R.string.prefkey_unsent_max), maxDefault.toString())
            val unsentMax = try {
                Integer.parseInt(unsentMaxString)
            } catch (e: Exception) {
                maxDefault
            }

            if (unsentMax > 0 && dataList.size > unsentMax) {
                dataList = dataList.subList(dataList.size - unsentMax, dataList.size)
            }

            // save unsent data
            AppUtils.saveObject(context, context.getString(R.string.prefkey_unsent_scrobble_data), dataList.toTypedArray())

            if (result == AbstractPluginService.CommandResult.IGNORE) {
                result = if (!session.isSubscriber)
                    AbstractPluginService.CommandResult.AUTH_FAILED
                else if (!notSave)
                    AbstractPluginService.CommandResult.SAVED
                else
                    AbstractPluginService.CommandResult.FAILED
            }
        } catch (e: Exception) {
            Logger.e(e)
            result = AbstractPluginService.CommandResult.FAILED
        } finally {
            if (result == AbstractPluginService.CommandResult.AUTH_FAILED) {
                AppUtils.showToast(context, R.string.message_account_not_auth)
            } else if (result == AbstractPluginService.CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media)
            } else if (result == AbstractPluginService.CommandResult.SUCCEEDED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), false))
                    AppUtils.showToast(context, getString(R.string.message_post_success, propertyData.getFirst(MediaProperty.TITLE)))
            } else if (result == AbstractPluginService.CommandResult.FAILED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
                    AppUtils.showToast(context, R.string.message_post_failure)
            }
        }
    }

    /**
     * Love.
     * @param session The session.
     */
    private fun love(session: Session?) {
        var result: AbstractPluginService.CommandResult = AbstractPluginService.CommandResult.IGNORE
        try {
            if (session == null) {
                result = AbstractPluginService.CommandResult.AUTH_FAILED
                return
            }

            if (propertyData.isMediaEmpty) {
                result = AbstractPluginService.CommandResult.NO_MEDIA
                return
            }

            val track = propertyData.getFirst(MediaProperty.TITLE)
            val artist = propertyData.getFirst(MediaProperty.ARTIST)
            if (TextUtils.isEmpty(track) || TextUtils.isEmpty(artist))
                return

            val res = Track.love(artist, track, session)
            result = if (res.isSuccessful)
                AbstractPluginService.CommandResult.SUCCEEDED
            else
                AbstractPluginService.CommandResult.FAILED
        } catch (e: Exception) {
            Logger.e(e)
            result = AbstractPluginService.CommandResult.FAILED
        } finally {
            if (result == AbstractPluginService.CommandResult.AUTH_FAILED) {
                AppUtils.showToast(context, R.string.message_account_not_auth)
            } else if (result == AbstractPluginService.CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media)
            } else if (result == AbstractPluginService.CommandResult.SUCCEEDED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), false))
                    AppUtils.showToast(context, context.getString(R.string.message_love_success, propertyData.getFirst(MediaProperty.TITLE)))
            } else if (result == AbstractPluginService.CommandResult.FAILED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
                    AppUtils.showToast(context, R.string.message_love_failure)
            }
        }
    }

    /**
     * UnLove.
     * @param session The session.
     */
    private fun unlove(session: Session?) {
        var result: AbstractPluginService.CommandResult = AbstractPluginService.CommandResult.IGNORE
        try {
            if (session == null) {
                result = AbstractPluginService.CommandResult.AUTH_FAILED
                return
            }

            if (propertyData.isMediaEmpty) {
                result = AbstractPluginService.CommandResult.NO_MEDIA
                return
            }

            val track = propertyData.getFirst(MediaProperty.TITLE)
            val artist = propertyData.getFirst(MediaProperty.ARTIST)
            if (TextUtils.isEmpty(track) || TextUtils.isEmpty(artist))
                return

            val res = Track.unlove(artist, track, session)
            result = if (res.isSuccessful)
                AbstractPluginService.CommandResult.SUCCEEDED
            else
                AbstractPluginService.CommandResult.FAILED
        } catch (e: Exception) {
            Logger.e(e)
            result = AbstractPluginService.CommandResult.FAILED
        } finally {
            if (result == AbstractPluginService.CommandResult.AUTH_FAILED) {
                AppUtils.showToast(context, R.string.message_account_not_auth)
            } else if (result == AbstractPluginService.CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media)
            } else if (result == AbstractPluginService.CommandResult.SUCCEEDED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), false))
                    AppUtils.showToast(context, context.getString(R.string.message_unlove_success, propertyData.getFirst(MediaProperty.TITLE)))
            } else if (result == AbstractPluginService.CommandResult.FAILED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
                    AppUtils.showToast(context, R.string.message_unlove_failure)
            }
        }
    }

}
