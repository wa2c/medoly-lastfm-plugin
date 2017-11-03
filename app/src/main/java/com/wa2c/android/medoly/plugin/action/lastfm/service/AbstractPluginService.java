package com.wa2c.android.medoly.plugin.action.lastfm.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.wa2c.android.medoly.library.ExtraData;
import com.wa2c.android.medoly.library.MediaPluginIntent;
import com.wa2c.android.medoly.library.PropertyData;
import com.wa2c.android.medoly.plugin.action.lastfm.R;
import com.wa2c.android.medoly.plugin.action.lastfm.Token;
import com.wa2c.android.medoly.plugin.action.lastfm.util.AppUtils;
import com.wa2c.android.medoly.plugin.action.lastfm.util.Logger;

import java.io.File;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Session;
import de.umass.lastfm.cache.Cache;
import de.umass.lastfm.cache.FileSystemCache;


/**
 * Intent service.
 */
public abstract class AbstractPluginService extends IntentService {

    /** Received receiver class name. */
    public static String RECEIVED_CLASS_NAME = "RECEIVED_CLASS_NAME";

    /** Previous data key. */
    public static final String PREFKEY_PREVIOUS_MEDIA_URI = "previous_media_uri";

    /**
     * Command result.
     */
    enum CommandResult {
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
    protected Context context = null;
    /** Preferences. */
    protected SharedPreferences sharedPreferences = null;
    /** Plugin intent. */
    protected MediaPluginIntent pluginIntent;
    /** Property data. */
    protected PropertyData propertyData;
    /** Received class name. */
    protected String receivedClassName;
    /** Session. */
    protected Session session;
    /** True if a result sent */
    private boolean resultSent = false;

    /**
     * Constructor.
     */
    public AbstractPluginService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Logger.d("onHandleIntent");
        if (intent == null)
            return;

        try {
            resultSent = false;
            context = getApplicationContext();
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            pluginIntent = new MediaPluginIntent(intent);
            propertyData = pluginIntent.getPropertyData();
            receivedClassName = pluginIntent.getStringExtra(RECEIVED_CLASS_NAME);

            // Initialize last.fm library
            try { Caller.getInstance().setCache(new FileSystemCache(new File(context.getExternalCacheDir(), "last.fm"))); } catch (Exception ignore) {}

            // Authenticate
            String username = sharedPreferences.getString(context.getString(R.string.prefkey_auth_username), "");
            String password = sharedPreferences.getString(context.getString(R.string.prefkey_auth_password), "");
            try {
                session = Authenticator.getMobileSession(username, password, Token.getConsumerKey(context), Token.getConsumerSecret(context));
            } catch (Exception e) {
                // Not error if authentication was failed.
                Logger.e(e);
            }
        } catch (Exception e) {
            Logger.e(e);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Logger.d("onDestroy");

        sendResult(null);

        context = null;
        sharedPreferences = null;
        pluginIntent = null;
    }

    /**
     * Send result
     * @param resultProperty A result property data.
     */
    protected void sendResult(PropertyData resultProperty) {
        sendResult(resultProperty, null);
    }

    /**
     * Send result
     * @param resultProperty A result property data.
     * @param resultExtra A result extra data.
     */
    protected void sendResult(PropertyData resultProperty, ExtraData resultExtra) {
        if (!resultSent && ( (this instanceof PluginGetPropertyService) || (this instanceof PluginGetAlbumArtService) ) ) {
            sendBroadcast(pluginIntent.createResultIntent(resultProperty, resultExtra));
            resultSent = true;
        }
    }

}
