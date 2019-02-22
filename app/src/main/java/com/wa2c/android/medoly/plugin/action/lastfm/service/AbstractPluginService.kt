package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.annotation.SuppressLint
import android.app.IntentService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wa2c.android.medoly.library.ExtraData
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.PluginOperationCategory
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils
import com.wa2c.android.prefs.Prefs
import de.umass.lastfm.Authenticator
import de.umass.lastfm.Caller
import de.umass.lastfm.Session
import de.umass.lastfm.cache.FileSystemCache
import timber.log.Timber
import java.io.File


/**
 * Abstract plugin service.
 */
abstract class AbstractPluginService(name: String) : IntentService(name) {

    companion object {
        /** Notification ID */
        private const val NOTIFICATION_ID = 1
        /** Notification Channel ID */
        private const val NOTIFICATION_CHANNEL_ID = "Notification"

        /** Received receiver class name.  */
        const val RECEIVED_CLASS_NAME = "RECEIVED_CLASS_NAME"
        /** Previous data key.  */
        const val PREFKEY_PREVIOUS_MEDIA_URI = "previous_media_uri"
    }


    /** Context.  */
    protected lateinit var context: Context
    /** Preferences.  */
    protected lateinit var prefs: Prefs
    /** Plugin intent.  */
    protected lateinit var pluginIntent: MediaPluginIntent
    /** Property data.  */
    protected lateinit var propertyData: PropertyData
    /** Received class name.  */
    protected lateinit var receivedClassName: String
    /** True if a result sent  */
    private var resultSent = false
    /** Session.  */
    protected var session: Session? = null
    /** Username */
    protected var username: String? = null



    @SuppressLint("NewApi")
    override fun onHandleIntent(intent: Intent?) {
        Timber.d("onHandleIntent")

        var notificationManager : NotificationManager? = null
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW)
                notificationManager.createNotificationChannel(channel)
                val builder = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText("")
                        .setSmallIcon(R.drawable.ic_notification)
                startForeground(NOTIFICATION_ID, builder.build())
            }

            if (intent == null)
                return

            resultSent = false
            context = applicationContext
            prefs = Prefs(this)
            pluginIntent = MediaPluginIntent(intent)
            propertyData = pluginIntent.propertyData ?: PropertyData()
            receivedClassName = pluginIntent.getStringExtra(RECEIVED_CLASS_NAME)

            createSession()

        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            if (notificationManager != null) {
                notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID)
                notificationManager.cancel(NOTIFICATION_ID)
                stopForeground(true)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy: %s", this.javaClass.simpleName)
        sendResult(null)
    }

    /**
     * Create last.fm session
     */
    private fun createSession() {
        try {
            // Initialize last.fm library
            try {
                Caller.getInstance().cache = FileSystemCache(File(context.externalCacheDir, "last.fm"))
            } catch (ignore: Exception) {
            }
            username = prefs.getString(R.string.prefkey_auth_username)
            session = Authenticator.getMobileSession(username, prefs.getString(R.string.prefkey_auth_password), Token.getConsumerKey(), Token.getConsumerSecret())
        } catch (e : Exception) {
        }
    }

    fun showMessage(result: CommandResult, succeededMessage: String, failedMessage: String) {
        if (result == CommandResult.AUTH_FAILED) {
            AppUtils.showToast(context, R.string.message_account_not_auth)
        } else if (result == CommandResult.NO_MEDIA) {
            AppUtils.showToast(context, R.string.message_no_media)
        } else if (result == CommandResult.SUCCEEDED) {
            if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || prefs.getBoolean(R.string.prefkey_post_success_message_show, defRes = R.bool.pref_default_post_success_message_show)) {
                AppUtils.showToast(context, succeededMessage)
            }
        } else if (result == CommandResult.FAILED) {
            if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || prefs.getBoolean(R.string.prefkey_post_failure_message_show, defRes = R.bool.pref_default_post_failure_message_show)) {
                AppUtils.showToast(context, failedMessage)
            }
        }
    }



    /**
     * Send result
     * @param resultProperty A result property data.
     * @param resultExtra A result extra data.
     */
    protected fun sendResult(resultProperty: PropertyData?, resultExtra: ExtraData? = null) {
        if (!resultSent && (this is PluginGetPropertyService || this is PluginGetAlbumArtService)) {
            AppUtils.sendResult(this, pluginIntent, resultProperty, resultExtra)
            resultSent = true
        }
    }

}
