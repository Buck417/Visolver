package com.example.android.visolver;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
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
    Rect[][] contourRects;
    Bitmap[][] ocrTiles;

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

    private void buildMat(Bitmap srcBmp){
        ImageProcessing imp = new ImageProcessing();
        //Threshold of original image for contouring
        Mat threshImg = imp.buildThresholdMat(srcBmp);
        //Mat of original image for warping
        Mat srcMat = new Mat();
        Utils.bitmapToMat(srcBmp, srcMat);

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(threshImg, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        int largestContourIdx = getMaxContour(contours);


        //Note: Android uses images with alpha values, so not including the last 255 value on Scalar means any contour
        //we draw will be transparent.
        //Imgproc.drawContours(srcMat, contours, largestContourIdx, new Scalar(0,255,255, 255), 5);

        //Largest contour list of perimeter points
        MatOfPoint matOfPoint = contours.get(largestContourIdx);

        org.opencv.core.Point[] srcCorners = findCorners(matOfPoint.toList());
        //Calculates largest side of the rectangle for creating a new bitmap with proper side lengths
        double distance = Math.hypot(Math.abs(srcCorners[1].x - srcCorners[0].x), Math.abs(srcCorners[1].y - srcCorners[0].y));
        for(int i = 0; i < 3; i++){
            distance = Math.max(distance, Math.hypot(Math.abs(srcCorners[i+1].x - srcCorners[i].x), Math.abs(srcCorners[i+1].y - srcCorners[i].y)));
        }

        //Next 11 lines perform warpPerspective and stores new Mat in output
        MatOfPoint2f srcPerspectiveMat = new MatOfPoint2f(srcCorners);
        org.opencv.core.Point[] destCorners = new org.opencv.core.Point[4];
        destCorners[0] = new Point(0, 0);
        destCorners[1] = new Point(distance-1, 0);
        destCorners[2] = new Point(distance-1, distance-1);
        destCorners[3] = new Point(0, distance-1);
        MatOfPoint2f destPerspectiveMat = new MatOfPoint2f(destCorners);

        Mat lambda;
        lambda = Imgproc.getPerspectiveTransform(srcPerspectiveMat, destPerspectiveMat);

        Mat output = new Mat((int) distance, (int) distance, CvType.CV_8UC3);
        Imgproc.warpPerspective(srcMat, output, lambda, output.size());


        /*Log.i(TAG, "Top left corner value is " + srcCorners[0].x + " " + srcCorners[0].y);
        Log.i(TAG, "Top right value is " + srcCorners[1].x + " " + srcCorners[1].y);
        Log.i(TAG, "Bottom right value is " + srcCorners[2].x + " " + srcCorners[2].y);
        Log.i(TAG, "Bottom left value is " + srcCorners[3].x + " " + srcCorners[3].y);
        Log.i(TAG, "Largest distance is " + distance);*/
        //Log.i(TAG, "Total height is " + r.height);
        //Log.i(TAG, "Total width is " + r.width);
        //Bitmap puzzle = Bitmap.createBitmap(srcBmp, r.x, r.y, r.width, r.height);
        //Utils.matToBitmap(srcMat, srcBmp);
        //buildFile(puzzle);

        //Testing circle corner points
        /*Imgproc.circle(srcMat, new Point(r.x, r.y), 50, new Scalar(255, 0, 0),2);
        Imgproc.circle(srcMat, new Point(r.x+r.width, r.y), 50, new Scalar(255, 0, 0),2);
        Imgproc.circle(srcMat, new Point(r.x, r.y+r.height), 50, new Scalar(255, 0, 0),2);
        Imgproc.circle(srcMat, new Point(r.x+r.width, r.y+r.height), 50, new Scalar(255, 0, 0),2);
        MatOfPoint2f src = new MatOfPoint2f(corners[0], corners[1], corners[2], corners[3]);
        MatOfPoint testMat = new MatOfPoint(contours.get(maxValIdx));*/

        //Creates new bitmap with the dimensions of the size of the grid identified
        Bitmap gridBitmap = Bitmap.createBitmap(output.width(), output.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(output, gridBitmap);

        //Bitmap scaledGrid = Bitmap.createScaledBitmap(gridBitmap, (int)(gridBitmap.getWidth()*1.8),(int) (gridBitmap.getHeight() * 1.8), true);

        //Prepare this new bitmap of the grid for OCR processing. Read the book
        //Mat gridThreshImg = imp.buildThresholdMat(gridBitmap);
        //Utils.matToBitmap(gridThreshImg, gridBitmap);


        Log.i(TAG, "Density of Gridmap is " + gridBitmap.getDensity());
        Bitmap[][] tiles = splitBitmap(gridBitmap, 9, 9);
        ocrTiles = processCells(tiles);
        /*for(int i = 0; i < 9; i++){
            for(int j = 0; j < 9; j++){
                String filename = "fixedgrid" + i + "" + j;
                buildFile(fixedTiles[i][j], filename);
            }
        }*/




        //Bitmap bmp22 = tiles[1][3];
        //buildFile(tiles[4][3], "grid43");
        //Mat testMat = new Mat();
        //Utils.bitmapToMat(bmp22, testMat);

        //Bitmap testImage = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.grid43);
        new PerformOCR().execute();
        //String[][] sudokuDigits = new String[9][9];
        //gridOCR(ocrTiles, sudokuDigits);
        //writeToTextFile(sudokuDigits);
        //previewImage.setImageBitmap(gridBitmap);
        //resultView.setText(myNumber);




        previewImage.setImageBitmap(gridBitmap);


    }


    private void gridOCR(Bitmap[][] tiles, int rows, int columns) {
        String[][] gridData = new String[rows][columns];
        AssetManager assetManager = getAssets();
        TessOCR tessOCR = new TessOCR(assetManager);
        //String ocrText = tessOCR.getResults(tiles[0][1], contourRects[0][1]);
        //Log.i(TAG, "Text found in square " + 0 + "" + 1 + " is " + ocrText);
        for(int i = 0; i < rows; i++){
            for(int j = 0; j < columns; j++){
                String ocrText = tessOCR.getResults(tiles[i][j], contourRects[i][j]);
                if(ocrText.equals("")){
                    ocrText = "-";

                }
                Log.i(TAG, "Text found in square " + i + "" + j + " is " + ocrText);
                gridData[i][j] = ocrText;
                Log.i(TAG, "Bitmap pixel count is " + tiles[i][j].getWidth() + " " + tiles[i][j].getHeight());
            }
        }
        printGrid(gridData, rows, columns);
    }

    private void printGrid(String[][] gridData, int rows, int columns) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < rows; i++){
            for(int j = 0; j < columns; j++){
                sb.append(gridData[i][j] + " ");
            }
            sb.append("\n");
        }
        Log.i(TAG, sb.toString());
    }

    private Bitmap[][] processCells(Bitmap[][] originalTiles){
        Bitmap[][] results = new Bitmap[9][9];
        ImageProcessing im = new ImageProcessing();
        contourRects = new Rect[9][9];
        for(int i = 0; i < 9; i++){
            for(int j = 0; j < 9; j++){
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalTiles[i][j], originalTiles[i][j].getWidth()*2, originalTiles[i][j].getHeight()*2, true);
                Mat matTile = im.buildThresholdMat(scaledBitmap);
                ArrayList<MatOfPoint> tileContours = new ArrayList<>();
                Imgproc.findContours(matTile, tileContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
                int largest = getMaxContour(tileContours);
                contourRects[i][j] = Imgproc.boundingRect(tileContours.get(largest));
                Mat testTile = new Mat(matTile.size(), CV_8UC3, new Scalar(0,0,0));
                Bitmap gridBitmap = Bitmap.createBitmap(testTile.width(), testTile.height(), Bitmap.Config.ARGB_8888);
                if(Imgproc.contourArea(tileContours.get(largest)) > ((testTile.width() * testTile.height()) / 25.0)) {
                    Imgproc.drawContours(testTile, tileContours, largest, new Scalar(255, 255, 255, 255), 15);
                    //Imgproc.rectangle(testTile, largestContourRect.tl(), largestContourRect.br(),new Scalar(0, 0, 255, 255), 10);
                }
                Utils.matToBitmap(testTile, gridBitmap);
                results[i][j] = gridBitmap;
                String filename = "fixedgrid" + i + "" + j;
                buildFile(results[i][j], filename);


            }
        }
        return results;
    }



    private Bitmap[][] splitBitmap(Bitmap bitmap, int xCount, int yCount) {
        int widthCutoff = 50;
        int heightCutoff = 50;
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
                // Create the sliced bitmap, flipping indexes since Bitmap x,y axis are opposite of array indexes
                bitmaps[x][y] = Bitmap.createBitmap(bitmap, (y * width) + widthCutoff, (x * height) + heightCutoff, (int)(width - widthCutoff*1.8) , (int) (height-heightCutoff*1.8));
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

    /*
    Given a list of points on a perimeter assumed to be a rectangle, finds the 4 corners and returns
    them in a Point array.
     */
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

    private class PerformOCR extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... params) {
            gridOCR(ocrTiles, 9, 9);
            return null;
        }

    }

}


