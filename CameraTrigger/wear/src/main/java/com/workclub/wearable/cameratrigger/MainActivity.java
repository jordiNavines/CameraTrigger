/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.workclub.wearable.cameratrigger;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Shows the photo you trigger from your watch and take with your phone the Wearable APIs.
 */
public class MainActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener, DataApi.DataListener, MessageApi.MessageListener,
        NodeApi.NodeListener {

    private static final String TAG = "MainActivity";

    private GoogleApiClient mGoogleApiClient;
    private ImageButton take_photo;
    private TextView countdown, loading;
    private View mLayout;
    private ImageView image;
    private LinearLayout intro_layout;
    private Handler mHandler;


    private static final String CLOSE_PATH = "/close";
    private static final String CLOSE_KEY = "close";

    String nodeId;

    private static final String TAKE_PICTURE_PATH = "/start-camera-activity";

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        mHandler = new Handler();
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.d("starting", "starting");

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override public void onLayoutInflated(WatchViewStub stub) {
                Log.d("inflating", "inflating");
                take_photo = (ImageButton) findViewById(R.id.take_photo);
                mLayout = findViewById(R.id.layout);
                countdown= (TextView) findViewById(R.id.countdown);
                loading= (TextView) findViewById(R.id.loading);
                intro_layout= (LinearLayout) findViewById(R.id.intro_layout);
                image= (ImageView) findViewById(R.id.image);

                take_photo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        takePhoto();
                    }
                });

                image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        image.setVisibility(ImageView.GONE);

                        intro_layout.setVisibility(LinearLayout.VISIBLE);
                    }
                });
            }
        });

        if (getIntent()!=null && getIntent().getExtras()!=null && getIntent().getExtras().get("NodeId")!=null){
            nodeId= getIntent().getExtras().getString("NodeId");
        }


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();



    }


    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected(): Successfully connected to Google API client");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + result);
    }


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged(): " + dataEvents);


        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (DataLayerListenerService.IMAGE_PATH.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    Asset photo = dataMapItem.getDataMap()
                            .getAsset(DataLayerListenerService.IMAGE_KEY);
                    final Bitmap bitmap = loadBitmapFromAsset(mGoogleApiClient, photo);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Setting background image..");
                            //mLayout.setBackground(new BitmapDrawable(getResources(), bitmap));
                            image.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                            image.setVisibility(ImageView.VISIBLE);

                            loading.setVisibility(TextView.GONE);
                        }
                    });

                } else if (DataLayerListenerService.DATA_ITEM__PATH.equals(path)) {
                    Log.d(TAG, "DataItem Changed "+ event.getDataItem().toString());
                } else if (CLOSE_PATH.equals(path)) {
                    finish();
                    Log.d(TAG, "Unrecognized path: " + path);
                }

            }  else {
                Log.d(TAG, "Unknown data event type: " + event.getType());
            }
        }
    }

    /**
     * Extracts {@link android.graphics.Bitmap} data from the
     * {@link com.google.android.gms.wearable.Asset}
     */
    private Bitmap loadBitmapFromAsset(GoogleApiClient apiClient, Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                apiClient, asset).await().getInputStream();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        return BitmapFactory.decodeStream(assetInputStream);
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        Log.d(TAG, "onMessageReceived: " + event.toString());

        nodeId=  event.getSourceNodeId();

        if (CLOSE_PATH.equals(event.getPath())) {
            finish();
            Log.d(TAG, "Unrecognized path: " + event.getPath());
        }
    }


    private void takePhoto() {

        if (nodeId!=null) {
            Wearable.MessageApi.sendMessage(
                mGoogleApiClient, nodeId, TAKE_PICTURE_PATH , null).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "node:" + nodeId + "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        }
                    }
                }
            );

            startCountDown();
        }else{
            showToast("Not connected");
        }
    }

    public void startCountDown(){
        intro_layout.setVisibility(LinearLayout.GONE);
        countdown.setVisibility(TextView.VISIBLE);

        start();
    }

    int count= 4;
    private Timer timer;
    private TimerTask timerTask= null;


    public void start() {
        if(timer != null) {
            return;
        }

        timerTask = new TimerTask() {

            @Override
            public void run() {
                if (count!=0){
                    count--;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            countdown.setText(String.valueOf(count));
                        }
                    });
                }else{
                    count=4;
                    stop();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            countdown.setVisibility(TextView.GONE);
                            showAnimation();
                        }
                    });
                }
            }
        };

        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 0, 1000);
    }

    public void stop() {
        timerTask.cancel();
        timerTask=null;
        timer.cancel();
        timer = null;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public void showAnimation(){
        Intent intent = new Intent(this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.SUCCESS_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                "Photo!!");
        startActivity(intent);
        loading.setVisibility(TextView.VISIBLE);
    }


    @Override
    public void onPeerConnected(Node node) {
        Log.d(TAG, "node"+ node.getId());
    }

    @Override
    public void onPeerDisconnected(Node node) {
        Log.d(TAG, "node Disconnected"+ node.getId());
    }


}
