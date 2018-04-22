package com.example.android.visolver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

/**
 * Created by Ryan on 4/17/2018.
 */

public class CameraActivity extends Activity {

    private static final String TAG = "CameraPreview";

    /**
     * Id of the camera to access. 0 is the first camera.
     */
    private static final int CAMERA_ID = 0;

    private PreviewCamera mPreview;
    private Camera mCamera;
    private Button picButton;
    private String mOriginalPhotoPath;
    private CamUtils camUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupCamera();

    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop camera access
        releaseCamera();
    }

    @Override
    public void onResume(){
        super.onResume();
        setupCamera();
    }

    private void setupCamera(){
        // Open an instance of the first camera and retrieve its info.
        mCamera = getCameraInstance(CAMERA_ID);
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(CAMERA_ID, cameraInfo);

        if (mCamera == null || cameraInfo == null) {
            // Camera is not available, display error message
            Toast.makeText(this, "Camera is not available.", Toast.LENGTH_SHORT).show();
            setContentView(R.layout.activity_camera_unavailable);
        } else {

            camUtils = new CamUtils(mCamera);

            setContentView(R.layout.activity_camera);

            // Get the rotation of the screen to adjust the preview image accordingly.
            final int displayRotation = getWindowManager().getDefaultDisplay()
                    .getRotation();

            // Create the Preview view and set it as the content of this Activity.
            mPreview = new PreviewCamera(this, mCamera, cameraInfo, displayRotation);
            RelativeLayout preview = (RelativeLayout) findViewById(R.id.rlPreview);
            preview.addView(mPreview);

            // Add a listener to the Capture button
            ImageButton captureButton = (ImageButton) findViewById(R.id.picButton);
            captureButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // get an image from the camera
                            mCamera.takePicture(null, null, mPicture);
                        }
                    }
            );
        }
    }

    /** A safe way to get an instance of the Camera object. */
    private Camera getCameraInstance(int cameraId) {
        Camera c = null;
        try {
            c = Camera.open(cameraId); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Toast.makeText(this, "Camera " + cameraId + " is not available: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
        return c; // returns null if camera is unavailable
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File(
                imageFileName +  /* prefix */
                ".jpg" +         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mOriginalPhotoPath = image.getAbsolutePath();
        Log.i(TAG, "The file location is: " + image.getAbsolutePath().toString());
        return image;
    }

    private void showPictureIntent(){
        Intent showPicture = new Intent(this, ShowPictureActivity.class);
        showPicture.putExtra("PATH", mOriginalPhotoPath);
        startActivity(showPicture);
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            Bitmap pictureBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Bitmap myBmp = resolveOrientation(pictureBitmap, 90);
            try {

                File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                if (pictureFile == null){
                    Log.d(TAG, "Error creating media file, check storage permissions");
                    return;
                }
                FileOutputStream fos = new FileOutputStream(pictureFile);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                myBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] newData = stream.toByteArray();
                fos.write(newData);
                fos.close();
                mOriginalPhotoPath = pictureFile.getAbsolutePath();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            if(mOriginalPhotoPath != null){
                //camUtils.identifyRotation(mOriginalPhotoPath);
                showPictureIntent();
            }

        }
    };


    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /** Create a file Uri for saving an image or video */
    private Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        /*if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("Visolver", "failed to create directory");
                return null;
            }
        }*/

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    private Bitmap resolveOrientation(Bitmap bitmap, int degree){
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImage = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return rotatedImage;
    }

}
