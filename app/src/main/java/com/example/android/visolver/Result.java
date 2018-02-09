package com.example.android.visolver;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

public class Result extends AppCompatActivity {

    TextView resultView;
    ImageView previewImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultView = (TextView) findViewById(R.id.result_text_view);
        previewImage = (ImageView) findViewById(R.id.preview_image);

        Intent myIntent = getIntent();
        if(myIntent.hasExtra("RESULTS")){
            String results = myIntent.getStringExtra("RESULTS");
            resultView.setText(results);
            File myPic = new File(myIntent.getStringExtra("BITMAP"));
            if(myPic.exists()){
                Bitmap myBitmap = BitmapFactory.decodeFile(myPic.getAbsolutePath());
                previewImage.setImageBitmap(myBitmap);
            }
        }
    }
}
