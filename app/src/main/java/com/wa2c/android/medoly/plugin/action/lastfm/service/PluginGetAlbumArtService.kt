package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import com.wa2c.android.medoly.library.*
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import java.util.*


/**
 * Intent service.
 */
/**
 * Constructor.
 */
class PluginGetAlbumArtService : AbstractPluginService(PluginGetAlbumArtService::class.java.simpleName) {

    override fun onHandleIntent(intent: Intent?) {
        super.onHandleIntent(intent)

        if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_GET_ALBUM_ART)) {
            sendResult(null)
            return
        }

        try {
            val operation = sharedPreferences.getString(getString(R.string.prefkey_event_get_album_art_operation), "")
            if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) ||
                    pluginIntent.hasCategory(PluginOperationCategory.OPERATION_MEDIA_OPEN) && PluginOperationCategory.OPERATION_MEDIA_OPEN.name == operation ||
                    pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_START) && PluginOperationCategory.OPERATION_PLAY_START.name == operation) {
                getAlbumArt()
            } else {
                sendResult(null)
            }
        } catch (e: Exception) {
            Logger.e(e)
            //AppUtils.showToast(this, R.string.error_app);
        }

    }

    /**
     * Get album art.
     */
    private fun getAlbumArt() {
        var result: AbstractPluginService.CommandResult = AbstractPluginService.CommandResult.IGNORE
        var resultProperty: PropertyData? = null
        try {
            if (propertyData.isMediaEmpty) {
                result = AbstractPluginService.CommandResult.NO_MEDIA
                return
            }

            // No property info
            val trackText = propertyData.getFirst(MediaProperty.TITLE)
            val albumText = propertyData.getFirst(MediaProperty.ALBUM)
            val artistText = propertyData.getFirst(MediaProperty.ARTIST)
            //            String trackMbidText = propertyData.getFirst(MediaProperty.MUSICBRAINZ_TRACK_ID);
            //            String albumMbidText = propertyData.getFirst(MediaProperty.MUSICBRAINZ_RELEASE_ID);
            //            String artistMbidText = propertyData.getFirst(MediaProperty.MUSICBRAINZ_ARTIST_ID);

            var remoteUri: String? = null
            var localUri: Uri? = null

            // Album image
            if (!TextUtils.isEmpty(artistText) && !TextUtils.isEmpty(albumText)) {
                val album = if (session != null) {
                    Album.getInfo(artistText, albumText, session?.username, session?.apiKey)
                } else {
                    Album.getInfo(artistText, albumText, Token.getConsumerKey(context))
                }

                if (album != null) {
                    val imageSizes = ImageSize.values()
                    for (i in imageSizes.indices.reversed()) {
                        remoteUri = album.getImageURL(imageSizes[i])
                        if (!TextUtils.isEmpty(remoteUri)) {
                            localUri = AppUtils.downloadUrl(context, remoteUri)
                            break
                        }
                    }
                }
            }

            // Track image
            if (localUri == null && !TextUtils.isEmpty(artistText) && !TextUtils.isEmpty(trackText)) {
                val track = if (session != null) {
                    Track.getInfo(artistText, trackText, Locale.getDefault(), session?.username, session?.apiKey)
                } else {
                    Track.getInfo(artistText, trackText, session?.apiKey)
                }

                if (track != null) {
                    val imageSizes = ImageSize.values()
                    for (i in imageSizes.indices.reversed()) {
                        remoteUri = track.getImageURL(imageSizes[i])
                        if (!TextUtils.isEmpty(remoteUri)) {
                            localUri = AppUtils.downloadUrl(context, remoteUri)
                            break
                        }
                    }
                }
            }

            // Artist image
            if (localUri == null && !TextUtils.isEmpty(artistText)) {
                val artist = if (session != null) {
                    Artist.getInfo(artistText, Locale.getDefault(), session?.username, session?.apiKey)
                } else {
                    Artist.getInfo(artistText, session?.apiKey)
                }

                if (artist != null) {
                    val imageSizes = ImageSize.values()
                    for (i in imageSizes.indices.reversed()) {
                        remoteUri = artist.getImageURL(imageSizes[i])
                        if (!TextUtils.isEmpty(remoteUri)) {
                            localUri = AppUtils.downloadUrl(context, remoteUri)
                            break
                        }
                    }
                }
            }

            if (localUri == null)
                return

            resultProperty = PropertyData()
            resultProperty.put(AlbumArtProperty.DATA_URI, localUri.toString())
            resultProperty.put(AlbumArtProperty.SOURCE_TITLE, getString(R.string.lastfm))
            resultProperty.put(AlbumArtProperty.SOURCE_URI, remoteUri)
            applicationContext.grantUriPermission(pluginIntent.srcPackage, localUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            result = AbstractPluginService.CommandResult.SUCCEEDED
        } catch (e: Exception) {
            Logger.e(e)
            resultProperty = null
            result = AbstractPluginService.CommandResult.FAILED
        } finally {
            sendResult(resultProperty)
            if (result == AbstractPluginService.CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media)
            } else if (result == AbstractPluginService.CommandResult.SUCCEEDED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), false))
                    AppUtils.showToast(context, R.string.message_get_data_success)
            } else if (result == AbstractPluginService.CommandResult.FAILED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
                    AppUtils.showToast(context, R.string.message_get_data_failure)
            }
        }
    }

}
