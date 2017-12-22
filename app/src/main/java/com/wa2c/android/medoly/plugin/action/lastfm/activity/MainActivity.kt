package com.wa2c.android.medoly.plugin.action.lastfm.activity

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.widget.TextView
import com.wa2c.android.medoly.library.MedolyEnvironment
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import com.wa2c.android.medoly.plugin.action.lastfm.dialog.AuthDialogFragment
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger
import de.umass.lastfm.Authenticator
import de.umass.lastfm.Caller
import de.umass.lastfm.cache.FileSystemCache
import de.umass.util.StringUtilities
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


/**
 * メイン画面のアクティビティ。
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ActionBar
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)

        // Account Auth
        accountAuthButton.setOnClickListener {
            val dialogFragment = AuthDialogFragment.newInstance()
            dialogFragment.clickListener = DialogInterface.OnClickListener { _, which ->
                val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    try {
                        // 認証
                        val username = dialogFragment.username
                        val password = dialogFragment.password
                        AsyncAuthTask(this, username, password).execute()
                    } catch (e: Exception) {
                        Logger.e(e)
                        // 失敗
                        preference.edit().remove(getString(R.string.prefkey_auth_username)).apply()
                        preference.edit().remove(getString(R.string.prefkey_auth_password)).apply()
                        AppUtils.showToast(applicationContext, R.string.message_auth_failure)
                    }

                } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                    // クリア
                    preference.edit().remove(getString(R.string.prefkey_auth_username)).apply()
                    preference.edit().remove(getString(R.string.prefkey_auth_password)).apply()
                    AppUtils.showToast(applicationContext, R.string.message_account_clear)
                }

                updateAuthMessage(this, accountAuthTextView)
            }


//            dialogFragment.setClickListener { dialog, which ->
//                val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
//                if (which == DialogInterface.BUTTON_POSITIVE) {
//                    try {
//                        // 認証
//                        val username = dialogFragment.username
//                        val password = dialogFragment.password
//                        AsyncAuthTask(username, password).execute()
//                    } catch (e: Exception) {
//                        Logger.e(e)
//                        // 失敗
//                        preference.edit().remove(getString(R.string.prefkey_auth_username)).apply()
//                        preference.edit().remove(getString(R.string.prefkey_auth_password)).apply()
//                        AppUtils.showToast(applicationContext, R.string.message_auth_failure)
//                    }
//
//                } else if (which == DialogInterface.BUTTON_NEGATIVE) {
//                    // クリア
//                    preference.edit().remove(getString(R.string.prefkey_auth_username)).apply()
//                    preference.edit().remove(getString(R.string.prefkey_auth_password)).apply()
//                    AppUtils.showToast(applicationContext, R.string.message_account_clear)
//                }
//
//                updateAuthMessage()
//            }
            dialogFragment.show(this)
        }

        // Unsent List
        unsentListButton.setOnClickListener {
            startActivity(Intent(this, UnsentListActivity::class.java))
        }

        // Open Last.fm
        lastfmSiteButton.setOnClickListener {
            val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val username = preference.getString(getString(R.string.prefkey_auth_username), "")
            val uri = if (TextUtils.isEmpty(username)) {
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

        //setSpinner();

        updateAuthMessage(this, accountAuthTextView)
    }

    //    /**
    //     * Set spinner.
    //     */
    //    private void setSpinner() {
    //        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    //
    //        final Spinner executeEventSpinner = (Spinner)findViewById(R.id.executeEventSpinner);
    //        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
    //        adapter.add(getString(R.string.label_spinner_event_none));          // 0
    //        adapter.add(getString(R.string.label_plugin_operation_play_start)); // 1
    //        adapter.add(getString(R.string.label_plugin_operation_play_now));   // 2
    //        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    //        executeEventSpinner.setAdapter(adapter);
    //        executeEventSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
    //            @Override
    //            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    //                preferences.edit().putInt(getString(R.string.pref_plugin_event), position).apply();
    //            }
    //            @Override
    //            public void onNothingSelected(AdapterView<?> parent) {
    //                executeEventSpinner.setSelection(2);
    //            }
    //        });
    //
    //        executeEventSpinner.setSelection(preferences.getInt(getString(R.string.pref_plugin_event), ProcessService.PLUGIN_EVENT_PLAY_NOW));
    //
    //        // old data convert
    //        if (preferences.getBoolean("operation_play_now_enabled", true)) {
    //            executeEventSpinner.setSelection(ProcessService.PLUGIN_EVENT_PLAY_NOW);
    //        } else if  (preferences.getBoolean("operation_play_start_enabled", true)) {
    //            executeEventSpinner.setSelection(ProcessService.PLUGIN_EVENT_PLAY_START);
    //        } else {
    //            executeEventSpinner.setSelection(ProcessService.PLUGIN_EVENT_NONE);
    //        }
    //        preferences.edit().remove("operation_play_now_enabled").apply();
    //        preferences.edit().remove("operation_play_start_enabled").apply();
    //    }

    /**
     * 投稿タスク。
     */
    private inner class AsyncAuthTask internal constructor(context: Context, private val username: String, password: String) : AsyncTask<String, Void, Boolean>() {
        private val preferences: SharedPreferences
        private val context: Context
        private val password: String

        init {
            this.preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            this.context = applicationContext
            this.password = StringUtilities.md5(password) // パスワードはMD5で処理;
        }

        override fun doInBackground(vararg params: String): Boolean? {
            try {
                // フォルダ設定 (getMobileSessionの前に入れる必要あり)
                Caller.getInstance().cache = FileSystemCache(File(context.externalCacheDir!!.path + File.separator + "last.fm"))

                // 認証
                val session = Authenticator.getMobileSession(username, password, Token.getConsumerKey(context), Token.getConsumerSecret(context))
                return session != null
            } catch (e: Exception) {
                Logger.e(e)
                return false
            }
        }

        override fun onPostExecute(result: Boolean?) {
            if (result!!) {
                preferences.edit().putString(getString(R.string.prefkey_auth_username), username).apply()
                preferences.edit().putString(getString(R.string.prefkey_auth_password), password).apply()
                AppUtils.showToast(context, R.string.message_auth_success) // Succeed
            } else {
                preferences.edit().remove(getString(R.string.prefkey_auth_username)).apply()
                preferences.edit().remove(getString(R.string.prefkey_auth_password)).apply()
                AppUtils.showToast(context, R.string.message_auth_failure) // Failed
            }

            updateAuthMessage(context, accountAuthTextView)
        }
    }


    /**
     * Update auth message.
     */
    private fun updateAuthMessage(context: Context, accountAuthTextView: TextView) {
        val preference = PreferenceManager.getDefaultSharedPreferences(context)
        val username = preference.getString(getString(R.string.prefkey_auth_username), "")
        val password = preference.getString(getString(R.string.prefkey_auth_password), "")

        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            accountAuthTextView.text = getString(R.string.message_account_auth)
        } else {
            accountAuthTextView.text = getString(R.string.message_account_not_auth)
        }
    }

}
