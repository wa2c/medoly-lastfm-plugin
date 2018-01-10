package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.annotation.SuppressLint
import android.app.IntentService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import com.wa2c.android.medoly.library.ExtraData
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger
import de.umass.lastfm.Session


/**
 * Intent service.
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
    protected lateinit var preferences: SharedPreferences
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



    @SuppressLint("NewApi")
    override fun onHandleIntent(intent: Intent?) {
        Logger.d("onHandleIntent")

        var notificationManager : NotificationManager? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
            val builder = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("")
                    .setSmallIcon(R.drawable.ic_launcher)
            startForeground(NOTIFICATION_ID, builder.build())
        }

        if (intent == null)
            return

        try {
            resultSent = false
            context = applicationContext
            preferences = PreferenceManager.getDefaultSharedPreferences(context)
            pluginIntent = MediaPluginIntent(intent)
            propertyData = pluginIntent.propertyData ?: PropertyData()
            receivedClassName = pluginIntent.getStringExtra(RECEIVED_CLASS_NAME)
        } catch (e: Exception) {
            Logger.e(e)
        } finally {
            if (notificationManager != null) {
                notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID)
                notificationManager.cancel(NOTIFICATION_ID)
                stopForeground(true)
            }
        }

        try { session = AppUtils.createSession(context) } catch (e: Exception) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d("onDestroy: " + this.javaClass.simpleName)
        sendResult(null)
    }

    /**
     * Send result
     * @param resultProperty A result property data.
     * @param resultExtra A result extra data.
     */
    @JvmOverloads protected fun sendResult(resultProperty: PropertyData?, resultExtra: ExtraData? = null) {
        if (!resultSent && (this is PluginGetPropertyService || this is PluginGetAlbumArtService)) {
            AppUtils.sendResult(this, pluginIntent, resultProperty, resultExtra)
            resultSent = true
        }
    }

}
