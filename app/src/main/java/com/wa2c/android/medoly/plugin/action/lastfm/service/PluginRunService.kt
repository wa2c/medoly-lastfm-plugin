package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.content.Intent
import android.net.Uri
import android.text.TextUtils

import com.wa2c.android.medoly.library.MediaProperty
import com.wa2c.android.medoly.library.PluginTypeCategory
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger

import de.umass.lastfm.Session
import de.umass.lastfm.Track


/**
 * Intent service.
 */
/**
 * Constructor.
 */
class PluginRunService : AbstractPluginService(PluginRunService::class.java!!.getSimpleName()) {

    override fun onHandleIntent(intent: Intent?) {
        super.onHandleIntent(intent)
        if (pluginIntent == null)
            return
        if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_RUN)) {
            return
        }
        try {
            if (receivedClassName == PluginReceivers.ExecuteTrackPageReceiver::class.java!!.getName()) {
                openTrackPage(session)
            } else if (receivedClassName == PluginReceivers.ExecuteLastfmSiteReceiver::class.java!!.getName()) {
                openLastfmPage(session)
            }
        } catch (e: Exception) {
            Logger.e(e)
            //AppUtils.showToast(this, R.string.error_app);
        }

    }

    /**
     * Start page.
     * @param uri The URI.
     */
    private fun startPage(uri: Uri?) {
        if (uri == null)
            return
        val launchIntent = Intent(Intent.ACTION_VIEW, uri)
        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context!!.startActivity(launchIntent)
    }

    /**
     * Open track page.
     * @param session The session.
     */
    private fun openTrackPage(session: Session?) {
        var result: AbstractPluginService.CommandResult = AbstractPluginService.CommandResult.IGNORE
        try {
            if (propertyData == null || propertyData.isMediaEmpty) {
                result = AbstractPluginService.CommandResult.NO_MEDIA
                return
            }

            val trackText = propertyData.getFirst(MediaProperty.TITLE)
            val artistText = propertyData.getFirst(MediaProperty.ARTIST)
            if (TextUtils.isEmpty(trackText) || TextUtils.isEmpty(artistText))
                result = AbstractPluginService.CommandResult.IGNORE

            // Get info
            val track: Track
            if (session != null) {
                track = Track.getInfo(artistText, trackText, null, session.username, session.apiKey)
            } else {
                track = Track.getInfo(artistText, trackText, Token.getConsumerKey(context))
            }

            startPage(Uri.parse(track.url))
            result = AbstractPluginService.CommandResult.SUCCEEDED
        } catch (e: Exception) {
            Logger.e(e)
            result = AbstractPluginService.CommandResult.FAILED
        } finally {
            if (result == AbstractPluginService.CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media)
            } else if (result == AbstractPluginService.CommandResult.FAILED) {
                AppUtils.showToast(context, R.string.message_page_failure)
            }
        }
    }

    /**
     * Open Last.fm page.
     * @param session The session.
     */
    private fun openLastfmPage(session: Session?) {
        var result: AbstractPluginService.CommandResult = AbstractPluginService.CommandResult.IGNORE
        try {
            // Last.fm
            val siteUri: Uri
            if (session != null) {
                // ユーザ認証済
                siteUri = Uri.parse(context!!.getString(R.string.lastfm_url_user, session.username))
            } else {
                // ユーザ未認証
                siteUri = Uri.parse(context!!.getString(R.string.lastfm_url))
            }

            startPage(siteUri)
            result = AbstractPluginService.CommandResult.SUCCEEDED
        } catch (e: android.content.ActivityNotFoundException) {
            Logger.d(e)
            result = AbstractPluginService.CommandResult.FAILED
        } finally {
            if (result == AbstractPluginService.CommandResult.FAILED) {
                AppUtils.showToast(context, R.string.message_page_failure)
            }
        }
    }

}
