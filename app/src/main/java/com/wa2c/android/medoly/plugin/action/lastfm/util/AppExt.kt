package com.wa2c.android.medoly.plugin.action.lastfm.util

import android.content.Context
import androidx.annotation.StringRes
import timber.log.Timber

/** Log a verbose message */
fun logV(message: Any?, vararg args: Any?) = Timber.asTree().v(message.toString(), *args)

/** Log a debug message */
fun logD(message: Any?, vararg args: Any?) = Timber.asTree().d(message.toString(), *args)

/** Log an info message */
fun logI(message: Any?, vararg args: Any?) = Timber.asTree().i(message.toString(), *args)

/** Log a warning message */
fun logW(message: Any?, vararg args: Any?) = Timber.asTree().w(message.toString(), *args)

/** Log an error message */
fun logE(message: Any?, vararg args: Any?) = Timber.asTree().e(message.toString(), *args)

/** Show toast message */
fun Context.toast(@StringRes messageRes: Int) { ToastReceiver.showToast(this.applicationContext, messageRes) }

/** Show toast message */
fun Context.toast(message: Any?) { ToastReceiver.showToast(this.applicationContext, message.toString()) }

