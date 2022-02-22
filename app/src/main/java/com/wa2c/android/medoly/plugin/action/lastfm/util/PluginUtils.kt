package com.wa2c.android.medoly.plugin.action.lastfm.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.work.Data
import com.wa2c.android.medoly.library.ExtraData
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.lastfm.BuildConfig
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.service.CommandResult
import com.wa2c.android.prefs.Prefs
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val SHARED_DIR_NAME = "download"
private const val PROVIDER_AUTHORITIES = BuildConfig.APPLICATION_ID + ".fileprovider"

private const val INTENT_SRC_PACKAGE = "INTENT_SRC_PACKAGE"
private const val INTENT_SRC_CLASS = "INTENT_SRC_CLASS"
private const val INTENT_ACTION_ID = "INTENT_ACTION_ID"
private const val INTENT_ACTION_LABEL = "INTENT_ACTION_LABEL"
private const val INTENT_ACTION_PRIORITY = "INTENT_ACTION_PRIORITY"
private const val INTENT_ACTION_IS_AUTOMATICALLY = "INTENT_ACTION_IS_AUTOMATICALLY"

/**
 * Download URI data.
 * @param context A context.
 * @param downloadUrl Download URI.
 * @return Shared URI.
 */
fun downloadUrl(context: Context, downloadUrl: String): Uri? {
    try {
        val url = URL(downloadUrl)
        if (ContentResolver.SCHEME_FILE == url.protocol || ContentResolver.SCHEME_CONTENT == url.protocol) {
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

        // InputStream
        val con = url.openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        con.connect()
        val status = con.responseCode
        if (status != HttpURLConnection.HTTP_OK) {
            return null
        }

        val pathElements = url.path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val saveFileName = pathElements[pathElements.size - 1]
        val sharedFile = File(sharedDir, saveFileName)

        BufferedInputStream(con.inputStream).use { input ->
            BufferedOutputStream(sharedFile.outputStream()).use { output ->
                val buffer = ByteArray(16384)
                while (true) {
                    val len = input.read(buffer)
                    if (len < 0) {
                        break
                    }
                    output.write(buffer, 0, len)
                }
                output.flush()
                return FileProvider.getUriForFile(context, PROVIDER_AUTHORITIES, sharedFile)
            }
        }
    } catch (e: Exception) {
        return null
    }
}

fun MediaPluginIntent.toWorkParams(): Data {
    return Data.Builder().apply {
        putString(INTENT_SRC_PACKAGE, srcPackage)
        putString(INTENT_SRC_CLASS, srcClass)
        putString(INTENT_ACTION_ID, actionId)
        putString(INTENT_ACTION_LABEL, actionLabel)
        putInt(INTENT_ACTION_PRIORITY, actionPriority ?: 0)
        putBoolean(INTENT_ACTION_IS_AUTOMATICALLY, isAutomatically)
        putAll(propertyData?.mapNotNull {
            (it.key ?: return@mapNotNull null) to (it.value?.firstOrNull() ?: return@mapNotNull null)
        }?.toMap() ?: emptyMap())
        putAll(extraData?.mapNotNull {
            (it.key ?: return@mapNotNull null) to (it.value?.firstOrNull() ?: return@mapNotNull null)
        }?.toMap() ?: emptyMap())
    }.build()
}

fun Data.toPluginIntent(resultProperty: PropertyData?, resultExtra: ExtraData? = null): MediaPluginIntent {
    val returnIntent = MediaPluginIntent()
    val srcPackage = getString(INTENT_SRC_PACKAGE) ?: throw IllegalArgumentException()
    val srcClass = getString(INTENT_SRC_CLASS) ?: throw IllegalArgumentException()
    val actionId = getString(INTENT_ACTION_ID)
    val actionLabel = getString(INTENT_ACTION_LABEL)
    returnIntent.setClassName(srcPackage, srcClass)
    returnIntent.propertyData = resultProperty
    returnIntent.extraData = resultExtra
    returnIntent.actionId = actionId
    returnIntent.actionLabel = actionLabel
    returnIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    return returnIntent
}


/**
 * Show message.
 */
fun Context.showMessage(result: CommandResult, succeededMessage: String?, failedMessage: String?, isAutomatically: Boolean) {
    val prefs = Prefs(this)
    if (result == CommandResult.AUTH_FAILED) {
        toast(R.string.message_account_not_auth)
    } else if (result == CommandResult.NO_MEDIA) {
        toast(R.string.message_no_media)
    } else if (result == CommandResult.SUCCEEDED && !succeededMessage.isNullOrEmpty()) {
        if (!isAutomatically || prefs.getBoolean(R.string.prefkey_post_success_message_show, defRes = R.bool.pref_default_post_success_message_show)) {
            toast(succeededMessage)
        }
    } else if (result == CommandResult.FAILED && !failedMessage.isNullOrEmpty()) {
        if (!isAutomatically|| prefs.getBoolean(R.string.prefkey_post_failure_message_show, defRes = R.bool.pref_default_post_failure_message_show)) {
            toast(failedMessage)
        }
    }
}


/**
 * Send result.
 * @param context A context.
 * @param pluginIntent A plugin intent.
 * @param resultProperty A result property data.
 * @param resultExtra A result extra data.
 */
fun sendResult(context: Context, pluginIntent: MediaPluginIntent, resultProperty: PropertyData? = null, resultExtra: ExtraData? = null) {
    context.sendBroadcast(pluginIntent.createResultIntent(resultProperty, resultExtra))
}

/**
 * Start web page.
 * @param uri Open url.
 */
fun Context.startPage(uri: Uri) {
    val launchIntent = Intent(Intent.ACTION_VIEW, uri)
    startActivity(launchIntent)
}
