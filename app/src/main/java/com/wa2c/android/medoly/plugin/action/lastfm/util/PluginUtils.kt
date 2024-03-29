package com.wa2c.android.medoly.plugin.action.lastfm.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.impl.utils.futures.SettableFuture
import com.google.common.util.concurrent.ListenableFuture
import com.softartdev.lastfm.Authenticator
import com.softartdev.lastfm.Caller
import com.softartdev.lastfm.Session
import com.softartdev.lastfm.cache.FileSystemCache
import com.softartdev.lastfm.scrobble.ScrobbleData
import com.wa2c.android.medoly.library.ExtraData
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.MediaProperty
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.lastfm.BuildConfig
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import com.wa2c.android.medoly.plugin.action.lastfm.plugin.CommandResult
import com.wa2c.android.prefs.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

private const val SHARED_DIR_NAME = "download"
private const val PROVIDER_AUTHORITIES = BuildConfig.APPLICATION_ID + ".fileprovider"

private const val INTENT_SRC_PACKAGE = "INTENT_SRC_PACKAGE"
private const val INTENT_SRC_CLASS = "INTENT_SRC_CLASS"
private const val INTENT_ACTION_ID = "INTENT_ACTION_ID"
private const val INTENT_ACTION_LABEL = "INTENT_ACTION_LABEL"
private const val INTENT_ACTION_PRIORITY = "INTENT_ACTION_PRIORITY"
private const val INTENT_ACTION_IS_AUTOMATICALLY = "INTENT_ACTION_IS_AUTOMATICALLY"

/** Notification ID */
private const val NOTIFICATION_ID = 1
/** Notification Channel ID */
private const val NOTIFICATION_CHANNEL_ID = "Notification"

/** Src package */
val WorkerParameters.srcPackage: String?
    get() = inputData.getString(INTENT_SRC_PACKAGE)

/** True if the action was run automatically. */
val WorkerParameters.isAutomaticallyAction: Boolean
    get() = inputData.getBoolean(INTENT_ACTION_IS_AUTOMATICALLY, false)

/** Media title */
val WorkerParameters.mediaTitle: String?
    get() = inputData.getString(MediaProperty.TITLE.keyName)

/** Media artist */
val WorkerParameters.mediaArtist: String?
    get() = inputData.getString(MediaProperty.ARTIST.keyName)

/** Media album */
val WorkerParameters.mediaAlbum: String?
    get() = inputData.getString(MediaProperty.ALBUM.keyName)

/**
 * Create last.fm session
 */
suspend fun createSession(context: Context, prefs: Prefs): Session? {
    return withContext(Dispatchers.IO) {
        try {
            // Initialize last.fm library
            Caller.getInstance().cache = FileSystemCache(File(context.cacheDir, "last.fm"))
            Authenticator.getMobileSession(
                prefs.getString(R.string.prefkey_auth_username),
                prefs.getString(R.string.prefkey_auth_password),
                Token.getConsumerKey(),
                Token.getConsumerSecret()
            )
        } catch (ignore: Exception) {
            null
        }
    }
}

/**
 * Download URI data.
 * @param context A context.
 * @param downloadUrl Download URI.
 * @return Shared URI.
 */
fun downloadUrl(context: Context, downloadUrl: String): Uri? {
    try {
        val url = URL(downloadUrl)
        if (url.protocol == ContentResolver.SCHEME_FILE || url.protocol == ContentResolver.SCHEME_CONTENT) {
            return Uri.parse(downloadUrl)
        }

        // Folder
        val sharedDir = File(context.filesDir, SHARED_DIR_NAME)
        if (!sharedDir.exists()) {
            // Create a folder
            sharedDir.mkdir()
        } else {
            // Delete all files
            sharedDir.listFiles()?.filter { it.isFile }?.forEach { it.delete() }
        }

        val pathElements = url.path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val saveFileName = pathElements[pathElements.size - 1]
        val sharedFile = File(sharedDir, saveFileName)

        url.openStream().use { input ->
            sharedFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return FileProvider.getUriForFile(context, PROVIDER_AUTHORITIES, sharedFile)
    } catch (e: Exception) {
        return null
    }
}

/**
 * Create WorkParams data from plugin intent.
 */
fun MediaPluginIntent.toWorkParams(): Data {
    return Data.Builder().apply {
        putString(INTENT_SRC_PACKAGE, srcPackage)
        putString(INTENT_SRC_CLASS, srcClass)
        putString(INTENT_ACTION_ID, actionId)
        putString(INTENT_ACTION_LABEL, actionLabel)
        putInt(INTENT_ACTION_PRIORITY, actionPriority ?: 0)
        putBoolean(INTENT_ACTION_IS_AUTOMATICALLY, isAutomatically)
        putAll(propertyData?.keys?.mapNotNull {
            (it ?: return@mapNotNull null) to (propertyData?.getFirst(it) ?: return@mapNotNull null)
        }?.toMap() ?: emptyMap())
        putAll(extraData?.keys?.mapNotNull {
            (it ?: return@mapNotNull null) to (propertyData?.getFirst(it) ?: return@mapNotNull null)
        }?.toMap() ?: emptyMap())
    }.build()
}

/**
 * Create plugin intent from WorkParams data.
 */
fun Data.toPluginIntent(resultProperty: PropertyData?, resultExtra: ExtraData? = null): MediaPluginIntent {
    val returnIntent = MediaPluginIntent()
    val srcPackage = getString(INTENT_SRC_PACKAGE) ?: throw IllegalArgumentException()
    val srcClass = getString(INTENT_SRC_CLASS) ?: throw IllegalArgumentException()
    returnIntent.setClassName(srcPackage, srcClass)
    returnIntent.propertyData = resultProperty
    returnIntent.extraData = resultExtra
    returnIntent.actionId = getString(INTENT_ACTION_ID)
    returnIntent.actionLabel = getString(INTENT_ACTION_LABEL)
    returnIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    return returnIntent
}

/**
 * Crate scrobble data from WorkParams data.
 * @return The scrobble data.
 */
fun Data.toScrobbleData(): ScrobbleData {
    val newData = ScrobbleData()
    newData.musicBrainzId = getString(MediaProperty.MUSICBRAINZ_TRACK_ID.keyName)
    newData.track = getString(MediaProperty.TITLE.keyName)
    newData.artist = getString(MediaProperty.ARTIST.keyName)
    newData.albumArtist = getString(MediaProperty.ALBUM_ARTIST.keyName)
    newData.album = getString(MediaProperty.ALBUM.keyName)

    try {
        newData.duration = ((getString(MediaProperty.DURATION.keyName)?.toLong() ?: 0) / 1000).toInt()
    } catch (ignore: NumberFormatException) {
    } catch (ignore: NullPointerException) {
    }

    try {
        newData.trackNumber = getString(MediaProperty.TRACK.keyName)?.toInt() ?: 0
    } catch (ignore: NumberFormatException) {
    } catch (ignore: NullPointerException) {
    }

    newData.timestamp = (System.currentTimeMillis() / 1000).toInt()
    return newData
}

/**
 * Get worker future.
 */
@SuppressLint("RestrictedApi")
fun createForegroundFuture(context: Context): ListenableFuture<ForegroundInfo> {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
    }

    val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID).apply {
        setContentTitle(context.getString(R.string.app_name))
        setSmallIcon(R.drawable.ic_notification)
    }.build()

    return SettableFuture.create<ForegroundInfo>().apply {
        set(ForegroundInfo(NOTIFICATION_ID, notification))
    }
}

/**
 * Show message.
 */
fun Worker.showMessage(prefs: Prefs, result: CommandResult, succeededMessage: String?, failedMessage: String?, isAutomatically: Boolean) {
    if (result == CommandResult.AUTH_FAILED) {
        applicationContext.toast(R.string.message_account_not_auth)
    } else if (result == CommandResult.NO_MEDIA) {
        applicationContext.toast(R.string.message_no_media)
    } else if (result == CommandResult.SUCCEEDED && !succeededMessage.isNullOrEmpty()) {
        if (!isAutomatically || prefs.getBoolean(R.string.prefkey_post_success_message_show, defRes = R.bool.pref_default_post_success_message_show)) {
            applicationContext.toast(succeededMessage)
        }
    } else if (result == CommandResult.FAILED && !failedMessage.isNullOrEmpty()) {
        if (!isAutomatically|| prefs.getBoolean(R.string.prefkey_post_failure_message_show, defRes = R.bool.pref_default_post_failure_message_show)) {
            applicationContext.toast(failedMessage)
        }
    }
}

/**
 * Start web page.
 * @param uri Open url.
 */
fun Context.startPage(uri: Uri) {
    val launchIntent = Intent(Intent.ACTION_VIEW, uri)
    startActivity(launchIntent)
}
