package com.workclub.wearable.cameratrigger.activity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.workclub.wearable.cameratrigger.R;
import com.workclub.wearable.cameratrigger.activity.bus.StartTimerBus;
import com.workclub.wearable.cameratrigger.activity.utils.Tools;
import com.workclub.wearable.cameratrigger.activity.utils.decode.DecodeUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.greenrobot.event.EventBus;


/**
 * Created by jordi Navines on 26/11/2014.
 */
public class CameraActivity extends MainAcitvity {

    private static final String TAG = "CameraActivity";

    private Camera mCamera;
    private CameraPreview mPreview;
    private ExifInterface exif;
    private int deviceHeight, deviceWidth;
    private Button ibCapture, back, start_wearable_activity;
    private ImageView image_taken;
    private String dir;
    private String path;
    private String fileName;


    private Context ctx;


    private boolean isPhotoTookShowing = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ctx = this;

        // Setting all the path for the image
        path = ctx.getExternalFilesDir(null).getAbsolutePath();
        dir = "/photos/";

        setupViews();

        // Selecting the resolution of the Android device so we can create a
        // proportional preview
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        deviceHeight = display.getHeight();
        deviceWidth = display.getWidth();
    }

    private void setupViews() {
        // Getting all the needed elements from the layout
        ibCapture = (Button) findViewById(R.id.ibCapture);
        back = (Button) findViewById(R.id.back);
        start_wearable_activity = (Button) findViewById(R.id.start_wearable_activity);
        image_taken = (ImageView) findViewById(R.id.image_taken);

        // Add a listener to the Capture button
        ibCapture.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCamera.takePicture(null, null, mPicture);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                isPhotoShowing();
            }
        });

    }


    private void createCamera() {
        // Create an instance of Camera
        mCamera = getCameraInstance();

        if (mCamera != null) {
            // Setting the right parameters in the camera
            Camera.Parameters params = mCamera.getParameters();
            params.setPictureSize(1600, 1200);
            params.setPictureFormat(PixelFormat.JPEG);
            params.setJpegQuality(85);
            mCamera.setParameters(params);

            // Rotate the camera to allow portrait mode
            mCamera.setDisplayOrientation(90);

            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(this, mCamera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(deviceWidth, deviceHeight);
            preview.setLayoutParams(layoutParams);

            // Adding the camera preview
            preview.addView(mPreview, 0);
        } else {
            //show error
            showToast("Error creating the camera");
        }
    }

    @Override
    public void onBackPressed() {
        //check if we are showing the photo we took
        if (!isPhotoShowing()) {
            super.onBackPressed();
        }
    }

    private boolean isPhotoShowing() {
        if (isPhotoTookShowing) {
            isPhotoTookShowing = false;
            // Restart the camera preview.
            mCamera.startPreview();
            image_taken.setVisibility(ImageView.GONE);

            ibCapture.setVisibility(Button.VISIBLE);
            back.setVisibility(Button.GONE);
            start_wearable_activity.setVisibility(Button.VISIBLE);
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Creating the camera
        createCamera();
        EventBus.getDefault().register(this);
    }



    @Override
    protected void onPause() {
        super.onPause();

        // release the camera immediately on pause event
        releaseCamera();

        // removing the inserted view - so when we come back to the app we
        // won't have the views on top of each other.
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.removeViewAt(0);

        EventBus.getDefault().unregister(this);
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release(); // release the camera for other applications
            mCamera = null;
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    private boolean checkSDCard() {
        boolean state = false;

        String sd = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(sd)) {
            state = true;
        }

        return state;
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            // attempt to get a Camera instance
            c = Camera.open();
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }

        // returns null if camera is unavailable
        return c;
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        public void onPictureTaken(byte[] data, Camera camera) {

            fileName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()).toString() + ".jpg";

            Tools.dirChecker(path + dir);

            FileOutputStream outStream = null;
            try {
                // write to sdcard
                outStream = new FileOutputStream(path + dir + fileName);
                outStream.write(data);
                outStream.close();
                //Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);
            } catch (FileNotFoundException e) {
                //e.printStackTrace();
            } catch (IOException e) {
                //e.printStackTrace();
            } finally {

            }

            // complet path is null we are coming from other part is not select images from gallery
            String pathFile = path + dir + fileName;

            /// we load the image from sd
            Uri uri = Uri.parse("file://" + pathFile);
            Bitmap bitmap = DecodeUtils.decode(ctx, uri, 1000, 1000);

            //rotate the image before saving it on the SD
            Matrix rotateRight = new Matrix();
            rotateRight.preRotate(90);

            //// front cam taking picture like mirror .. correct image
            if (android.os.Build.VERSION.SDK_INT > 13 && mPreview.frontCamera) {
                float[] mirrorY = {-1, 0, 0, 0, 1, 0, 0, 0, 1};
                Matrix matrixMirrorY = new Matrix();

                matrixMirrorY.preRotate(90);

                matrixMirrorY.setValues(mirrorY);

                bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), matrixMirrorY, true);
            }

            if (bitmap != null) {

                isPhotoTookShowing = true;

                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotateRight, true);

                //image_taken.setImageBitmap(bitmap);

                 sendPhoto(toAsset(bitmap));

                mCamera.stopPreview();

                ibCapture.setVisibility(Button.GONE);
                start_wearable_activity.setVisibility(Button.GONE);
                back.setVisibility(Button.VISIBLE);
            } else {
                Log.d(TAG, "bitmap null!");
                showToast("Error taking the picture");
            }
        }
    };



    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Start the countdown of three seconds to take the photo
     */
    public void startTimner() {
        //remove the photo taken previously
        isPhotoShowing();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mCamera.takePicture(null, null, mPicture);
            }
        }, 3000);
    }


    public void onEvent(StartTimerBus e) {
        startTimner();
    }

}