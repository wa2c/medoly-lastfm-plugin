package com.wa2c.android.medoly.plugin.action.lastfm.dialog

import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import android.view.LayoutInflater
import com.wa2c.android.medoly.plugin.action.lastfm.R
import com.wa2c.android.medoly.plugin.action.lastfm.databinding.DialogAuthBinding

/**
 * Authentication dialog.
 */
class AuthDialogFragment : AbstractDialogFragment() {

    private lateinit var binding: DialogAuthBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        binding = DataBindingUtil.inflate(LayoutInflater.from(activity), R.layout.dialog_auth, null, false)

        val username = prefs.getString(R.string.prefkey_auth_username)
        binding.dialogAuthUsernameEditText.setText(username)

        // Dialog build
        val builder = AlertDialog.Builder(context)
        builder.setView(binding.root)
        builder.setTitle(R.string.title_dialog_auth)

        // Auth
        builder.setPositiveButton(R.string.label_dialog_auth_auth, null)
        // Clear
        builder.setNegativeButton(R.string.label_dialog_auth_clear, null)
        // Cancel
        builder.setNeutralButton(android.R.string.cancel, null)

        return builder.create()
    }

    override fun invokeListener(which: Int, bundle: Bundle?, close: Boolean) {
        val result = bundle ?: Bundle()
        if (which == DialogInterface.BUTTON_POSITIVE) {
            result.putString(RESULT_USERNAME, binding.dialogAuthUsernameEditText.text.toString())
            result.putString(RESULT_PASSWORD, binding.dialogAuthPasswordEditText.text.toString())
        }
        super.invokeListener(which, result, close)
    }

    companion object {
        const val RESULT_USERNAME = "RESULT_USERNAME"
        const val RESULT_PASSWORD = "RESULT_PASSWORD"

        /**
         * Create dialog instance
         */
        fun newInstance(): AuthDialogFragment {
            val args = Bundle()

            val fragment = AuthDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

}
