package com.wa2c.android.medoly.plugin.action.lastfm;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.wa2c.android.medoly.library.ExtraData;
import com.wa2c.android.medoly.library.MediaPluginIntent;
import com.wa2c.android.medoly.library.MediaProperty;
import com.wa2c.android.medoly.library.PluginOperationCategory;
import com.wa2c.android.medoly.library.PluginTypeCategory;
import com.wa2c.android.medoly.library.PropertyData;
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils;
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;
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

    public static int PLUGIN_EVENT_NONE = 0;
    public static int PLUGIN_EVENT_PLAY_START = 1;
    public static int PLUGIN_EVENT_PLAY_NOW = 2;
    public static int PLUGIN_EVENT_MEDIA_OPEN = 2;

    /** 前回のファイルパス設定キー。 */
    private static final String PREFKEY_PREVIOUS_MEDIA_URI = "previous_media_uri";

    /**
     * Post type.
     */
    private enum PostType {
        /** Now Playing */
        NOW_PLAYING,
        /** Scrobble */
        SCROBBLE,
        /** Love */
        LOVE,
        /** Unlove */
        UNLOVE,
        /** Page Track */
        PAGE_TRACK,
        /** Page Artist */
        PAGE_ARTIST,
        /** Ban */
        BAN,
        /** Unban */
        UNBAN,
        /** Site */
        SITE
    }


    /**
     * Post result.
     */
    private enum PostResult {
        /** Succeeded. */
        SUCCEEDED,
        /** Failed. */
        FAILED,
        /** Authorization failed. */
        AUTH_FAILED,
        /** No media. */
        NO_MEDIA,
        /** Post saved. */
        SAVED,
        /** Ignore. */
        IGNORE
    }




    /** Context. */
    private Context context = null;
    /** Preferences. */
    private SharedPreferences sharedPreferences = null;
    /** Plugin intent. */
    private MediaPluginIntent pluginIntent;
    /** Property data. */
    private PropertyData propertyData;

    /**
     * Constructor.
     */
    public PostIntentService() {
        super(PostIntentService.class.getSimpleName());
    }

    @Override
    protected synchronized void onHandleIntent(Intent intent) {
        if (intent == null)
            return;

        try {
            context = getApplicationContext();
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            pluginIntent = new MediaPluginIntent(intent);
            propertyData = pluginIntent.getPropertyData();

            // Start playing
            if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_START)) {
                if (sharedPreferences.getBoolean(getString(R.string.prefkey_update_now_playing), true))
                    post(PostType.NOW_PLAYING);
            }

            // Get Property
            if (pluginIntent.hasCategory(PluginTypeCategory.TYPE_GET_PROPERTY)) {
//                int event = sharedPreferences.getInt(context.getString(R.string.pref_plugin_event), PLUGIN_EVENT_PLAY_NOW);
//                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_START)) {
//                    // Play Start
//                    if (!pluginIntent.isAutomatically() || event == PLUGIN_EVENT_PLAY_START) {
//                        post(PostType.SCROBBLE);
//                    }
//                }
//                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_NOW)) {
//                    // Play Now
//                    if (!pluginIntent.isAutomatically() || event == PLUGIN_EVENT_PLAY_NOW) {
//                        post(PostType.SCROBBLE);
//                    }
//                }
                getProperties();
            }

            // Post Message
            if (pluginIntent.hasCategory(PluginTypeCategory.TYPE_POST_MESSAGE)) {
                int event = sharedPreferences.getInt(context.getString(R.string.pref_plugin_event), PLUGIN_EVENT_PLAY_NOW);
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_START)) {
                    // Play Start
                    if (!pluginIntent.isAutomatically() || event == PLUGIN_EVENT_PLAY_START) {
                        post(PostType.SCROBBLE);
                    }
                }
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_NOW)) {
                    // Play Now
                    if (!pluginIntent.isAutomatically() || event == PLUGIN_EVENT_PLAY_NOW) {
                        post(PostType.SCROBBLE);
                    }
                }
            }

            // Execute
            if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE)) {
                if (pluginIntent.hasExecuteId("execute_id_love")) {
                    // Love
                    post(PostType.LOVE);
                } else if (pluginIntent.hasExecuteId("execute_id_unlove")) {
                    // UnLove
                    post(PostType.UNLOVE);
                } else if (pluginIntent.hasExecuteId("execute_id_ban")) {
                    // Ban
                    post(PostType.BAN);
                } else if (pluginIntent.hasExecuteId("execute_id_unban")) {
                    // UnBan
                    post(PostType.UNBAN);
                } else if (pluginIntent.hasExecuteId("execute_id_page_track")) {
                    // Page Track
                    post(PostType.PAGE_TRACK);
                } else if (pluginIntent.hasExecuteId("execute_id_page_artist")) {
                    // Page Artist
                    post(PostType.PAGE_ARTIST);
                } else if (pluginIntent.hasExecuteId("execute_id_site")) {
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
        } catch (Exception e) {
            AppUtils.showToast(this, R.string.error_app);
        } finally {
            context = null;
            sharedPreferences = null;
            pluginIntent = null;
        }
    }


    private PostResult checkMediaAvailable() {
        // 音楽データ無し
        if (pluginIntent.getMediaUri() == null) {
            return PostResult.NO_MEDIA;
        }

        // 必須情報無し
        if (propertyData == null ||
            propertyData.isEmpty(MediaProperty.TITLE) ||
            propertyData.isEmpty(MediaProperty.ARTIST)) {
            return PostResult.IGNORE;
        }

        // 認証確認
        String username = sharedPreferences.getString(context.getString(R.string.prefkey_auth_username), "");
        if (TextUtils.isEmpty(username)) {
            return PostResult.AUTH_FAILED;
        }

        return null;
    }

    private void getProperties() {
        try {
            // Check data
            PostResult checkResult = checkMediaAvailable();
            if (checkResult != null) {
                sendPropertyResult(checkResult, null, null);
                return;
            }

            // 無効データを無視
            String titleText = propertyData.getFirst(MediaProperty.TITLE);
            String artistText = propertyData.getFirst(MediaProperty.ARTIST);
            if (TextUtils.isEmpty(titleText) || TextUtils.isEmpty(artistText)) {
                sendPropertyResult(PostResult.IGNORE, null, null);
                return;
            }

            // セッション
            String username = sharedPreferences.getString(context.getString(R.string.prefkey_auth_username), "");
            String password = sharedPreferences.getString(context.getString(R.string.prefkey_auth_password), "");
            Session session = null;
            try {
                // 認証
                // フォルダ設定 (getMobileSessionの前に入れる必要あり)
                Caller.getInstance().setCache(new FileSystemCache(new File(context.getExternalCacheDir().getPath() + File.separator + "last.fm")));
                session = Authenticator.getMobileSession(username, password, Token.getConsumerKey(context), Token.getConsumerSecret(context));
            } catch (Exception e) {
                // 認証失敗してもエラーとしない
                Logger.e(e);
            }
            if (session == null) {
                sendPropertyResult(PostResult.FAILED, null, null);
                return;
            }

            Track track;
            if (TextUtils.isEmpty(session.getUsername()))
                track = Track.getInfo(artistText, titleText, session.getApiKey());
            else
                track = Track.getInfo(artistText, titleText, Locale.getDefault(), session.getUsername(), session.getApiKey());

            // Property data
            PropertyData property = new PropertyData();
            property.put(MediaProperty.TITLE, track.getName());
            property.put(MediaProperty.ARTIST, track.getArtist());
            property.put(MediaProperty.ALBUM, track.getAlbum());
            property.put(MediaProperty.MUSICBRAINZ_TRACK_ID, track.getMbid());
            property.put(MediaProperty.MUSICBRAINZ_ARTISTID, track.getArtistMbid());
            property.put(MediaProperty.MUSICBRAINZ_RELEASEID, track.getAlbumMbid());

            // Extra data
            ExtraData extra = new ExtraData();
            if (track.getUserPlaycount() > 0)
                extra.put(getString(R.string.label_extra_data_user_play_count), String.valueOf(track.getUserPlaycount()));
            if (track.getUserPlaycount() > 0)
                extra.put(getString(R.string.label_extra_data_play_count), String.valueOf(track.getPlaycount()));
            if (track.getListeners() > 0)
                extra.put(getString(R.string.label_extra_data_listener_count), String.valueOf(track.getListeners()));
            extra.put(getString(R.string.label_extra_data_lastfm_track_url), track.getUrl());

            sendPropertyResult(PostResult.SUCCEEDED, property, extra);
        } catch (Exception e) {
            sendPropertyResult(PostResult.FAILED, null, null);
            Logger.e(e);
        }
    }


    /**
     * Send lyrics info.
     * @param propertyData The property data.
     */
    private void sendPropertyResult(PostResult postResult,  PropertyData propertyData, ExtraData extraData) {
        MediaPluginIntent returnIntent = pluginIntent.createReturnIntent(propertyData, extraData);
        sendBroadcast(returnIntent);
        showResult(postResult, null);
    }



    /**
     * 投稿を開始。
     * @param postType 投稿種別。
     */
    private void post(PostType postType) {
        // Check data
        PostResult checkResult = checkMediaAvailable();
        if (checkResult != null) {
            showResult(checkResult, null);
            return;
        }

        // 前回メディア確認
        if (postType == PostType.SCROBBLE) {
            String mediaUriText = pluginIntent.getMediaUri().toString();
            String previousMediaUri = sharedPreferences.getString(PREFKEY_PREVIOUS_MEDIA_URI, "");
            boolean previousMediaEnabled = sharedPreferences.getBoolean(context.getString(R.string.prefkey_previous_media_enabled), false);
            if (!previousMediaEnabled && !TextUtils.isEmpty(mediaUriText) && !TextUtils.isEmpty(previousMediaUri) && mediaUriText.equals(previousMediaUri)) {
                // 前回と同じメディアは無視
                showResult(PostResult.IGNORE, postType);
                return;
            }
            sharedPreferences.edit().putString(PREFKEY_PREVIOUS_MEDIA_URI, mediaUriText).apply();
        }

        // セッション
        String username = sharedPreferences.getString(context.getString(R.string.prefkey_auth_username), "");
        String password = sharedPreferences.getString(context.getString(R.string.prefkey_auth_password), "");
        Session session = null;
        try {
            // 認証
            // フォルダ設定 (getMobileSessionの前に入れる必要あり)
            Caller.getInstance().setCache(new FileSystemCache(new File(context.getExternalCacheDir().getPath() + File.separator + "last.fm")));
            session = Authenticator.getMobileSession(username, password, Token.getConsumerKey(context), Token.getConsumerSecret(context));
        } catch (Exception e) {
            // 認証失敗してもエラーとしない
            Logger.e(e);
        }

        // 投稿
        PostResult result = PostResult.IGNORE;
        try {
            switch (postType) {
                case NOW_PLAYING:
                    result = updateNowPlaying(session);
                    break;
                case SCROBBLE:
                    result = scrobble(session);
                    break;
                case LOVE:
                    result = love(session);
                    break;
                case UNLOVE:
                    result = unlove(session);
                    break;
                case BAN:
                    result = ban(session);
                    break;
                case UNBAN:
                    result = unban(session);
                    break;
                case PAGE_TRACK:
                    result = trackPage(session);
                    break;
                case PAGE_ARTIST:
                    result = artistPage(session);
                    break;
            }
        } catch (Exception e) {
            Logger.e(e);
            result = PostResult.FAILED;
        }
        showResult(result, postType);
    }

    /**
     * Crate scrobble data.
     * @return The scrobble data.
     */
    private ScrobbleData createScrobbleData() {
        ScrobbleData newData = new ScrobbleData();
        newData.setMusicBrainzId(propertyData.getFirst(MediaProperty.MUSICBRAINZ_TRACK_ID));
        newData.setTrack(propertyData.getFirst(MediaProperty.TITLE));
        newData.setArtist(propertyData.getFirst(MediaProperty.ARTIST));
        newData.setAlbumArtist(propertyData.getFirst(MediaProperty.ALBUM_ARTIST));
        newData.setAlbum(propertyData.getFirst(MediaProperty.ALBUM));

        try {
            newData.setDuration(Integer.valueOf(pluginIntent.getPropertyData().getFirst(MediaProperty.DURATION)));
        } catch (NumberFormatException | NullPointerException e) {
            Logger.e(e);
        }
        try {
            newData.setTrackNumber(Integer.valueOf(pluginIntent.getPropertyData().getFirst(MediaProperty.TRACK)));
        } catch (NumberFormatException | NullPointerException e) {
            Logger.e(e);
        }
        newData.setTimestamp((int) (System.currentTimeMillis() / 1000));
        return newData;
    }

    /**
     * Update now playing.
     * @param session The session.
     * @return The result.
     */
    private PostResult updateNowPlaying(Session session) {
        try {
            ScrobbleData scrobbleData = createScrobbleData();
            if (TextUtils.isEmpty(scrobbleData.getTrack()) || TextUtils.isEmpty(scrobbleData.getArtist()))
                return PostResult.IGNORE; // ignore invalid data

            ScrobbleResult result = Track.updateNowPlaying(scrobbleData, session);
            if (result.isSuccessful())
                return PostResult.SUCCEEDED;
            else
                return PostResult.FAILED;
        } catch (Exception e) {
            Logger.e(e);
            return PostResult.FAILED;
        }
    }

    /**
     * Scrobble.
     * @param session The session.
     * @return The result.
     */
    private PostResult scrobble(Session session) {
        try {
            ScrobbleData scrobbleData = createScrobbleData();
            if (TextUtils.isEmpty(scrobbleData.getTrack()) || TextUtils.isEmpty(scrobbleData.getArtist()))
                return PostResult.IGNORE; // ignore invalid data

            // 送信データ作成
            List<ScrobbleData> dataList = new ArrayList<>();
            ScrobbleData[] dataArray = AppUtils.loadObject(context, context.getString(R.string.prefkey_unsent_scrobble_data), ScrobbleData[].class);
            if (dataArray != null) dataList.addAll(Arrays.asList(dataArray)); // 未送信データ読み込み
            dataList.add(scrobbleData);

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
                dataList.remove(scrobbleData);
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

            if (session == null && !notSave)
                return PostResult.SAVED;
            else if (dataList.size() == 0)
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
     * @param session The session.
     * @return The result.
     */
    private PostResult love(Session session) {
         try {
            String track  = pluginIntent.getPropertyData().getFirst(MediaProperty.TITLE);
            String artist  = pluginIntent.getPropertyData().getFirst(MediaProperty.ARTIST);
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
     * @param session The session.
     * @return The result.
     */
    private PostResult unlove(Session session) {
        try {
            String track  = pluginIntent.getPropertyData().getFirst(MediaProperty.TITLE);
            String artist  = pluginIntent.getPropertyData().getFirst(MediaProperty.ARTIST);
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
     * @param session The session.
     * @return The result.
     */
    private PostResult ban(Session session) {
        try {
            String track  = pluginIntent.getPropertyData().getFirst(MediaProperty.TITLE);
            String artist  = pluginIntent.getPropertyData().getFirst(MediaProperty.ARTIST);
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
     * @param session The session.
     * @return The result.
     */
    private PostResult unban(Session session) {
       try {
            String track  = pluginIntent.getPropertyData().getFirst(MediaProperty.TITLE);
            String artist  = pluginIntent.getPropertyData().getFirst(MediaProperty.ARTIST);
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
     * Track Page.
     * @param session The session.
     * @return The result.
     */
    private PostResult trackPage(Session session) {
        try {
            String track  = pluginIntent.getPropertyData().getFirst(MediaProperty.TITLE);
            String artist  = pluginIntent.getPropertyData().getFirst(MediaProperty.ARTIST);
            if (TextUtils.isEmpty(track) || TextUtils.isEmpty(artist))
                return PostResult.IGNORE;

            Track trackInfo = Track.getInfo(artist, track, session.getApiKey());
            Uri uri = Uri.parse(trackInfo.getUrl());

            Intent launchIntent = new Intent(Intent.ACTION_VIEW, uri);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
            return PostResult.SUCCEEDED;
        } catch (Exception e) {
            Logger.e(e);
            return PostResult.FAILED;
        }
    }

    /**
     * Artist Page.
     * @param session The session.
     * @return The result.
     */
    private PostResult artistPage(Session session) {
        try {
            String artist  = pluginIntent.getPropertyData().getFirst(MediaProperty.ARTIST);
            if (TextUtils.isEmpty(artist))
                return PostResult.IGNORE;

            Artist artistInfo = Artist.getInfo(artist, session.getApiKey());
            Uri uri = Uri.parse(artistInfo.getUrl());

            Intent launchIntent = new Intent(Intent.ACTION_VIEW, uri);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
            return PostResult.SUCCEEDED;
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
    private void showResult(PostResult result, PostType postType) {
        if (result == PostResult.IGNORE || result == PostResult.SAVED) {
            return;
        } else if (result == PostResult.NO_MEDIA) {
            AppUtils.showToast(context, R.string.message_no_media);
            return;
        } else if (result == PostResult.AUTH_FAILED) {
            AppUtils.showToast(context, R.string.message_account_not_auth);
            return;
        }

        switch (postType) {
            case SCROBBLE:
                if (result == PostResult.SUCCEEDED) {
                    if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), false)) {
                        AppUtils.showToast(context, R.string.message_post_success); // Succeed
                    }
                } else {
                    if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true)) {
                        AppUtils.showToast(context, R.string.message_post_failure); // Failed
                    }
                }
                break;
            case LOVE:
                if (result == PostResult.SUCCEEDED) {
                    AppUtils.showToast(context, context.getString(R.string.message_love_success,  pluginIntent.getPropertyData().getFirst(MediaProperty.TITLE.getKeyName()))); // Succeed
                } else {
                    AppUtils.showToast(context, R.string.message_love_failure); // Failed
                }
                break;
            case UNLOVE:
                if (result == PostResult.SUCCEEDED) {
                    AppUtils.showToast(context, context.getString(R.string.message_unlove_success,  pluginIntent.getPropertyData().getFirst(MediaProperty.TITLE.getKeyName()))); // Succeed
                } else {
                    AppUtils.showToast(context, R.string.message_unlove_failure); // Failed
                }
                break;
            case BAN:
                if (result == PostResult.SUCCEEDED) {
                    AppUtils.showToast(context, context.getString(R.string.message_ban_success,  pluginIntent.getPropertyData().getFirst(MediaProperty.TITLE.getKeyName()))); // Succeed
                } else {
                    AppUtils.showToast(context, R.string.message_ban_failure); // Failed
                }
                break;
            case UNBAN:
                if (result == PostResult.SUCCEEDED) {
                    AppUtils.showToast(context, context.getString(R.string.message_unban_success,  pluginIntent.getPropertyData().getFirst(MediaProperty.TITLE.getKeyName()))); // Succeed
                } else {
                    AppUtils.showToast(context, R.string.message_unban_failure); // Failed
                }
                break;
            case PAGE_TRACK:
                if (result == PostResult.SUCCEEDED) {
                    Logger.d(R.string.message_page_track_success); // Succeed
                } else {
                    AppUtils.showToast(context, R.string.message_page_track_failure); // Failed
                }
                break;
            case PAGE_ARTIST:
                if (result == PostResult.SUCCEEDED) {
                    Logger.d(R.string.message_page_artist_success); // Succeed
                } else {
                    AppUtils.showToast(context, R.string.message_page_artist_failure); // Failed
                }
                break;
        }
    }
}
