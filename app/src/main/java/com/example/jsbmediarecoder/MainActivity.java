package com.example.jsbmediarecoder;

import android.content.Intent;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.jsbmediarecoder.camera2.Camera2VideoActivity;

public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnStartRecord = (Button)findViewById(R.id.btnStartRecord);
        btnStartRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                Intent intent = new Intent();
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                    intent.setClass(MainActivity.this,Camera2VideoActivity.class);
                }else{
                    intent.setClass(MainActivity.this,VideoRecorderActivity.class);
                }
                startActivity(intent);
            }
        });
    }

}
