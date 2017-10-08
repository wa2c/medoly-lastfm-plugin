package com.wa2c.android.medoly.plugin.action.lastfm.service;

import android.content.Intent;
import android.text.TextUtils;

import com.wa2c.android.medoly.library.ExtraData;
import com.wa2c.android.medoly.library.MediaProperty;
import com.wa2c.android.medoly.library.PluginOperationCategory;
import com.wa2c.android.medoly.library.PluginTypeCategory;
import com.wa2c.android.medoly.library.PropertyData;
import com.wa2c.android.medoly.plugin.action.lastfm.R;
import com.wa2c.android.medoly.plugin.action.lastfm.Token;
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils;
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger;

import de.umass.lastfm.Track;


/**
 * Intent service.
 */
public class PluginGetPropertyService extends AbstractPluginService {
    /**
     * Constructor.
     */
    public PluginGetPropertyService() {
         super(PluginGetPropertyService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);
        if (pluginIntent == null)
            return;
        if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_GET_PROPERTY)) {
            sendResult(null);
            return;
        }

        try {
            String operation = sharedPreferences.getString(getString(R.string.prefkey_event_get_property_operation), "");
            if (    pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) ||
                    ( pluginIntent.hasCategory(PluginOperationCategory.OPERATION_MEDIA_OPEN) && PluginOperationCategory.OPERATION_MEDIA_OPEN.name().equals(operation) ) ||
                    ( pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_START) && PluginOperationCategory.OPERATION_PLAY_START.name().equals(operation) ) ) {
                getProperties();
            } else {
                sendResult(null);
            }
        } catch (Exception e) {
            AppUtils.showToast(this, R.string.error_app);
        }
    }

    /**
     * Get properties.
     */
    private void getProperties() {
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
            sendResult(resultProperty, resultExtra);
            if (result == CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media);
            } else if (result == CommandResult.SUCCEEDED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_success_message_show), false))
                    AppUtils.showToast(context, R.string.message_get_data_success);
            } else if (result == CommandResult.FAILED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(context.getString(R.string.prefkey_post_failure_message_show), true))
                    AppUtils.showToast(context, R.string.message_get_data_failure);
            }
        }
    }

}
