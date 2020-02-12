package com.wa2c.android.medoly.plugin.action.lastfm.activity

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.softartdev.lastfm.Authenticator
import com.softartdev.lastfm.Caller
import com.softartdev.lastfm.cache.FileSystemCache
import com.wa2c.android.medoly.library.MedolyEnvironment
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import com.wa2c.android.medoly.plugin.action.lastfm.databinding.ActivityMainBinding
import com.wa2c.android.medoly.plugin.action.lastfm.dialog.AuthDialogFragment
import com.wa2c.android.medoly.plugin.action.lastfm.util.logE
import com.wa2c.android.medoly.plugin.action.lastfm.util.toast
import com.wa2c.android.prefs.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File


/**
 * Main activity.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // ActionBar
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayShowTitleEnabled(true)
            setIcon(R.drawable.ic_launcher)
        }

        // Account Auth
        binding.accountAuthButton.setOnClickListener {
            val dialogFragment = AuthDialogFragment.newInstance()
            dialogFragment.clickListener = { _, which, bundle ->
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    val username = bundle?.getString(AuthDialogFragment.RESULT_USERNAME) ?: ""
                    val password = bundle?.getString(AuthDialogFragment.RESULT_PASSWORD) ?: ""
                    authUser(username, password)
                } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                    clearUser()
                }
            }
            dialogFragment.show(this)
        }

        // Unsent List
        binding.unsentListButton.setOnClickListener {
            startActivity(Intent(this, UnsentListActivity::class.java))
        }

        // Settings
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Open Last.fm
        binding.lastfmSiteButton.setOnClickListener {
            val username = prefs.getStringOrNull(R.string.prefkey_auth_username)
            val uri = if (username.isNullOrEmpty()) {
                Uri.parse(getString(R.string.lastfm_url)) // Authorized
            } else {
                Uri.parse(getString(R.string.lastfm_url_user, username)) // Unauthorized
            }
            val i = Intent(Intent.ACTION_VIEW, uri)
            startActivity(i)
        }

        // Launch Medoly
        binding.launchMedolyButton.setOnClickListener {
            val intent = packageManager.getLaunchIntentForPackage(MedolyEnvironment.MEDOLY_PACKAGE)
            if (intent == null) {
                toast(R.string.message_no_medoly)
                return@setOnClickListener
            }
            startActivity(intent)
        }

        updateAuthMessage()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.getSystemService(this, PowerManager::class.java)?.let { pm ->
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    val intent = Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
            }

        }
    }

    /**
     * Update auth message.
     */
    private fun updateAuthMessage() {
        val prefs = Prefs(this)
        val username: String? = prefs[R.string.prefkey_auth_username]
        val password: String? = prefs[R.string.prefkey_auth_password]

        if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
            binding.accountAuthTextView.text = getString(R.string.message_account_auth)
        } else {
            binding.accountAuthTextView.text = getString(R.string.message_account_not_auth)
        }
    }

    /**
     * Auth user
     * @param username Username
     * @param password Password
     */
    private fun authUser(username: String, password: String) {
        GlobalScope.launch(Dispatchers.Main) {
            // Auth
            val session = GlobalScope.async(Dispatchers.Default) {
                try {
                    Caller.getInstance().cache = FileSystemCache(File(cacheDir.path + File.separator + "last.fm"))
                    return@async Authenticator.getMobileSession(username, password, Token.getConsumerKey(), Token.getConsumerSecret())
                } catch (e: Exception) {
                    logE(e)
                    return@async null
                }
            }.await()
            // Save user
            if (session != null) {
                prefs[R.string.prefkey_auth_username] = username
                prefs[R.string.prefkey_auth_password] = password
                toast(R.string.message_auth_success) // Succeed
            } else {
                prefs.remove(R.string.prefkey_auth_username)
                prefs.remove(R.string.prefkey_auth_password)
                toast(R.string.message_auth_failure) // Failed
            }

            updateAuthMessage()
        }
    }

    /**
     * Clear user
     */
    private fun clearUser() {
        prefs.remove(R.string.prefkey_auth_username)
        prefs.remove(R.string.prefkey_auth_password)
        toast(R.string.message_account_clear)

        updateAuthMessage()
    }

}
