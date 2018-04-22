package com.example.android.visolver;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

/**
 * Created by Ryan on 4/21/2018.
 */

public class ShowPictureActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_picture);

        Bundle extras = getIntent().getExtras();
        if(extras != null){
            if (extras.containsKey("PATH")) {
                String imagePath = extras.getString("PATH");
                ImageView iv = (ImageView) findViewById(R.id.picture);
                iv.setImageURI(Uri.parse(imagePath));
            }
        }
    }
}
