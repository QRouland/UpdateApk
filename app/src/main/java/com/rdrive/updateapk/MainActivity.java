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

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static com.rdrive.updateapk.CheckUpdate.downloadLastVersion;
import static com.rdrive.updateapk.CheckUpdate.openApk;

public class MainActivity extends AppCompatActivity {
    private static final int ASK_WRITE_EXTERNAL_STORAGE_FOR_UPDATE = 1;
    private DownloadManager dm;
    private Button btn_update;
    TextView currentVersionValue;
    TextView lastVersionValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_update = (Button) findViewById(R.id.button_update);
        btn_update.setEnabled(false);

        currentVersionValue = (TextView)findViewById(R.id.currentVersionValue);
        lastVersionValue = (TextView)findViewById(R.id.lastVersionValue);

        dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        new CheckUpdate() {
            @Override
            protected void onPostExecute(Boolean aBoolean) {
                currentVersionValue.setText(this.getCurrentVersion().toString());
                lastVersionValue.setText(this.getLastVersion().toString());
                if (!aBoolean) {
                    btn_update.setEnabled(true);
                }
            }
        }.execute(getContext());


        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT > 22) {
                    if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, ASK_WRITE_EXTERNAL_STORAGE_FOR_UPDATE);
                        return;
                    }
                }
                downloadLastVersion(getContext(), dm);
            }
        });


        BroadcastReceiver attachmentDownloadCompleteReceive = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long downloadId = intent.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadId);
                    Cursor cursor = dm.query(query);
                    if (cursor.moveToFirst()) {
                        int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        String downloadLocalUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                        if ((downloadStatus == DownloadManager.STATUS_SUCCESSFUL) && downloadLocalUri != null) {
                            openApk(getContext(), downloadLocalUri);
                        }
                    }
                    cursor.close();
                }
            }
        };

        registerReceiver(attachmentDownloadCompleteReceive, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    public Context getContext() {
        return this.getBaseContext();
    }

    public Activity getActivity() {
        return this;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case ASK_WRITE_EXTERNAL_STORAGE_FOR_UPDATE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    downloadLastVersion(getContext(), dm);
                } else {
                    Log.e("ERROR", "No write access");
                }

                break;
            }
        }
    }
}
