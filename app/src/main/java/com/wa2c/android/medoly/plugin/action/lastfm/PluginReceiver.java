package com.wa2c.android.medoly.plugin.action.lastfm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.wa2c.android.medoly.plugin.action.ActionPluginParam;
import com.wa2c.android.medoly.plugin.action.Logger;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Result;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.cache.FileSystemCache;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;


/**
 * メッセージプラグイン受信レシーバ。
 */
public class PluginReceiver extends BroadcastReceiver {

    /** 前回のファイルパス設定キー。 */
    private static final String PREFKEY_PREVIOUS_MEDIA_PATH = "previous_media_path";

    /** コンテキスト。 */
    private Context context;
    /** 設定。 */
    private SharedPreferences sharedPreferences;

    /**
     * 投稿種別。
     */
    private enum PostType {
        /** Scrobble */
        SCROBBLE,
        /** Love */
        LOVE
    }



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
        if (categories == null || categories.size() == 0) {
            return;
        }

        if (!categories.contains(ActionPluginParam.PluginTypeCategory.TYPE_POST_MESSAGE.getCategoryValue())) {
            return;
        }

        // 値を取得
        HashMap<String, String> propertyMap = null;
        boolean isEvent = false;
        try {
            if (intent.hasExtra(ActionPluginParam.PLUGIN_VALUE_KEY)) {
                Serializable serializable = intent.getSerializableExtra(ActionPluginParam.PLUGIN_VALUE_KEY);
                if (serializable != null) {
                    propertyMap = (HashMap<String, String>) serializable;
                }
            }
            if (propertyMap == null || propertyMap.isEmpty()) { return; }

            if (intent.hasExtra(ActionPluginParam.PLUGIN_EVENT_KEY))
                isEvent = intent.getBooleanExtra(ActionPluginParam.PLUGIN_EVENT_KEY, false);
        } catch (ClassCastException | NullPointerException e) {
            Logger.e(e);
            return;
        }


        if (categories.contains(ActionPluginParam.PluginOperationCategory.OPERATION_PLAY_START.getCategoryValue())) {
           // 再生開始
           if (!isEvent || this.sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_start_enabled), false)) {
               post(propertyMap, PostType.SCROBBLE);
           }
        } else if (categories.contains(ActionPluginParam.PluginOperationCategory.OPERATION_PLAY_NOW.getCategoryValue())) {
           // 再生中
           if (!isEvent || this.sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_now_enabled), true)) {
               post(propertyMap, PostType.SCROBBLE);
           }
       } else if (categories.contains(ActionPluginParam.PluginOperationCategory.OPERATION_EXECUTE.getCategoryValue())) {
           // 実行
           Bundle extras = intent.getExtras();
           if (extras != null) {
               if (extras.keySet().contains("id_execute_tweet")) {
                   post(propertyMap, PostType.LOVE);
               } else if (extras.keySet().contains("id_execute_site")) {
                   String username = sharedPreferences.getString(context.getString(R.string.prefkey_auth_username), "");
                   Uri uri;
                   if (TextUtils.isEmpty(username)) {
                       // ユーザ未認証
                       uri = Uri.parse(context.getString(R.string.lastfm_url));
                   } else {
                       // ユーザ認証済
                       uri = Uri.parse(context.getString(R.string.lastfm_url_user, username));
                   }

                   Intent launchIntent = new Intent(Intent.ACTION_VIEW, uri);
                   try {
                       launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                       context.startActivity(launchIntent);
                   } catch (android.content.ActivityNotFoundException e) {
                       Logger.d(e);
                   }
               }
           }
       }
    }



    /**
     * 投稿。
     * @param propertyMap プロパティ情報。
     */
    @SuppressWarnings("unchecked")
    private void post(HashMap<String, String> propertyMap, PostType postType) {
        // 音楽データ無し
        if (!propertyMap.containsKey(ActionPluginParam.MediaProperty.FOLDER_PATH.getKeyName()) ||
            !propertyMap.containsKey(ActionPluginParam.MediaProperty.FILE_NAME.getKeyName())) {
            AppUtils.showToast(context, R.string.message_no_media);
            return;
        }

        // 情報無し
        if (!propertyMap.containsKey(ActionPluginParam.MediaProperty.TITLE.getKeyName()) &&
            !propertyMap.containsKey(ActionPluginParam.MediaProperty.ARTIST.getKeyName())) {
            return;
        }

       (new AsyncPostTask(propertyMap, postType)).execute();
    }

    /**
     * 投稿タスク。
     */
    private class AsyncPostTask extends AsyncTask<String, Void, Boolean> {
        /** プロパティマップ。 */
        private Map<String, String> propertyMap;
        /** 当校種別。 */
        private PostType postType;

        /** セッション */
        private Session session;

        private SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        public AsyncPostTask(Map<String, String> propertyMap, PostType postType) {
            this.propertyMap = propertyMap;
            this.postType = postType;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                // フォルダ設定 (getMobileSessionの前に入れる必要あり)
                Caller.getInstance().setCache(new FileSystemCache(new File(context.getExternalCacheDir().getPath() + File.separator + "last.fm")));

                // 認証
                String username = preferences.getString(context.getString(R.string.prefkey_auth_username), "");
                String password = preferences.getString(context.getString(R.string.prefkey_auth_password), "");
                session = Authenticator.getMobileSession(username, password, Token.getKey1(context), Token.getKey2(context));
                if (session == null) {
                    return false;
                }

                if (postType == PostType.SCROBBLE) {
                    // Scrobble
                    String key;
                    ScrobbleData data = new ScrobbleData();

                    key = ActionPluginParam.MediaProperty.MUSICBRAINZ_RELEASEID.getKeyName();
                    if (propertyMap.containsKey(key)) data.setMusicBrainzId(propertyMap.get(key));
                    key = ActionPluginParam.MediaProperty.TITLE.getKeyName();
                    if (propertyMap.containsKey(key)) data.setTrack(propertyMap.get(key));
                    key = ActionPluginParam.MediaProperty.ARTIST.getKeyName();
                    if (propertyMap.containsKey(key)) data.setArtist(propertyMap.get(key));
                    key = ActionPluginParam.MediaProperty.ALBUM_ARTIST.getKeyName();
                    if (propertyMap.containsKey(key)) data.setAlbumArtist(propertyMap.get(key));
                    key = ActionPluginParam.MediaProperty.ALBUM.getKeyName();
                    if (propertyMap.containsKey(key)) data.setAlbum(propertyMap.get(key));

                    try {
                        key = ActionPluginParam.MediaProperty.DURATION.getKeyName();
                        if (propertyMap.containsKey(key)) data.setDuration(Integer.valueOf(key));
                    } catch (Exception e) {
                        Logger.e(e);
                    }
                    try {
                        key = ActionPluginParam.MediaProperty.TRACK.getKeyName();
                        if (propertyMap.containsKey(key)) data.setTrackNumber(Integer.valueOf(key));
                    } catch (Exception e) {
                        Logger.e(e);
                    }
                    data.setTimestamp((int) (System.currentTimeMillis() / 1000));

                    ScrobbleResult result = Track.scrobble(data, session);
                    return result.isSuccessful();
                } else if (postType == PostType.LOVE) {
                    // Love
                    String track  = propertyMap.get(ActionPluginParam.MediaProperty.TITLE.getKeyName());
                    String artist  = propertyMap.get(ActionPluginParam.MediaProperty.ARTIST.getKeyName());

                    Result res = Track.love(artist, track, session);
                    return res.isSuccessful();
                } else {
                    return false;
                }

            } catch (Exception e) {
                Logger.e(e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (isCancelled())
                return; // キャンセルの場合は無視

            if (postType == PostType.SCROBBLE) {
                // Scrobble
                if (result) {
                    if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_tweet_success_message_show), false)) {
                        AppUtils.showToast(context, R.string.message_post_success); // Succeed
                    }
                } else {
                    if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_tweet_failure_message_show), true)) {
                        AppUtils.showToast(context, R.string.message_post_failure); // Failed
                    }
                }
            } else {
                // Love
                if (result) {
                    AppUtils.showToast(context, R.string.message_love_success); // Succeed
                } else {
                    AppUtils.showToast(context, R.string.message_love_failure); // Failed
                }
            }

            session = null;
        }
    }


}
