package com.wa2c.android.medoly.plugin.action.lastfm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.wa2c.android.medoly.library.MediaPluginIntent;
import com.wa2c.android.medoly.plugin.action.lastfm.service.PostIntentService;


/**
 * メッセージプラグイン受信レシーバ。
 */
public class PluginReceiver extends BroadcastReceiver {
    /**
     * メッセージ受信。
     * @param context コンテキスト。
     * @param intent インテント。
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // IntentService起動
        Intent serviceIntent = new Intent(intent);
        serviceIntent.setClass(context, PostIntentService.class);
        context.startService(serviceIntent);

        MediaPluginIntent pluginIntent = new MediaPluginIntent(intent);
        MediaPluginIntent returnIntent = pluginIntent.createReturnIntent(null, null);
        context.sendBroadcast(returnIntent);
    }

}
