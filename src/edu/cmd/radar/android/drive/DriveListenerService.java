package edu.cmd.radar.android.drive;

import edu.cmd.radar.android.location.LocationDecider;
import edu.cmd.radar.android.location.SerializableLocation;
import edu.cmd.radar.android.ui.MainSettingsActivity;
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

	public static int numOfServicesLaunched = 0;

	public static boolean isRunning = false;

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
	private static final int TIME_TO_SLEEP_IN_MINUTES = 5;

	/**
	 * The minimum speed considered 'driving'
	 */
	private static final double MIN_SPEED_THRESHOLD_MILE_PER_HOUR = 25;

	// edit this to change accuacy. untested so far

	/**
	 * How many tfixes to get before picking the best one.
	 */
	private static final int NUM_OF_FIXES_TO_GET = 1;

	// Don't edit these constants to improve performance or for testing.

	private static final int MINUTE_IN_MILLIS = 1000 * 60;
	private static final double METERS_PER_MILE = 1609.344;

	private static final double MINUTES_PER_HOUR = 60;
	private static final double MIN_SPEED_THRESHOLD_METERS_PER_MINUTE = (MIN_SPEED_THRESHOLD_MILE_PER_HOUR * METERS_PER_MILE)
			/ MINUTES_PER_HOUR;

	/**
	 * Key to grab serializable location from bundle in intent
	 */
	public static final String PREVIOUS_LOCATION_FIX = "PREVIOUS_LOCATION_FIX";

	/**
	 * The unique action id for the intent that launches the WifiChangeReceiver
	 * after the timer expires
	 */
	public static final String TIMER_FOR_LOCATION_SLEEP = "com.cmd.android.radar.TIMER_FOR_LOCATION_SLEEP";

	private static final String UNIQUE_ID_KEY = "UNIQUE_ID_KEY";

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

	private int uniqueID;

	@Override
	public IBinder onBind(Intent intent) {
		// No need to use this yet, the service is so far unbound
		return null;
	}

	@Override
	public void onCreate() {
		DriveListenerService.isRunning = true;
		super.onCreate();
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
			uniqueID = DriveListenerService.numOfServicesLaunched;
			DriveListenerService.numOfServicesLaunched++;
		} else {
			uniqueID = bundle.getInt(UNIQUE_ID_KEY);
			firstFixStateIndicator = false;
			SerializableLocation serialPrevFix = (SerializableLocation) bundle
					.get(PREVIOUS_LOCATION_FIX);
			previousFix = serialPrevFix.returnEquivalentLocation();
		}
		Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING,
				"Launched service with" + " id: " + uniqueID);

		Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING,
				"We have no prev fix = " + firstFixStateIndicator + " id: "
						+ uniqueID);

		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		locationListener = new LocationListener() {

			// Called when location CHANGES. This can be a problem if the
			// location is not changing...
			public void onLocationChanged(Location location) {

				Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING,
						"got fix at " + location.getLatitude() + " "
								+ location.getLongitude() + " with acc: "+location.getAccuracy()+" id: " + uniqueID);

				// Add the fix to the list
				addLocationToPossible(location);
				numOfFixes++;

				Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING,
						"numOfFixes= " + numOfFixes + " id: " + uniqueID);

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
			extraBundle.putInt(UNIQUE_ID_KEY, uniqueID);

			intentForNextFix.putExtras(extraBundle);

			PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
					intentForNextFix, 0);

			// Set the broadcast alarm for a specified time and stop the service
			AlarmManager alarmManager = (AlarmManager) getSystemService(Service.ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC_WAKEUP,
					System.currentTimeMillis() + TIME_TO_SLEEP_IN_MINUTES
							* MINUTE_IN_MILLIS, pendingIntent);
			Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING,
					"Now sleeping service with" + " id: " + uniqueID);
			stopSelf();

		} else {
			if (isDriving(previousFix, locationDecider.getBestLocation())) {
				stopSelf();
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
		double distanceinMPH = distance * .000621371192;
		long sleepTime = (bestNewLocation.getTime() - bestLastLocation
				.getTime());
		float speed = distance / MINUTE_IN_MILLIS * sleepTime;
		double speedInMPH = speed * .000621371192 * 60;
		double drivingDistanceThreshold = MIN_SPEED_THRESHOLD_METERS_PER_MINUTE
				* sleepTime / MINUTE_IN_MILLIS;

		Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING,
				"Prev fix: Lat = " + bestLastLocation.getLatitude()
						+ " Long = " + bestLastLocation.getLongitude()
						+ " Acc = " + bestLastLocation.getAccuracy() + " id: "
						+ uniqueID);

		Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING,
				"Curr fix: Lat = " + bestNewLocation.getLatitude() + " Long = "
						+ bestNewLocation.getLongitude() + " Acc = "
						+ bestNewLocation.getAccuracy() + " id: " + uniqueID);

		Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING,
				"MIN_SPEED_THRESHOLD_METERS_PER_MINUTE: "
						+ MIN_SPEED_THRESHOLD_METERS_PER_MINUTE + " id: "
						+ uniqueID);
		Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING,
				"MIN_THRESHOLD_DISTANCE_FOR_DRIVING: "
						+ drivingDistanceThreshold + " id: " + uniqueID);

		Toast.makeText(this, "S: " + speedInMPH + " D: " + distanceinMPH,
				Toast.LENGTH_LONG).show();

		Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING, "You traveled :"
				+ distanceinMPH + " Miles" + " id: " + uniqueID);
		Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING, "Speed:"
				+ speedInMPH + " Miles per hour" + " id: " + uniqueID);

		Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING, "You traveled :"
				+ distance + " Meters" + " id: " + uniqueID);
		Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING, "Speed:" + speed
				+ "Meters per minute" + " id: " + uniqueID);

		if (speed > MIN_SPEED_THRESHOLD_METERS_PER_MINUTE) {
			Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING, "Driving"
					+ " id: " + uniqueID);
			return true;
		}
		return false;
	}

	@Override
	public void onDestroy() {
		DriveListenerService.isRunning = false;
		// If killed, release resources
		locationManager.removeUpdates(locationListener);
		super.onDestroy();

	}
}
