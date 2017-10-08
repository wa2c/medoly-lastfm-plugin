package com.wa2c.android.medoly.plugin.action.lastfm.service;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.wa2c.android.medoly.library.AlbumArtProperty;
import com.wa2c.android.medoly.library.MediaProperty;
import com.wa2c.android.medoly.library.PluginOperationCategory;
import com.wa2c.android.medoly.library.PluginTypeCategory;
import com.wa2c.android.medoly.library.PropertyData;
import com.wa2c.android.medoly.plugin.action.lastfm.R;
import com.wa2c.android.medoly.plugin.action.lastfm.Token;
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils;
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger;

import de.umass.lastfm.Album;
import de.umass.lastfm.ImageSize;


/**
 * Intent service.
 */
public class PluginGetAlbumArtService extends AbstractPluginService {

    /**
     * Constructor.
     */
    public PluginGetAlbumArtService() {
         super(PluginGetAlbumArtService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);
        if (pluginIntent == null)
            return;
        if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_GET_ALBUM_ART)) {
            sendResult(null);
            return;
        }
        try {
            String operation = sharedPreferences.getString(getString(R.string.prefkey_event_get_album_art_operation), "");
            if (    pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) ||
                    ( pluginIntent.hasCategory(PluginOperationCategory.OPERATION_MEDIA_OPEN) && PluginOperationCategory.OPERATION_MEDIA_OPEN.name().equals(operation) ) ||
                    ( pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_START) && PluginOperationCategory.OPERATION_PLAY_START.name().equals(operation) ) ) {
                getAlbumArt();
            } else {
                sendResult(null);
            }
        } catch (Exception e) {
            Logger.e(e);
            AppUtils.showToast(this, R.string.error_app);
        }
    }

    /**
     * Get album art.
     */
    private void getAlbumArt() {
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
            String remoteUri = null;
            Uri localUri = null;
            ImageSize[] imageSizes = ImageSize.values();
            for (int i = imageSizes.length - 1; i >= 0; i--) {
                remoteUri = album.getImageURL(imageSizes[i]);
                if (!TextUtils.isEmpty(remoteUri)) {
                    localUri = AppUtils.downloadUrl(context, remoteUri);
                    break;
                }
            }


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
           sendResult(resultProperty);
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
