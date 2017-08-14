package com.wa2c.android.medoly.plugin.action.lastfm.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Plugin receiver classes.
 */
public class PluginReceiver {

    public static abstract class AbstractPluginReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent serviceIntent = new Intent(intent);
            serviceIntent.putExtra(ProcessService.RECEIVED_CLASS_NAME, intent.getComponent().getClassName());
            serviceIntent.setClass(context, ProcessService.class);
            context.startService(serviceIntent);        }
    }

    // Event

    public static class EventScrobbleReceiver extends AbstractPluginReceiver { }

    public static class EventNowPlayingReceiver extends AbstractPluginReceiver { }

    public static class EventGetAlbumArtReceiver extends AbstractPluginReceiver { }

    public static class EventGetPropertyReceiver extends AbstractPluginReceiver { }

    // Execution

    public static class ExecuteLoveReceiver extends AbstractPluginReceiver { }

    public static class ExecuteUnLoveReceiver extends AbstractPluginReceiver { }

    public static class ExecuteGetAlbumArtReceiver extends AbstractPluginReceiver { }

    public static class ExecuteGetPropertyReceiver extends AbstractPluginReceiver { }

    public static class ExecuteTrackPageReceiver extends AbstractPluginReceiver { }

    public static class ExecuteLastfmSiteReceiver extends AbstractPluginReceiver { }

}
