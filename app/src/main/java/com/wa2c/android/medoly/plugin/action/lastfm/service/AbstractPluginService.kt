package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.softartdev.lastfm.Authenticator
import com.softartdev.lastfm.Caller
import com.softartdev.lastfm.Session
import com.softartdev.lastfm.cache.FileSystemCache
import com.wa2c.android.medoly.library.ExtraData
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.PluginOperationCategory
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils
import com.wa2c.android.medoly.plugin.action.lastfm.util.logD
import com.wa2c.android.medoly.plugin.action.lastfm.util.toast
import com.wa2c.android.prefs.Prefs
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InvalidObjectException


/**
 * Abstract plugin service.
 */
abstract class AbstractPluginService(name: String) : Service() {

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
    /** Notification manager. */
    private var notificationManager : NotificationManager? = null

    protected abstract fun runPlugin()

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logD("onStartCommand")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).apply {
                setContentTitle(getString(R.string.app_name))
                setSmallIcon(R.drawable.ic_notification)
            }.build()
            startForeground(NOTIFICATION_ID, notification)
        }

        if (intent == null)
            throw InvalidObjectException("Null intent")

        resultSent = false
        context = applicationContext
        prefs = Prefs(this)
        pluginIntent = MediaPluginIntent(intent)
        propertyData = pluginIntent.propertyData ?: PropertyData()
        receivedClassName = pluginIntent.getStringExtra(RECEIVED_CLASS_NAME) ?: ""

        createSession()

        stopSelf()

        runPlugin()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager?.let {
            it.cancel(NOTIFICATION_ID)
            stopForeground(true)
        }
        logD("onDestroy: %s", this.javaClass.simpleName)
        sendResult(null)
    }

    /**
     * Create last.fm session
     */
    private fun createSession() {
        try {
            // Initialize last.fm library
            try {
                Caller.getInstance().cache = FileSystemCache(File(context.cacheDir, "last.fm"))
            } catch (ignore: Exception) {
            }
            username = prefs.getString(R.string.prefkey_auth_username)
            session = Authenticator.getMobileSession(username, prefs.getString(R.string.prefkey_auth_password), Token.getConsumerKey(), Token.getConsumerSecret())
        } catch (e : Exception) {
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

    /**
     * Show message.
     */
    fun showMessage(result: CommandResult, succeededMessage: String?, failedMessage: String?) {
        if (result == CommandResult.AUTH_FAILED) {
            toast(R.string.message_account_not_auth)
        } else if (result == CommandResult.NO_MEDIA) {
            toast(R.string.message_no_media)
        } else if (result == CommandResult.SUCCEEDED && !succeededMessage.isNullOrEmpty()) {
            if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || prefs.getBoolean(R.string.prefkey_post_success_message_show, defRes = R.bool.pref_default_post_success_message_show)) {
                toast(succeededMessage)
            }
        } else if (result == CommandResult.FAILED && !failedMessage.isNullOrEmpty()) {
            if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || prefs.getBoolean(R.string.prefkey_post_failure_message_show, defRes = R.bool.pref_default_post_failure_message_show)) {
                toast(failedMessage)
            }
        }
    }



    companion object {
        /** Notification ID */
        private const val NOTIFICATION_ID = 1
        /** Notification Channel ID */
        private const val NOTIFICATION_CHANNEL_ID = "Notification"

        /** Received receiver class name.  */
        const val RECEIVED_CLASS_NAME = "RECEIVED_CLASS_NAME"
        /** Previous data key.  */
        const val PREFKEY_PREVIOUS_MEDIA_URI = "previous_media_uri"

        /**
         * Create notification
         */
        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                return

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_MIN)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

}
