package com.wa2c.android.medoly.plugin.action.lastfm.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.EditText

import com.wa2c.android.medoly.plugin.action.lastfm.R

/**
 * 確認ダイアログを表示する。
 */
class AuthDialogFragment : AbstractDialogFragment() {

    private var dialogAuthUsernameEditText: EditText? = null
    private var dialogAuthPasswordEditText: EditText? = null


    /**
     * 入力ユーザ名を取得する。
     * @return ユーザ名。
     */
    val username: String
        get() = dialogAuthUsernameEditText!!.text.toString()

    /**
     * 入力パスワードを取得する。
     * @return パスワード。
     */
    val password: String
        get() = dialogAuthPasswordEditText!!.text.toString()


    /**
     * onCreateDialogイベント処理。
     */
    override fun onCreateDialog(savedInstanceState: Bundle): Dialog? {
        val content = View.inflate(activity, R.layout.dialog_auth, null)

        val pref = PreferenceManager.getDefaultSharedPreferences(activity)
        val username = pref.getString(getString(R.string.prefkey_auth_username), "")

        // default
        dialogAuthUsernameEditText = content.findViewById(R.id.dialogAuthUsernameEditText) as EditText
        dialogAuthPasswordEditText = content.findViewById(R.id.dialogAuthPasswordEditText) as EditText
        dialogAuthUsernameEditText!!.setText(username)

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
         * ダイアログのインスタンスを作成する。
         * @return ダイアログのインスタンス。
         */
        fun newInstance(): AuthDialogFragment {
            val fragment = AuthDialogFragment()
            val args = Bundle()
            fragment.arguments = args

            return fragment
        }
    }

}
