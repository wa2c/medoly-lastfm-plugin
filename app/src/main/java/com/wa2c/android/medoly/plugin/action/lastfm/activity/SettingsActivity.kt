package com.wa2c.android.medoly.plugin.action.lastfm.activity

import android.app.Activity
import android.os.Bundle
import android.view.MenuItem
import com.wa2c.android.medoly.plugin.action.lastfm.R


/**
 * Settings activity
 */
class SettingsActivity : Activity() {

    /**
     * onCreate event.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setTitle(R.string.title_activity_settings)
    }

    /**
     * onOptionsItemSelected event.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

}
