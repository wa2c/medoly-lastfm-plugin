package com.wa2c.android.medoly.plugin.action.lastfm.activity

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import com.wa2c.android.medoly.library.MedolyEnvironment
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import com.wa2c.android.medoly.plugin.action.lastfm.dialog.AuthDialogFragment
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger
import com.wa2c.android.medoly.plugin.action.lastfm.util.Prefs
import de.umass.lastfm.Authenticator
import de.umass.lastfm.Caller
import de.umass.lastfm.cache.FileSystemCache
import de.umass.util.StringUtilities
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.lang.ref.WeakReference


/**
 * Main activity.
 */
class MainActivity : Activity() {

    /** Preferences controller.  */
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        // ActionBar
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)

        // Account Auth
        accountAuthButton.setOnClickListener {
            val dialogFragment = AuthDialogFragment.newInstance()
            dialogFragment.clickListener = DialogInterface.OnClickListener { _, which ->
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    try {
                        // Authenticate
                        val username = dialogFragment.username
                        val password = dialogFragment.password
                        AsyncAuthTask(this, username, password).execute()
                    } catch (e: Exception) {
                        Logger.e(e)
                        // Failed
                        prefs.remove(R.string.prefkey_auth_username)
                        prefs.remove(R.string.prefkey_auth_password)
                        AppUtils.showToast(applicationContext, R.string.message_auth_failure)
                    }

                } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                    // Clear
                    prefs.remove(R.string.prefkey_auth_username)
                    prefs.remove(R.string.prefkey_auth_password)
                    AppUtils.showToast(applicationContext, R.string.message_account_clear)
                }

                updateAuthMessage()
            }
            dialogFragment.show(this)
        }

        // Unsent List
        unsentListButton.setOnClickListener {
            startActivity(Intent(this, UnsentListActivity::class.java))
        }

        // Open Last.fm
        lastfmSiteButton.setOnClickListener {
            val username = prefs.getString(R.string.prefkey_auth_username)
            val uri = if (username.isNullOrEmpty()) {
                Uri.parse(getString(R.string.lastfm_url)) // Authorized
            } else {
                Uri.parse(getString(R.string.lastfm_url_user, username)) // Unauthorized
            }
            val i = Intent(Intent.ACTION_VIEW, uri)
            startActivity(i)
        }

        // Settings
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Launch Medoly
        launchMedolyButton.setOnClickListener {
            val intent = packageManager.getLaunchIntentForPackage(MedolyEnvironment.MEDOLY_PACKAGE)
            if (intent == null) {
                AppUtils.showToast(this, R.string.message_no_medoly)
                return@setOnClickListener
            }
            startActivity(intent)
        }

        updateAuthMessage()
    }

    /**
     * Update auth message.
     */
    fun updateAuthMessage() {
        val prefs = Prefs(this)
        val username = prefs.getString(R.string.prefkey_auth_username)
        val password = prefs.getString(R.string.prefkey_auth_password)

        if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
            accountAuthTextView.text = getString(R.string.message_account_auth)
        } else {
            accountAuthTextView.text = getString(R.string.message_account_not_auth)
        }
    }



    /**
     * Companion objects
     */
    private companion object {

        /**
         * Authentication task
         */
        class AsyncAuthTask internal constructor(context: MainActivity, private val username: String, password: String) : AsyncTask<String, Void, Boolean>() {
            private val weakActivity: WeakReference<MainActivity> = WeakReference(context)
            private val password: String = StringUtilities.md5(password)

            override fun doInBackground(vararg params: String): Boolean {
                try {
                    val context = weakActivity.get() ?: return false

                    // フォルダ設定 (getMobileSessionの前に入れる必要あり)
                    Caller.getInstance().cache = FileSystemCache(File(context.externalCacheDir.path + File.separator + "last.fm"))

                    // 認証
                    val session = Authenticator.getMobileSession(username, password, Token.getConsumerKey(context), Token.getConsumerSecret(context))
                    return session != null
                } catch (e: Exception) {
                    Logger.e(e)
                    return false
                }
            }

            override fun onPostExecute(result: Boolean) {
                val context = weakActivity.get() ?: return
                val prefs = Prefs(context)
                if (result) {
                    prefs.putValue(R.string.prefkey_auth_username, username)
                    prefs.putValue(R.string.prefkey_auth_password, password)
                    AppUtils.showToast(context, R.string.message_auth_success) // Succeed
                } else {
                    prefs.remove(R.string.prefkey_auth_username)
                    prefs.remove(R.string.prefkey_auth_password)
                    AppUtils.showToast(context, R.string.message_auth_failure) // Failed
                }

                context.updateAuthMessage()
            }
        }
    }

}
