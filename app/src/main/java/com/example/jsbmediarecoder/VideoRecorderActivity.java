package com.example.jsbmediarecoder;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentActivity;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.example.jsbmediarecoder.camera2.AutoFitTextureView;
import com.example.jsbmediarecoder.utils.Camera2VideoUtils;
import com.example.jsbmediarecoder.utils.MediaUtils;
import com.example.jsbmediarecoder.widget.SendView;
import com.example.jsbmediarecoder.widget.VideoProgressBar;

import java.util.UUID;

/**
 * Created by wanbo on 2017/1/18.
 */

public class VideoRecorderActivity extends FragmentActivity {

    private MediaUtils mediaUtils;
    private Camera2VideoUtils camera2VideUtils;
    private boolean isCancel;
    private VideoProgressBar progressBar;
    private int mProgress;
    private TextView btnInfo , btn;
    private SendView send;
    private RelativeLayout recordLayout, switchLayout;
    private VideoRecorderActivity ctx;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        setContentView(R.layout.activity_video);
        Toast.makeText(ctx, "5.1系统之下", Toast.LENGTH_SHORT).show();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.main_surface_view);
        AutoFitTextureView mTextureView = (AutoFitTextureView)findViewById(R.id.texture);

        // setting
        mTextureView.setVisibility(View.GONE);
        surfaceView.setVisibility(View.VISIBLE);
        mediaUtils = new MediaUtils(this);
        mediaUtils.setRecorderType(MediaUtils.MEDIA_VIDEO);
        mediaUtils.setTargetDir(Environment.getExternalStoragePublicDirectory("mpviees"));
        mediaUtils.setTargetName(UUID.randomUUID() + ".mp4");
        mediaUtils.setSurfaceView(surfaceView);

        // btn
        send = (SendView) findViewById(R.id.view_send);
        //view = (TextView) findViewById(R.id.view);
        btnInfo = (TextView) findViewById(R.id.tv_info);
        btn = (TextView) findViewById(R.id.main_press_control);
        btn.setOnTouchListener(btnTouch);
        findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        send.backLayout.setOnClickListener(backClick);
        send.selectLayout.setOnClickListener(selectClick);
        recordLayout = (RelativeLayout) findViewById(R.id.record_layout);
        switchLayout = (RelativeLayout) findViewById(R.id.btn_switch);
        switchLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaUtils.switchCamera();

            }
        });
        // progress
        progressBar = (VideoProgressBar) findViewById(R.id.main_progress_bar);
        progressBar.setOnProgressEndListener(listener);
        progressBar.setCancel(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setCancel(true);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }



    View.OnTouchListener btnTouch = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            boolean ret = false;
            float downY = 0;
            int action = event.getAction();

            switch (v.getId()) {
                case R.id.main_press_control: {
                    switch (action) {
                        case MotionEvent.ACTION_DOWN:
                            mediaUtils.record();

                            startView();
                            ret = true;
                            break;
                        case MotionEvent.ACTION_UP:
                            if (!isCancel) {
                                if (mProgress == 0) {
                                    stopView(false);
                                    break;
                                }
                                if (mProgress < 10) {
                                    //时间太短不保存
                                    mediaUtils.stopRecordUnSave();
                                    Toast.makeText(VideoRecorderActivity.this, "时间太短", Toast.LENGTH_SHORT).show();
                                    stopView(false);
                                    break;
                                }
                                //停止录制
                                mediaUtils.stopRecordSave();
                                stopView(true);
                            } else {

                                //现在是取消状态,不保存
                                mediaUtils.stopRecordUnSave();
                                Toast.makeText(VideoRecorderActivity.this, "取消保存", Toast.LENGTH_SHORT).show();
                                stopView(false);
                            }
                            ret = false;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float currentY = event.getY();
                            isCancel = downY - currentY > 10;
                            moveView();
                            break;
                    }
                }

            }
            return ret;
        }
    };

    VideoProgressBar.OnProgressEndListener listener = new VideoProgressBar.OnProgressEndListener() {
        @Override
        public void onProgressEndListener() {
            progressBar.setCancel(true);
            //停止录制
            mediaUtils.stopRecordSave();
        }
    };


    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    progressBar.setProgress(mProgress);
                    if (mediaUtils.isRecording()) {
                        mProgress = mProgress + 1;
                        sendMessageDelayed(handler.obtainMessage(0), 100);
                    }
                    break;
            }
        }
    };



    private void startView() {
        startAnim();
        mProgress = 0;
        handler.removeMessages(0);
        handler.sendMessage(handler.obtainMessage(0));
    }

    private void moveView() {
        if (isCancel) {
            btnInfo.setText("");
        } else {
            btnInfo.setText("");
        }
    }

    private void stopView(boolean isSave) {
        stopAnim();
        progressBar.setCancel(true);
        mProgress = 0;
        handler.removeMessages(0);
        if (isSave) {
            recordLayout.setVisibility(View.GONE);
            send.startAnim();
        }
    }

    private void startAnim() {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(btn, "scaleX", 1, 0.5f),
                ObjectAnimator.ofFloat(btn, "scaleY", 1, 0.5f),
                ObjectAnimator.ofFloat(progressBar, "scaleX", 1, 1.3f),
                ObjectAnimator.ofFloat(progressBar, "scaleY", 1, 1.3f)
        );
        set.setDuration(250).start();
    }

    private void stopAnim() {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(btn, "scaleX", 0.5f, 1f),
                ObjectAnimator.ofFloat(btn, "scaleY", 0.5f, 1f),
                ObjectAnimator.ofFloat(progressBar, "scaleX", 1.3f, 1f),
                ObjectAnimator.ofFloat(progressBar, "scaleY", 1.3f, 1f)
        );
        set.setDuration(250).start();
    }

    private View.OnClickListener backClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            send.stopAnim();
            recordLayout.setVisibility(View.VISIBLE);
            mediaUtils.deleteTargetFile();
            btnInfo.setText("轻触拍照，长按摄像");
        }
    };

    private View.OnClickListener selectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String savePath = "";
            savePath = mediaUtils.getTargetFilePath();

            Toast.makeText(VideoRecorderActivity.this, "文件以保存至：" + savePath, Toast.LENGTH_SHORT).show();
            send.stopAnim();
            recordLayout.setVisibility(View.VISIBLE);
            btnInfo.setText("轻触拍照，长按摄像");
        }
    };
}
