package com.wa2c.android.medoly.plugin.action.lastfm.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils;


/**
 * Event receiver.
 */
public class EventReceiver {
     public static class EventAllReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            AppUtils.startService(context, intent);
        }
    }
}
