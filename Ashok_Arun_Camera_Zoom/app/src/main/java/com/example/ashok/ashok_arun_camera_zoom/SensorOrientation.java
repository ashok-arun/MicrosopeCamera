package com.example.ashok.ashok_arun_camera_zoom;

import android.content.Context;
import android.view.OrientationEventListener;

public class SensorOrientation extends OrientationEventListener {
	private static SensorOrientation mOrientation;
	private OnChangedListener listener;

	private SensorOrientation(Context context) {
		super(context);
	}

	public static SensorOrientation getInstance(Context context) {
		if(mOrientation == null) {
			mOrientation = new SensorOrientation(context);
		}
		return mOrientation;
	}

	public  void startSensorOrientation(OnChangedListener listener){
		this.listener = listener;

		if(mOrientation != null) {
			mOrientation.enable();
		}
	}
	
	public void stopSensorOrientation(){
		if(mOrientation != null) {
			mOrientation.disable();
		}
	}

	@Override
	public void onOrientationChanged(int orientation) {
		if(listener != null) {
			listener.onOrientatonChanged(orientation);
		}
	}

	public interface OnChangedListener {
		void onOrientatonChanged(int orientation);
	}
}
