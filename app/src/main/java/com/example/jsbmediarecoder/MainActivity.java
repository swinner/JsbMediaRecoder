package com.example.jsbmediarecoder;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.jsbmediarecoder.camera2.Camera2VideoActivity;
import com.tbruyelle.rxpermissions.RxPermissions;

import rx.Subscriber;
import rx.functions.Action1;

public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnStartRecord = (Button)findViewById(R.id.btnStartRecord);
        btnStartRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                RxPermissions.getInstance(MainActivity.this)
                        .request(Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean aBoolean) {
                                if(aBoolean){
                                    Intent intent = new Intent();
                                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                                        intent.setClass(MainActivity.this,Camera2VideoActivity.class);
                                    }else{
                                        intent.setClass(MainActivity.this,VideoRecorderActivity.class);
                                    }
                                    startActivity(intent);
                                }else{
                                    Toast.makeText(MainActivity.this, "权限未开启", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

//        View decorView  = getWindow().getDecorView();
//        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_FULLSCREEN;
//        decorView.setSystemUiVisibility(uiOptions);
        hideBottomUIMenu();
    }

    /**
     * 隐藏虚拟按键，并且全屏
     */
    protected void hideBottomUIMenu() {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }
}
