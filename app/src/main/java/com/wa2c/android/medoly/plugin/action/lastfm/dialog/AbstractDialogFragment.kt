package com.wa2c.android.medoly.plugin.action.lastfm.dialog

import android.app.Activity
import android.app.Dialog
import android.app.DialogFragment
import android.app.Fragment
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast

import com.wa2c.android.medoly.plugin.action.lastfm.R

import java.util.HashMap


/**
 * ダイアログの抽象クラス。
 */
abstract class AbstractDialogFragment : DialogFragment() {

    /** アクティビティ。  */
    protected var context: Activity
    /** 設定。  */
    protected var preferences: SharedPreferences


    /** クリックのリスナ。  */
    protected var clickListener: DialogInterface.OnClickListener? = null


    /***
     * ダイアログを表示する。
     * @param activity アクティビティ。
     */
    fun show(activity: Activity?) {
        if (activity == null) {
            Toast.makeText(getActivity(), R.string.error_dialog_dismissed, Toast.LENGTH_SHORT).show()
            return
        }

        val key = this.javaClass.getName()
        if (shownDialogMap.containsKey(key) && shownDialogMap[key] != null) {
            shownDialogMap[key].dismiss()
        }

        super.show(activity.fragmentManager, key)
        shownDialogMap.put(key, this)
    }

    /**
     * ダイアログを表示する。
     * @param fragment フラグメント。
     */
    fun show(fragment: Fragment?) {
        if (fragment == null) {
            Toast.makeText(activity, R.string.error_dialog_dismissed, Toast.LENGTH_SHORT).show()
            return
        }

        val key = this.javaClass.getName()
        if (shownDialogMap.containsKey(key) && shownDialogMap[key] != null) {
            shownDialogMap[key].dismiss()
        }

        super.show(fragment.fragmentManager, key)
        shownDialogMap.put(key, this)
    }


    /**
     * onCreateDialogイベント処理。
     */
    override fun onCreateDialog(savedInstanceState: Bundle): Dialog? {
        super.onCreateDialog(savedInstanceState)

        context = activity
        preferences = PreferenceManager.getDefaultSharedPreferences(context)

        return null
    }

    /**
     * onConfigurationChangedイベント処理。
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val d = dialog
        d?.cancel() // 回転等が発生した場合は閉じる
    }

    /**
     * onStartイベント。
     */
    override fun onStart() {
        super.onStart()
    }

    /**
     * onStopイベント処理。 c
     */
    override fun onStop() {
        super.onStop()
        val d = dialog
        d?.cancel() // 回転等が発生した場合は閉じる
    }

    /**
     * onDissmissイベント処理。 c
     */
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        val key = this.javaClass.getName()
        shownDialogMap.remove(key)
    }

    /**
     * クリックのリスナを設定する。
     * @param listener 実行処理。
     */
    fun setClickListener(listener: DialogInterface.OnClickListener) {
        this.clickListener = listener
    }

    /**
     * クリックイベントを実行する。
     * @param dialog ダイアログ。
     * @param which クリックされたボタン。
     * @param close ダイアログを閉じる場合はtrue。
     */
    @JvmOverloads protected fun onClickButton(dialog: DialogInterface?, which: Int, close: Boolean = true) {
        if (dialog != null && clickListener != null) {
            clickListener!!.onClick(dialog, which)
            if (close) dialog.dismiss()
        } else {
            if (dialog != null && close) dialog.cancel()
        }
    }

    companion object {

        /**
         * ダイアログ状態管理。
         */
        private val shownDialogMap = HashMap<String, DialogFragment>()
    }

}
/**
 * クリックイベントを実行する。
 * @param dialog ダイアログ。
 * @param which クリックされたボタン。
 */
