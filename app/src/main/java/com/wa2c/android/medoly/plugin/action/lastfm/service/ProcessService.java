package com.wa2c.android.medoly.plugin.action.lastfm.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.wa2c.android.medoly.library.AlbumArtProperty;
import com.wa2c.android.medoly.library.ExtraData;
import com.wa2c.android.medoly.library.MediaPluginIntent;
import com.wa2c.android.medoly.library.MediaProperty;
import com.wa2c.android.medoly.library.PluginOperationCategory;
import com.wa2c.android.medoly.library.PluginTypeCategory;
import com.wa2c.android.medoly.library.PropertyData;
import com.wa2c.android.medoly.plugin.action.lastfm.R;
import com.wa2c.android.medoly.plugin.action.lastfm.Token;
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils;
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.umass.lastfm.Album;
import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.ImageSize;
import de.umass.lastfm.Result;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.cache.FileSystemCache;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;



/**
 * Intent service.
 */
public class ProcessService extends IntentService {

    /** Received receiver class name. */
    public static String RECEIVED_CLASS_NAME = "RECEIVED_CLASS_NAME";

    /** Previous data key. */
    private static final String PREFKEY_PREVIOUS_MEDIA_URI = "previous_media_uri";

    /**
     * Command result.
     */
    private enum CommandResult {
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
    public ProcessService() {
        super(ProcessService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null)
            return;

        try {
            context = getApplicationContext();
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            pluginIntent = new MediaPluginIntent(intent);
            propertyData = pluginIntent.getPropertyData();

            // Initialize last.fm library
            Caller.getInstance().setCache(new FileSystemCache(new File(context.getExternalCacheDir(), "last.fm")));

            // Authenticate
            String username = sharedPreferences.getString(context.getString(R.string.prefkey_auth_username), "");
            String password = sharedPreferences.getString(context.getString(R.string.prefkey_auth_password), "");
            Session session = null;
            try {
                session = Authenticator.getMobileSession(username, password, Token.getConsumerKey(context), Token.getConsumerSecret(context));
            } catch (Exception e) {
                // Not error if authentication was failed.
                Logger.e(e);
            }

            // Execute

            if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE)) {
                String receivedClassName = pluginIntent.getStringExtra(RECEIVED_CLASS_NAME);
                 if (receivedClassName.equals(PluginReceiver.ExecuteLoveReceiver.class.getName())) {
                     love(session);
                } else if (receivedClassName.equals(PluginReceiver.ExecuteUnLoveReceiver.class.getName())) {
                     unlove(session);
                } else if (receivedClassName.equals(PluginReceiver.ExecuteGetAlbumArtReceiver.class.getName())) {
                    getAlbumArt(session);
                } else if (receivedClassName.equals(PluginReceiver.ExecuteGetPropertyReceiver.class.getName())) {
                    getProperties(session);
                } else if (receivedClassName.equals(PluginReceiver.ExecuteTrackPageReceiver.class.getName())) {
                    openTrackPage(session);
                } else if (receivedClassName.equals(PluginReceiver.ExecuteLastfmSiteReceiver.class.getName())) {
                    openLastfmPage(session);
                }
                return;
            }

            // Event

            // Get property
            if (pluginIntent.hasCategory(PluginTypeCategory.TYPE_GET_PROPERTY)) {
                String operation = sharedPreferences.getString(getString(R.string.prefkey_event_get_property_operation), "");
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_MEDIA_OPEN) && PluginOperationCategory.OPERATION_MEDIA_OPEN.name().equals(operation)) {
                    getProperties(session); // media open
                } else if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_START) && PluginOperationCategory.OPERATION_PLAY_START.name().equals(operation)) {
                    getProperties(session); // play start
                } else {
                    sendBroadcast(pluginIntent.createResultIntent(null));
                }
                return;
            }

            // Get album art
            if (pluginIntent.hasCategory(PluginTypeCategory.TYPE_GET_ALBUM_ART)) {
                String operation = sharedPreferences.getString(getString(R.string.prefkey_event_get_album_art_operation), "");
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_MEDIA_OPEN) && PluginOperationCategory.OPERATION_MEDIA_OPEN.name().equals(operation)) {
                    getAlbumArt(session);
                } else if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_START) && PluginOperationCategory.OPERATION_PLAY_START.name().equals(operation)) {
                    getAlbumArt(session);
                } else {
                    sendBroadcast(pluginIntent.createResultIntent(null));
                }
                return;
            }

            // Scrobble / Now playing
            if (pluginIntent.hasCategory(PluginTypeCategory.TYPE_POST_MESSAGE)) {
                // Start playing
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_START)) {
                    if (sharedPreferences.getBoolean(getString(R.string.prefkey_now_playing_enabled), true)) {
                        updateNowPlaying(session);
                        return;
                    }
                }

                // Scrobble
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_NOW)) {
                    if (sharedPreferences.getBoolean(getString(R.string.prefkey_scrobble_enabled), true)) {
                        scrobble(session);
                        //return;
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
            newData.setDuration((int)(Long.valueOf(propertyData.getFirst(MediaProperty.DURATION)) / 1000));
        } catch (NumberFormatException | NullPointerException e) {
            Logger.e(e);
        }
        try {
            newData.setTrackNumber(Integer.valueOf(propertyData.getFirst(MediaProperty.TRACK)));
        } catch (NumberFormatException | NullPointerException e) {
            Logger.e(e);
        }
        newData.setTimestamp((int) (System.currentTimeMillis() / 1000));
        return newData;
    }

    /**
     * Start page.
     * @param uri The URI.
     */
    private void startPage(Uri uri) {
        if (uri == null)
            return;
        Intent launchIntent = new Intent(Intent.ACTION_VIEW, uri);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launchIntent);
    }



    /**
     * Update now playing.
     * @param session The session.
     */
    private void updateNowPlaying(Session session) {
        CommandResult result = CommandResult.IGNORE;
        try {
            if (session == null) {
                result = CommandResult.AUTH_FAILED;
                return;
            }

            if (propertyData == null || propertyData.isMediaEmpty()) {
                result = CommandResult.NO_MEDIA;
                return;
            }

            // create scrobble data
            ScrobbleData scrobbleData = createScrobbleData();
            if (TextUtils.isEmpty(scrobbleData.getTrack()) || TextUtils.isEmpty(scrobbleData.getArtist()))
                return;

            ScrobbleResult scrobbleResult = Track.updateNowPlaying(scrobbleData, session);
            if (scrobbleResult.isSuccessful())
                result = CommandResult.SUCCEEDED;
            else
                result = CommandResult.FAILED;
        } catch (Exception e) {
            Logger.e(e);
            result = CommandResult.FAILED;
        } finally {
            if (result == CommandResult.AUTH_FAILED) {
                AppUtils.showToast(context, R.string.message_account_not_auth);
            } else if (result == CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media);
//            } else if (result == CommandResult.SUCCEEDED) {
//                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), true))
//                    AppUtils.showToast(context, R.string.message_post_success);
//            } else if (result == CommandResult.FAILED) {
//                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
//                    AppUtils.showToast(context, R.string.message_post_failure);
            }
        }
    }

    /**
     * Scrobble.
     * @param session The session.
     */
    private void scrobble(Session session) {
        CommandResult result = CommandResult.IGNORE;
        try {
            if (propertyData == null || propertyData.isMediaEmpty()) {
                result = CommandResult.NO_MEDIA;
                return;
            }

            // Check previous media
            String mediaUriText = propertyData.getMediaUri().toString();
            String previousMediaUri = sharedPreferences.getString(PREFKEY_PREVIOUS_MEDIA_URI, "");
            boolean previousMediaEnabled = sharedPreferences.getBoolean(context.getString(R.string.prefkey_previous_media_enabled), false);
            if (!previousMediaEnabled && !TextUtils.isEmpty(mediaUriText) && !TextUtils.isEmpty(previousMediaUri) && mediaUriText.equals(previousMediaUri)) {
               return;
            }
            sharedPreferences.edit().putString(PREFKEY_PREVIOUS_MEDIA_URI, mediaUriText).apply();

            if (session == null) {
                result = CommandResult.AUTH_FAILED;
                return;
            }

            // create scrobble data
            ScrobbleData scrobbleData = createScrobbleData();
            if (TextUtils.isEmpty(scrobbleData.getTrack()) || TextUtils.isEmpty(scrobbleData.getArtist()))
                return;

            // create scrobble list data
            List<ScrobbleData> dataList = new ArrayList<>();
            ScrobbleData[] dataArray = AppUtils.loadObject(context, context.getString(R.string.prefkey_unsent_scrobble_data), ScrobbleData[].class);
            if (dataArray != null) dataList.addAll(Arrays.asList(dataArray)); // load unsent data
            dataList.add(scrobbleData);

            // send if session is not null
            if (!session.isSubscriber()) {
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
                    if (cancelSending) break; // cancel follow process if failed
                    from += maxSize;
                }

                // delete succeeded data
                for (int i = resultList.size() - 1; i >= 0; i--) {
                    ScrobbleResult scrobbleResult = resultList.get(i);
                    if (scrobbleResult.isSuccessful()) {
                        dataList.remove(i);
                    }
                }

                if (dataList.size() == 0)
                    result = CommandResult.SUCCEEDED;
                else
                    result = CommandResult.FAILED;
            }

            boolean notSave = sharedPreferences.getBoolean(context.getString(R.string.prefkey_unsent_scrobble_not_save), false);

            // not save (leave exists data)
            if (notSave) {
                dataList.remove(scrobbleData);
            }

            // truncate to limit
            int unsentMax;
            int maxDefault = context.getResources().getInteger(R.integer.pref_default_unsent_max);
            String unsentMaxString = sharedPreferences.getString(context.getString(R.string.prefkey_unsent_max), String.valueOf(maxDefault));
            try {
                unsentMax = Integer.parseInt(unsentMaxString);
            } catch (Exception e) {
                unsentMax = maxDefault;
            }

            if (unsentMax > 0 && dataList.size() > unsentMax) {
                dataList = dataList.subList(dataList.size() - unsentMax, dataList.size());
            }

            // save unsent data
            AppUtils.saveObject(context, context.getString(R.string.prefkey_unsent_scrobble_data), dataList.toArray());

            if (result == CommandResult.IGNORE) {
                if (!session.isSubscriber())
                    result = CommandResult.AUTH_FAILED;
                else if (!notSave)
                    result = CommandResult.SAVED;
                else
                    result = CommandResult.FAILED;
            }
        } catch (Exception e) {
            Logger.e(e);
            result = CommandResult.FAILED;
        } finally {
            if (result == CommandResult.AUTH_FAILED) {
                AppUtils.showToast(context, R.string.message_account_not_auth);
            } else if (result == CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media);
            } else if (result == CommandResult.SUCCEEDED) {
                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), false))
                    AppUtils.showToast(context, R.string.message_post_success);
            } else if (result == CommandResult.FAILED) {
                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
                    AppUtils.showToast(context, R.string.message_post_failure);
            }
        }
    }

    /**
     * Love.
     * @param session The session.
     */
    private void love(Session session) {
        CommandResult result = CommandResult.IGNORE;
        try {
            if (session == null) {
                result = CommandResult.AUTH_FAILED;
                return;
            }

            if (propertyData == null || propertyData.isMediaEmpty()) {
                result = CommandResult.NO_MEDIA;
                return;
            }

            String track  = propertyData.getFirst(MediaProperty.TITLE);
            String artist  = propertyData.getFirst(MediaProperty.ARTIST);
            if (TextUtils.isEmpty(track) || TextUtils.isEmpty(artist))
                return;

            Result res = Track.love(artist, track, session);
            if (res.isSuccessful())
                result = CommandResult.SUCCEEDED;
            else
                result = CommandResult.FAILED;
        } catch (Exception e) {
            Logger.e(e);
            result = CommandResult.FAILED;
        } finally {
            if (result == CommandResult.AUTH_FAILED) {
                AppUtils.showToast(context, R.string.message_account_not_auth);
            } else if (result == CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media);
            } else if (result == CommandResult.SUCCEEDED) {
                AppUtils.showToast(context, context.getString(R.string.message_love_success,  propertyData.getFirst(MediaProperty.TITLE)));
            } else if (result == CommandResult.FAILED) {
                AppUtils.showToast(context, R.string.message_love_failure);
            }
         }
    }

    /**
     * UnLove.
     * @param session The session.
     */
    private void unlove(Session session) {
        CommandResult result = CommandResult.IGNORE;
        try {
            if (session == null) {
                result = CommandResult.AUTH_FAILED;
                return;
            }

            if (propertyData == null || propertyData.isMediaEmpty()) {
                result = CommandResult.NO_MEDIA;
                return;
            }

            String track  = propertyData.getFirst(MediaProperty.TITLE);
            String artist  = propertyData.getFirst(MediaProperty.ARTIST);
            if (TextUtils.isEmpty(track) || TextUtils.isEmpty(artist))
                return;

            Result res = Track.unlove(artist, track, session);
            if (res.isSuccessful())
                result = CommandResult.SUCCEEDED;
            else
                result = CommandResult.FAILED;
        } catch (Exception e) {
            Logger.e(e);
            result = CommandResult.FAILED;
        } finally {
            if (result == CommandResult.AUTH_FAILED) {
                AppUtils.showToast(context, R.string.message_account_not_auth);
            } else if (result == CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media);
            } else if (result == CommandResult.SUCCEEDED) {
                AppUtils.showToast(context, context.getString(R.string.message_unlove_success,  propertyData.getFirst(MediaProperty.TITLE)));
            } else if (result == CommandResult.FAILED) {
                AppUtils.showToast(context, R.string.message_unlove_failure);
            }
        }
    }

    /**
     * Get album art.
     * @param session The session.
     */
    private void getAlbumArt(Session session) {
        CommandResult result = CommandResult.IGNORE;
        PropertyData resultProperty = null;
        try {
            if (propertyData == null || propertyData.isMediaEmpty()) {
                result = CommandResult.NO_MEDIA;
                return;
            }

            // No property info
            String albumText = propertyData.getFirst(MediaProperty.ALBUM);
            String artistText = propertyData.getFirst(MediaProperty.ARTIST);
            if (TextUtils.isEmpty(albumText) || TextUtils.isEmpty(artistText))
                return;

            // Get info
            Album album;
            if (session != null) {
                album = Album.getInfo(artistText, albumText, session.getUsername(), session.getApiKey());
            } else {
                album = Album.getInfo(artistText, albumText, Token.getConsumerKey(context));
            }


//            // Track image
//            Track track;
//            if (TextUtils.isEmpty(session.getUsername()))
//                track = Track.getInfo(artistText, titleText, session.getApiKey());
//            else
//                track = Track.getInfo(artistText, titleText, Locale.getDefault(), session.getUsername(), session.getApiKey());
//            String imageUrl = track.getImageURL(ImageSize.EXTRALARGE);
//            Uri downloadUri = LastfmUtils.downloadUrl(context, imageUrl, "test");

            // Album image
            String remoteUri = album.getImageURL(ImageSize.MEGA);
            Uri localUri = AppUtils.downloadUrl(context, remoteUri);

//            // Artist image
//            Artist artist;
//            if (TextUtils.isEmpty(session.getUsername()))
//                artist = Artist.getInfo(artistText, session.getApiKey());
//            else
//                artist = Artist.getInfo(artistText, session.getUsername(), session.getApiKey());
//            String artistUrl = artist.getImageURL(ImageSize.ORIGINAL);

            if (localUri == null)
                return;

            resultProperty = new PropertyData();
            resultProperty.put(AlbumArtProperty.DATA_URI, localUri.toString());
            resultProperty.put(AlbumArtProperty.SOURCE_TITLE, getString(R.string.lastfm));
            resultProperty.put(AlbumArtProperty.SOURCE_URI, remoteUri);
            getApplicationContext().grantUriPermission(pluginIntent.getSrcPackage(), localUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            result = CommandResult.SUCCEEDED;
        } catch (Exception e) {
            Logger.e(e);
            resultProperty = null;
            result = CommandResult.FAILED;
        } finally {
            sendBroadcast(pluginIntent.createResultIntent(resultProperty, null));
            if (result == CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media);
            } else if (result == CommandResult.SUCCEEDED) {
                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), false))
                    AppUtils.showToast(context, R.string.message_get_data_success);
            } else if (result == CommandResult.FAILED) {
                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
                    AppUtils.showToast(context, R.string.message_get_data_failure);
            }
        }
    }

    /**
     * Get properties.
     * @param session The session.
     */
    private void getProperties(Session session) {
        CommandResult result = CommandResult.IGNORE;
        PropertyData resultProperty = null;
        ExtraData resultExtra = null;
        try {
            if (propertyData == null || propertyData.isMediaEmpty()) {
                result = CommandResult.NO_MEDIA;
                return;
            }

            String trackText  = propertyData.getFirst(MediaProperty.TITLE);
            String artistText  = propertyData.getFirst(MediaProperty.ARTIST);
            if (TextUtils.isEmpty(trackText) || TextUtils.isEmpty(artistText))
                return;

            // Get info
            Track track;
            if (session != null) {
                track = Track.getInfo(artistText, trackText, null, session.getUsername(), session.getApiKey());
            } else {
                track = Track.getInfo(artistText, trackText, Token.getConsumerKey(context));
            }

            // Property data
            resultProperty = new PropertyData();
            resultProperty.put(MediaProperty.TITLE, track.getName());
            resultProperty.put(MediaProperty.ARTIST, track.getArtist());
            resultProperty.put(MediaProperty.ALBUM, track.getAlbum());
            resultProperty.put(MediaProperty.MUSICBRAINZ_TRACK_ID, track.getMbid());
            resultProperty.put(MediaProperty.MUSICBRAINZ_ARTIST_ID, track.getArtistMbid());
            resultProperty.put(MediaProperty.MUSICBRAINZ_RELEASE_ID, track.getAlbumMbid());

            // Extra data
            resultExtra = new ExtraData();
            if (track.getUserPlaycount() > 0)
                resultExtra.put(getString(R.string.label_extra_data_user_play_count), String.valueOf(track.getUserPlaycount()));
            if (track.getUserPlaycount() > 0)
                resultExtra.put(getString(R.string.label_extra_data_play_count), String.valueOf(track.getPlaycount()));
            if (track.getListeners() > 0)
                resultExtra.put(getString(R.string.label_extra_data_listener_count), String.valueOf(track.getListeners()));
            resultExtra.put(getString(R.string.label_extra_data_lastfm_track_url), track.getUrl());
            result = CommandResult.SUCCEEDED;
        } catch (Exception e) {
            Logger.e(e);
            resultProperty = null;
            resultExtra = null;
            result = CommandResult.FAILED;
        } finally {
            sendBroadcast(pluginIntent.createResultIntent(resultProperty, resultExtra));
            if (result == CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media);
            } else if (result == CommandResult.SUCCEEDED) {
                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), false))
                    AppUtils.showToast(context, R.string.message_get_data_success);
            } else if (result == CommandResult.FAILED) {
                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
                    AppUtils.showToast(context, R.string.message_get_data_failure);
            }
        }
    }
    /**
     * Open track page.
     * @param session The session.
     */
    private void openTrackPage(Session session) {
        CommandResult result = CommandResult.IGNORE;
        try {
            if (propertyData == null || propertyData.isMediaEmpty()) {
                result = CommandResult.NO_MEDIA;
                return;
            }

            String trackText = propertyData.getFirst(MediaProperty.TITLE);
            String artistText  = propertyData.getFirst(MediaProperty.ARTIST);
            if (TextUtils.isEmpty(trackText) || TextUtils.isEmpty(artistText))
                result = CommandResult.IGNORE;

            // Get info
            Track track;
            if (session != null) {
                track = Track.getInfo(artistText, trackText, null, session.getUsername(), session.getApiKey());
            } else {
                track = Track.getInfo(artistText, trackText, Token.getConsumerKey(context));
            }

            startPage(Uri.parse(track.getUrl()));
            result = CommandResult.SUCCEEDED;
        } catch (Exception e) {
            Logger.e(e);
            result = CommandResult.FAILED;
        } finally {
            if (result == CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media);
            } else if (result == CommandResult.FAILED) {
                AppUtils.showToast(context, R.string.message_page_failure);
            }
        }
    }

    /**
     * Open Last.fm page.
     * @param session The session.
     */
    private void openLastfmPage(Session session) {
        CommandResult result = CommandResult.IGNORE;
        try {
            // Last.fm
            Uri siteUri;
            if (session != null) {
                // ユーザ認証済
                siteUri = Uri.parse(context.getString(R.string.lastfm_url_user, session.getUsername()));
            } else {
                // ユーザ未認証
                siteUri = Uri.parse(context.getString(R.string.lastfm_url));
            }

            startPage(siteUri);
            result = CommandResult.SUCCEEDED;
        } catch (android.content.ActivityNotFoundException e) {
            Logger.d(e);
            result = CommandResult.FAILED;
        } finally {
            if (result == CommandResult.FAILED) {
                AppUtils.showToast(context, R.string.message_page_failure);
            }
        }
    }

}
