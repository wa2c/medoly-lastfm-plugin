package com.wa2c.android.medoly.plugin.action.lastfm.dialog

import android.content.DialogInterface
import android.os.Bundle

/**
 * Dialog click listener.
 */
interface DialogClickListener {

    /**
     * Invoke dialog click event.
     * @param dialog The dialog.
     * @param which The clicked button.
     * @param bundle Return data.
     */
    fun onClick(dialog: DialogInterface?, which: Int, bundle: Bundle?)

    companion object {
        /** Positive button. */
        const val BUTTON_POSITIVE = DialogInterface.BUTTON_POSITIVE
        /** Neutral button. */
        const val BUTTON_NEUTRAL = DialogInterface.BUTTON_NEUTRAL
        /** Negative button. */
        const val BUTTON_NEGATIVE = DialogInterface.BUTTON_NEGATIVE
    }
}
