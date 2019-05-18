package com.wa2c.android.medoly.plugin.action.lastfm.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.wa2c.android.prefs.Prefs

/**
 * Abstract dialog class.
 */
abstract class AbstractDialogFragment : DialogFragment() {

    /** The called activity.  */
    protected lateinit var context: FragmentActivity
    /** Prefs */
    protected lateinit var prefs: Prefs
    /** Click listener. */
    var clickListener: ((dialog: DialogInterface?, which: Int, bundle: Bundle?) -> Unit)? = null


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        context = activity!!
        prefs = Prefs(context)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        dialog?.cancel() // close on rotation
    }

    override fun onStart() {
        super.onStart()
        setDialogButtonEvent(DialogInterface.BUTTON_POSITIVE, null)
        setDialogButtonEvent(DialogInterface.BUTTON_NEGATIVE, null)
        setDialogButtonEvent(DialogInterface.BUTTON_NEUTRAL, null)
    }

    override fun onStop() {
        super.onStop()
        dialog?.cancel()
    }

    /**
     * Set button event. Use this onStart or later.
     * @param which The button id.
     * @param listener The event listener. null set default.
     */
    protected fun setDialogButtonEvent(which: Int, listener: View.OnClickListener?) {
        if (listener != null) {
            (dialog as AlertDialog?)?.getButton(which)?.setOnClickListener(listener)
        } else {
            (dialog as AlertDialog?)?.getButton(which)?.setOnClickListener {
                clickListener?.invoke(dialog, which, null)
                dialog?.dismiss()
            }
        }
    }

    /**
     * Fragment tag.
     */
    private val fragmentTag: String by lazy { this.javaClass.name }

    /***
     * Show dialog.
     * @param activity A activity.
     */
    fun show(activity: FragmentActivity?) {
        if (activity == null)
            return
        val manager = activity.supportFragmentManager
        val fragment = manager.findFragmentByTag(fragmentTag) as? AbstractDialogFragment
        fragment?.dismiss()
        super.show(manager, fragmentTag)
    }

}
