package com.wa2c.android.medoly.plugin.action.lastfm.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.FileProvider;

import com.google.gson.Gson;
import com.wa2c.android.medoly.plugin.action.lastfm.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * App utilities.
 */
public class AppUtils {

    /**
     * Show message.
     * @param context context.
     * @param text message.
     */
    public static void showToast(Context context, String text) {
        ToastReceiver.showToast(context, text);
    }

    /**
     * Show message.
     * @param context context
     * @param stringId resource id.
     */
    public static void showToast(Context context, int stringId) {
        ToastReceiver.showToast(context, stringId);
    }

    /**
     * Save object to shared preference.
     * @param context context
     * @param prefKey preference key.
     * @param saveObject save object.
     * @return succeeded / failed
     */
    public static boolean saveObject(Context context, String prefKey,  Object saveObject) {
        try {
            Gson gson = new Gson();
            String json = gson.toJson(saveObject);

            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            pref.edit().putString(prefKey, json).apply();
            return true;
        } catch (Exception e) {
            Logger.e(e);
            return false;
        }
    }

    /**
     * Load object from shared preference.
     * @param context Context.
     * @param prefKey Preference key.
     * @param clazz Object class.
     * @return Loaded object. null as failed.
     */
    public static <T> T loadObject(Context context, String prefKey, Class<T> clazz) {
        try {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            String json = pref.getString(prefKey, "");

            Gson gson = new Gson();

            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            Logger.e(e);
            return null;
        }
    }



    private static final String SHARED_DIR_NAME = "download";
    private static final String PROVIDER_AUTHORITIES = BuildConfig.APPLICATION_ID + ".fileprovider";

    /**
     * Download URI data.
     * @param context A context.
     * @param downloadUrl Download URI.
     * @return Shared URI.
     */
    public static Uri downloadUrl(Context context, String downloadUrl) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        Uri providerUri;
        try {
            final URL url = new URL(downloadUrl);
            if (ContentResolver.SCHEME_FILE.equals(url.getProtocol()) ||
                    ContentResolver.SCHEME_CONTENT.equals(url.getProtocol()))
            {
                return Uri.parse(downloadUrl);
            }

            // Folder
            final File sharedDir = new File(context.getFilesDir(), SHARED_DIR_NAME);
            if (!sharedDir.exists()) {
                // Create a folder
                sharedDir.mkdir();
            } else {
                // Delete all files
                File[] files = sharedDir.listFiles();
                for (File f : files) {
                    if (f.isFile())
                        f.delete();
                }
            }

            // InputStream
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.connect();
            final int status = con.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                return null;
            }
            inputStream = con.getInputStream();

            // OutputStream
            String[] pathElements = url.getPath().split("/");
            String saveFileName = pathElements[pathElements.length - 1];
            final File sharedFile = new File(sharedDir, saveFileName);
            outputStream = new FileOutputStream(sharedFile);

            // Stream copy
            byte[] buffer = new byte[16384];
            while (true) {
                int len = inputStream.read(buffer);
                if (len < 0) {
                    break;
                }
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();

            providerUri = FileProvider.getUriForFile(context, PROVIDER_AUTHORITIES, sharedFile);
        } catch (Exception e) {
            return null;
        } finally {
            if (inputStream != null)
                try { inputStream.close(); } catch (IOException e) { e.printStackTrace(); }
            if (outputStream != null)
                try { outputStream.close(); } catch (IOException e) { e.printStackTrace(); }
        }

        // URI
        return providerUri;
    }
}
