package com.wa2c.android.medoly.plugin.action.lastfm.activity

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.preference.MultiSelectListPreference
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.provider.Settings
import android.text.InputType
import android.text.TextUtils

import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.dialog.AboutDialogFragment

import java.util.HashMap
import java.util.LinkedHashMap


/**
 * Settings Activity
 */
class SettingsFragment : PreferenceFragment() {


    /**
     * App info.
     */
    private val applicationDetailsPreferenceClickListener = Preference.OnPreferenceClickListener {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.data = Uri.parse("package:" + activity.packageName)
        startActivity(intent)
        true
    }

    /**
     * About.
     */
    private val aboutPreferenceClickListener = Preference.OnPreferenceClickListener {
        AboutDialogFragment.newInstance().show(activity)
        true
    }

    /**
     * On change settings.
     */
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key -> updatePrefSummary(findPreference(key)) }

    /**
     * onCreate event.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref_settings)

        // App info
        findPreference(getString(R.string.prefkey_application_details)).onPreferenceClickListener = applicationDetailsPreferenceClickListener
        // About
        findPreference(getString(R.string.prefkey_about)).onPreferenceClickListener = aboutPreferenceClickListener

        initSummary(preferenceScreen)
    }

    /**
     * onResume event.
     */
    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * onPause event.
     */
    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Initialize summary.
     * @param p target item.
     */
    private fun initSummary(p: Preference?) {
        if (p == null) return

        // get summary length
        val summary = p.summary
        if (summary != null && summary.length > 0) {
            if (summary.toString().lastIndexOf("\n") != 0) p.summary = summary.toString() + "\n" // add break
            summaryLengthMap.put(p, p.summary.length)
        } else {
            summaryLengthMap.put(p, 0)
        }

        // update summary
        if (p is PreferenceCategory) {
            val pCat = p as PreferenceCategory?
            for (i in 0 until pCat!!.preferenceCount) {
                initSummary(pCat!!.getPreference(i))
            }
        } else if (p is PreferenceScreen) {
            val ps = p as PreferenceScreen?
            for (i in 0 until ps!!.preferenceCount) {
                initSummary(ps!!.getPreference(i))
            }
        } else {
            updatePrefSummary(p)
        }
    }

    /**
     * Update summary.
     * @param p target preference.
     */
    private fun updatePrefSummary(p: Preference?) {
        if (p == null) return

        val key = p.key
        var summary = p.summary
        if (TextUtils.isEmpty(key)) return
        if (TextUtils.isEmpty(summary)) summary = ""

        // for instance type
        if (p is ListPreference) {
            // ListPreference
            val pref = p as ListPreference?
            pref!!.value = p.sharedPreferences.getString(pref.key, "")
            p.setSummary(summary.subSequence(0, summaryLengthMap[p]).toString() + getString(R.string.settings_summary_current_value, pref.entry))
        } else if (p is MultiSelectListPreference) {
            // MultiSelectListPreference
            val pref = p as MultiSelectListPreference?
            val stringSet = pref!!.sharedPreferences.getStringSet(pref.key, null)
            var text = ""
            if (stringSet != null && stringSet.size > 0) {
                pref.values = stringSet // update value once
                val builder = StringBuilder()
                for (i in 0 until pref.entries.size) {
                    if (stringSet.contains(pref.entryValues[i])) {
                        builder.append(pref.entries[i]).append(",")
                    }
                }
                if (builder.length > 0) {
                    text = builder.substring(0, builder.length - 1) // remove end comma
                }
            }
            p.setSummary(summary.subSequence(0, summaryLengthMap[p]).toString() + getString(R.string.settings_summary_current_value, text))
        } else if (p is EditTextPreference) {
            // EditTextPreference
            val pref = p as EditTextPreference?
            var text = p.sharedPreferences.getString(pref!!.key, "")

            // adjust numeric values
            val inputType = pref.editText.inputType
            try {
                if (inputType and InputType.TYPE_CLASS_NUMBER > 0) {
                    if (inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL > 0) {
                        // float
                        var `val` = java.lang.Float.valueOf(text)!!
                        if (inputType and InputType.TYPE_NUMBER_FLAG_SIGNED == 0 && `val` < 0) {
                            `val` = 0f
                        }
                        text = `val`.toString()
                    } else {
                        // integer
                        var `val` = Integer.valueOf(text)!!
                        if (inputType and InputType.TYPE_NUMBER_FLAG_SIGNED == 0 && `val` < 0) {
                            `val` = 0
                        }
                        text = `val`.toString()
                    }
                }
            } catch (e: NumberFormatException) {
                text = "0"
            }

            pref.text = text // update once
            p.setSummary(summary.subSequence(0, summaryLengthMap[p]).toString() + getString(R.string.settings_summary_current_value, text))
        }
    }

    companion object {

        /** Summary length map.  */
        private val summaryLengthMap = LinkedHashMap<Preference, Int>()
    }

}
