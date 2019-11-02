package com.wa2c.android.medoly.plugin.action.lastfm.service

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.softartdev.lastfm.Session
import com.softartdev.lastfm.Track
import com.wa2c.android.medoly.library.MediaProperty
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import com.wa2c.android.medoly.plugin.action.lastfm.util.logD
import com.wa2c.android.medoly.plugin.action.lastfm.util.logE
import com.wa2c.android.medoly.plugin.action.lastfm.util.toast


/**
 * Run plugin service.
 */
class PluginRunService : AbstractPluginService(PluginRunService::class.java.simpleName) {

    override fun onHandleIntent(intent: Intent?) {
        try {
            super.onHandleIntent(intent)
            if (receivedClassName == PluginReceivers.ExecuteTrackPageReceiver::class.java.name) {
                openTrackPage(session)
            } else if (receivedClassName == PluginReceivers.ExecuteLastfmSiteReceiver::class.java.name) {
                openLastfmPage(session)
            }
        } catch (e: Exception) {
            logE(e)
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
        context.startActivity(launchIntent)
    }

    /**
     * Open track page.
     * @param session The session.
     */
    private fun openTrackPage(session: Session?) {
        var result = CommandResult.IGNORE
        try {
            val trackText = propertyData.getFirst(MediaProperty.TITLE)
            val artistText = propertyData.getFirst(MediaProperty.ARTIST)
            val track = if (session != null) {
                Track.getInfo(artistText, trackText, null, session.username, session.apiKey)
            } else {
                Track.getInfo(artistText, trackText, Token.getConsumerKey())
            }

            startPage(Uri.parse(track.url))
            result = CommandResult.SUCCEEDED
        } catch (e: Exception) {
            logE(e)
            result = CommandResult.FAILED
        } finally {
            if (result == CommandResult.FAILED) {
                toast(R.string.message_page_failure)
            }
        }
    }

    /**
     * Open Last.fm page.
     * @param session The session.
     */
    private fun openLastfmPage(session: Session?) {
        var result = CommandResult.IGNORE
        try {
            // Last.fm
            val siteUri = if (!username.isNullOrEmpty()) {
                Uri.parse(context.getString(R.string.lastfm_url_user, username)) // Authorized
            } else {
                Uri.parse(context.getString(R.string.lastfm_url)) // Unauthorized
            }

            startPage(siteUri)
            result = CommandResult.SUCCEEDED
        } catch (e: ActivityNotFoundException) {
            logD(e)
            result = CommandResult.FAILED
        } finally {
            if (result == CommandResult.FAILED) {
                toast(R.string.message_page_failure)
            }
        }
    }

}
