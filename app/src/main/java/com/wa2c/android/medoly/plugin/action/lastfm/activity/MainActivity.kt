package com.wa2c.android.medoly.plugin.action.lastfm.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.databinding.ActivityMainBinding

/**
 * Main activity.
 */
class MainActivity : AppCompatActivity() {

    /** Binding */
    private lateinit var binding: ActivityMainBinding

    /**
     * Back stack change listener
     */
    private val backStackChangedListener = FragmentManager.OnBackStackChangedListener {
        // Tool bar
        supportFragmentManager.findFragmentById(R.id.fragment_container)?.let {
            val isHome = it is MainFragment
            supportActionBar?.setDisplayShowHomeEnabled(isHome)
            supportActionBar?.setDisplayHomeAsUpEnabled(!isHome)
        }
        // Title
        val backStackCount = supportFragmentManager.backStackEntryCount
        if (backStackCount > 0) {
            val name = supportFragmentManager.getBackStackEntryAt(backStackCount - 1).name
            if (!name.isNullOrEmpty()) title = name
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // ActionBar
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayShowTitleEnabled(true)
            setIcon(R.drawable.ic_launcher)
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container, MainFragment())
                    .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        supportFragmentManager.addOnBackStackChangedListener(backStackChangedListener)

    }

    override fun onStop() {
        super.onStop()
        supportFragmentManager.removeOnBackStackChangedListener(backStackChangedListener)
    }


//    /**
//     * Update auth message.
//     */
//    private fun updateAuthMessage() {
//        val prefs = Prefs(this)
//        val username: String? = prefs[R.string.prefkey_auth_username]
//        val password: String? = prefs[R.string.prefkey_auth_password]
//
//        if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
//            binding.accountAuthTextView.text = getString(R.string.message_account_auth)
//        } else {
//            binding.accountAuthTextView.text = getString(R.string.message_account_not_auth)
//        }
//    }
//
//    /**
//     * Auth user
//     * @param username Username
//     * @param password Password
//     */
//    private fun authUser(username: String, password: String) {
//        GlobalScope.launch(Dispatchers.Main) {
//            // Auth
//            val session = GlobalScope.async(Dispatchers.Default) {
//                try {
//                    Caller.getInstance().cache = FileSystemCache(File(cacheDir.path + File.separator + "last.fm"))
//                    return@async Authenticator.getMobileSession(username, password, Token.getConsumerKey(), Token.getConsumerSecret())
//                } catch (e: Exception) {
//                    logE(e)
//                    return@async null
//                }
//            }.await()
//            // Save user
//            if (session != null) {
//                prefs[R.string.prefkey_auth_username] = username
//                prefs[R.string.prefkey_auth_password] = password
//                toast(R.string.message_auth_success) // Succeed
//            } else {
//                prefs.remove(R.string.prefkey_auth_username)
//                prefs.remove(R.string.prefkey_auth_password)
//                toast(R.string.message_auth_failure) // Failed
//            }
//
//            updateAuthMessage()
//        }
//    }
//
//    /**
//     * Clear user
//     */
//    private fun clearUser() {
//        prefs.remove(R.string.prefkey_auth_username)
//        prefs.remove(R.string.prefkey_auth_password)
//        toast(R.string.message_account_clear)
//
//        updateAuthMessage()
//    }

}
