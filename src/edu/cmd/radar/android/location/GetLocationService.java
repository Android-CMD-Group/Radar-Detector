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

public class GetLocationService extends Service {

	/**
	 * The suggested distance to wait between gps update bursts
	 */
	private static final int MIN_WAIT_DISTANCE = 1;

	/**
	 * The maximum time to wait for getting speed and bearing
	 */
	private static final long MAX_TIME_FOR_SPEED_AND_BEARING = 50000;

	/**
	 * The suggested time to wait between gps update bursts
	 */
	private static final int MIN_WAIT_TIME = 10000;

	/**
	 * The key to the location we serialize
	 */
	public static final String LOCATION_KEY = "LOCATION_KEY";

	public static final String LOCATION_TYPE_REQUEST = "LOCATION_TYPE_REQUEST";

	// The different types of locations to request
	
	public static final String SPEED_AND_BEARING_LOCATION_TYPE = "SPEED_AND_BEARING_LOCATION_TYPE";

	public static final String SIMPLE_GPS_LOCATION_TYPE = "SIMPLE_GPS_LOCATION_TYPE";

	/**
	 * The location object to send to the server
	 */
	private Location firstLocation = null;

	private LocationManager locationManager = null;

	/**
	 * Custom class to handle gps updates
	 */
	private LocationListener locationListener = null;

	/**
	 * Time at which service was started, used to measure against
	 * MAX_TIME_FOR_SPEED_AND_BEARING
	 */
	private long serviceStartTime = 0;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		Log.d(MainSettingsActivity.LOG_TAG_LOCATION,
				"GetLocationService Started");
	
		if(intent.getExtras().getString(LOCATION_TYPE_REQUEST).equals(SPEED_AND_BEARING_LOCATION_TYPE)){
			Log.d(MainSettingsActivity.LOG_TAG_LOCATION,
					"Getting location with speed and bearing");
			
			// Set up GPS listener
			locationManager = (LocationManager) this
					.getSystemService(Context.LOCATION_SERVICE);

			locationListener = new SpeedAndBearingGPSLocationListener();

			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					MIN_WAIT_TIME, MIN_WAIT_DISTANCE, locationListener);
			
		}else if(intent.getExtras().getString(LOCATION_TYPE_REQUEST).equals(SIMPLE_GPS_LOCATION_TYPE)){
			Log.d(MainSettingsActivity.LOG_TAG_LOCATION,
					"Getting location with simple gps coordinates");


			locationManager = (LocationManager) this
					.getSystemService(Context.LOCATION_SERVICE);

			locationListener = new SimpleGPSLocationListener();

			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					MIN_WAIT_TIME, MIN_WAIT_DISTANCE, locationListener);
		}

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Sends out a broadcast with the location
	 * 
	 * @param loc
	 *            The location info to upload
	 */
	private void broadcastLocation(Location loc, String locationActionString) {

		
		Intent intent = new Intent();
		Bundle b = new Bundle();

		// package up location
		b.putSerializable(GetLocationService.LOCATION_KEY,
				new SerializableLocation(loc));

		intent.putExtras(b);
		intent.setAction(locationActionString);
		sendBroadcast(intent);

		// calls destroy
		stopSelf();

	}
	
	public void broadcastServiceFailed(
			String locationActionString) {
		Intent intent = new Intent();
		intent.setAction(locationActionString);
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


	public class SpeedAndBearingGPSLocationListener implements LocationListener {

		public static final String LOCATION_OBTAINED_WITH_SPEED_AND_BEARING_ACTION = 
				"edu.cmd.radar.android.location.LOCATION_OBTAINED_WITH_SPEED_AND_BEARING_ACTION";
		
		public static final String FAILED_TO_OBTAIN_WITH_SPEED_AND_BEARING_ACTION =
				"edu.cmd.radar.android.location.FAILED_TO_OBTAIN_WITH_SPEED_AND_BEARING_ACTION";

		private double numOfSpeeds = 0;
		private double speedTotal = 0;

		@Override
		public void onLocationChanged(Location location) {
			Log.d(MainSettingsActivity.LOG_TAG_LOCATION,
					"Location Changed, location is now:\n"
							+ location.toString());

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

					// set the speed to the average speed. If this is being
					// inaccurate, change to median
					firstLocation.setSpeed((float) (speedTotal / numOfSpeeds));
				}

			}

			// If the info has a bearing, put it in first location info
			if (location.hasBearing()) {
				firstLocation.setBearing(location.getBearing());
			}

			// Once bearing and speed info is received, send off the location
			// info, and shut down this service
			if (firstLocation.hasSpeed() && firstLocation.hasBearing()) {
				locationManager.removeUpdates(locationListener);
				Log.d(MainSettingsActivity.LOG_TAG_LOCATION,
						"fix has speed and bearing, done");
				broadcastLocation(firstLocation, LOCATION_OBTAINED_WITH_SPEED_AND_BEARING_ACTION);

			}

			// Only try to get the speed and bearing info for
			// MAX_TIME_FOR_SPEED_AND_BEARING seconds, then just send all info
			// received
			if (SystemClock.uptimeMillis() - serviceStartTime > MAX_TIME_FOR_SPEED_AND_BEARING) {
				locationManager.removeUpdates(locationListener);
				Log.d(MainSettingsActivity.LOG_TAG_LOCATION,
						"Time ran out to get speed and bearing, sending back null ");
				broadcastServiceFailed(FAILED_TO_OBTAIN_WITH_SPEED_AND_BEARING_ACTION);
			}

		}

		@Override
		public void onProviderDisabled(String provider) {
			locationManager.removeUpdates(locationListener);
			broadcastServiceFailed(FAILED_TO_OBTAIN_WITH_SPEED_AND_BEARING_ACTION);

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

	public class SimpleGPSLocationListener implements LocationListener {

		public static final String SIMPLE_GPS_LOCATION_OBTAINED_ACTION = 
				"edu.cmd.radar.android.report.SIMPLE_GPS_LOCATION_OBTAINED";
		public static final String FAILED_TO_OBTAIN_SIMPLE_GPS_LOCATION_ACTION = 
				"edu.cmd.radar.android.report.FAILED_TO_OBTAIN_SIMPLE_GPS_LOCATION_ACTION";
		
		@Override
		public void onLocationChanged(Location location) {
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT, "Location Changed");
			locationManager.removeUpdates(locationListener);
			broadcastLocation(location, SIMPLE_GPS_LOCATION_OBTAINED_ACTION);
			

		}

		@Override
		public void onProviderDisabled(String provider) {
			locationManager.removeUpdates(locationListener);
			broadcastServiceFailed(FAILED_TO_OBTAIN_SIMPLE_GPS_LOCATION_ACTION);

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
