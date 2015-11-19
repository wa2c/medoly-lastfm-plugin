package com.wa2c.android.medoly.plugin.action.lastfm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.gson.Gson;
import com.wa2c.android.medoly.utils.Logger;


/**
 * アプリユーティリティ。
 */
public class AppUtils {

    /**
     * トーストを表示。
     * @param context コンテキスト。
     * @param text メッセージ。
     */
    public static void showToast(Context context, String text) {
        ToastReceiver.showToast(context, text);
    }

    /**
     * トーストを表示。
     * @param context コンテキスト。
     * @param stringId メッセージ。
     */
    public static void showToast(final Context context, final int stringId) {
        showToast(context, context.getString(stringId));
    }



    /**
     * オブジェクトを設定に書込む。
     * @param context コンテキスト。
     * @param prefKey 設定キー。
     * @param saveObject 保存オブジェクト。
     * @return 成功した場合はtrue。
     */
    public static boolean saveObject(Context context, String prefKey,  Object saveObject) {
        try {
            Gson gson = new Gson();
            String json = gson.toJson(saveObject);

            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            pref.edit().putString(prefKey, json).apply();
            return true;
        } catch (Exception e) {
            Logger.e(e);
            return false;
        }
    }

    /**
     * オブジェクトを設定から読み込む。
     * @param context コンテキスト、
     * @param prefKey 設定キー。
     * @param clazz オブジェクトのクラス。
     * @return 設定。
     */
    public static <T> T loadObject(Context context, String prefKey, Class<T> clazz) {
        try {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            String json = pref.getString(prefKey, "");

            Gson gson = new Gson();

            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            Logger.e(e);
            return null;
        }
    }
}
