package com.example.ashok.ashok_arun_camera_zoom;

import android.hardware.Camera;

import java.util.List;

public class CameraUtil {
    public static  boolean isSupportFocusAuto(Camera.Parameters p) {
        boolean isSupport = false;
        List<String> modes = p.getSupportedFocusModes();
        for (String mode : modes) {
            if(mode.equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
                isSupport = true;
                break;
            }
        }
        return isSupport;
    }
}
