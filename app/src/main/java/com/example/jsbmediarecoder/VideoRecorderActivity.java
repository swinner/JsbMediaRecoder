package com.example.jsbmediarecoder;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.example.jsbmediarecoder.widget.SendView;
import com.example.jsbmediarecoder.widget.VideoProgressBar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Created by wanbo on 2017/1/18.
 */

public class VideoRecorderActivity extends FragmentActivity implements SurfaceHolder.Callback {

    //    private MediaUtils mediaUtils;
    private boolean isCancel;
    private VideoProgressBar progressBar;
    private int mProgress;
    private TextView btnInfo , btn;
    private SendView send;
    private RelativeLayout recordLayout, switchLayout;
    private VideoRecorderActivity ctx;

    private SurfaceView mSurfaceView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        setContentView(R.layout.activity_video);
        Toast.makeText(ctx, "5.1系统之下", Toast.LENGTH_SHORT).show();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mSurfaceView = (SurfaceView) findViewById(R.id.main_surface_view);

        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        Point h = new Point();
        wm.getDefaultDisplay().getSize(h);
        mWidthScreen = h.x;
        mHeightScreen = h.y;

//        mediaUtils = new MediaUtils(this,widthScreen,heightScreen);
//        mediaUtils.setRecorderType(MediaUtils.MEDIA_VIDEO);
//        mediaUtils.setTargetDir(Environment.getExternalStoragePublicDirectory("mpviees"));
//        mediaUtils.setTargetName(UUID.randomUUID() + ".mp4");
//        mediaUtils.setSurfaceView(surfaceView);

        setSurfaceView();


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
                switchCamera();

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



    boolean doRecord = false;
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
                            doRecord = false;
                            handler.sendEmptyMessageDelayed(99,300);
                            ret = true;
                            break;
                        case MotionEvent.ACTION_UP:
                            if(doRecord){
                                if (mProgress == 0) {
                                    stopView(false);
                                    break;
                                }
                                if (mProgress < 10) {
                                    //时间太短不保存
                                    stopRecordUnSave();
                                    Toast.makeText(ctx, "时间太短", Toast.LENGTH_SHORT).show();
                                    break;
                                }
                                //停止录制
                                stopRecordSave();
                            }else{
                                handler.removeMessages(99);
                                takePhoto();
                                btnInfo.setText("");
                            }

                            ret = false;
                            break;
                        case MotionEvent.ACTION_MOVE:
//                            float currentY = event.getY();
//                            isCancel = downY - currentY > 10;
                            //moveView();
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
            stopRecordSave();
        }
    };


    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    progressBar.setProgress(mProgress);
                    if (isRecording()) {
                        mProgress = mProgress + 1;
                        sendMessageDelayed(handler.obtainMessage(0), 100);
                    }
                    break;
                case 99:
                    doRecord = true;
                    record();
                    startView();
                    break;
            }
        }
    };



    private void startView() {
        btnInfo.setText("");
        startAnim();
        mProgress = 0;
        handler.removeMessages(0);
        handler.sendMessage(handler.obtainMessage(0));
    }

//    private void moveView() {
//        if (isCancel) {
//            btnInfo.setText("");
//        } else {
//            btnInfo.setText("");
//        }
//    }

    private void stopView(final boolean isSave) {
        ctx.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stopAnim();
                progressBar.setCancel(true);
                mProgress = 0;
                handler.removeMessages(0);
                if (isSave) {
                    recordLayout.setVisibility(View.GONE);
                    send.startAnim();
                }else{
                    btnInfo.setText("轻触拍照，长按摄像");
                }
            }
        });
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
            deleteTargetFile();
            startPreView();
            btnInfo.setText("轻触拍照，长按摄像");
        }
    };

    private String getTargetFilePath() {
        return targetFile.getPath();
    }

    private boolean deleteTargetFile() {
        if (targetFile.exists()) {
            return targetFile.delete();
        } else {
            return false;
        }
    }

    private View.OnClickListener selectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String savePath = "";
            savePath = getTargetFilePath();

            Toast.makeText(VideoRecorderActivity.this, "文件以保存至：" + savePath, Toast.LENGTH_SHORT).show();
            send.stopAnim();
            startPreView();
            recordLayout.setVisibility(View.VISIBLE);
            btnInfo.setText("轻触拍照，长按摄像");
        }
    };







    public static final int MEDIA_AUDIO = 0;
    public static final int MEDIA_VIDEO = 1;
    private MediaRecorder mMediaRecorder;
    private CamcorderProfile profile;
    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private File targetDir;
    private String targetName;
    private File targetFile;
    private int previewWidth, previewHeight;
    private int recorderType;
    private boolean isRecording;
    private int or = 90;
    private int cameraPosition = 1;//0代表前置摄像头，1代表后置摄像头

    private int mWidthScreen = 0;
    private int mHeightScreen = 0;



    private void setSurfaceView() {
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setFixedSize(previewWidth, previewHeight);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);

//        mDetector = new GestureDetector(activity, new ZoomGestureListener());
//        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                mDetector.onTouchEvent(event);
//                return true;
//            }
//        });
    }



    private boolean isRecording() {
        return isRecording;
    }

    private void record() {
        if (isRecording) {
            try {
                mMediaRecorder.stop();  // stop the recording
            } catch (RuntimeException e) {
                // RuntimeException is thrown when stop() is called immediately after start().
                // In this case the output file is not properly constructed ans should be deleted.
                Log.d("tag", "RuntimeException: stop() is called immediately after start()");
                //noinspection ResultOfMethodCallIgnored
                targetFile.delete();
            }
            releaseMediaRecorder(); // release the MediaRecorder object
            mCamera.lock();         // take camera access back from MediaRecorder
            isRecording = false;
        } else {
            startRecordThread();
        }
    }

    private boolean prepareRecord() {
        try {

            mMediaRecorder = new MediaRecorder();
            if (recorderType == MEDIA_VIDEO) {
//                mCamera.lock();
                mCamera.stopPreview();
                mCamera.unlock();
                mMediaRecorder.setCamera(mCamera);
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
                mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                mMediaRecorder.setProfile(profile);
                // 实际视屏录制后的方向
                if(cameraPosition == 0){
                    mMediaRecorder.setOrientationHint(270);
                }else {
                    mMediaRecorder.setOrientationHint(or);
                }

            } else if (recorderType == MEDIA_AUDIO) {
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            }

            File storageDir = Environment.getExternalStoragePublicDirectory("mpviees");
            if(!storageDir.exists()){
                storageDir.mkdirs();
            }
            targetFile = new File(storageDir, UUID.randomUUID() + ".mp4");

            mMediaRecorder.setOutputFile(targetFile.getPath());

        } catch (Exception e) {
            e.printStackTrace();
            Log.d("MediaRecorder", "Exception prepareRecord: ");
            releaseMediaRecorder();
            return false;
        }
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d("MediaRecorder", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d("MediaRecorder", "IOException preparing MediaRecorder==>: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void stopRecordSave() {
        Log.d("Recorder", "stopRecordSave");
        if (isRecording) {
            isRecording = false;
            try {
                mMediaRecorder.stop();
                Log.d("Recorder", targetFile.getPath());
            } catch (RuntimeException r) {
                Log.d("Recorder", "RuntimeException: stop() is called immediately after start()");
            } finally {
                releaseMediaRecorder();
            }
        }
    }

    private void stopRecordUnSave() {
        Log.d("Recorder", "stopRecordUnSave");
        if (isRecording) {
            isRecording = false;
            try {
                mMediaRecorder.stop();
            } catch (RuntimeException r) {
                Log.d("Recorder", "RuntimeException: stop() is called immediately after start()");
                if (targetFile.exists()) {
                    //不保存直接删掉
                    targetFile.delete();
                }
            } finally {
                releaseMediaRecorder();
            }
            if (targetFile.exists()) {
                //不保存直接删掉
                targetFile.delete();
            }
        }
    }
    private void startPreView() {
        startPreView(mSurfaceHolder);
    }

    private void startPreView(SurfaceHolder holder) {
        if (mCamera == null) {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
        if (mCamera != null) {
            mCamera.setDisplayOrientation(or);
            try {
                mCamera.setPreviewDisplay(holder);
                Camera.Parameters parameters = mCamera.getParameters();

                List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
                List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();

                Camera.Size mVideoSize = chooseVideoSize(mSupportedVideoSizes,mWidthScreen,mHeightScreen);
                Camera.Size chooseVideoSize =chooseOptimalSize2(mSupportedPreviewSizes,mWidthScreen,mHeightScreen,mVideoSize);


//                Camera.Size optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes,
//                        mSupportedPreviewSizes, mSurfaceView.getWidth(), mSurfaceView.getHeight());
                // Use the same size for recording profile.
                previewWidth = chooseVideoSize.width;
                previewHeight = chooseVideoSize.height;

                parameters.setPreviewSize(previewWidth, previewHeight);
                parameters.setJpegQuality(100); // 设置照片质量
                if (parameters.getSupportedFocusModes().contains(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 连续对焦模式
                }

                profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
                // 这里是重点，分辨率和比特率
                // 分辨率越大视频大小越大，比特率越大视频越清晰
                // 清晰度由比特率决定，视频尺寸和像素量由分辨率决定
                // 比特率越高越清晰（前提是分辨率保持不变），分辨率越大视频尺寸越大。
                profile.videoFrameWidth = mVideoSize.width;
                profile.videoFrameHeight = mVideoSize.height;
                // 这样设置 1080p的视频 大小在5M , 可根据自己需求调节
                profile.videoBitRate = 2 * mVideoSize.width * mVideoSize.height;
                List<String> focusModes = parameters.getSupportedFocusModes();
                if (focusModes != null) {
                    for (String mode : focusModes) {
                        mode.contains("continuous-video");
                        parameters.setFocusMode("continuous-video");
                    }
                }
                mCamera.setParameters(parameters);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }






    private Camera.Size chooseVideoSize(List<Camera.Size> videoSizes, int wScreen, int hScreen) {
        for (Camera.Size size : videoSizes) {
            if (size.width == size.width * hScreen / wScreen) {
                return size;
            }
        }
        Log.e("tag", "Couldn't find any suitable video size");
        return Collections.min(videoSizes, new VideoRecorderActivity.CompareSizesByArea2((float) wScreen, (float)  hScreen));
        //return choices[0];
    }

    private  Camera.Size chooseOptimalSize2(List<Camera.Size> videoSizes,  int width, int height, Camera.Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Camera.Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.width;
        int h = aspectRatio.height;
        float screenScale = (float)height/(float)width;
        for (Camera.Size option : videoSizes) {
            //if (option.getHeight() == option.getWidth() * h / w) {
            if(screenScale == ((float)option.width/(float)option.height)){
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.max(bigEnough, new VideoRecorderActivity.CompareSizesByArea());
        } else {
            Log.e("tag", "Couldn't find any suitable preview size");
            return Collections.min(videoSizes, new VideoRecorderActivity.CompareSizesByArea2((float) width, (float)  height));
            //return choices[0];
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Camera.Size> {

        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.width * lhs.height -
                    (long) rhs.width * rhs.height);
        }

    }


    static class CompareSizesByArea2 implements Comparator<Camera.Size> {
        float ratio;
        public CompareSizesByArea2(float w,float h){
            ratio = h/w;
        }
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            float r1= Math.abs((float) lhs.width/lhs.height - ratio);
            float r2= Math.abs((float) rhs.width/rhs.height - ratio);
            float r0 = r1 - r2;
            if(r0 > 0.0f){
                return 1;
            }else if( r0 < 0.0f){
                return -1;
            }else {
                return 0;
            }
        }
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            Log.d("Recorder", "release Recorder");
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            // release the camera for other applications
            mCamera.release();
            mCamera = null;
            Log.d("Recorder", "release Camera");
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        startPreView(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            Log.d("tag", "surfaceDestroyed: ");
            releaseCamera();
        }
        if (mMediaRecorder != null) {
            releaseMediaRecorder();
        }
    }

    private void startRecordThread() {
        if (prepareRecord()) {
            try {
                mMediaRecorder.start();
                isRecording = true;
                Log.d("Recorder", "Start Record");
            } catch (RuntimeException r) {
                releaseMediaRecorder();
                Log.d("Recorder", "RuntimeException: start() is called immediately after stop()");
            }
        }
    }
    /**
     *@authorc: gaotengfei
     *@time: 2017/11/1
     *@desc: 暂时去掉双击放大功能
     */
//    private class ZoomGestureListener extends GestureDetector.SimpleOnGestureListener {
//        //双击手势事件
//        @Override
//        public boolean onDoubleTap(MotionEvent e) {
//            super.onDoubleTap(e);
//            Log.d(TAG, "onDoubleTap: 双击事件");
//            if (!isZoomIn) {
//                setZoom(20);
//                isZoomIn = true;
//            } else {
//                setZoom(0);
//                isZoomIn = false;
//            }
//            return true;
//        }
//    }

    private void setZoom(int zoomValue) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.isZoomSupported()) {
                int maxZoom = parameters.getMaxZoom();
                if (maxZoom == 0) {
                    return;
                }
                if (zoomValue > maxZoom) {
                    zoomValue = maxZoom;
                }
                parameters.setZoom(zoomValue);
                mCamera.setParameters(parameters);
            }
        }
    }

    private String getVideoThumb(String path) {
        MediaMetadataRetriever media = new MediaMetadataRetriever();
        media.setDataSource(path);
        return bitmap2File(media.getFrameAtTime());
    }

    private String bitmap2File(Bitmap bitmap) {
        File thumbFile = new File(targetDir,
                targetName);
        if (thumbFile.exists()) thumbFile.delete();
        FileOutputStream fOut;
        try {
            fOut = new FileOutputStream(thumbFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            return null;
        }
        return thumbFile.getAbsolutePath();
    }

    public void switchCamera() {
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();//得到摄像头的个数

        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);//得到每一个摄像头的信息
            if (cameraPosition == 1) {
                //现在是后置，变更为前置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    mCamera.stopPreview();//停掉原来摄像头的预览
                    mCamera.release();//释放资源
                    mCamera = null;//取消原来摄像头
                    mCamera = Camera.open(i);//打开当前选中的摄像头
                    startPreView(mSurfaceHolder);
                    cameraPosition = 0;
                    break;
                }
            } else {
                //现在是前置， 变更为后置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    mCamera.stopPreview();//停掉原来摄像头的预览
                    mCamera.release();//释放资源
                    mCamera = null;//取消原来摄像头
                    mCamera = Camera.open(i);//打开当前选中的摄像头
                    startPreView(mSurfaceHolder);
                    cameraPosition = 1;
                    break;
                }
            }
        }
    }
    /**
     *@authorc: gaotengfei
     *@time: 2017/11/6
     *@desc: 5.0之下的拍照处理
     *
     */
    private boolean mWaitForTakePhoto = false;
    public void takePhoto() {
        if (mCamera == null || mWaitForTakePhoto) {
            return;
        }
        mWaitForTakePhoto = true;
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                onTakePhoto(data);
                stopView(true);
                mWaitForTakePhoto = false;
            }
        });
    }
    private  String getPictureFilePath() {
        File storageDir = Environment.getExternalStoragePublicDirectory("mpviees");
        if(!storageDir.exists()){
            storageDir.mkdirs();
        }
        targetFile = new File(storageDir, UUID.randomUUID() + "pic.jpg");
        return  targetFile.getAbsolutePath();
    }

    private void onTakePhoto(byte[] data) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(getPictureFilePath());
            fos.write(data);
            fos.flush();
            //启动我的裁剪界面
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
