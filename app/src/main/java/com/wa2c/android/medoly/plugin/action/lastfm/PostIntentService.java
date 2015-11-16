package com.wa2c.android.medoly.plugin.action.lastfm;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.wa2c.android.medoly.library.MediaProperty;
import com.wa2c.android.medoly.library.MedolyParam;
import com.wa2c.android.medoly.library.PluginOperationCategory;
import com.wa2c.android.medoly.utils.Logger;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
 * 投稿サービス。
 */
public class PostIntentService extends IntentService {

    /** 前回のファイルパス設定キー。 */
    private static final String PREFKEY_PREVIOUS_MEDIA_URI = "previous_media_uri";

    /**
     * 投稿種別。
     */
    private enum PostType {
        /** Scrobble */
        SCROBBLE,
        /** Love */
        LOVE,
        /** Unlove */
        UNLOVE,
        /** Ban */
        BAN,
        /** Unban */
        UNBAN,
    }


    /**
     * 投稿結果。
     */
    private enum PostResult {
        /** 成功。 */
        SUCCEEDED,
        /** 失敗。 */
        FAILED,
        /** 認証失敗。 */
        AUTH_FAILED,
        /** 投稿一時保存。 */
        SAVED,
        /** 無視。 */
        IGNORE
    }




    /** コンテキスト。 */
    private Context context = null;
    /** 設定。 */
    private SharedPreferences sharedPreferences = null;
    /** 受信データ。 */
    private HashMap<String, String> propertyMap = null;
    /** メディアURI。 */
    private Uri mediaUri = null;



    /**
     * コンストラクタ。
     */
    public PostIntentService() {
        super(PostIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null)
            return;

        try {
            context = getApplicationContext();
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            Bundle extras = intent.getExtras();

            // URIを取得
            Object extraStream;
            if (extras != null && (extraStream = intent.getExtras().get(Intent.EXTRA_STREAM)) != null && extraStream instanceof Uri) {
                mediaUri = (Uri) extraStream;
            } else if (intent.getData() != null) {
                // Old version
                mediaUri = intent.getData();
            }

            // 値を取得
            boolean isEvent = false;
            try {
                if (intent.hasExtra(MedolyParam.PLUGIN_VALUE_KEY)) {
                    Serializable serializable = intent.getSerializableExtra(MedolyParam.PLUGIN_VALUE_KEY);
                    if (serializable != null) {
                        propertyMap = (HashMap<String, String>) serializable;
                    }
                }
                if (propertyMap == null || propertyMap.isEmpty()) {
                    return;
                }

                if (intent.hasExtra(MedolyParam.PLUGIN_EVENT_KEY))
                    isEvent = intent.getBooleanExtra(MedolyParam.PLUGIN_EVENT_KEY, false);
            } catch (ClassCastException | NullPointerException e) {
                Logger.e(e);
                return;
            }

            // カテゴリを取得
            Set<String> categories = intent.getCategories();
            if (categories == null || categories.size() == 0) {
                return;
            }

            // 各アクション実行
            if (categories.contains(PluginOperationCategory.OPERATION_PLAY_START.getCategoryValue())) {
                // Play Start
                if (!isEvent || sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_start_enabled), false)) {
                    startPost(PostType.SCROBBLE);
                }
            } else if (categories.contains(PluginOperationCategory.OPERATION_PLAY_NOW.getCategoryValue())) {
                // Play Now
                if (!isEvent || sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_now_enabled), true)) {
                    startPost(PostType.SCROBBLE);
                }
            } else if (categories.contains(PluginOperationCategory.OPERATION_EXECUTE.getCategoryValue())) {
                // Execute
                final String EXECUTE_LOVE_ID = "execute_id_love";
                final String EXECUTE_UNLOVE_ID = "execute_id_unlove";
                final String EXECUTE_BAN_ID = "execute_id_ban";
                final String EXECUTE_UNBAN_ID = "execute_id_unban";
                final String EXECUTE_SITE_ID = "execute_id_site";

                if (extras != null) {
                    if (extras.keySet().contains(EXECUTE_LOVE_ID)) {
                        // Love
                        startPost(PostType.LOVE);
                    } else if (extras.keySet().contains(EXECUTE_UNLOVE_ID)) {
                        // UnLove
                        startPost(PostType.UNLOVE);
                    } else if (extras.keySet().contains(EXECUTE_BAN_ID)) {
                        // Ban
                        startPost(PostType.BAN);
                    } else if (extras.keySet().contains(EXECUTE_UNBAN_ID)) {
                        // UnBan
                        startPost(PostType.UNBAN);
                    } else if (extras.keySet().contains(EXECUTE_SITE_ID)) {
                        // Last.fm
                        String username = sharedPreferences.getString(context.getString(R.string.prefkey_auth_username), "");
                        Uri siteUri;
                        if (TextUtils.isEmpty(username)) {
                            // ユーザ未認証
                            siteUri = Uri.parse(context.getString(R.string.lastfm_url));
                        } else {
                            // ユーザ認証済
                            siteUri = Uri.parse(context.getString(R.string.lastfm_url_user, username));
                        }

                        Intent launchIntent = new Intent(Intent.ACTION_VIEW, siteUri);
                        try {
                            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(launchIntent);
                        } catch (android.content.ActivityNotFoundException e) {
                            Logger.d(e);
                        }
                    }
                }
            }
        } finally {
            context = null;
            sharedPreferences = null;
            propertyMap = null;
            mediaUri = null;
        }
    }


    /**
     * 投稿。
     * @param postType 投稿k種別。
     */
    @SuppressWarnings("unchecked")
    private void startPost(PostType postType) {
        // 音楽データ無し
        if (mediaUri == null) {
            AppUtils.showToast(getApplicationContext(), R.string.message_no_media);
            return;
        }

        // 必須情報無し
        if (TextUtils.isEmpty(propertyMap.get(MediaProperty.TITLE.getKeyName())) ||
            TextUtils.isEmpty(propertyMap.get(MediaProperty.ARTIST.getKeyName()))) {
            return;
        }

        if (postType == PostType.SCROBBLE) {
            String mediaUriText = mediaUri.toString();
            String previousMediaUri = sharedPreferences.getString(PREFKEY_PREVIOUS_MEDIA_URI, "");
            boolean previousMediaEnabled = sharedPreferences.getBoolean(context.getString(R.string.prefkey_previous_media_enabled), false);
            if (!previousMediaEnabled && !TextUtils.isEmpty(mediaUriText) && !TextUtils.isEmpty(previousMediaUri) && mediaUriText.equals(previousMediaUri)) {
                // 前回と同じメディアは無視
                return;
            }
            sharedPreferences.edit().putString(PREFKEY_PREVIOUS_MEDIA_URI, mediaUriText).apply();
        }

        PostResult result = post(postType);
        onPostExecute(result,  postType);
    }


    private PostResult post(PostType postType) {
        // 認証情報取得
        String username = sharedPreferences.getString(context.getString(R.string.prefkey_auth_username), "");
        String password = sharedPreferences.getString(context.getString(R.string.prefkey_auth_password), "");
        if (TextUtils.isEmpty(username)) {
            return PostResult.AUTH_FAILED;
        }

        /** セッション */
        Session session = null;

        try {
            // 認証
            // フォルダ設定 (getMobileSessionの前に入れる必要あり)
            Caller.getInstance().setCache(new FileSystemCache(new File(context.getExternalCacheDir().getPath() + File.separator + "last.fm")));
            session = Authenticator.getMobileSession(username, password, Token.getKey1(context), Token.getKey2(context));
        } catch (Exception e) {
            // 認証失敗してもエラーとしない
            Logger.e(e);
        }

        try {
            if (postType == PostType.SCROBBLE) {
                return scrobble(session);
            } else if (postType == PostType.LOVE) {
                return love(session);
            } else if (postType == PostType.UNLOVE) {
                return unlove(session);
            } else if (postType == PostType.BAN) {
                return ban(session);
            } else if (postType == PostType.UNBAN) {
                return unban(session);
            } else {
                return PostResult.IGNORE;
            }
        } catch (Exception e) {
            Logger.e(e);
            return PostResult.FAILED;
        }
    }




    /**
     * Scrobble.
     * @param session セッション。
     * @return 投稿結果。
     */
    private PostResult scrobble(Session session) {
        if (propertyMap == null)
            return PostResult.FAILED;

        try {
            String key;
            ScrobbleData newData = new ScrobbleData();

            key = MediaProperty.MUSICBRAINZ_RELEASEID.getKeyName();
            if (propertyMap.containsKey(key)) newData.setMusicBrainzId(propertyMap.get(key));
            key = MediaProperty.TITLE.getKeyName();
            if (propertyMap.containsKey(key)) newData.setTrack(propertyMap.get(key));
            key = MediaProperty.ARTIST.getKeyName();
            if (propertyMap.containsKey(key)) newData.setArtist(propertyMap.get(key));
            key = MediaProperty.ALBUM_ARTIST.getKeyName();
            if (propertyMap.containsKey(key)) newData.setAlbumArtist(propertyMap.get(key));
            key = MediaProperty.ALBUM.getKeyName();
            if (propertyMap.containsKey(key)) newData.setAlbum(propertyMap.get(key));

            try {
                key = MediaProperty.DURATION.getKeyName();
                if (propertyMap.containsKey(key)) newData.setDuration(Integer.valueOf(key));
            } catch (Exception e) {
                Logger.e(e);
            }
            try {
                key = MediaProperty.TRACK.getKeyName();
                if (propertyMap.containsKey(key)) newData.setTrackNumber(Integer.valueOf(key));
            } catch (Exception e) {
                Logger.e(e);
            }
            newData.setTimestamp((int) (System.currentTimeMillis() / 1000));

            // 無効データを無視
            if (TextUtils.isEmpty(newData.getTrack()) || TextUtils.isEmpty(newData.getArtist()))
                return PostResult.IGNORE;

            // 送信データ作成
            List<ScrobbleData> dataList = new ArrayList<>();
            ScrobbleData[] dataArray = AppUtils.loadObject(context, context.getString(R.string.prefkey_unsent_scrobble_data), ScrobbleData[].class);
            if (dataArray != null) dataList.addAll(Arrays.asList(dataArray)); // 未送信データ読み込み
            dataList.add(newData);

            // セッションがある場合は送信
            if (session != null) {
                List<ScrobbleResult> resultList = new ArrayList<>(dataList.size());
                final int maxSize = 50;
                int from = 0;
                boolean cancelSending = true;
                while (from < dataList.size()) {
                    int to = Math.min(from + maxSize, dataList.size());
                    List<ScrobbleData> subDataList = dataList.subList(from, to);
                    resultList.addAll(Track.scrobble(subDataList, session));
                    for (ScrobbleResult r : resultList) {
                        if (r.isSuccessful()) {
                            cancelSending = false;
                            break;
                        }
                    }
                    if (cancelSending) break; // 1件も送信できていない場合、以降の送信処理をキャンセル
                    from += maxSize;
                }

                // 送信成功項目を削除
                for (int i = resultList.size() - 1; i >= 0; i--) {
                    ScrobbleResult result = resultList.get(i);
                    if (result.isSuccessful()) {
                        dataList.remove(i);
                    }
                }
            }

            boolean notSave = sharedPreferences.getBoolean(context.getString(R.string.prefkey_unsent_scrobble_not_save), false);

            // 保存なし (既存のデータはそのまま残す)
            if (notSave) {
                dataList.remove(newData);
            }

            // 未送信データ保存件数上限取得
            int unsentMax;
            int maxDefault = context.getResources().getInteger(R.integer.pref_default_unsent_max);
            String unsentMaxString = sharedPreferences.getString(context.getString(R.string.prefkey_unsent_max), String.valueOf(maxDefault));
            try {
                unsentMax = Integer.parseInt(unsentMaxString);
            } catch (Exception e) {
                unsentMax = maxDefault;
            }

            // 上限以上を削除
            if (unsentMax > 0 && dataList.size() > unsentMax) {
                dataList = dataList.subList(dataList.size() - unsentMax, dataList.size());
            }

            // 未送信データを保存
            AppUtils.saveObject(context, context.getString(R.string.prefkey_unsent_scrobble_data), dataList.toArray());

            if (session == null && !notSave) {
                return PostResult.SAVED;
            } else if (dataList.size() == 0)
                return PostResult.SUCCEEDED;
            else
                return PostResult.FAILED;
        } catch (Exception e) {
            Logger.e(e);
            return PostResult.FAILED;
        }
    }

    /**
     * Love.
     * @param session Session.
     * @return PostResult.
     */
    private PostResult love(Session session) {
        if (session == null || propertyMap == null)
            return PostResult.FAILED;

        try {
            // 無効データを無視
            String track  = propertyMap.get(MediaProperty.TITLE.getKeyName());
            String artist  = propertyMap.get(MediaProperty.ARTIST.getKeyName());
            if (TextUtils.isEmpty(track) || TextUtils.isEmpty(artist))
                return PostResult.IGNORE;

            Result res = Track.love(artist, track, session);
            if (res.isSuccessful())
                return PostResult.SUCCEEDED;
            else
                return PostResult.FAILED;
        } catch (Exception e) {
            Logger.e(e);
            return PostResult.FAILED;
        }
    }

    /**
     * UnLove.
     * @param session Session.
     * @return PostResult.
     */
    private PostResult unlove(Session session) {
        if (session == null || propertyMap == null)
            return PostResult.FAILED;

        try {
            // 無効データを無視
            String track  = propertyMap.get(MediaProperty.TITLE.getKeyName());
            String artist  = propertyMap.get(MediaProperty.ARTIST.getKeyName());
            if (TextUtils.isEmpty(track) || TextUtils.isEmpty(artist))
                return PostResult.IGNORE;

            Result res = Track.unlove(artist, track, session);
            if (res.isSuccessful())
                return PostResult.SUCCEEDED;
            else
                return PostResult.FAILED;
        } catch (Exception e) {
            Logger.e(e);
            return PostResult.FAILED;
        }
    }

    /**
     * Ban.
     * @param session Session.
     * @return PostResult.
     */
    private PostResult ban(Session session) {
        if (session == null || propertyMap == null)
            return PostResult.FAILED;

        try {
            // 無効データを無視
            String track  = propertyMap.get(MediaProperty.TITLE.getKeyName());
            String artist  = propertyMap.get(MediaProperty.ARTIST.getKeyName());
            if (TextUtils.isEmpty(track) || TextUtils.isEmpty(artist))
                return PostResult.IGNORE;

            Result res = Track.ban(artist, track, session);
            if (res.isSuccessful())
                return PostResult.SUCCEEDED;
            else
                return PostResult.FAILED;
        } catch (Exception e) {
            Logger.e(e);
            return PostResult.FAILED;
        }
    }


    /**
     * Unban.
     * @param session Session.
     * @return PostResult.
     */
    private PostResult unban(Session session) {
        if (session == null || propertyMap == null)
            return PostResult.FAILED;

        try {
            // 無効データを無視
            String track  = propertyMap.get(MediaProperty.TITLE.getKeyName());
            String artist = propertyMap.get(MediaProperty.ARTIST.getKeyName());
            if (TextUtils.isEmpty(track) || TextUtils.isEmpty(artist))
                return PostResult.IGNORE;

            Result res = Track.unban(artist, track, session);
            if (res.isSuccessful())
                return PostResult.SUCCEEDED;
            else
                return PostResult.FAILED;
        } catch (Exception e) {
            Logger.e(e);
            return PostResult.FAILED;
        }
    }


    /**
     * 投稿結果を出力。
     * @param result 投稿結果。
     * @param postType 投稿種別。
     */
    protected void onPostExecute(PostResult result, PostType postType) {
        if (result == PostResult.IGNORE || result == PostResult.SAVED) {
            return;
        } else if (result == PostResult.AUTH_FAILED) {
            AppUtils.showToast(context, R.string.message_account_not_auth);
            return;
        }

        if (postType == PostType.SCROBBLE) {
            // Scrobble
            if (result == PostResult.SUCCEEDED) {
                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), false)) {
                    AppUtils.showToast(context, R.string.message_post_success); // Succeed
                }
            } else {
                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true)) {
                    AppUtils.showToast(context, R.string.message_post_failure); // Failed
                }
            }
        } else if (postType == PostType.LOVE) {
            // Love
            if (result == PostResult.SUCCEEDED) {
                AppUtils.showToast(context, context.getString(R.string.message_love_success,  propertyMap.get(MediaProperty.TITLE.getKeyName()))); // Succeed
            } else {
                AppUtils.showToast(context, R.string.message_love_failure); // Failed
            }
        } else if (postType == PostType.UNLOVE) {
            // UnLove
            if (result == PostResult.SUCCEEDED) {
                AppUtils.showToast(context, context.getString(R.string.message_unlove_success,  propertyMap.get(MediaProperty.TITLE.getKeyName()))); // Succeed
            } else {
                AppUtils.showToast(context, R.string.message_unlove_failure); // Failed
            }
        } else if (postType == PostType.BAN) {
            // Love
            if (result == PostResult.SUCCEEDED) {
                AppUtils.showToast(context, context.getString(R.string.message_ban_success,  propertyMap.get(MediaProperty.TITLE.getKeyName()))); // Succeed
            } else {
                AppUtils.showToast(context, R.string.message_ban_failure); // Failed
            }
        } else if (postType == PostType.UNBAN) {
            // UnLove
            if (result == PostResult.SUCCEEDED) {
                AppUtils.showToast(context, context.getString(R.string.message_unban_success,  propertyMap.get(MediaProperty.TITLE.getKeyName()))); // Succeed
            } else {
                AppUtils.showToast(context, R.string.message_unban_failure); // Failed
            }
        }
    }
}