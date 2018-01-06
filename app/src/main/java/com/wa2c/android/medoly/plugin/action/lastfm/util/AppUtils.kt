package com.wa2c.android.medoly.plugin.action.lastfm.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.preference.PreferenceManager
import android.support.v4.content.FileProvider
import com.google.gson.Gson
import com.wa2c.android.medoly.library.ExtraData
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.lastfm.BuildConfig
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import de.umass.lastfm.Authenticator
import de.umass.lastfm.Caller
import de.umass.lastfm.Session
import de.umass.lastfm.cache.FileSystemCache
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL


/**
 * App utilities.
 */
object AppUtils {

    private val SHARED_DIR_NAME = "download"
    private val PROVIDER_AUTHORITIES = BuildConfig.APPLICATION_ID + ".fileprovider"

    /**
     * Show message.
     * @param context context.
     * @param text message.
     */
    fun showToast(context: Context, text: String) {
        ToastReceiver.showToast(context, text)
    }

    /**
     * Show message.
     * @param context context
     * @param stringId resource id.
     */
    fun showToast(context: Context, stringId: Int) {
        ToastReceiver.showToast(context, stringId)
    }

    /**
     * Save object to shared preference.
     * @param context context
     * @param prefKey preference key.
     * @param saveObject save object.
     * @return succeeded / failed
     */
    fun saveObject(context: Context, prefKey: String, saveObject: Any): Boolean {
        return try {
            val gson = Gson()
            val json = gson.toJson(saveObject)

            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            pref.edit().putString(prefKey, json).apply()
            true
        } catch (e: Exception) {
            Logger.e(e)
            false
        }
    }

    /**
     * Load object from shared preference.
     * @param context Context.
     * @param prefKey Preference key.
     * @return Loaded object. null as failed.
     */
    inline fun <reified T> loadObject(context: Context, prefKey: String): T? {
        return try {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            val json = pref.getString(prefKey, "")

            val gson = Gson()
            gson.fromJson(json, T::class.java)
        } catch (e: Exception) {
            Logger.e(e)
            null
        }


    }



    /**
     * Create last.fm session
     */
    fun createSession(context: Context): Session? {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        // Initialize last.fm library
        try {
            Caller.getInstance().cache = FileSystemCache(File(context.externalCacheDir, "last.fm"))
        } catch (ignore: Exception) {
        }

        // Authenticate
        val username = preferences.getString(context.getString(R.string.prefkey_auth_username), "")
        val password = preferences.getString(context.getString(R.string.prefkey_auth_password), "")
        return Authenticator.getMobileSession(username, password!!, Token.getConsumerKey(context), Token.getConsumerSecret(context))
    }


    /**
     * Download URI data.
     * @param context A context.
     * @param downloadUrl Download URI.
     * @return Shared URI.
     */
    fun downloadUrl(context: Context, downloadUrl: String): Uri? {
        var providerUri: Uri? = null
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
                val files = sharedDir.listFiles()
                files.filter { it.isFile }.forEach { it.delete() }
            }

            // InputStream
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "GET"
            con.connect()
            val status = con.responseCode
            if (status != HttpURLConnection.HTTP_OK) {
                return null
            }

            val pathElements = url.path.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
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
                    providerUri = FileProvider.getUriForFile(context, PROVIDER_AUTHORITIES, sharedFile)
                }
            }
        } catch (e: Exception) {
            return null
        }

        // URI
        return providerUri
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
}
