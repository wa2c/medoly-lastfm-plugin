package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.content.Intent
import android.net.Uri
import com.wa2c.android.medoly.library.AlbumArtProperty
import com.wa2c.android.medoly.library.MediaProperty
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import timber.log.Timber
import java.util.*


/**
 * Get album art plugin service.
 */
class PluginGetAlbumArtService : AbstractPluginService(PluginGetAlbumArtService::class.java.simpleName) {

    override fun onHandleIntent(intent: Intent?) {
        try {
            super.onHandleIntent(intent)
            getAlbumArt()
        } catch (e: Exception) {
            Timber.e(e)
            //AppUtils.showToast(this, R.string.error_app);
        }
    }

    /**
     * Get album art.
     */
    private fun getAlbumArt() {
        var result = CommandResult.IGNORE
        var resultProperty: PropertyData? = null
        try {
            val trackText = propertyData.getFirst(MediaProperty.TITLE)
            val albumText = propertyData.getFirst(MediaProperty.ALBUM)
            val artistText = propertyData.getFirst(MediaProperty.ARTIST)

            var remoteUri: String? = null
            var localUri: Uri? = null

            // Album image
            if (!artistText.isNullOrEmpty() && !albumText.isNullOrEmpty()) {
                val album = if (session != null) {
                    Album.getInfo(artistText, albumText, session?.username, session?.apiKey)
                } else {
                    Album.getInfo(artistText, albumText, Token.getConsumerKey())
                }

                if (album != null) {
                    val imageSizes = ImageSize.values()
                    for (i in imageSizes.indices.reversed()) {
                        remoteUri = album.getImageURL(imageSizes[i])
                        if (!remoteUri.isNullOrEmpty()) {
                            localUri = AppUtils.downloadUrl(context, remoteUri)
                            break
                        }
                    }
                }
            }

            // Track image
            if (localUri == null && !artistText.isNullOrEmpty() && !trackText.isNullOrEmpty()) {
                val track = if (session != null) {
                    Track.getInfo(artistText, trackText, Locale.getDefault(), session?.username, session?.apiKey)
                } else {
                    Track.getInfo(artistText, trackText, session?.apiKey)
                }

                if (track != null) {
                    val imageSizes = ImageSize.values()
                    for (i in imageSizes.indices.reversed()) {
                        remoteUri = track.getImageURL(imageSizes[i])
                        if (!remoteUri.isNullOrEmpty()) {
                            localUri = AppUtils.downloadUrl(context, remoteUri)
                            break
                        }
                    }
                }
            }

            // Artist image
            if (localUri == null && !artistText.isNullOrEmpty()) {
                val artist = if (session != null) {
                    Artist.getInfo(artistText, Locale.getDefault(), session?.username, session?.apiKey)
                } else {
                    Artist.getInfo(artistText, session?.apiKey)
                }

                if (artist != null) {
                    val imageSizes = ImageSize.values()
                    for (i in imageSizes.indices.reversed()) {
                        remoteUri = artist.getImageURL(imageSizes[i])
                        if (!remoteUri.isNullOrEmpty()) {
                            localUri = AppUtils.downloadUrl(context, remoteUri)
                            break
                        }
                    }
                }
            }

            if (localUri == null) {
                result = CommandResult.FAILED
                return
            }

            resultProperty = PropertyData()
            resultProperty[AlbumArtProperty.DATA_URI] = localUri.toString()
            resultProperty[AlbumArtProperty.SOURCE_TITLE] = getString(R.string.lastfm)
            resultProperty[AlbumArtProperty.SOURCE_URI] = remoteUri
            applicationContext.grantUriPermission(pluginIntent.srcPackage, localUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            result = CommandResult.SUCCEEDED
        } catch (e: Exception) {
            Timber.e(e)
            resultProperty = null
            result = CommandResult.FAILED
        } finally {
            sendResult(resultProperty)

            // show message
            val succeeded = getString(R.string.message_get_album_art_success)
            val failed = getString(R.string.message_get_album_art_failure)
            showMessage(result, succeeded, failed)
        }
    }

}
