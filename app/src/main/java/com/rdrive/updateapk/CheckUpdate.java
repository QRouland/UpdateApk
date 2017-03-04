/*
Copyright 2017 Quentin Rouland

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.

 */
package com.rdrive.updateapk;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

class CheckUpdate extends AsyncTask<Context, Integer, Boolean> {
    private static final String URL_CHECK_UPDATE = "http://repo.rdrive.ovh/BonjourSenorita/Bonjour_Senorita/last";
    private static final String URL_DOWNLOAD_UPDATE = "http://repo.rdrive.ovh/download/BonjourSenorita/Bonjour_Senorita/last";

    private Version currentVersion;
    private Version lastVersion;

    private static Version lastVersion() {
        String last = null ;
        Version v = new Version();
        try {
            URL url = new URL(CheckUpdate.URL_CHECK_UPDATE);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = urlConnection.getInputStream();
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                last = stringBuilder.toString();
            }
            finally{
                urlConnection.disconnect();
            }
        }
        catch(Exception e) {
            Log.e("ERROR", e.getMessage(), e);
            return null;
        }

        try {
            JSONObject obj = new JSONObject(last);
            v.version_major = obj.getString("version_major");
            v.version_minor = obj.getString("version_minor");
            v.version_release = obj.getString("version_release");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return v;
    }

    private static boolean isLastVersion(Context context) {
        Version current_version = currentVersion(context);
        Version last_version = lastVersion();

        assert current_version != null;
        return current_version.equals(last_version);
    }

    private static Version currentVersion(Context context) {
        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        String version = pInfo.versionName;
        System.out.println(version);
        int i = 0;
        Version current_version = new Version();
        current_version.version_major = "0";
        current_version.version_minor = "0";
        current_version.version_release = "0";
        for (String retval: version.split("\\.")) {
            System.out.println(retval);
            if(i==0)
                current_version.version_major = retval;
            else if(i==1)
                current_version.version_minor = retval;
            else if(i==3)
                current_version.version_release = retval;
            i++;
        }
        return current_version;
    }

    static String urlLastVersion() {
        return URL_DOWNLOAD_UPDATE;
    }

    static void downloadLastVersion(Context context, DownloadManager dm) {
        String url = urlLastVersion();
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Update");
        request.setTitle("Some title");
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "name-of-the-file.ext");
        dm.enqueue(request);
    }

    static void openApk(Context context, String fileName) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(fileName), "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected Boolean doInBackground(Context... params) {
        currentVersion = currentVersion(params[0]);
        lastVersion = lastVersion();
        return isLastVersion(params[0]);
    }

    Version getCurrentVersion() {
        return currentVersion;
    }

    Version getLastVersion() {
        return lastVersion;
    }
}
