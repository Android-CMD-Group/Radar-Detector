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

/**
 * This Service is activated via WifiChangeReceiver and grabs the user location.
 * Then if it just got the users location for the first time, it sleeps for a
 * specified amount of time then wakes up again and grabs user location. It
 * compares to two to see if the user is driving.
 * 
 * @author satshabad
 * 
 */
/**
 * @author satshabad
 * 
 */
public class DriveListenerService extends Service {

	// Only edit these two constants to adjust the threshold of speed and time
	// for sleeping.

	// Example: if the user has traveled at MIN_SPEED_THRESHOLD_MILE_PER_HOUR
	// after the service slept, he is driving.
	// Note: TIME_TO_SLEEP_IN_MINUTES is independent of
	// MIN_SPEED_THRESHOLD_MILE_PER_HOUR. You can change one without changing
	// the other.

	/**
	 * How long to sleep the service in minutes between location grabs
	 */
	private static final int TIME_TO_SLEEP_IN_MINUTES = 1;

	/**
	 * The minimum speed considered 'driving'
	 */
	private static final double MIN_SPEED_THRESHOLD_MILE_PER_HOUR = 20;

	// edit this to change accuacy. untested so far

	/**
	 * How many tfixes to get before picking the best one.
	 */
	private static final int NUM_OF_FIXES_TO_GET = 1;

	// Don't edit these constants to improve performance or for testing.

	private static final int MINUTE_IN_MILLIS = 1000 * 60;
	private static final double METERS_PER_MILE = 1609.344;

	private static final double MINUTES_PER_HOUR = 60;
	private static final double MIN_SPEED_THRESHOLD_METERS_PER_MINUTE = (MIN_SPEED_THRESHOLD_MILE_PER_HOUR * METERS_PER_MILE)/MINUTES_PER_HOUR;
	private static final double MIN_THRESHOLD_DISTANCE_FOR_DRIVING = MIN_SPEED_THRESHOLD_METERS_PER_MINUTE*TIME_TO_SLEEP_IN_MINUTES;

	/**
	 * Key to grab serializable location from bundle in intent
	 */
	public static final String PREVIOUS_LOCATION_FIX = "PREVIOUS_LOCATION_FIX";

	/**
	 * The unique action id for the intent that launches the WifiChangeReceiver
	 * after the timer expires
	 */
	public static final String TIMER_FOR_LOCATION_SLEEP = "com.cmd.android.radar.TIMER_FOR_LOCATION_SLEEP";

	/**
	 * Number of fixes received.
	 */
	private int numOfFixes;

	/**
	 * True if this is the first time the service has been launched, or false if
	 * we already have a location to comapre with
	 */
	private boolean firstFixStateIndicator;

	/**
	 * The location of the last fix
	 */
	private Location previousFix;

	/**
	 * Manages our location stuff
	 */
	private LocationManager locationManager;

	/**
	 * Use this to register for location updates
	 */
	private LocationListener locationListener;

	/**
	 * This object helps decide what coordinates to use when there is more than
	 * one fix
	 */
	private LocationDecider locationDecider;

	@Override
	public IBinder onBind(Intent intent) {
		// No need to use this yet, the service is so far unbound
		return null;
	}

	// called when the service starts
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// init variables
		numOfFixes = 0;
		locationDecider = new LocationDecider();

		Bundle bundle = intent.getExtras();

		if (bundle == null) {
			firstFixStateIndicator = true;
		} else {
			firstFixStateIndicator = false;
			SerializableLocation serialPrevFix = (SerializableLocation) bundle
					.get(PREVIOUS_LOCATION_FIX);
			previousFix = serialPrevFix.returnEquivalentLocation();
		}

		Log.d(MainSettingsActivity.LOG_TAG,
				"We have no prev fix = " + firstFixStateIndicator + " in "
						+ DriveListenerService.class.getName());

		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		locationListener = new LocationListener() {

			// Called when location CHANGES. This can be a problem if the
			// location is not changing...
			public void onLocationChanged(Location location) {

				Log.d(MainSettingsActivity.LOG_TAG,
						"got fix at " + location.getLatitude() + " "
								+ location.getLongitude());

				// Add the fix to the list
				addLocationToPossible(location);
				numOfFixes++;

				Log.d(MainSettingsActivity.LOG_TAG, "numOfFixes= " + numOfFixes);

				if (numOfFixes >= NUM_OF_FIXES_TO_GET) {
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

	/**
	 * Adds the location to list of possible locations in locationDecider
	 * 
	 * @param location
	 *            location to add
	 */
	protected void addLocationToPossible(Location location) {
		locationDecider.addPossibleLocation(location);
	}

	/**
	 * Gets the best fix from the locationDecider and either sends the service
	 * to sleep if it's the first launch, or checks for driving and takes action
	 */
	protected void useBestFix() {

		// Now all the locations are in so, unregister for updates.
		locationManager.removeUpdates(locationListener);

		// If it's only the first time, pack up the location and sleep
		if (firstFixStateIndicator) {
			Intent intentForNextFix = new Intent(
					"com.cmd.android.radar.WifiChangeReceiver");

			intentForNextFix.setAction(TIMER_FOR_LOCATION_SLEEP);

			Bundle extraBundle = new Bundle();
			extraBundle.putSerializable(PREVIOUS_LOCATION_FIX,
					locationDecider.getBestLocationInSerializableForm());

			intentForNextFix.putExtras(extraBundle);

			PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
					intentForNextFix, 0);

			// Set the broadcast alarm for a specified time and stop the service
			AlarmManager alarmManager = (AlarmManager) getSystemService(Service.ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC_WAKEUP,
					System.currentTimeMillis() + TIME_TO_SLEEP_IN_MINUTES*MINUTE_IN_MILLIS,
					pendingIntent);

			stopSelf();

		} else {
			if (isDriving(previousFix, locationDecider.getBestLocation())) {
				// TODO: Launch shake listener
			}
		}
	}

	/**
	 * Given the two points and the time slept, determine whether the user was
	 * driving or not.
	 * 
	 * @param bestLastLocation
	 *            the previous fix
	 * @param bestNewLocation
	 *            the new fix
	 * @return true if driving, false if not
	 */
	private boolean isDriving(Location bestLastLocation,
			Location bestNewLocation) {
		float distance = bestLastLocation.distanceTo(bestNewLocation);

		float speed = distance / MINUTE_IN_MILLIS
				* (bestNewLocation.getTime() - bestLastLocation.getTime());

		Log.d(MainSettingsActivity.LOG_TAG, "You traveled :" + distance + " Meters");
		Log.d(MainSettingsActivity.LOG_TAG, "Speed:" + speed + "Meters per minute");
		Log.d(MainSettingsActivity.LOG_TAG, "MIN_SPEED_THRESHOLD_METERS_PER_MINUTE: " +  MIN_SPEED_THRESHOLD_METERS_PER_MINUTE);
		Log.d(MainSettingsActivity.LOG_TAG, "MIN_THRESHOLD_DISTANCE_FOR_DRIVING: " +   MIN_THRESHOLD_DISTANCE_FOR_DRIVING);
		
		if (speed > MIN_SPEED_THRESHOLD_METERS_PER_MINUTE) {
			Log.d(MainSettingsActivity.LOG_TAG, "Driving");
			return true;
		}
		return false;
	}

	@Override
	public void onDestroy() {
		// If killed, release resources
		locationManager.removeUpdates(locationListener);
		super.onDestroy();

	}
}
