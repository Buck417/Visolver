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

/**
 * Created by Ryan on 4/19/2018.
 */

public class CamUtils {

    private Camera mCamera;

    private static final String TAG = "CamUtils";


    public CamUtils(Camera camera){
        mCamera = camera;
    }

    public void identifyRotation(String filePath){
        ExifInterface exif = null;
        try{
            exif = new ExifInterface(filePath);
        }
        catch(IOException e){
            e.printStackTrace();
        }
        Bitmap myBitmap = BitmapFactory.decodeFile(filePath);
        int orientValue = exif.getAttributeInt(exif.TAG_ORIENTATION, exif.ORIENTATION_NORMAL);
        switch(orientValue){
            case ExifInterface.ORIENTATION_ROTATE_90:
                resolveOrientation(myBitmap, 90, filePath);

            case ExifInterface.ORIENTATION_ROTATE_180:
                resolveOrientation(myBitmap, 180, filePath);

            case ExifInterface.ORIENTATION_ROTATE_270:
                resolveOrientation(myBitmap, 270, filePath);

            default:
        }
    }

    private void resolveOrientation(Bitmap bitmap, int degree, String filePath){
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImage = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        File outFile = new File(filePath);
        try{
            FileOutputStream out = new FileOutputStream(outFile, false);
            rotatedImage.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    public void releaseCamera(){
        if(mCamera != null){
            mCamera.release();
        }
    }
}
