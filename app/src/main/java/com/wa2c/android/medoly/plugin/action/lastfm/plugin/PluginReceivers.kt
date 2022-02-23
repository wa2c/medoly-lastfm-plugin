package com.wa2c.android.medoly.plugin.action.lastfm.plugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.wa2c.android.medoly.library.*
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.util.logD
import com.wa2c.android.medoly.plugin.action.lastfm.util.toWorkParams
import com.wa2c.android.medoly.plugin.action.lastfm.util.toast
import com.wa2c.android.prefs.Prefs

/**
 * Plugin request receiver
 */
abstract class AbstractPluginReceiver : BroadcastReceiver() {

    protected lateinit var prefs: Prefs

    override fun onReceive(context: Context, intent: Intent) {
        logD("onReceive: %s", this.javaClass.simpleName)
        prefs = Prefs(context)
        val pluginIntent = MediaPluginIntent(intent)
        val result = runPlugin(context, pluginIntent)
        setResult(result.resultCode, null, null)
    }

    abstract fun runPlugin(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult

    /**
     * True if exists media.
     */
    protected fun existsMedia(context: Context, propertyData: PropertyData): Boolean {
        // media
        if (propertyData.isMediaEmpty) {
            context.toast(R.string.message_no_media)
            return false
        }
        // property
        if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
            return false
        }
        return true
    }

    /**
     * Launch worker.
     */
    protected inline fun <reified T : Worker> launchWorker(context: Context, params: Data) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val request = OneTimeWorkRequestBuilder<T>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(params)
            .build()
        workManager.enqueue(request)
    }
}

// Event

/**
 * Scrobble.
 */
class EventScrobbleReceiver : AbstractPluginReceiver() {
    override fun runPlugin(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult {
        val propertyData = pluginIntent.propertyData ?: return PluginBroadcastResult.CANCEL
        if (!existsMedia(context, propertyData)) return PluginBroadcastResult.CANCEL

        // enabled
        if (!prefs.getBoolean(R.string.prefkey_scrobble_enabled, defRes = R.bool.pref_default_scrobble_enabled)) {
            return PluginBroadcastResult.CANCEL
        }

        // previous media
        val mediaUriText = propertyData.mediaUri.toString()
        val previousMediaUri = prefs.getString(PluginPostScrobbleWorker.PREFKEY_PREVIOUS_MEDIA_URI)
        val previousMediaEnabled = prefs.getBoolean(R.string.prefkey_previous_media_enabled, defRes = R.bool.pref_default_previous_media_enabled)
        if (!previousMediaEnabled && mediaUriText.isNotEmpty() && previousMediaUri.isNotEmpty() && mediaUriText == previousMediaUri) {
            return PluginBroadcastResult.CANCEL
        }

        launchWorker<PluginPostScrobbleWorker>(context.applicationContext, pluginIntent.toWorkParams())
        return PluginBroadcastResult.COMPLETE
    }
}

/**
 * Now Playing.
 */
class EventNowPlayingReceiver : AbstractPluginReceiver() {
    override fun runPlugin(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult {
        val propertyData = pluginIntent.propertyData ?: return PluginBroadcastResult.CANCEL
        if (!existsMedia(context, propertyData)) return PluginBroadcastResult.CANCEL

        // enabled
        if (!prefs.getBoolean(R.string.prefkey_now_playing_enabled, defRes = R.bool.pref_default_now_playing_enabled)) {
            return PluginBroadcastResult.CANCEL
        }

        launchWorker<PluginPostNowPlayingWorker>(context.applicationContext, pluginIntent.toWorkParams())
        return PluginBroadcastResult.COMPLETE
    }
}

/**
 * Get album art.
 */
open class EventGetAlbumArtReceiver : AbstractPluginReceiver() {
    override fun runPlugin(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult {
        val propertyData = pluginIntent.propertyData ?: return PluginBroadcastResult.CANCEL
        if (!existsMedia(context, propertyData)) return PluginBroadcastResult.CANCEL

        // operation
        val operation = prefs.getString(R.string.prefkey_event_get_album_art_operation, defRes = R.string.pref_default_event_get_album_art_operation)
        if (!pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) && !pluginIntent.hasCategory(operation)) {
            return PluginBroadcastResult.CANCEL
        }

        launchWorker<PluginGetAlbumArtWorker>(context.applicationContext, pluginIntent.toWorkParams())
        return PluginBroadcastResult.PROCESSING
    }
}

/**
 * Get property.
 */
open class EventGetPropertyReceiver : AbstractPluginReceiver() {
    override fun runPlugin(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult {
        val propertyData = pluginIntent.propertyData ?: return PluginBroadcastResult.CANCEL
        if (!existsMedia(context, propertyData)) return PluginBroadcastResult.CANCEL

        // operation
        val operation = prefs.getString(R.string.prefkey_event_get_property_operation, defRes = R.string.pref_default_event_get_property_operation)
        if (!pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) && !pluginIntent.hasCategory(operation)) {
            return PluginBroadcastResult.CANCEL
        }

        launchWorker<PluginGetPropertyWorker>(context.applicationContext, pluginIntent.toWorkParams())
        return PluginBroadcastResult.PROCESSING
    }
}

// Execution

/**
 * Love
 */
class ExecuteLoveReceiver : AbstractPluginReceiver() {
    override fun runPlugin(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult {
        val propertyData = pluginIntent.propertyData ?: return PluginBroadcastResult.CANCEL
        if (!existsMedia(context, propertyData)) return PluginBroadcastResult.CANCEL

        launchWorker<PluginPostLoveWorker>(context.applicationContext, pluginIntent.toWorkParams())
        return PluginBroadcastResult.COMPLETE
    }
}

/**
 * Unlove
 */
class ExecuteUnLoveReceiver : AbstractPluginReceiver() {
    override fun runPlugin(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult {
        val propertyData = pluginIntent.propertyData ?: return PluginBroadcastResult.CANCEL
        if (!existsMedia(context, propertyData)) return PluginBroadcastResult.CANCEL

        launchWorker<PluginPostUnloveWorker>(context.applicationContext, pluginIntent.toWorkParams())
        return PluginBroadcastResult.COMPLETE
    }
}

/**
 * Get album art.
 */
class ExecuteGetAlbumArtReceiver : EventGetAlbumArtReceiver()

/**
 * Get property.
 */
class ExecuteGetPropertyReceiver : EventGetPropertyReceiver()