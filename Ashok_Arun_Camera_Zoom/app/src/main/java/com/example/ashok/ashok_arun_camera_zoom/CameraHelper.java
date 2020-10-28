package com.example.ashok.ashok_arun_camera_zoom;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class CameraHelper implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final String TAG = "CameraHelper";
    private final MediaRecordUtil mRecordUtil;
    private int width = 1280;
    private int height = 720;
    private Camera mCamera;
    private static CameraHelper mCameraHelper;
    private WeakReference<SurfaceView> mSurfaceViewRf;
    private SurfaceHolder mHolder;
    private OnCameraHelperListener mHelperListener;
    private SensorOrientation mOriSensor;
    private SensorAccelerator mAccelerSensor;
    private int mPhoneDegree;
    private boolean isFrontCamera = false;

    private CameraHelper() {
        mRecordUtil = MediaRecordUtil.getInstance();
    }

    public static CameraHelper createCameraHelper() {
        if (mCameraHelper == null) {
            mCameraHelper = new CameraHelper();
        }
        return mCameraHelper;
    }

    public interface OnCameraHelperListener {
        // Take a photo
        void OnTakePicture(String path, Bitmap bm);

        // Focus
        void onCameraFocus(boolean success, Camera camera);

        // Preview
        void onCameraPreview(byte[] data, Camera camera);

        // Zoom
        void onZoomChanged(int maxZoomVaule, int zoomValue);
    }

    public void setOnCameraHelperListener(OnCameraHelperListener listener) {
        this.mHelperListener = listener;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (data == null)
            return;
        if (mHelperListener != null) {
            mHelperListener.onCameraPreview(data, camera);
        }
        if (mCamera != null) {
            mCamera.addCallbackBuffer(data);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        createCamera();
        startPreview();
        startOrientationSensor();
        startAcceleratorSensor();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopOrientationSensor();
        stopAcceleratorSensor();
        stopPreview();
        destoryCamera();
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        mSurfaceViewRf = new WeakReference<>(surfaceView);
        mHolder = mSurfaceViewRf.get().getHolder();
        mHolder.addCallback(this);
    }

    public void takePicture(final String path) {
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                if (mHelperListener != null) {
                    if (data != null) {
                        File file = new File(path);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(file);
                            // Rotate the image
                            // The front camera needs to change the vertical direction, otherwise the photo is upside down
                            int rotation = (mPhoneDegree == 270 ? 0 : mPhoneDegree + 90);
                            if (isFrontCamera) {
                                if (rotation == 90) {
                                    rotation = 270;
                                } else if (rotation == 270) {
                                    rotation = 90;
                                }
                            }
                            Matrix matrix = new Matrix();
                            matrix.setRotate(rotation);
                            Bitmap rotateBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                            rotateBmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            // Return result
                            mHelperListener.OnTakePicture(path, rotateBmp);
                            // Re preview
                            stopPreview();
                            startPreview();
                        } catch (FileNotFoundException e) {
                            Log.e(TAG, "Photo failed: Please make sure the path is correct or storage permission\n");
                            e.printStackTrace();
                        } finally {
                            try {
                                if (fos != null) {
                                    fos.close();
                                }
                                bitmap.recycle();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });
    }

    private void createCamera() {
        try {
            // Initialize resources
            if (mCamera != null) {
                stopPreview();
                destoryCamera();
            }
            if (!isFrontCamera) {
                mCamera = Camera.open();
            } else {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                int numofCameras = Camera.getNumberOfCameras();
                for (int i = 0; i < numofCameras; i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        mCamera = Camera.open(i);
                    }
                }
            }
            setParameters();
        } catch (Exception e) {
            Log.e(TAG, "Create Camera failed：" + e.getMessage());
        }
    }

    private void setParameters() {
        if (mCamera == null) {
            Log.w(TAG, "mCamera=null,Please make sure you have created Camera");
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        // Preview resolution, default 640x480
        parameters.setPreviewSize(width, height);
        // Preview color format, default NV21
        parameters.setPreviewFormat(ImageFormat.NV21);
        // auto focus
        if (CameraUtil.isSupportFocusAuto(parameters)) {
            parameters.setFocusMode(Camera.Parameters.FLASH_MODE_AUTO);
        }
        // Picture format, default JPEG
        parameters.setPictureFormat(ImageFormat.JPEG);
        // Image size, consistent with preview size
        parameters.setPictureSize(width, height);
        // Picture quality, default is best
        parameters.setJpegQuality(100);
        // Image thumbnail quality
        parameters.setJpegThumbnailQuality(100);
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90);
    }

    private void destoryCamera() {
        if (mCamera == null)
            return;
        mCamera.release();
        mCamera = null;
    }

    private void startPreview() {
        if (mHolder != null) {
            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mCamera.startPreview();
        // Turn on preview, auto focus once
        mCamera.autoFocus(null);
        // Register the preview callback interface, the cache size is the number of bytes occupied by one frame of image
        // That is, (width * height * number of bits per pixel) / 8
        int previewFormat = mCamera.getParameters().getPreviewFormat();
        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        int bufferSize = previewSize.width * previewSize.height *
                ImageFormat.getBitsPerPixel(previewFormat) / 8;
        mCamera.addCallbackBuffer(new byte[bufferSize]);
        mCamera.setPreviewCallbackWithBuffer(this);
    }

    private void stopPreview() {
        if (mCamera == null)
            return;
        try {
            mCamera.stopPreview();
            mCamera.setPreviewDisplay(null);
            mCamera.setPreviewCallbackWithBuffer(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Camera focus
    public void cameraFocus() {
        if (mCamera == null || mRecordUtil.isRecording())
            return;
        Camera.Parameters parameter = mCamera.getParameters();
        if (CameraUtil.isSupportFocusAuto(parameter)) {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (mHelperListener != null) {
                        mHelperListener.onCameraFocus(success, camera);
                    }
                }
            });
        }
    }

    // Camera resolution switching
    public void updateResolution(int width, int height) {
        this.width = width;
        this.height = height;
        stopPreview();
        destoryCamera();
        createCamera();
        startPreview();
    }

    public List<Camera.Size> getPreviewSizes() {
        if(mCamera == null)
            return null;
        Camera.Parameters param = mCamera.getParameters();
        if(param != null) {
            return param.getSupportedPreviewSizes();
        }
        return  null;
    }

    public void startRecordMp4(String videoPath) {
        MediaRecordUtil.RecordParams params = new MediaRecordUtil.RecordParams();
        params.setFrontCamera(isFrontCamera);
        params.setPhoneDegree(mPhoneDegree);
        params.setVideoPath(videoPath);
        mRecordUtil.startMediaRecorder(mCamera, mSurfaceViewRf.get().getHolder().getSurface(), params);
    }

    public void stopRecordMp4() {
        mRecordUtil.stopMediaRecorder();
    }

    // Zoom increase，inZoomIn = true
    // Zoom out，inZoomIn = false
    public void setZoom(boolean isZoomIn) {
        if (!isSupportZoom() || isFrontCamera) {
            Log.w("dddd", "(front) camera does not support zoom");
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        int maxZoom = parameters.getMaxZoom();
        int curZoom = parameters.getZoom();

        if (isZoomIn && curZoom < maxZoom) {
            curZoom++;
        } else if (curZoom > 0) {
            curZoom--;
        }
        parameters.setZoom(curZoom);
        mCamera.setParameters(parameters);
        if (mHelperListener != null) {
            mHelperListener.onZoomChanged(maxZoom, curZoom);
        }
    }

    private boolean isSupportZoom() {
        boolean isSupport = false;
        if (mCamera.getParameters().isZoomSupported()) {
            isSupport = true;
        }
        return isSupport;
    }

    private void startOrientationSensor() {
        mOriSensor = SensorOrientation.getInstance(mSurfaceViewRf.get().getContext());
        mOriSensor.startSensorOrientation(new SensorOrientation.OnChangedListener() {
            @Override
            public void onOrientatonChanged(int orientation) {
                // Suppose a range to determine the current direction of the phone
                // mPhoneDegree = 0, normal vertical direction
                // mPhoneDegree = 90, horizontal to the right ...
                int rotate = 0;
                if ((orientation >= 0 && orientation <= 45) || (orientation > 315)) {
                    rotate = 0;
                } else if (orientation > 45 && orientation <= 135) {
                    rotate = 90;
                } else if (orientation > 135 && orientation <= 225) {
                    rotate = 180;
                } else if (orientation > 225 && orientation <= 315) {
                    rotate = 270;
                } else {
                    rotate = 0;
                }
                if (rotate == orientation)
                    return;
                mPhoneDegree = rotate;
                Log.i(TAG, "Phone direction angle\n：" + mPhoneDegree);
            }
        });
        mOriSensor.enable();
    }

    private void stopOrientationSensor() {
        if (mOriSensor != null) {
            mOriSensor.disable();
        }
    }

    private void startAcceleratorSensor() {
        mAccelerSensor = SensorAccelerator.getSensorInstance();
        mAccelerSensor.startSensorAccelerometer(mSurfaceViewRf.get().getContext(), new SensorAccelerator.OnSensorChangedResult() {
            @Override
            public void onMoving(int x, int y, int z) {

            }

            @Override
            public void onStopped() {
                // Start focusing
                cameraFocus();
            }
        });
    }

    private void stopAcceleratorSensor() {
        if (mAccelerSensor != null) {
            mAccelerSensor.stopSensorAccelerometer();
        }
    }
}
