package com.wa2c.android.medoly.plugin.action.lastfm.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Plugin receiver classes.
 */
public class PluginReceivers {

    public static abstract class AbstractPluginReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent serviceIntent = new Intent(intent);
            Class c = this.getClass();
            serviceIntent.putExtra(ProcessService.RECEIVED_CLASS_NAME, c.getName());

            if (c == EventScrobbleReceiver.class ||
                c == EventNowPlayingReceiver.class ||
                c == ExecuteLoveReceiver.class ||
                c == ExecuteUnLoveReceiver.class) {
                serviceIntent.setClass(context, PluginPostService.class);
            } else if (
                    c == EventGetAlbumArtReceiver.class ||
                    c == ExecuteGetAlbumArtReceiver.class) {
                serviceIntent.setClass(context, PluginGetAlbumArtService.class);
            } else if (c == EventGetPropertyReceiver.class ||
                    c == ExecuteGetPropertyReceiver.class) {
                serviceIntent.setClass(context, PluginGetPropertyService.class);
            } else {
                serviceIntent.setClass(context, PluginRunService.class);
            }

            context.stopService(serviceIntent);
            context.startService(serviceIntent);
        }
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
