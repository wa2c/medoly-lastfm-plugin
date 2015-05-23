package com.wa2c.android.medoly.plugin.action.lastfm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.cache.FileSystemCache;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;


/**
 * メッセージプラグイン受信レシーバ。
 */
public class PluginReceiver extends BroadcastReceiver {

    /** 値マップのキー。 */
    private static final String PLUGIN_VALUE_KEY  = "value_map";
    /** 前回のファイルパス設定キー。 */
    private static final String PREFKEY_PREVIOUS_MEDIA_PATH = "previous_media_path";

    /** コンテキスト。 */
    private Context context;
    /** 設定。 */
    private SharedPreferences sharedPreferences;


    /**
     * メッセージ受信。
     * @param context コンテキスト。
     * @param intent インテント。
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        Set<String> categories = intent.getCategories();
       if (categories.contains(ActionPluginParam.PluginOperationCategory.OPERATION_PLAY_START.getCategoryValue())) {
           // 再生開始
           if (this.sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_start_enabled), false)) {
               post(intent);
           }
        } else if (categories.contains(ActionPluginParam.PluginOperationCategory.OPERATION_PLAY_NOW.getCategoryValue())) {
           // 再生中
           if (this.sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_now_enabled), true)) {
               post(intent);
           }
       }
    }

    /**
     * 投稿前準備。
     * @param intent インテント。
     */
    @SuppressWarnings("unchecked")
    private void post(Intent intent) {
        Serializable serializable = intent.getSerializableExtra(PLUGIN_VALUE_KEY);
        if (serializable != null) {
            HashMap<String, String> propertyMap = (HashMap<String, String>) serializable;

            String filePath = propertyMap.get(ActionPluginParam.MediaProperty.FOLDER_PATH.getKeyName()) + propertyMap.get(ActionPluginParam.MediaProperty.FILE_NAME.getKeyName());
            String previousMediaPath = sharedPreferences.getString(PREFKEY_PREVIOUS_MEDIA_PATH, "");
            boolean previousMediaEnabled = sharedPreferences.getBoolean(context.getString(R.string.prefkey_previous_media_enabled), false);
            if (!TextUtils.isEmpty(filePath) && !TextUtils.isEmpty(previousMediaPath) && filePath.equals(previousMediaPath) && !previousMediaEnabled) {
                // 前回と同じメディアは無視
                return;
            }

           (new AsyncPostTask(propertyMap)).execute();
           sharedPreferences.edit().putString(PREFKEY_PREVIOUS_MEDIA_PATH, filePath).apply();
        }
    }

    /**
     * 投稿タスク。
     */
    private class AsyncPostTask extends AsyncTask<String, Void, Boolean> {
        /** プロパティマップ。 */
        private Map<String, String> propertyMap;
        /** セッション */
        private Session session;

        private SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        public AsyncPostTask(Map<String, String> propertyMap) {
            this.propertyMap = propertyMap;

        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                // フォルダ設定 (getMobileSessionの前に入れる必要あり)
                Caller.getInstance().setCache(new FileSystemCache(new File(context.getExternalCacheDir().getPath() + File.separator + "last.fm")));

                // 認証
                if (session == null) {
                    String username = preferences.getString(context.getString(R.string.prefkey_auth_username), "");
                    String password = preferences.getString(context.getString(R.string.prefkey_auth_password), "");
                    String k = context.getString(R.string.base_app_name) + "__" + context.getString(R.string.domain_name);
                    session = Authenticator.getMobileSession(username, password, Token.getKey1(context), Token.getKey2(context));
                    if (session == null) {
                        return false;
                    }
                }

                // 送信
                ScrobbleData data = new ScrobbleData();
                data.setMusicBrainzId(propertyMap.get(ActionPluginParam.MediaProperty.MUSICBRAINZ_RELEASEID.getKeyName()));
                data.setTrack(propertyMap.get(ActionPluginParam.MediaProperty.TITLE.getKeyName()));
                data.setArtist(propertyMap.get(ActionPluginParam.MediaProperty.ARTIST.getKeyName()));
                data.setAlbumArtist(propertyMap.get(ActionPluginParam.MediaProperty.ALBUM_ARTIST.getKeyName()));
                data.setAlbum(propertyMap.get(ActionPluginParam.MediaProperty.ALBUM.getKeyName()));
                try { data.setTrackNumber(Integer.valueOf(propertyMap.get(ActionPluginParam.MediaProperty.TRACK.getKeyName()))); } catch (Exception e) { Logger.e(e); }
                data.setTimestamp((int) (System.currentTimeMillis() / 1000));

                ScrobbleResult result = Track.scrobble(data, session);
                return result.isSuccessful();
            } catch (Exception e) {
                Logger.e(e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_tweet_success_message_show), false)) {
                    AppUtils.showToast(context, R.string.message_post_success); // Succeed
                }
            } else {
                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_tweet_failure_message_show), true)) {
                    AppUtils.showToast(context, R.string.message_post_failure); // Failed
                }
                session = null;
            }
        }
    }


}
