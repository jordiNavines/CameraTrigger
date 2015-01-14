package com.workclub.wearable.cameratrigger.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.workclub.wearable.cameratrigger.activity.bus.StartTimerBus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import de.greenrobot.event.EventBus;

/**
 * Created by jordinavines on 08/12/2014.
 */
public class MainAcitvity extends Activity implements
        MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {


    private static final String TAG = "MainAcitvity";

    // wear
    private static final String CLOSE_PATH = "/close";
    private static final String IMAGE_PATH = "/image";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String TAKE_PICTURE_PATH = "/start-camera-activity";

    private static final String IMAGE_KEY = "photo";


    private boolean mResolvingError = false;
    private GoogleApiClient mGoogleApiClient;


    public Handler mHandler;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        createGoogleApiclient();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (!mResolvingError) {
            SendMessageTask task= new SendMessageTask();
            task.execute(CLOSE_PATH);
        }else{
            closeGoogleAPIClient();
        }
    }

    public void closeGoogleAPIClient(){
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    public void sendMessage(String node, String path) {
        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, node, path, null).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        }
                    }
                }
        );
    }


    private void createGoogleApiclient(){
         mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }



    /**
     * Builds an {@link com.google.android.gms.wearable.Asset} from a bitmap. The image that we get
     * back from the camera in "data" is a thumbnail size. Typically, your image should not exceed
     * 320x320 and if you want to have zoom and parallax effect in your app, limit the size of your
     * image to 640x400. Resize your image before transferring to your wearable device.
     */
    public static Asset toAsset(Bitmap bitmap) {
        ByteArrayOutputStream byteStream = null;
        try {
            byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return Asset.createFromBytes(byteStream.toByteArray());
        } finally {
            if (null != byteStream) {
                try {
                    byteStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Sends the asset that was created form the photo we took by adding it to the Data Item store.
     */
    public void sendPhoto(Asset asset) {
        PutDataMapRequest dataMap = PutDataMapRequest.create(IMAGE_PATH);
        dataMap.getDataMap().putAsset(IMAGE_KEY, asset);
        dataMap.getDataMap().putLong("time", new Date().getTime());
        PutDataRequest request = dataMap.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        Log.d(TAG, "Sending image was successful: " + dataItemResult.getStatus()
                                .isSuccess());
                    }
                });

    }


    /**
     * Sends an RPC to start a fullscreen Activity on the wearable.
     */
    public void onStartWearableActivityClick(View view) {
        Log.d(TAG, "Generating RPC");

        // Trigger an AsyncTask that will query for a list of connected nodes and send a
        // "start-activity" message to each connected node.
        SendMessageTask task= new SendMessageTask();
        task.execute(START_ACTIVITY_PATH);
    }


    public class SendMessageTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... args) {
            Log.d(TAG, "doInBackground ");

            String path= args[0];

            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendMessage(node, path);
            }

            if (path.equals(CLOSE_PATH)) {
                closeGoogleAPIClient();
            }

            return null;
        }
    }



    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }




    /**
     *
     * Listeners for GoogleApiClient and MessageApi
     *
     */

    @Override //ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "Google API Client was connected");
        mResolvingError = false;
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override //ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Connection to Google API client was suspended");
    }

    @Override //OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {

            // There was an error with the resolution intent. Try again.
            mResolvingError = true;
            mGoogleApiClient.connect();

        } else {
            Log.e(TAG, "Connection to Google API client has failed");
            mResolvingError = false;

            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        }
    }



    @Override //MessageListener
    public void onMessageReceived(final MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived() A message from watch was received:" + messageEvent
                .getRequestId() + " " + messageEvent.getPath());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (messageEvent.getPath().equals(TAKE_PICTURE_PATH)) {
                    EventBus.getDefault().post(new StartTimerBus());
                }else{
                    //"Message from watch", messageEvent.toString()));
                }
            }
        });
    }



}
