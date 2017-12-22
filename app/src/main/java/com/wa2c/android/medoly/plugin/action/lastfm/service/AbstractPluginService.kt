package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.wa2c.android.medoly.library.ExtraData
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger
import de.umass.lastfm.Authenticator
import de.umass.lastfm.Caller
import de.umass.lastfm.Session
import de.umass.lastfm.cache.FileSystemCache
import java.io.File


/**
 * Intent service.
 */
abstract class AbstractPluginService(name: String) : IntentService(name) {

    companion object {
        /** Received receiver class name.  */
        val RECEIVED_CLASS_NAME = "RECEIVED_CLASS_NAME"
        /** Previous data key.  */
        val PREFKEY_PREVIOUS_MEDIA_URI = "previous_media_uri"
    }

    /**
     * Command result.
     */
    internal enum class CommandResult {
        /** Succeeded.  */
        SUCCEEDED,
        /** Failed.  */
        FAILED,
        /** Authorization failed.  */
        AUTH_FAILED,
        /** No media.  */
        NO_MEDIA,
        /** Post saved.  */
        SAVED,
        /** Ignore.  */
        IGNORE
    }



    /** Context.  */
    protected lateinit var context: Context
    /** Preferences.  */
    protected lateinit var sharedPreferences: SharedPreferences
    /** Plugin intent.  */
    protected lateinit var pluginIntent: MediaPluginIntent
    /** Property data.  */
    protected lateinit var propertyData: PropertyData
    /** Received class name.  */
    protected lateinit var receivedClassName: String
    /** Session.  */
    protected var session: Session? = null
    /** True if a result sent  */
    private var resultSent = false



    override fun onHandleIntent(intent: Intent?) {
        Logger.d("onHandleIntent")
        if (intent == null)
            return

        try {
            resultSent = false
            context = applicationContext
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            pluginIntent = MediaPluginIntent(intent)
            propertyData = pluginIntent.propertyData!!
            if (propertyData == null)
                propertyData = PropertyData()
            receivedClassName = pluginIntent.getStringExtra(RECEIVED_CLASS_NAME)

            // Initialize last.fm library
            try {
                Caller.getInstance().cache = FileSystemCache(File(context.externalCacheDir, "last.fm"))
            } catch (ignore: Exception) {
            }

            // Authenticate
            val username = sharedPreferences.getString(context.getString(R.string.prefkey_auth_username), "")
            val password = sharedPreferences.getString(context.getString(R.string.prefkey_auth_password), "")
            session = Authenticator.getMobileSession(username, password!!, Token.getConsumerKey(context), Token.getConsumerSecret(context))
        } catch (e: Exception) {
            Logger.e(e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d("onDestroy")
        sendResult(null)
    }

    /**
     * Send result
     * @param resultProperty A result property data.
     * @param resultExtra A result extra data.
     */
    @JvmOverloads protected fun sendResult(resultProperty: PropertyData?, resultExtra: ExtraData? = null) {
        if (!resultSent && (this is PluginGetPropertyService || this is PluginGetAlbumArtService)) {
            sendBroadcast(pluginIntent.createResultIntent(resultProperty, resultExtra))
            resultSent = true
        }
    }


}
