package com.cmd.android.radar;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

public class ShakeListenerService extends Service {
	
	private final String LOG_TAG = "ShakeListenerService";		// Used for LogCat
	
//	private static final float ACCEL_THRESHOLD = 5;
//	private float mAccel;			// Acceleration apart form gravity
//	private float mAccelCurrent;	// Current acceleration including gravity
//	private float mAccelLast;		// Last acceleration including gravity
//	private SensorManager mSensorManager;
//	
//	private final SensorEventListener mSensorListener = new SensorEventListener() {
//		
//		@Override
//		public void onAccuracyChanged(Sensor s, int accuracy) {
//			
//		}
//
//		@Override
//		public void onSensorChanged(SensorEvent sEvent) {
//			// values[] measured in SI units (m/s^2)
//			// depend on which sensor type is being monitored (Sensor.TYPE_ACCELEROMETER)
//			float x = sEvent.values[0];		// Acceleration - Gx on x-axis
//			float y = sEvent.values[1];		// Acceleration - Gy on y-axis
//			float z = sEvent.values[2];		// Acceleration - Gz on z-axis
//			mAccelLast = mAccelCurrent;
//			mAccelCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));
//			float delta = mAccelCurrent - mAccelLast;
//			mAccel = mAccel * 0.9f + delta; // Perform low-cut filter
//			
//			if (Math.abs(mAccel) > ACCEL_THRESHOLD)
//				Log.d(LOG_TAG, "Shake Detected! mAccel = " + mAccel);
//		}
//		
//		
//		protected void onResume() {
//			mSensorManager.registerListener(
//				mSensorListener, 
//				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
//				SensorManager.SENSOR_DELAY_NORMAL 
//			);
//		}
//		
//	};
	
	
	
	
	
	
/************************************************************************************/
	
	// Do not need at the moment
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
    /**
     * Called when the service starts.
     */
    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
//	   	mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//    	mSensorManager.registerListener(
//				mSensorListener, 
//				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
//				SensorManager.SENSOR_DELAY_FASTEST
//			);
//    	mAccel = 0.00f;
//    	mAccelCurrent = SensorManager.GRAVITY_EARTH;
//    	mAccelLast = SensorManager.GRAVITY_EARTH;
    	
		Log.d(this.LOG_TAG, "ShakeListenerService launched.");
		return START_STICKY;
	}
    
    public void onDestroy() {
    	
    	//Log.d(this.LOG_TAG, "mSensorManager == null? " + (mSensorManager == null));
//    	mSensorManager.unregisterListener(mSensorListener);
		Log.d(this.LOG_TAG, "ShakeListenerService killed.");
    }
}
