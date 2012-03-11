package com.cmd.android.radar;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

public class Shaker {
	private SensorManager mSensorManager = null;
	private long lastShakeTimestamp = 0;
	private double threshold = 1.0d;
	private long gap = 0;
	private Shaker.Callback cb = null;
	
	public Shaker(Context ctxt, double threshold, long gap, Shaker.Callback cb) {
		this.threshold = Math.pow(threshold, 2);
		this.threshold = this.threshold * SensorManager.GRAVITY_EARTH
				* SensorManager.GRAVITY_EARTH;
		this.gap = gap;
		this.cb = cb;

		mSensorManager = (SensorManager) ctxt
				.getSystemService(Context.SENSOR_SERVICE);
		mSensorManager.registerListener(listener,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_UI);

	}
	private SensorEventListener listener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				double netForce = Math.pow(event.values[0], 2);
				netForce += Math.pow(event.values[1], 2);
				netForce += Math.pow(event.values[2], 2);

				if (netForce > threshold) {
					Log.d(ShakeListenerService.LOG_TAG, "netforce = " + netForce);
					isShaking();
				} else {
					isNotShaking();
				}
			}
		}

	};

	private void isShaking() {
		long now = SystemClock.uptimeMillis();

		if (lastShakeTimestamp == 0) {
			lastShakeTimestamp = now;

			if (cb != null) {
				cb.shakingStarted();
			}
		} else {
			lastShakeTimestamp = now;
		}
	}

	private void isNotShaking() {
		long now = SystemClock.uptimeMillis();

		if (lastShakeTimestamp > 0) {
			if (now - lastShakeTimestamp > gap) {
				lastShakeTimestamp = 0;

				if (cb != null) {
					cb.shakingStopped();
				}
			}
		}
	}

	public void close() {
		mSensorManager.unregisterListener(listener);
	}

	public interface Callback {
		void shakingStarted();

		void shakingStopped();
	}



}
