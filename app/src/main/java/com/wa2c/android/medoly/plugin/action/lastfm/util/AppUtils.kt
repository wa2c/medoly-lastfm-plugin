package com.wa2c.android.medoly.plugin.action.lastfm.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.preference.PreferenceManager
import android.support.v4.content.FileProvider
import com.google.gson.Gson
import com.wa2c.android.medoly.plugin.action.lastfm.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
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
     * @param clazz Object class.
     * @return Loaded object. null as failed.
     */
//    fun <T> loadObject(context: Context, prefKey: String, clazz: Class<T>): T? {
//        return try {
//            val pref = PreferenceManager.getDefaultSharedPreferences(context)
//            val json = pref.getString(prefKey, "")
//
//            val gson = Gson()
//
//            gson.fromJson(json, clazz)
//        } catch (e: Exception) {
//            Logger.e(e)
//            null
//        }
//    }
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
     * Download URI data.
     * @param context A context.
     * @param downloadUrl Download URI.
     * @return Shared URI.
     */
    fun downloadUrl(context: Context, downloadUrl: String): Uri? {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        val providerUri: Uri
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
            inputStream = con.inputStream

            // OutputStream
            val pathElements = url.path.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            val saveFileName = pathElements[pathElements.size - 1]
            val sharedFile = File(sharedDir, saveFileName)
            outputStream = FileOutputStream(sharedFile)

            // Stream copy
            val buffer = ByteArray(16384)
            while (true) {
                val len = inputStream!!.read(buffer)
                if (len < 0) {
                    break
                }
                outputStream.write(buffer, 0, len)
            }
            outputStream.flush()

            providerUri = FileProvider.getUriForFile(context, PROVIDER_AUTHORITIES, sharedFile)
        } catch (e: Exception) {
            return null
        } finally {
            if (inputStream != null)
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            if (outputStream != null)
                try {
                    outputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

        }

        // URI
        return providerUri
    }
}
