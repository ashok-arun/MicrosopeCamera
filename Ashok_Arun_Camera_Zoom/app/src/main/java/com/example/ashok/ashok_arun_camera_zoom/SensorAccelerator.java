package com.example.ashok.ashok_arun_camera_zoom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.Calendar;

public class SensorAccelerator implements SensorEventListener {
	private int status = -1; 
	private static final int STATUS_MOVING = 1;
	private static final int STATUS_STOP = 2;
	private static final String TAG = "SensorAccelerator";
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private OnSensorChangedResult reslistener;
	private static SensorAccelerator sensorMeter;
	private static long STATIC_DELAY_TIME = 1000;
	private long laststamp; 
	private int lastX;
	private int lastY;
	private int lastZ;
	//Autofocus mark to prevent continuous focus
	private boolean isFocused = false;
	
	//External callback result interface
	public interface OnSensorChangedResult{
		void onMoving(int x, int y, int z);
		void onStopped();
	}

	private SensorAccelerator(){}
	
	public static SensorAccelerator getSensorInstance(){
		if(sensorMeter == null){
			sensorMeter = new SensorAccelerator();
		}
		return sensorMeter;
	}
	
	public  void startSensorAccelerometer(Context mContext, OnSensorChangedResult reslistener){
		//Register sports event result listener
		this.reslistener = reslistener;
		//Initialize sensor
		mSensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
		//Start acceleration sensor
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		lastX = 0;
		lastY = 0;
		lastZ = 0;
		Log.i(TAG, "Start acceleration sensor\n");
	}
	
	public void stopSensorAccelerometer(){
		if(mSensorManager == null){
			return;
		}
		//Stop acceleration sensor
		mSensorManager.unregisterListener(this, mAccelerometer);
		Log.i(TAG, "Stop acceleration sensor\n");
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onSensorChanged(SensorEvent event) {
		if(reslistener == null || event.sensor == null){
			return;
		}
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
			//Get the current motion coordinate value, timestamp
			int x = (int)event.values[0];
			int y = (int)event.values[1];
			int z = (int)event.values[2];
			long stamp = Calendar.getInstance().getTimeInMillis();
			//Calculate the acceleration based on the coordinate change value
			int px = Math.abs(lastX-x);
			int py = Math.abs(lastY-y);
			int pz = Math.abs(lastZ-z);
			double accelerometer = Math.sqrt(px*px+py*py+pz*pz);
			if(accelerometer > 1.4){
				isFocused = false;
				reslistener.onMoving(x,y,z);
				status = STATUS_MOVING;
			}else{
				//Record the start and end time of the static, if the rest time exceeds 800ms, call back onStopped to achieve focus
				if(status == STATUS_MOVING){
					laststamp = stamp;
				}				
				if((stamp - laststamp> STATIC_DELAY_TIME) && !isFocused){
					isFocused  = true;
					reslistener.onStopped();
				}
				status = STATUS_STOP;
			}
			//Cache current coordinates for next calculation
			lastX = x;
			lastY = y;
			lastZ = z;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}
}
