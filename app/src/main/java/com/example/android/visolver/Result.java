package com.example.android.visolver;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;

public class Result extends AppCompatActivity {

    private static final String TAG = "Result";

    TextView resultView;
    ImageView previewImage;
    private Bitmap originalImage;
    private Bitmap sudokuImage;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    buildMat(sudokuImage);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultView = (TextView) findViewById(R.id.result_text_view);
        previewImage = (ImageView) findViewById(R.id.preview_image);

        Intent myIntent = getIntent();
        if(myIntent.hasExtra("RESULTS")){
            String results = myIntent.getStringExtra("RESULTS");
            Log.v(TAG, results);
            resultView.setText(results);
            File myPic = new File(myIntent.getStringExtra("BITMAP"));
            if(myPic.exists()){
                originalImage = BitmapFactory.decodeFile(myPic.getAbsolutePath());
            }
        }
        sudokuImage = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.sudokuwide);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void buildMat(Bitmap bmp){
        Mat myImg = new Mat();
        Utils.bitmapToMat(bmp, myImg);
        Mat grayImg = new Mat();
        Imgproc.cvtColor(myImg, grayImg, Imgproc.COLOR_RGB2GRAY);
        org.opencv.core.Size s = new Size(5, 5);
        Mat blurImg = new Mat();
        Imgproc.GaussianBlur(grayImg, blurImg, s, 5, 5);
        Mat threshImg = new Mat();
        Imgproc.adaptiveThreshold(blurImg, threshImg, 255, 1, 1, 11, 2);

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat cannyEdges = new Mat();
        Mat hierarchy = new Mat();

        Imgproc.Canny(threshImg, cannyEdges, 10, 100);
        Log.i(TAG, "Got canny edges finished");
        Imgproc.findContours(cannyEdges, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        Log.i(TAG, "Finished finding Contours " + contours.size());
        Mat myContours = new Mat();
        myContours.create(cannyEdges.rows(), cannyEdges.cols(), CvType.CV_8UC3);
        Log.i(TAG, "Created new Mat myContours");
        /*Random r = new Random();
        for(int i = 0; i < 10; i++){
            Imgproc.drawContours(myContours, contours, i, new Scalar(r.nextInt(255), r.nextInt(255), r.nextInt(255)), -1);
            Log.i(TAG,"Drawing contours");
        }*/
        double maxVal = 0;
        int maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
        {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
            if (maxVal < contourArea)
            {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }

        Imgproc.drawContours(myImg, contours, maxValIdx, new Scalar(0,255,0), 10);



        //This section works for a single contour to create a red star
        /*Imgproc.findContours(threshImg, contours, hierarchy, Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            Imgproc.drawContours(myImg, contours, contourIdx, new Scalar(255, 0, 0), -1);
        }*/



        Utils.matToBitmap(myImg, bmp);
        previewImage.setImageBitmap(bmp);
    }
}
