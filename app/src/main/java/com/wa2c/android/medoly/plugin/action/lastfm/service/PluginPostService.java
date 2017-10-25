package com.wa2c.android.medoly.plugin.action.lastfm.service;

import android.content.Intent;
import android.text.TextUtils;

import com.wa2c.android.medoly.library.MediaProperty;
import com.wa2c.android.medoly.library.PluginOperationCategory;
import com.wa2c.android.medoly.library.PluginTypeCategory;
import com.wa2c.android.medoly.plugin.action.lastfm.R;
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils;
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.umass.lastfm.Result;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;


/**
 * Intent service.
 */
public class PluginPostService extends AbstractPluginService {

    /**
     * Constructor.
     */
    public PluginPostService() {
        super(PluginPostService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);
        if (pluginIntent == null)
            return;
        if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_POST_MESSAGE)) {
            return;
        }

        try {
            if (receivedClassName.equals(PluginReceivers.EventNowPlayingReceiver.class.getName())) {
                // Update Now Playing (event: OPERATION_PLAY_START)
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_START) && sharedPreferences.getBoolean(getString(R.string.prefkey_now_playing_enabled), true)) {
                    updateNowPlaying(session);
                }
            } else if (receivedClassName.equals(PluginReceivers.EventScrobbleReceiver.class.getName())) {
                // Scrobble (event: OPERATION_PLAY_NOW)
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_NOW) && sharedPreferences.getBoolean(getString(R.string.prefkey_scrobble_enabled), true)) {
                    scrobble(session);
                }
            } else if (receivedClassName.equals(PluginReceivers.ExecuteLoveReceiver.class.getName())) {
                // Love
                love(session);
            } else if (receivedClassName.equals(PluginReceivers.ExecuteUnLoveReceiver.class.getName())) {
                // Unlove
                unlove(session);
            }
        } catch (Exception e) {
            AppUtils.showToast(this, R.string.error_app);
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
            String previousMediaUri = sharedPreferences.getString( PREFKEY_PREVIOUS_MEDIA_URI, "");
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
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), false))
                    AppUtils.showToast(context, getString(R.string.message_post_success, propertyData.getFirst(MediaProperty.TITLE)));
            } else if (result == CommandResult.FAILED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
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
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), false))
                    AppUtils.showToast(context, context.getString(R.string.message_love_success,  propertyData.getFirst(MediaProperty.TITLE)));
            } else if (result == CommandResult.FAILED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
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
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), false))
                    AppUtils.showToast(context, context.getString(R.string.message_unlove_success,  propertyData.getFirst(MediaProperty.TITLE)));
            } else if (result == CommandResult.FAILED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
                    AppUtils.showToast(context, R.string.message_unlove_failure);
            }
        }
    }

}
