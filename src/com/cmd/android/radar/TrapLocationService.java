package com.cmd.android.radar;

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
import edu.cmd.radar.android.location.SerializableLocation;
import edu.cmd.radar.android.shake.ShakeListenerService;
import edu.cmd.radar.android.ui.MainSettingsActivity;

public class TrapLocationService extends Service {

	private static final long MAX_TIME_FOR_SPEED_AND_BEARING = 35000;
	public static boolean isRunning = false;
	private Location firstLocation = null;
	private LocationManager locationManager = null;
	private GpsListener locationListener = null;
	private long serviceStartTime = 0;
	private Intent orginalIntent;
	public static final String LOCATION_KEY = "LOCATION_KEY";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		serviceStartTime = SystemClock.uptimeMillis();
		TrapLocationService.isRunning = true;
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT,
				"TrapLocationService started");
		orginalIntent = intent;
		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
		locationListener = new GpsListener();
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5,
				0, locationListener);
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	private void startUploadService(Location loc) {
		locationManager.removeUpdates(locationListener);
		Intent serviceIntent = new Intent(this,
				TrapReportUploadingService.class);
		Bundle b = new Bundle();
		
		b.putSerializable(TrapLocationService.LOCATION_KEY,
				new SerializableLocation(loc));
		
		b.putLong(ShakeListenerService.TIME_REPORTED_PREF_KEY, orginalIntent
				.getLongExtra(ShakeListenerService.TIME_REPORTED_PREF_KEY, -1));
		
		serviceIntent.putExtras(b);
		this.startService(serviceIntent);
		stopSelf();

	}

	@Override
	public void onDestroy() {
		locationManager.removeUpdates(locationListener);
		TrapLocationService.isRunning = false;
		super.onDestroy();
	}

	public class GpsListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT, "Location Changed");
			if (firstLocation == null) {
				firstLocation = location;
			}

			if (location.hasSpeed()) {
				Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT,
						"fix has speed: " + location.getSpeed());
				firstLocation.setSpeed(location.getSpeed());
			}

			if (location.hasBearing()) {
				Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT,
						"fix has bearing: " + location.getBearing());
				firstLocation.setBearing(location.getBearing());
			}

			if (location.hasSpeed() && location.hasBearing()) {
				Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT,
						"fix has speed and bearing");
				startUploadService(firstLocation);

			}

			if (SystemClock.uptimeMillis() - serviceStartTime > MAX_TIME_FOR_SPEED_AND_BEARING) {
				Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT,
						"Time ran out to get speed and bearing");
				startUploadService(firstLocation);
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
