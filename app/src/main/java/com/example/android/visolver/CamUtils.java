package com.example.android.visolver;

import android.hardware.Camera;

/**
 * Created by Ryan on 4/19/2018.
 */

public class CamUtils {

    private Camera mCamera;

    public CamUtils(Camera camera){
        mCamera = camera;
    }

    public void releaseCamera(){
        if(mCamera != null){
            mCamera.release();
        }
    }
}
