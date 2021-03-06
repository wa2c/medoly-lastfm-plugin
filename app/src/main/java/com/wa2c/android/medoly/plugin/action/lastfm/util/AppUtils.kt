package com.wa2c.android.medoly.plugin.action.lastfm.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.wa2c.android.medoly.library.ExtraData
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.lastfm.BuildConfig
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL


/**
 * App utilities.
 */
object AppUtils {

    private const val SHARED_DIR_NAME = "download"
    private const val PROVIDER_AUTHORITIES = BuildConfig.APPLICATION_ID + ".fileprovider"

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
}
