package com.wa2c.android.medoly.plugin.action.lastfm.activity

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.softartdev.lastfm.Authenticator
import com.softartdev.lastfm.Caller
import com.softartdev.lastfm.cache.FileSystemCache
import com.wa2c.android.medoly.library.MedolyEnvironment
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.Token
import com.wa2c.android.medoly.plugin.action.lastfm.activity.component.viewBinding
import com.wa2c.android.medoly.plugin.action.lastfm.databinding.FragmentMainBinding
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
 * Main Fragment
 */
class MainFragment : Fragment(R.layout.fragment_main) {

    /** Binding */
    private val binding: FragmentMainBinding by viewBinding()
    /** Prefs */
    private val prefs: Prefs by lazy { Prefs(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.app_name)

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
            dialogFragment.show(requireActivity())
        }

        // Unsent List
        binding.unsentListButton.setOnClickListener {
            //startActivity(Intent(requireContext(), UnsentListActivity::class.java))
            parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, UnsentListFragment())
                    .addToBackStack(null)
                    .commit()
        }

        // Settings
        binding.settingsButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SettingsFragment())
                    .addToBackStack(null)
                    .commit()
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
            val intent = requireContext().packageManager.getLaunchIntentForPackage(MedolyEnvironment.MEDOLY_PACKAGE)
            if (intent == null) {
                toast(R.string.message_no_medoly)
                return@setOnClickListener
            }
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        updateAuthMessage()
    }

    /**
     * Update auth message.
     */
    private fun updateAuthMessage() {
        val prefs = Prefs(requireContext())
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
                    Caller.getInstance().cache = FileSystemCache(File(requireContext().cacheDir.path + File.separator + "last.fm"))
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
