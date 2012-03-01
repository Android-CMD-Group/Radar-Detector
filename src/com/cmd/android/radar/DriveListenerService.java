package com.cmd.android.radar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class DriveListenerService extends Service {

	public static final String PREVIOUS_LOCATION_FIX = "PREVIOUS_LOCATION_FIX";
	public static final int LOWEST_SPEED_DISTANCE_THRESHOLD = 2680;
	public static final String TIMER_FOR_LOCATION_SLEEP = "com.cmd.android.radar.TIMER_FOR_LOCATION_SLEEP";
	private int numOfFixes;
	private boolean firstFixStateIndicator;
	private Location previousFix;
	private LocationManager locationManager;
	private LocationListener locationListener;
	private LocationDecider locationDecider;

	@Override
	public IBinder onBind(Intent intent) {
		// No need to use this yet, the service is so far, unbound
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(MainSettingsActivity.LOG_TAG,
				"DriveListenerService onStartCommand Called");
		numOfFixes =0;
		locationDecider = new LocationDecider();
		
		Bundle bundle = intent.getExtras();
		Log.d(MainSettingsActivity.LOG_TAG, "bundle = "
				+ bundle);
		
		if (bundle == null) {
			firstFixStateIndicator = true;
		} else {
			firstFixStateIndicator = false;
			SerializableLocation serialPrevFix = (SerializableLocation) bundle.get(PREVIOUS_LOCATION_FIX);
		Log.d(MainSettingsActivity.LOG_TAG, "serialPrevFix = "
				+ serialPrevFix);
			previousFix = serialPrevFix.returnEquivalentLocation();
		}

		Log.d(MainSettingsActivity.LOG_TAG, "firstLocationFix = "
				+ firstFixStateIndicator);

		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				Log.d(MainSettingsActivity.LOG_TAG,
						"got fix at " + location.getLatitude() + " "
								+ location.getLongitude());
				addLocationToPossible(location);
				numOfFixes++;
				Log.d(MainSettingsActivity.LOG_TAG, "numOfFixes= " + numOfFixes);
				if (numOfFixes >= 1) {
					useBestFix();
				}
			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}
		};

		// Register the listener with the Location Manager to receive location
		// updates
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
		return super.onStartCommand(intent, flags, startId);

	}
	
	protected void addLocationToPossible(Location location){
		locationDecider.addPossibleLocation(location);
	}

	protected void useBestFix() {
		Log.d(MainSettingsActivity.LOG_TAG, "Using best fix");
		locationManager.removeUpdates(locationListener);
		if (firstFixStateIndicator) {
			Intent intentForNextFix = new Intent(
					"com.cmd.android.radar.WifiChangeReceiver");
			intentForNextFix.setAction(TIMER_FOR_LOCATION_SLEEP);
			Bundle extraBundle = new Bundle();
			extraBundle.putSerializable(PREVIOUS_LOCATION_FIX, locationDecider.getBestLocationInSerializableForm());
			intentForNextFix.putExtras(extraBundle);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
					intentForNextFix, 0);

			AlarmManager alarmManager = (AlarmManager) getSystemService(Service.ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC_WAKEUP,
					System.currentTimeMillis() + 60 * 1000, pendingIntent);
			stopSelf();

		} else {
			if (isDriving(previousFix, locationDecider.getBestLocation())) {
				// TODO: Launch shake listener
			}
		}

	}

	@Override
	public void onCreate() {
		Log.d(MainSettingsActivity.LOG_TAG, "DriveListenerService created");
		super.onCreate();
	}

	private boolean isDriving(Location bestLastLocation,
			Location bestNewLocation) {
		// 2680 is 20 miles per hour in meters per
		// minute after 5 minutes
		float distance = bestLastLocation.distanceTo(bestNewLocation);
		float speed = distance
				/ (1000*60)*(bestNewLocation.getTime() - bestLastLocation.getTime());
		if (distance > LOWEST_SPEED_DISTANCE_THRESHOLD) {
			Toast.makeText(this, "You're driving at speed: " + speed,
					Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

	}
}
