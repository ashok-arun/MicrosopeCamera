package com.example.ashok.ashok_arun_camera_zoom;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements CameraHelper.OnCameraHelperListener{
    private static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    private SurfaceView mCameraView;
    private CameraHelper mCamHelper;
    private float lastDistance;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        mCameraView = (SurfaceView)findViewById(R.id.id_camera);
        mCamHelper = CameraHelper.createCameraHelper();
        mCamHelper.setSurfaceView(mCameraView);
        mCamHelper.setOnCameraHelperListener(this);
    }
    
    public void onTakePitureClick(View view) {
        String picPath = ROOT_PATH + File.separator
                + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date(System.currentTimeMillis()))
                + ".jpg";
        mCamHelper.takePicture(picPath);
    }

    public void onRecordMp4(View view) {
        String videoPath = ROOT_PATH + File.separator
                + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date(System.currentTimeMillis()))
                + ".mp4";
        if(! isRecording) {
            mCamHelper.startRecordMp4(videoPath);
            showMsg("Start Recording");
        } else {
            mCamHelper.stopRecordMp4();
            showMsg("Stop Recording");
        }
        isRecording = !isRecording;
    }

    @Override
    public void OnTakePicture(String path, Bitmap bm) {
        showMsg(bm==null ? "Photo Fail" : path);
    }

    @Override
    public void onCameraFocus(boolean success, Camera camera) {
        showMsg(success ? "Focused" : "Not Focused");
    }

    @Override
    public void onCameraPreview(byte[] data, Camera camera) {
    }

    @Override
    public void onZoomChanged(int maxZoomVaule, int zoomValue) {
        Log.i("debug","Zoom="+zoomValue+"；Max Zoom="+maxZoomVaule);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            mCamHelper.cameraFocus();
            return true;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                lastDistance = getFingersDistance(event);
                break;
            case MotionEvent.ACTION_MOVE:
                float newDistance = getFingersDistance(event);
                if (newDistance > lastDistance) {
                    mCamHelper.setZoom(true);
                } else if (newDistance < lastDistance) {
                    mCamHelper.setZoom(false);
                }
                break;
        }
        return true;
    }
    private float getFingersDistance(MotionEvent event) {
        float xDistance = event.getX(0) - event.getY(1);
        float yDistance = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(xDistance * xDistance + yDistance * yDistance);
    }

    private void showMsg(String msg) {
        Toast.makeText(MainActivity.this,msg,Toast.LENGTH_SHORT).show();
    }
}

