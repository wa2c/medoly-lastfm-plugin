package com.wa2c.android.medoly.plugin.action.lastfm.plugin

/**
 * Command result.
 */
enum class CommandResult {
    /** Succeeded.  */
    SUCCEEDED,
    /** Failed.  */
    FAILED,
    /** Scrobble saved */
    SAVED,
    /** Authorization failed.  */
    AUTH_FAILED,
    /** No media.  */
    NO_MEDIA,
    /** Ignore.  */
    IGNORE
}
