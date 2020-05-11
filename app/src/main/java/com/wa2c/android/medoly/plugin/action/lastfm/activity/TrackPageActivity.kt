package com.wa2c.android.medoly.plugin.action.lastfm.activity

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import com.softartdev.lastfm.Session
import com.softartdev.lastfm.Track
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.MediaProperty
import com.wa2c.android.medoly.library.PluginBroadcastResult
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import com.wa2c.android.medoly.plugin.action.lastfm.service.CommandResult
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils.startPage
import com.wa2c.android.medoly.plugin.action.lastfm.util.logD
import com.wa2c.android.medoly.plugin.action.lastfm.util.logE
import com.wa2c.android.medoly.plugin.action.lastfm.util.toast
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Track page open activity
 */
class TrackPageActivity : AppCompatActivity(R.layout.layout_loading) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        openTrackPage(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        openTrackPage(intent)
    }

    /**
     * Open track page.
     */
    private fun openTrackPage(launchIntent: Intent?) {
        runBlocking {
            GlobalScope.launch {
                val result = let result@{
                    try {
                        if (launchIntent == null) {
                            setResult(PluginBroadcastResult.CANCEL.resultCode)
                            return@result PluginBroadcastResult.COMPLETE
                        }

                        val intent = MediaPluginIntent(intent)
                        val propertyData: PropertyData = intent.propertyData.let {
                            if (it == null || it.isEmpty()) {
                                toast(R.string.message_no_media)
                                setResult(PluginBroadcastResult.CANCEL.resultCode)
                                return@result PluginBroadcastResult.COMPLETE
                            }
                            it
                        }

                        val trackText = propertyData.getFirst(MediaProperty.TITLE)
                        val artistText = propertyData.getFirst(MediaProperty.ARTIST)
                        val track = Track.getInfo(artistText, trackText, Token.getConsumerKey())

                        startPage(Uri.parse(track.url))
                        PluginBroadcastResult.COMPLETE
                    } catch (e: Exception) {
                        logE(e)
                        toast(R.string.message_page_failure)
                        PluginBroadcastResult.CANCEL
                    }
                }

                setResult(result.resultCode)
                finish()
            }
        }
    }
}
