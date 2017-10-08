package com.wa2c.android.medoly.plugin.action.lastfm.service;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.wa2c.android.medoly.library.MediaProperty;
import com.wa2c.android.medoly.library.PluginTypeCategory;
import com.wa2c.android.medoly.plugin.action.lastfm.R;
import com.wa2c.android.medoly.plugin.action.lastfm.Token;
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils;
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger;

import de.umass.lastfm.Session;
import de.umass.lastfm.Track;


/**
 * Intent service.
 */
public class PluginRunService extends AbstractPluginService {

    /**
     * Constructor.
     */
    public PluginRunService() {
        super(PluginRunService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);
        if (pluginIntent == null)
            return;
        if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_RUN)) {
            return;
        }
        try {
            if (receivedClassName.equals(PluginReceivers.ExecuteTrackPageReceiver.class.getName())) {
                openTrackPage(session);
            } else if (receivedClassName.equals(PluginReceivers.ExecuteLastfmSiteReceiver.class.getName())) {
                openLastfmPage(session);
            }
        } catch (Exception e) {
            AppUtils.showToast(this, R.string.error_app);
        }
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
