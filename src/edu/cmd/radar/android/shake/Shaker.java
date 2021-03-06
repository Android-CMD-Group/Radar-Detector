package edu.cmd.radar.android.shake;

import edu.cmd.radar.android.ui.MainSettingsActivity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

public class Shaker {
	private SensorManager sensorMgr = null; // Used for accessing Accelerometer
	private double threshold = 3.25d; // Threshold for detecting shakes
	private long gap = 1000; // Minimum time (ms) between shakes
	private long confirmDelay = 750; // Delay time to wait when user starts
										// shaking, to be sure the shake is on
										// purpose
	private long delayTimeStamp = 0; // time stamp used for delay
	private long lastShakeTimestamp = 0;
	private Shaker.Callback cb = null;

	/**
	 * Shake Listener Calculates the net force given the acceleration from the
	 * x, y, and z axis if (net force > THRESHOLD) the device is shaking
	 */
	private SensorEventListener shakeListener = new SensorEventListener() {
		/**
		 * Called when there is a sensor change
		 */
		@Override
		public void onSensorChanged(SensorEvent event) {

			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				double netForce = Math.pow(event.values[0], 2);
				netForce += Math.pow(event.values[1], 2);
				netForce += Math.pow(event.values[2], 2);

				if (netForce > threshold) {
					Log.d(MainSettingsActivity.LOG_TAG_SHAKE_LISTENER,
							"above threshold");
					if (delayTimeStamp != 0) {
						// user has started shaking, see if they have been
						// shaking for the right amount of time
						Log.d(MainSettingsActivity.LOG_TAG_SHAKE_LISTENER,
								"curr - delay = "
										+ (SystemClock.uptimeMillis() - delayTimeStamp));
						if (SystemClock.uptimeMillis() - delayTimeStamp > confirmDelay) {
							isShaking();
						}
					} else {
						// the user has just started shaking, record the time
						delayTimeStamp = SystemClock.uptimeMillis();
					}

				} else {
					isNotShaking();
				}

			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	};

	/**
	 * Shaker class.
	 */
	public Shaker(Context ctxt, Shaker.Callback cb) {
		this.threshold = Math.pow(threshold, 2);
		this.threshold = this.threshold * SensorManager.GRAVITY_EARTH
				* SensorManager.GRAVITY_EARTH;
		this.cb = cb;

		/**
		 * Build the SensorManager and register a SesnorEventListener
		 */
		this.sensorMgr = (SensorManager) ctxt
				.getSystemService(Context.SENSOR_SERVICE);
		Sensor sensor = this.sensorMgr
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		int sensorDelay = SensorManager.SENSOR_DELAY_UI;
		if (sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			this.sensorMgr.registerListener(shakeListener, sensor, sensorDelay);
	}

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
					// reset to zero for next shake
					delayTimeStamp = 0;
					cb.shakingStopped();
				}

			}
		}
	}

	/**
	 * Used to unregister the shakeListener. IMPORTANT: a listener must be
	 * unregistered or it consumes battery
	 */
	public void close() {
		sensorMgr.unregisterListener(shakeListener);
	}

	public interface Callback {
		void shakingStarted();

		void shakingStopped();
	}
}