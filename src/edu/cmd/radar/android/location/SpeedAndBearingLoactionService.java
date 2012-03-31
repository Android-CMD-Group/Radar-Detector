package edu.cmd.radar.android.location;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import edu.cmd.radar.android.report.TrapReportUploadingService;
import edu.cmd.radar.android.shake.ShakeListenerService;
import edu.cmd.radar.android.ui.MainSettingsActivity;

public class SpeedAndBearingLoactionService extends Service {

	/**
	 * The suggested distance to wait between gps update bursts
	 */
	private static final int SUGGESTED_MIN_DISTANCE = 1;

	/**
	 * The maximum time to wait for getting speed and bearing
	 */
	private static final long MAX_TIME_FOR_SPEED_AND_BEARING = 50000;

	/**
	 * The suggested time to wait between gps update bursts
	 */
	private static final int SUGGESTED_MIN_TIME = 10000;

	/**
	 * The key to the location we serialize
	 */
	public static final String LOCATION_KEY = "LOCATION_KEY";

	/**
	 * The location object to send to the server
	 */
	private Location firstLocation = null;

	private LocationManager locationManager = null;

	/**
	 * Custom class to handle gps updates
	 */
	private GpsListener locationListener = null;

	/**
	 * Time at which service was started, used to measure against
	 * MAX_TIME_FOR_SPEED_AND_BEARING
	 */
	private long serviceStartTime = 0;

	public static final String SPEED_AND_BEARING_LOCATION_OBTAINED_ACTION = "edu.cmd.radar.android.location.SPEED_AND BEARING_LOCATION_OBTAINED";


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Log.d(MainSettingsActivity.LOG_TAG_LOCATION,
				"SpeedAndBEaringloactionService started");

		// Set up GPS listener
		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
		locationListener = new GpsListener();
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				SUGGESTED_MIN_TIME, SUGGESTED_MIN_DISTANCE, locationListener);
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Starts new service to upload the loc and then stops self
	 * @param loc The location info to upload
	 */
	private void broadcastLocation(Location loc) {
		
		// Stop getting updates
		locationManager.removeUpdates(locationListener);
		Intent intent = new Intent();
		Bundle b = new Bundle();
		
		Log.d(MainSettingsActivity.LOG_TAG_LOCATION,
				"Speed: "+ loc.getSpeed());
		
		// send loc to next service
		b.putSerializable(SpeedAndBearingLoactionService.LOCATION_KEY,
				new SerializableLocation(loc));
		intent.putExtras(b);
		
		intent.setAction(SpeedAndBearingLoactionService.SPEED_AND_BEARING_LOCATION_OBTAINED_ACTION);
		sendBroadcast(intent);

		
		// calls destroy
		stopSelf();

	}

	@Override
	public void onDestroy() {

		// release recourses
		locationManager.removeUpdates(locationListener);
		super.onDestroy();
	}

	/**
	 * This class receives gps updates
	 */
	public class GpsListener implements LocationListener {

		private double numOfSpeeds = 0;
		private double speedTotal = 0;

		@Override
		public void onLocationChanged(Location location) {
			Log.d(MainSettingsActivity.LOG_TAG_LOCATION, "Location Changed");

			// record the first location because it has the most accurate
			// coordinates
			if (firstLocation == null) {
				serviceStartTime = SystemClock.uptimeMillis();
				firstLocation = location;
			}

			// use any speed info received
			if (location.hasSpeed()) {

				// 0.0 means speed dne. Updates the total and the count
				if (location.getSpeed() != 0.0) {
					numOfSpeeds++;
					speedTotal += location.getSpeed();
				}

				Log.d(MainSettingsActivity.LOG_TAG_LOCATION,
						"fix has speed: " + location.getSpeed());

				// set the speed to the average speed. If this is being
				// inaccurate, change to median
				firstLocation.setSpeed((float) (speedTotal / numOfSpeeds));
			}

			// If the info has a bearing, put it in first location info
			if (location.hasBearing()) {
				Log.d(MainSettingsActivity.LOG_TAG_LOCATION,
						"fix has bearing: " + location.getBearing());
				firstLocation.setBearing(location.getBearing());
			}

			// Once bearing and speed info is received, send off the location
			// info, and shut down this service
			if (firstLocation.hasSpeed() && firstLocation.hasBearing()) {
				Log.d(MainSettingsActivity.LOG_TAG_LOCATION,
						"fix has speed and bearing");
				broadcastLocation(firstLocation);

			}

			// Only try to get the speed and bearing info for
			// MAX_TIME_FOR_SPEED_AND_BEARING seconds, then just send all info
			// received
			if (SystemClock.uptimeMillis() - serviceStartTime > MAX_TIME_FOR_SPEED_AND_BEARING) {
				Log.d(MainSettingsActivity.LOG_TAG_LOCATION,
						"Time ran out to get speed and bearing");
				broadcastLocation(firstLocation);
			}

		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}

	}

}
