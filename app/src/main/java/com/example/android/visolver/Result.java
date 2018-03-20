package com.example.android.visolver;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_8UC3;

public class Result extends AppCompatActivity {

    private static final String TAG = "Result";

    TextView resultView;
    ImageView previewImage;
    private Bitmap originalImage;
    private Bitmap sudokuImage;
    String picPath;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    buildMat(originalImage);
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
            picPath = myIntent.getStringExtra("BITMAP");
            File myPic = new File(myIntent.getStringExtra("BITMAP"));
            if(myPic.exists()){
                originalImage = BitmapFactory.decodeFile(myPic.getAbsolutePath());
                //buildFile(originalImage);
            }
        }
        //sudokuImage = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.sudokutest);
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
        Mat hierarchy = new Mat();

        Imgproc.findContours(threshImg, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);



        int largestContourIdx = getMaxContour(contours);







        //Note: Android uses images with alpha values, so not including the last 255 value on Scalar means any contour
        //we draw will be transparent.
        //Imgproc.drawContours(myImg, contours, largestContourIdx, new Scalar(0,255,255, 255), 5);

        //This attempts to build a Rect from the points of the largest contour
        //Would need a canvas to draw the Rect on the original image
        MatOfPoint matOfPoint = contours.get(largestContourIdx);

        org.opencv.core.Point[] srcCorners = findCorners(matOfPoint.toList());

        double distance = Math.hypot(Math.abs(srcCorners[1].x - srcCorners[0].x), Math.abs(srcCorners[1].y - srcCorners[0].y));
        for(int i = 0; i < 3; i++){
            distance = Math.max(distance, Math.hypot(Math.abs(srcCorners[i+1].x - srcCorners[i].x), Math.abs(srcCorners[i+1].y - srcCorners[i].y)));
        }
        MatOfPoint2f srcMat = new MatOfPoint2f(srcCorners);

        org.opencv.core.Point[] destCorners = new org.opencv.core.Point[4];
        destCorners[0] = new Point(0, 0);
        destCorners[1] = new Point(distance-1, 0);
        destCorners[2] = new Point(distance-1, distance-1);
        destCorners[3] = new Point(0, distance-1);
        MatOfPoint2f destMat = new MatOfPoint2f(destCorners);


        Mat lambda;
        lambda = Imgproc.getPerspectiveTransform(srcMat, destMat);

        Mat output = new Mat((int) distance, (int) distance, CvType.CV_8UC3);
        Imgproc.warpPerspective(threshImg, output, lambda, output.size());


        Log.i(TAG, "Top left corner value is " + srcCorners[0].x + " " + srcCorners[0].y);
        Log.i(TAG, "Top right value is " + srcCorners[1].x + " " + srcCorners[1].y);
        Log.i(TAG, "Bottom right value is " + srcCorners[2].x + " " + srcCorners[2].y);
        Log.i(TAG, "Bottom left value is " + srcCorners[3].x + " " + srcCorners[3].y);
        Log.i(TAG, "Largest distance is " + distance);
        //Log.i(TAG, "Total height is " + r.height);
        //Log.i(TAG, "Total width is " + r.width);
        //Bitmap puzzle = Bitmap.createBitmap(bmp, r.x, r.y, r.width, r.height);
        //Utils.matToBitmap(myImg, bmp);
        //buildFile(puzzle);

        //Testing circle corner points
        /*Imgproc.circle(myImg, new Point(r.x, r.y), 50, new Scalar(255, 0, 0),2);
        Imgproc.circle(myImg, new Point(r.x+r.width, r.y), 50, new Scalar(255, 0, 0),2);
        Imgproc.circle(myImg, new Point(r.x, r.y+r.height), 50, new Scalar(255, 0, 0),2);
        Imgproc.circle(myImg, new Point(r.x+r.width, r.y+r.height), 50, new Scalar(255, 0, 0),2);
        MatOfPoint2f src = new MatOfPoint2f(corners[0], corners[1], corners[2], corners[3]);
        MatOfPoint testMat = new MatOfPoint(contours.get(maxValIdx));

        MatOfPoint dst = new MatOfPoint(myImg);
                new Point(0, 0),
                new Point(1000-1, 0),
                new Point(0, 1000-1),
                new Point(1000-1, 1000-1));
        Mat warpMat = Imgproc.getPerspectiveTransform(testMat, dst);
        Mat destImage = new Mat();
        Imgproc.warpPerspective(myImg, destImage, warpMat, myImg.size());*/


        /*final int w = r.width;
        final int h = r.height;

        Bitmap compare = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        int color1, color2, a, g, b, red;

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                bmp.getPixel(r.x+x, r.y+y);




                compare.setPixel(x, y, Color.argb(0, 0, 0, 0));
            }
        }*/




        //This section works for a single contour to create a red star
        /*Imgproc.findContours(threshImg, contours, hierarchy, Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            Imgproc.drawContours(myImg, contours, contourIdx, new Scalar(255, 0, 0), -1);
        }*/


        /*int finalWidth = previewImage.getMaxWidth();
        int finalHeight = previewImage.getMaxHeight();
        ArrayList<Point> sourcePoints = new ArrayList<>();
        sourcePoints.add(new Point(0.0, 0.0));
        sourcePoints.add(new Point(puzzle.getWidth(), 0.0));
        sourcePoints.add(new Point(puzzle.getWidth(), puzzle.getHeight()));
        sourcePoints.add(new Point(0.0, puzzle.getHeight()));
        Mat srcMat = Converters.vector_Point2f_to_Mat(sourcePoints);

        ArrayList<Point> destPoints = new ArrayList<>();
        destPoints.add(new Point(0.0, 0.0));
        destPoints.add(new Point(finalWidth, 0.0));
        destPoints.add(new Point(finalWidth, finalHeight));
        destPoints.add(new Point(0.0, finalHeight));
        Mat destMat = Converters.vector_Point2f_to_Mat(destPoints);

        Mat result = Imgproc.getPerspectiveTransform(srcMat, destMat);
        Log.i(TAG, "Dims of src is " + srcMat.dims());
        Log.i(TAG, "Dims of dst is " + destMat.dims());

        Imgproc.warpPerspective(srcMat, destMat, result, destMat.size());*/
        Bitmap bmp2 = Bitmap.createBitmap(output.width(), output.height(), Bitmap.Config.RGB_565);

        Utils.matToBitmap(output, bmp2);






        //Utils.matToBitmap(threshImg, bmp);
        Bitmap[][] tiles = splitBitmap(bmp2, 9, 9);
        /*for(int i = 0; i < 9; i++){
            for(int j = 0; j < 9; j++){
                String filename = "grid" + i + "" + j;
                buildFile(tiles[i][j], filename);
            }
        }*/
        previewImage.setImageBitmap(bmp2);


    }

    private Bitmap[][] splitBitmap(Bitmap bitmap, int xCount, int yCount) {
        // Allocate a two dimensional array to hold the individual images.
        Bitmap[][] bitmaps = new Bitmap[xCount][yCount];
        int width, height;
        // Divide the original bitmap width by the desired vertical column count
        width = bitmap.getWidth() / xCount;
        // Divide the original bitmap height by the desired horizontal row count
        height = bitmap.getHeight() / yCount;
        // Loop the array and create bitmaps for each coordinate
        for(int x = 0; x < xCount; ++x) {
            for(int y = 0; y < yCount; ++y) {
                // Create the sliced bitmap
                bitmaps[x][y] = Bitmap.createBitmap(bitmap, x * width, y * height, width, height);
            }
        }
        // Return the array
        return bitmaps;
    }

    private int getMaxContour(ArrayList<MatOfPoint> contours){
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
        return maxValIdx;
    }

    private Point[] findCorners(List<Point> box){
        Point[] vertices = new Point[4];
        double topLeftX, topLeftY;
        double topRightX, topRightY;
        double bottomRightX, bottomRightY;
        double bottomLeftX, bottomLeftY;
        topLeftX = topLeftY = bottomLeftX = bottomLeftY = Integer.MAX_VALUE;
        topRightX = topRightY = bottomRightX = bottomRightY = 0;


        for(Point p : box){
            //Minimize x+y for top left corner
            if((p.x+p.y) < (topLeftX + topLeftY)){
                topLeftX = p.x;
                topLeftY = p.y;
            }
            //Maximize x-y for top right corner
            if((p.x-p.y) > (topRightX - topRightY)){
                topRightX = p.x;
                topRightY = p.y;
            }
            //Maximize x+y for bottom right corner
            if((p.x+p.y) > (bottomRightX + bottomRightY)){
                bottomRightX = p.x;
                bottomRightY = p.y;
            }
            //Minimize x-y for bottom left corner
            if((p.x-p.y) < (bottomLeftX - bottomLeftY)){
                bottomLeftX = p.x;
                bottomLeftY = p.y;
            }
        }
        vertices[0] = new Point(topLeftX, topLeftY);
        vertices[1] = new Point(topRightX, topRightY);
        vertices[2] = new Point(bottomRightX, bottomRightY);
        vertices[3] = new Point(bottomLeftX, bottomLeftY);
        return vertices;
    }

    private void buildFile(Bitmap bmp, String filename){
        // Assume block needs to be inside a Try/Catch block.
        try {
            String path = Environment.getExternalStorageDirectory().toString() + "/grid";
            OutputStream fOut = null;
            Integer counter = 0;
            File file = new File(path, filename + ".jpeg"); // the File to save , append increasing numeric counter to prevent files from getting overwritten.
            fOut = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 0, fOut);
            fOut.flush(); // Not really required
            fOut.close(); // do not forget to close the stream
            MediaStore.Images.Media.insertImage(getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
            Log.i(TAG, "Finished building a file");
        } catch(Exception e){
            e.printStackTrace();
        }

         // obtaining the Bitmap
        // saving the Bitmap to a file compressed as a JPEG with 85% compression rate


    }

    public File getPublicAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }

    private Mat findLargestRectangle(Mat original_image) {
        Mat imgSource = original_image;
        //Mat untouched = original_image.clone();

        //convert the image to black and white
        Imgproc.cvtColor(imgSource, imgSource, Imgproc.COLOR_BGR2GRAY);

        //convert the image to black and white does (8 bit)
        Imgproc.Canny(imgSource, imgSource, 50, 50);

        //apply gaussian blur to smoothen lines of dots
        Imgproc.GaussianBlur(imgSource, imgSource, new Size(5, 5), 5);

        //find the contours
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(imgSource, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = -1;
        int maxAreaIdx = -1;
        MatOfPoint temp_contour = contours.get(0); //the largest is at the index 0 for starting point
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        MatOfPoint2f maxCurve = new MatOfPoint2f();
        List<MatOfPoint> largest_contours = new ArrayList<MatOfPoint>();
        for (int idx = 0; idx < contours.size(); idx++) {
            temp_contour = contours.get(idx);
            double contourarea = Imgproc.contourArea(temp_contour);
            //compare this contour to the previous largest contour found
            if (contourarea > maxArea) {
                //check if this contour is a square
                MatOfPoint2f new_mat = new MatOfPoint2f( temp_contour.toArray() );
                int contourSize = (int)temp_contour.total();
                Imgproc.approxPolyDP(new_mat, approxCurve, contourSize*0.1, true);
                if (approxCurve.total() == 4) {
                    maxCurve = approxCurve;
                    maxArea = contourarea;
                    maxAreaIdx = idx;
                    largest_contours.add(temp_contour);
                }
            }
        }

        //create the new image here using the largest detected square
        Mat new_image = new Mat(imgSource.size(), CvType.CV_8U); //we will create a new black blank image with the largest contour
        Imgproc.cvtColor(new_image, new_image, Imgproc.COLOR_BayerBG2RGB);
        Imgproc.drawContours(new_image, contours, maxAreaIdx, new Scalar(255, 255, 255), 1); //will draw the largest square/rectangle

        double temp_double[] = maxCurve.get(0, 0);
        Point p1 = new Point(temp_double[0], temp_double[1]);
        Imgproc.circle(new_image, new Point(p1.x, p1.y), 20, new Scalar(255, 0, 0), 5); //p1 is colored red


        temp_double = maxCurve.get(1, 0);
        Point p2 = new Point(temp_double[0], temp_double[1]);
        Imgproc.circle(new_image, new Point(p2.x, p2.y), 20, new Scalar(0, 255, 0), 5); //p2 is colored green


        temp_double = maxCurve.get(2, 0);
        Point p3 = new Point(temp_double[0], temp_double[1]);
        Imgproc.circle(new_image, new Point(p3.x, p3.y), 20, new Scalar(0, 0, 255), 5); //p3 is colored blue


        temp_double = maxCurve.get(3, 0);
        Point p4 = new Point(temp_double[0], temp_double[1]);
        Imgproc.circle(new_image, new Point(p4.x, p4.y), 20, new Scalar(0, 255, 255), 5); //p1 is colored violet


        return new_image;
    }
}
