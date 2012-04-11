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

public class SimpleLocationService extends Service {

	
	private static final long MAX_TIME_FOR_GPS_LOCATION = 50000;

	/**
	 * The key to the location we serialize
	 */
	public static final String LOCATION_KEY = "LOCATION_KEY";

	private LocationManager locationManager = null;

	/**
	 * Custom class to handle gps updates
	 */
	private SimpleLocationListener locationListener = null;

	/**
	 * Time at which service was started, used to measure against
	 * MAX_TIME_FOR_GSP_LOCATION
	 */
	private long serviceStartTime = 0;

	public static final String SIMPLE_LOCATION_OBTAINED_ACTION = "edu.cmd.radar.android.report.SIMPLE_LOCATION_OBTAINED";


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT,
				"SimplelocationService started");

		// Set up GPS listener
		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
		locationListener = new SimpleLocationListener();
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				0, 0, locationListener);
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

		// send loc to next service
		b.putSerializable(LOCATION_KEY,
				new SerializableLocation(loc));
		intent.putExtras(b);
		
		intent.setAction(SIMPLE_LOCATION_OBTAINED_ACTION);
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
	 * This class receives location updates
	 */
	public class SimpleLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT, "Location Changed");
			broadcastLocation(location);

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
