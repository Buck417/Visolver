package com.example.android.visolver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

/**
 * Created by Ryan on 4/21/2018.
 */

public class ShowPictureActivity extends Activity {

    private Button retryBtn;
    private Button solveBtn;
    private String picturePath;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_picture);

        Bundle extras = getIntent().getExtras();
        if(extras != null){
            if (extras.containsKey("PATH")) {
                picturePath = extras.getString("PATH");
                ImageView iv = (ImageView) findViewById(R.id.ivImage);
                iv.setImageURI(Uri.parse(picturePath));
            }
        }

        retryBtn = (Button) findViewById(R.id.retry);
        retryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        solveBtn = (Button) findViewById(R.id.solve);
        solveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSolve();
            }
        });
    }

    private void startSolve() {
        Intent solveIntent = new Intent(this, Result.class);
        solveIntent.putExtra("PATH", picturePath);
        startActivity(solveIntent);
    }
}
