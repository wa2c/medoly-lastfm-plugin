package com.wa2c.android.medoly.plugin.action.lastfm.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.wa2c.android.medoly.plugin.action.lastfm.service.PostIntentService;


/**
 * Plugin receiver.
 */
public class PluginReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Stop exists service
        Intent stopIntent = new Intent(context, PostIntentService.class);
        context.stopService(stopIntent);

        // Launch service
        Intent serviceIntent = new Intent(intent);
        serviceIntent.setClass(context, PostIntentService.class);
        context.startService(serviceIntent);
    }

}
