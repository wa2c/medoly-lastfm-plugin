package com.wa2c.android.medoly.plugin.action.lastfm.plugin

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.softartdev.lastfm.Album
import com.softartdev.lastfm.Artist
import com.softartdev.lastfm.ImageSize
import com.softartdev.lastfm.Track
import com.wa2c.android.medoly.library.AlbumArtProperty
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import com.wa2c.android.medoly.plugin.action.lastfm.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Get album art worker.
 */
class PluginGetAlbumArtWorker(private val context: Context, private val params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val result = runBlocking {
            try {
                getProperties().let { context.sendBroadcast(it) }
                CommandResult.SUCCEEDED
            } catch (e: Exception) {
                logE(e)
                CommandResult.FAILED
            }
        }

        val succeeded = context.getString(R.string.message_get_album_art_success)
        val failed = context.getString(R.string.message_get_album_art_failure)
        context.showMessage(result, succeeded, failed, params.isAutomaticallyAction)
        return Result.success()
    }

    /**
     * Get properties.
     */
    private suspend fun getProperties(): MediaPluginIntent {
        return withContext(Dispatchers.IO) {
            var uriPair: Pair<Uri, Uri>? = null // URI (first: remote, second: local)
            if (uriPair == null) uriPair = getAlbumImage(params.mediaArtist, params.mediaAlbum) // Album image
            if (uriPair == null) uriPair = getTrackImage(params.mediaArtist, params.mediaTitle) // Track image
            if (uriPair == null) uriPair = getArtistImage(params.mediaArtist) // Artist image
            if (uriPair == null) { throw RuntimeException() }
            val (remoteUri, localUri) = uriPair

            val resultProperty = PropertyData().apply {
                this[AlbumArtProperty.DATA_URI] = localUri.toString()
                this[AlbumArtProperty.SOURCE_TITLE] = context.getString(R.string.lastfm)
                this[AlbumArtProperty.SOURCE_URI] = remoteUri.toString()
            }

            // grant permission
            applicationContext.grantUriPermission(params.srcPackage, localUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            params.inputData.toPluginIntent(resultProperty)
        }
    }

    /**
     * Download album image
     * @return Uri pari (first: remote, second: local)
     */
    private fun getAlbumImage(artistText: String?, albumText: String?): Pair<Uri, Uri>? {
        if (artistText.isNullOrEmpty() || albumText.isNullOrEmpty()) return null
        try {
            // Album.getInfo(artistText, albumText, session?.username, session?.apiKey)
            Album.getInfo(artistText, albumText, Token.getConsumerKey())?.let { album ->
                val imageSizes = ImageSize.values()
                for (i in imageSizes.indices.reversed()) {
                    album.getImageURL(imageSizes[i])?.let { url ->
                        downloadUrl(context, url)?.let {
                            return Pair(Uri.parse(url), it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logE(e)
        }
        return null
    }

    /**
     * Download track image
     * @return Uri pari (first: remote, second: local)
     */
    private fun getTrackImage(artistText: String?, trackText: String?): Pair<Uri, Uri>? {
        if (artistText.isNullOrEmpty() || trackText.isNullOrEmpty()) return null
        try {
            // Album.getInfo(artistText, albumText, session?.username, session?.apiKey)
            Track.getInfo(artistText, trackText, Token.getConsumerKey())?.let { track ->
                val imageSizes = ImageSize.values()
                for (i in imageSizes.indices.reversed()) {
                    track.getImageURL(imageSizes[i])?.let { url ->
                        downloadUrl(context, url)?.let {
                            return Pair(Uri.parse(url), it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logE(e)
        }
        return null
    }

    /**
     * Download artist image
     * @return Uri pari (first: remote, second: local)
     */
    private fun getArtistImage(artistText: String?): Pair<Uri, Uri>? {
        if (artistText.isNullOrEmpty()) return null
        try {
            // Artist.getInfo(artistText, Locale.getDefault(), session?.username, session?.apiKey)
            Artist.getInfo(artistText, Token.getConsumerKey())?.let { artist ->
                val imageSizes = ImageSize.values()
                for (i in imageSizes.indices.reversed()) {
                    artist.getImageURL(imageSizes[i])?.let { url ->
                        downloadUrl(context, url)?.let {
                            return Pair(Uri.parse(url), it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logE(e)
        }
        return null
    }
}