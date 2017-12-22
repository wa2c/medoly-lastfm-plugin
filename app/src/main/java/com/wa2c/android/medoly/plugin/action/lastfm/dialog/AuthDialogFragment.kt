package com.wa2c.android.medoly.plugin.action.lastfm.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import com.wa2c.android.medoly.plugin.action.lastfm.R

import kotlinx.android.synthetic.main.dialog_auth.*
import kotlinx.android.synthetic.main.dialog_auth.view.*

/**
 * Authentication dialog.
 */
class AuthDialogFragment : AbstractDialogFragment() {

    /**
     * Get input user name.
     * @return Input user name.
     */
    val username: String
        get() = dialog?.dialogAuthUsernameEditText?.text.toString()

    /**
     * Get input user name.
     * @return Input password.
     */
    val password: String
        get() = dialog?.dialogAuthPasswordEditText?.text.toString()


    /**
     * onCreateDialogイベント処理。
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog? {
        super.onCreateDialog(savedInstanceState)
        val content = View.inflate(activity, R.layout.dialog_auth, null)

        val pref = PreferenceManager.getDefaultSharedPreferences(activity)
        val username = pref.getString(getString(R.string.prefkey_auth_username), "")


        // default
//        dialogAuthUsernameEditText = content.findViewById(R.id.dialogAuthUsernameEditText) as EditText
//        dialogAuthPasswordEditText = content.findViewById(R.id.dialogAuthPasswordEditText) as EditText
        content.dialogAuthUsernameEditText.setText(username)

        // Dialog build
        val builder = AlertDialog.Builder(activity)
        builder.setView(content)
        builder.setTitle(R.string.title_dialog_auth)

        // Auth
        builder.setPositiveButton(R.string.label_dialog_auth_auth, clickListener)
        // Clear
        builder.setNegativeButton(R.string.label_dialog_auth_clear, clickListener)
        // Cancel
        builder.setNeutralButton(android.R.string.cancel, clickListener)


        return builder.create()
    }

    companion object {
        /**
         *Create dialog instance
         */
        fun newInstance(): AuthDialogFragment {
            val fragment = AuthDialogFragment()
            val args = Bundle()
            fragment.arguments = args

            return fragment
        }
    }

}
