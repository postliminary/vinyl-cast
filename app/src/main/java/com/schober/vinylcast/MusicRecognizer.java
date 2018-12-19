package com.schober.vinylcast;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.acrcloud.rec.sdk.ACRCloudClient;
import com.acrcloud.rec.sdk.ACRCloudConfig;
import com.acrcloud.rec.sdk.IACRCloudListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import kaaes.spotify.webapi.android.SpotifyApi;

/**
 * Used to perform ACR of the stream from the raw audio input stream.
 * Based on Android Gracenote example app.
 */

public class MusicRecognizer implements IACRCloudListener {
    private static final String TAG = "MusicRecognizer";
    public static final String ACCESS_KEY = "[ACCESS_KEY]";
    public static final String ACCESS_SECRET = "[ACCESS_SECRET]";
    public static final String HOST = "identify-eu-west-1.acrcloud.com";

    private boolean mProcessing = false;
    private boolean initState = false;

    private String path = "";

    private long startTime = 0;
    private long stopTime = 0;

    // ACRCloud objects
    private ACRCloudClient mClient;
    private ACRCloudConfig mConfig;

    private Context context;
    private final MainActivity activity;

    public MusicRecognizer(Context context, MainActivity activity) {
        this.context = context;
        this.activity = activity;
        initialize();
    }

    public void start() {
        if (!mProcessing) {
            mProcessing = true;
            if (this.mClient == null || !this.mClient.startRecognize()) {
                mProcessing = false;
            }
            startTime = System.currentTimeMillis();
        }
    }

    public void stop() {
        if (mProcessing && this.mClient != null) {
            this.mClient.stopRecordToRecognize();
        }
        mProcessing = false;

        stopTime = System.currentTimeMillis();
    }

    private void initialize() {
        path = Environment.getDataDirectory().toString() + "/acrcloud/model";

        File file = new File(path);
        if(!file.exists()){
            file.mkdirs();
        }

        this.mConfig = new ACRCloudConfig();
        this.mConfig.acrcloudListener = this;

        // If you implement IACRCloudResultWithAudioListener and override "onResult(ACRCloudResult result)", you can get the Audio data.
        //this.mConfig.acrcloudResultWithAudioListener = this;

        this.mConfig.context = context;
        this.mConfig.host = HOST;
        this.mConfig.dbPath = path; // offline db path, you can change it with other path which this app can access.
        this.mConfig.accessKey = ACCESS_KEY;
        this.mConfig.accessSecret = ACCESS_SECRET;


        this.mConfig.protocol = ACRCloudConfig.ACRCloudNetworkProtocol.PROTOCOL_HTTPS; // PROTOCOL_HTTPS
        this.mConfig.reqMode = ACRCloudConfig.ACRCloudRecMode.REC_MODE_REMOTE;
        //this.mConfig.reqMode = ACRCloudConfig.ACRCloudRecMode.REC_MODE_LOCAL;
        //this.mConfig.reqMode = ACRCloudConfig.ACRCloudRecMode.REC_MODE_BOTH;

        this.mClient = new ACRCloudClient();
        // If reqMode is REC_MODE_LOCAL or REC_MODE_BOTH,
        // the function initWithConfig is used to load offline db, and it may cost long time.
        this.initState = this.mClient.initWithConfig(this.mConfig);
        if (this.initState) {
            this.mClient.startPreRecord(3000); //start prerecord, you can call "this.mClient.stopPreRecord()" to stop prerecord.
        }
    }

    @Override
    public void onResult(String s) {
        Log.d(TAG, s);

        try {
            JSONObject response = new JSONObject(s);

            if (response.has("metadata")) {;
                //String albumId = response.getJSONObject("metadata").getJSONObject("music").getJSONObject("external_metadata").getJSONObject("spotify").getJSONObject("album").getString("id");

                JSONObject meta = response.getJSONObject("metadata").getJSONArray("music").getJSONObject(0);

                String title = meta.getString("title");
                String artist = meta.getJSONArray("artists").getJSONObject(0).getString("name");
                String album = meta.getJSONObject("album").getString("name");

                long playOffset = meta.getLong("play_offset_ms");
                long duration = meta.getLong("duration_ms");
                long delay = duration - playOffset;

                stop();

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        start();
                    }
                }, delay);

                activity.updateMetaDataFields(title, album, artist);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onVolumeChanged(double v) {}
}
