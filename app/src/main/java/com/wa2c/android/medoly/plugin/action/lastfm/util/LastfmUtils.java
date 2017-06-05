package com.wa2c.android.medoly.plugin.action.lastfm.util;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Last.fm utility.
 */
public class LastfmUtils {

    private static final String SHARED_DIR_NAME = "download";
    private static final String SHARED_FILE_NAME = "image";
    private static final String PROVIDER_AUTHORITIES = "com.wa2c.android.medoly.plugin.action.lastfm.fileprovider";

    /**
     *
     * @param context
     * @param downloadUrl
     * @param saveFileName
     * @return
     */
    public static Uri downloadUrl(Context context, String downloadUrl, String saveFileName) {
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
            final File sharedFile = new File(sharedDir, saveFileName);
            outputStream = new FileOutputStream(sharedFile);

            // Stream copy
            byte[] buffer = new byte[4096];
            while (true) {
                int len = inputStream.read(buffer);
                if (len < 0) {
                    break;
                }
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
            outputStream.close();

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
