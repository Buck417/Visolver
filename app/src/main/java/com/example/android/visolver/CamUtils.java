package com.example.android.visolver;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by Ryan on 4/19/2018.
 */

public class CamUtils {

    private Camera mCamera;

    private static final String TAG = "CamUtils";


    public CamUtils(Camera camera){
        mCamera = camera;

        Camera.Parameters params = mCamera.getParameters();
        List<String> focusList = params.getSupportedFocusModes();

        if (focusList.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        Camera.Size mSize;
        for (Camera.Size size : sizes) {
            Log.i(TAG, "Available resolution: "+size.width+" "+size.height);
            //mSize = size;
        }
        mSize = params.getPictureSize();
        Log.i(TAG, "Used resolution: " + mSize.width + " " + mSize.height);


        mCamera.setParameters(params);
    }

    public Bitmap identifyRotation(Bitmap bmp, int orientValue){
        switch(orientValue){
            case 90:
                return resolveOrientation(bmp, 90);

            case 180:
                return resolveOrientation(bmp, 180);

            case 270:
                return resolveOrientation(bmp, 270);

            default:
                return bmp;
        }
    }

    private Bitmap resolveOrientation(Bitmap bitmap, int degree){
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImage = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return rotatedImage;
    }

    public void releaseCamera(){
        if(mCamera != null){
            mCamera.release();
        }
    }

    public boolean hasAutofocus() {
        Camera.Parameters params = mCamera.getParameters();
        List<String> focusList = params.getSupportedFocusModes();
        return focusList.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }
}
