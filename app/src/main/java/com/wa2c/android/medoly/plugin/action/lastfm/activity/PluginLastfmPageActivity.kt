package com.wa2c.android.medoly.plugin.action.lastfm.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wa2c.android.medoly.library.PluginBroadcastResult
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.util.logD
import com.wa2c.android.medoly.plugin.action.lastfm.util.startPage
import com.wa2c.android.medoly.plugin.action.lastfm.util.toast
import com.wa2c.android.prefs.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Last.fm page open activity
 */
class PluginLastfmPageActivity : AppCompatActivity(R.layout.layout_loading) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        openLastfmPage()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        openLastfmPage()
    }

    /**
     * Open Last.fm page.
     */
    private fun openLastfmPage() {
        CoroutineScope(Dispatchers.Main + Job()).launch {
            val result = try {
                // Last.fm
                val username =
                    Prefs(this@PluginLastfmPageActivity).getStringOrNull(R.string.prefkey_auth_username)
                val siteUri = if (!username.isNullOrEmpty()) {
                    Uri.parse(getString(R.string.lastfm_url_user, username)) // Authorized
                } else {
                    Uri.parse(getString(R.string.lastfm_url)) // Unauthorized
                }

                startPage(siteUri)
                PluginBroadcastResult.COMPLETE
            } catch (e: ActivityNotFoundException) {
                logD(e)
                toast(R.string.message_page_failure)
                PluginBroadcastResult.CANCEL
            }

            setResult(result.resultCode)
        }

        moveTaskToBack(true)
        finish()
    }
}
