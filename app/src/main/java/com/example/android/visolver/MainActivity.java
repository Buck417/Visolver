package com.example.android.visolver;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.R.attr.bitmap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button camera_button;
    private SurfaceView preview_img;
    private final int RequestCameraPermissionID = 1001;
    private CameraSource cameraSource;
    private TextView textView;
    private TextView pictureText;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCameraPermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        cameraSource.start(preview_img.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
            break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera_button = (Button) findViewById(R.id.take_photo);
        camera_button.setOnClickListener(this);
        preview_img = (SurfaceView) findViewById(R.id.surface_view);
        textView = (TextView) findViewById(R.id.text_view);
        pictureText = (TextView) findViewById(R.id.picture_text);

        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.w("MainActivity", "Detector dependencies are not yet available");
        } else {
            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setRequestedFps(2.0f)
                    .setAutoFocusEnabled(true)
                    .build();
            preview_img.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {

                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    RequestCameraPermissionID);
                            return;
                        }
                        cameraSource.start(preview_img.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    cameraSource.stop();
                }
            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {

                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {

                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() != 0) {
                        textView.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int i = 0; i < items.size(); ++i) {
                                    TextBlock item = items.valueAt(i);
                                    stringBuilder.append(item.getValue());
                                    stringBuilder.append("\n");
                                }
                                textView.setText(stringBuilder.toString());
                            }
                        });
                    }
                }
            });
        }
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;

    static final int REQUEST_TAKE_PHOTO = 1;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            File myPic = new File(mCurrentPhotoPath);
            if (myPic.exists()) {
                Bitmap myBitmap = constructBitmap(myPic);
                detectText(myBitmap);
                //ImageView myImage = (ImageView) findViewById(R.id.preview);
                //myImage.setImageBitmap(myBitmap);
            }
        }
    }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onClick(View v) {
        if(v == camera_button){
            dispatchTakePictureIntent();
        }
    }

    private void detectText(Bitmap image){
        Frame imageFrame = new Frame.Builder().setBitmap(image).build();

        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if(!textRecognizer.isOperational()){
            Log.w("MainActivity", "Text Recognizer isn't started yet, wait and try again soon.");
        }
        else{
            SparseArray<TextBlock> myTextBlock = textRecognizer.detect(imageFrame);
            StringBuilder stringBuilder = new StringBuilder();
            int newLine = 0;
            for (int i = 0; i < myTextBlock.size(); ++i) {
                TextBlock item = myTextBlock.valueAt(i);
                //if(isNumeric(item.getValue())){
                    stringBuilder.append(item.getValue());
                    if(newLine < 2){
                        stringBuilder.append(" ");
                        newLine++;
                    }
                    else{
                        stringBuilder.append("\n");
                        newLine = 0;
                    }
                //}
                //Rect box = item.getBoundingBox();

            }

            Intent resultActivity = new Intent(MainActivity.this, Result.class);
            resultActivity.putExtra("RESULTS", stringBuilder.toString());
            resultActivity.putExtra("BITMAP", mCurrentPhotoPath);
            startActivity(resultActivity);
            pictureText.setText(stringBuilder.toString());

        }
        textRecognizer.release();
    }

    private Bitmap constructBitmap(File imgFile){
        ExifInterface exif = null;
        try{
            exif = new ExifInterface(mCurrentPhotoPath);
        }
        catch(IOException e){
            e.printStackTrace();
        }
        Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        int orientValue = exif.getAttributeInt(exif.TAG_ORIENTATION, exif.ORIENTATION_NORMAL);
        switch(orientValue){
            case ExifInterface.ORIENTATION_ROTATE_90:
               return resolveOrientation(myBitmap, 90);

            case ExifInterface.ORIENTATION_ROTATE_180:
                return resolveOrientation(myBitmap, 180);

            case ExifInterface.ORIENTATION_ROTATE_270:
                return resolveOrientation(myBitmap, 270);

            default:
                return myBitmap;
        }
    }

    private boolean isNumeric(String value){
        try{
            int number = Integer.parseInt(value);
        }
        catch(NumberFormatException e){
            return false;
        }
        return true;
    }

    private Bitmap resolveOrientation(Bitmap bitmap, int degree){
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImage = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return rotatedImage;
    }
}
