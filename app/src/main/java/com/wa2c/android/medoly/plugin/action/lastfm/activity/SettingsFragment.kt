package com.wa2c.android.medoly.plugin.action.lastfm.activity

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.mikepenz.aboutlibraries.LibsBuilder
import com.thelittlefireman.appkillermanager.managers.KillerManager
import com.wa2c.android.medoly.plugin.action.lastfm.BuildConfig
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.activity.component.initSummary
import com.wa2c.android.medoly.plugin.action.lastfm.activity.component.preference
import com.wa2c.android.medoly.plugin.action.lastfm.activity.component.setListener
import com.wa2c.android.medoly.plugin.action.lastfm.activity.component.updatePrefSummary
import com.wa2c.android.medoly.plugin.action.lastfm.util.toast


/**
 * Settings fragment
 */
class SettingsFragment : PreferenceFragmentCompat() {

    /** On change settings. */
    private val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> updatePrefSummary(key) }

    /** KillerManager action */
    private var managerAction: KillerManager.Actions? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_settings)

        // Auto Start Manager
        KillerManager.init(activity)
        managerAction = when {
            KillerManager.isActionAvailable(activity, KillerManager.Actions.ACTION_POWERSAVING) -> KillerManager.Actions.ACTION_POWERSAVING
            KillerManager.isActionAvailable(activity, KillerManager.Actions.ACTION_AUTOSTART) -> KillerManager.Actions.ACTION_AUTOSTART
            KillerManager.isActionAvailable(activity, KillerManager.Actions.ACTION_NOTIFICATIONS) -> KillerManager.Actions.ACTION_NOTIFICATIONS
            else -> null
        }

        // Unsent Max
        (findPreference(getString(R.string.prefkey_unsent_max)) as? EditTextPreference)?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL
            it.keyListener = DigitsKeyListener.getInstance("0123456789")
            it.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(5))
        }

        setClickListener()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.title_screen_settings)
        initSummary(preferenceScreen)
        preference<Preference>(R.string.prefkey_info_app_version)?.summary = BuildConfig.VERSION_NAME
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(changeListener)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(changeListener)
    }


    private fun setClickListener() {
        // Auto Start Manager
        if (managerAction != null) {
            setListener(R.string.prefkey_device_auto_start) {
                activity?.let {
                    if (!KillerManager.doAction(it, managerAction)) {
                        it.toast(R.string.message_unsupported_device)
                    }
                }
            }
        } else {
            preference<Preference>(R.string.prefkey_device_auto_start)?.isEnabled = false
        }

        // App Version
        setListener(R.string.prefkey_info_app_version) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_version_url))))
        }

        // Author
        setListener(R.string.prefkey_info_author) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_author_url))))
        }

        // Manual
        setListener(R.string.prefkey_info_manual) {
            Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_manual_url))).let {
                startActivity(it)
            }
        }

        // License
        setListener(R.string.prefkey_info_app_license) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_license_url))))
        }

        // Open Source License
        setListener(R.string.prefkey_info_library_license) {
            val libs = LibsBuilder().withAboutAppName(getString(R.string.app_name))
            val ft = parentFragmentManager.beginTransaction()
            ft.replace(R.id.fragment_container, libs.supportFragment())
            ft.addToBackStack(getString(R.string.pref_title_info_library_license))
            ft.commit()
        }

        // Privacy Policy
        setListener(R.string.prefkey_info_privacy_policy) {
            val url = getString(R.string.app_privacy_policy_url)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        // App Info
        setListener(R.string.prefkey_info_app) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + requireContext().packageName)))
        }

        // App Store
        setListener(R.string.prefkey_info_store) {
            val url = getString(R.string.app_store_url)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }


}
