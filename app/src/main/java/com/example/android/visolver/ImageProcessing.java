package com.example.android.visolver;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Ryan on 3/27/2018.
 */

public class ImageProcessing {

    public ImageProcessing(){

    }

    /*
        Takes a bitmap bmp and uses OpenCV libraries to conver the bitmap to grayscale, apply Gaussian/Median blur
        and then apply a threshold.
     */
    public Mat buildThresholdMat(Bitmap bmp){
        Mat bmpMat = new Mat();
        Utils.bitmapToMat(bmp, bmpMat);
        Mat grayImg = new Mat();
        Imgproc.cvtColor(bmpMat, grayImg, Imgproc.COLOR_RGB2GRAY);

        org.opencv.core.Size s = new Size(5, 5);
        Mat blurImg = new Mat();
        Imgproc.GaussianBlur(grayImg, blurImg, s, 5, 5);
        Mat medianBlur = new Mat();
        Imgproc.medianBlur(blurImg, medianBlur, 5);
        Mat threshImg = new Mat();
        Imgproc.adaptiveThreshold(medianBlur, threshImg, 255, 1, 1, 11, 2);
        return threshImg;
    }
}
