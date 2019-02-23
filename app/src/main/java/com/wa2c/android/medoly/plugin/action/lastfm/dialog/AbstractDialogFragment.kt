package com.wa2c.android.medoly.plugin.action.lastfm.dialog

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Button
import com.wa2c.android.prefs.Prefs
import java.util.*

/**
 * Abstract dialog class.
 */
abstract class AbstractDialogFragment : DialogFragment() {

    protected lateinit var prefs: Prefs

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog? {
        prefs = Prefs(activity)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        dialog?.cancel() // close on rotation
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as AlertDialog?
        if (dialog != null) {
            if (dialog.getButton(DialogInterface.BUTTON_POSITIVE) != null)
                setPositiveButton(dialog, dialog.getButton(DialogInterface.BUTTON_POSITIVE))
            if (dialog.getButton(DialogInterface.BUTTON_NEGATIVE) != null)
                setNegativeButton(dialog, dialog.getButton(DialogInterface.BUTTON_NEGATIVE))
            if (dialog.getButton(DialogInterface.BUTTON_NEUTRAL) != null)
                setNeutralButton(dialog, dialog.getButton(DialogInterface.BUTTON_NEUTRAL))
        }
    }

    override fun onStop() {
        super.onStop()
        dialog?.cancel()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        shownDialogMap.remove(this.javaClass.name)
    }



    /**
     * Set positive button
     */
    protected open fun setPositiveButton(dialog: AlertDialog, button: Button) {
        button.setOnClickListener {
            clickListener?.onClick(dialog, DialogClickListener.BUTTON_POSITIVE, null)
            dialog.dismiss()
        }
    }

    /**
     * Set negative button
     */
    protected open fun setNegativeButton(dialog: AlertDialog, button: Button) {
        button.setOnClickListener {
            clickListener?.onClick(dialog, DialogClickListener.BUTTON_NEGATIVE, null)
            dialog.dismiss()
        }
    }

    /**
     * Set neutral button
     */
    protected open fun setNeutralButton(dialog: AlertDialog, button: Button) {
        button.setOnClickListener {
            clickListener?.onClick(dialog, DialogClickListener.BUTTON_NEUTRAL, null)
            dialog.dismiss()
        }
    }


    /***
     * Show dialog.
     * @param activity A activity.
     */
    fun show(activity: Activity) {
        val key = this.javaClass.name
        shownDialogMap[key]?.dismiss()

        super.show(activity.fragmentManager, key)
        shownDialogMap[key] = this
    }


    /** Click listener. */
    var clickListener: DialogClickListener? = null



    companion object {
        private val shownDialogMap = HashMap<String, DialogFragment>()
    }

}
