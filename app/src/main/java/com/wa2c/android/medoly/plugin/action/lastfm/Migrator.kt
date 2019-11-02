package com.wa2c.android.medoly.plugin.action.lastfm

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.wa2c.android.medoly.library.PluginOperationCategory
import com.wa2c.android.medoly.plugin.action.lastfm.util.logD
import com.wa2c.android.prefs.Prefs

/**
 * Migrator
 */
class Migrator(private val context: Context) {
    private val prefs = Prefs(context)

    /**
     * Get current app version code.
     * @return Current version.
     */
    private val currentVersionCode: Int
        get() {
            return try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                PackageInfoCompat.getLongVersionCode(packageInfo).toInt()
            } catch (e: Exception) {
                logD(e)
                0
            }
        }

    /**
     * Get saved app version code. (previous version)
     * @return Saved version.
     */
    private val savedVersionCode: Int
        get() = prefs.getInt(R.string.prefkey_app_version_code, 0)

    /**
     * Save current version.
     */
    private fun saveCurrentVersionCode() {
        val version = currentVersionCode
        prefs[R.string.prefkey_app_version_code] = version
    }



    /**
     * Version up.
     */
    fun versionUp(): Boolean {
        val prevVersionCode = savedVersionCode
        val currentVersionCode = currentVersionCode

        if (currentVersionCode <= prevVersionCode)
            return false

        // migration
        versionUpFrom17(prevVersionCode)

        // save version
        saveCurrentVersionCode()
        return true
    }

    /**
     * Ver > Ver. 2.1.0 (17)
     */
    private fun versionUpFrom17(prevVersionCode: Int) {
        if (prevVersionCode > 17)
            return

        // Get Property
        val property: String? = prefs[R.string.prefkey_event_get_property_operation]
        if (property == "OPERATION_MEDIA_OPEN")
            prefs[R.string.prefkey_event_get_property_operation] = PluginOperationCategory.OPERATION_MEDIA_OPEN.categoryValue
        else if (property == "OPERATION_PLAY_START")
            prefs[R.string.prefkey_event_get_property_operation] = PluginOperationCategory.OPERATION_PLAY_START.categoryValue

        // Get Album Art
        val albumArt: String? = prefs[R.string.prefkey_event_get_album_art_operation]
        if (albumArt == "OPERATION_MEDIA_OPEN")
            prefs[R.string.prefkey_event_get_album_art_operation] = PluginOperationCategory.OPERATION_MEDIA_OPEN.categoryValue
        else if (albumArt == "OPERATION_PLAY_START")
            prefs[R.string.prefkey_event_get_album_art_operation] = PluginOperationCategory.OPERATION_PLAY_START.categoryValue
    }

}
