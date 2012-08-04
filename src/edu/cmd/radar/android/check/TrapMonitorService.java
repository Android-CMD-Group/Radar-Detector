package edu.cmd.radar.android.check;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import edu.cmd.radar.android.location.GetLocationService;
import edu.cmd.radar.android.location.LocationRequestReceiver;
import edu.cmd.radar.android.location.SerializableLocation;
import edu.cmd.radar.android.ui.MainSettingsActivity;

/**
 * This service will wake up intermediately to check how close the user is from
 * traps. If the user is sufficiently close an alert will be raised for the
 * user. This service also starts other services to get users locations and a
 * list of nearby traps. It receives this information in broadcasts through
 * registered receivers.
 * 
 * THIS CLASS ASSUMES GPS IS ACTIVATED
 * 
 * @author satshabad
 * 
 */
public class TrapMonitorService extends Service {

	/**
	 * The key to get the last known location from an intent
	 */
	public static final String LAST_LOCATION_KEY = "LOCATION_KEY";

	/**
	 * The ratio of the distance to the closest trap to wait before waking up.
	 * i.e if the trap is 10 miles away, wake up in 6.6 miles
	 */
	public static final float RATIO_OF_DISTANCE_TO_WAIT = 0.666f;

	/**
	 * The key to get the trapLocations from an intent
	 */
	public static final String TRAP_LOCATION_INFO_KEY = "TRAP_LOCATION_INFO_KEY";

	/**
	 * The max distance to be from the furthest away trap before getting enw
	 * info. This is 5 miles in meters
	 */
	private static final float DISTANCE_TO_FURTHEST_TRAP_UPDATE_THRESHOLD = 8046.72f;

	/**
	 * Double the range of degrees from the current bearing for which traps will
	 * still be considered valid
	 */
	private static final double PAST_POINT_VALID_BEARING_RANGE = 120;

	private static final String TRAP_CALCULATIONS_FAILED_ACTION = "TRAP_CALCULATIONS_FAILED_ACTION";

	private static final String TRAP_INFO_OUT_OF_DATE_ACTION = "edu.cmd.radar.android.check.TRAP_INFO_OUT_OF_DATE_ACTION";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
				"TrapMonitorService starting");

		// Get the trap info and last known location from files

		TrapLocations trapLocations = null;
		SerializableLocation lastKnownLocation = null;
		try {
			FileInputStream fis = this
					.openFileInput(GetTrapsService.TRAP_LOCATIONS_FILE_NAME);
			ObjectInputStream is = new ObjectInputStream(fis);
			trapLocations = (TrapLocations) is.readObject();
			is.close();

			Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
					"\t\t\tThere is prior info on some traps\n"
							+ trapLocations.toString());

			FileInputStream fis2 = this
					.openFileInput(GetTrapsService.LAST_KNOWN_LOCATION_FILE_NAME);
			ObjectInputStream is2 = new ObjectInputStream(fis2);
			lastKnownLocation = (SerializableLocation) is.readObject();
			is2.close();

			Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
					"\t\t\tThe last known location is:\n" + lastKnownLocation);

		} catch (StreamCorruptedException e) {
			serviceFailed();
			e.printStackTrace();
		} catch (OptionalDataException e) {
			serviceFailed();
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			serviceFailed();
			e.printStackTrace();
		} catch (IOException e) {
			serviceFailed();
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			serviceFailed();
			e.printStackTrace();
		}

		SerializableLocation currentLocation = (SerializableLocation) intent
				.getExtras().getSerializable(GetLocationService.LOCATION_KEY);
		
		// This means that the most current location was not passed here.
		// Probably because the info is fresh
		if(currentLocation == null){
			currentLocation = lastKnownLocation;
		}

		// remove any traps that are not in our current direction of travel
		// (within some threshold)
		removePastTraps(currentLocation, lastKnownLocation, trapLocations);

		// calculate the distance to each speed trap and store that in the
		// trapLocations object
		setUpdatedDistances(currentLocation, lastKnownLocation, trapLocations);

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
				"\t\t\tAfter distance calc, trapLocations: \n" + trapLocations);

		// Check to that the traps in trapLocations are current. If not, get new
		// ones.
		if (infoIsOutOfDate(currentLocation, lastKnownLocation, trapLocations)) {
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
					"Info is out of date, get the info from the server");

			Intent broadcastIntent = new Intent();
			Bundle b = new Bundle();
			b.putString(GetLocationService.LOCATION_TYPE_REQUEST, GetLocationService.SPEED_AND_BEARING_LOCATION_TYPE);
			broadcastIntent.putExtras(b);
			broadcastIntent.setAction(LocationRequestReceiver.GET_LOCATION_ACTION);
			sendBroadcast(broadcastIntent);

			stopSelf();
		}

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Info is still valid");

		// Pick the closest target within alert range and set and alert to tell
		// the user that the trap is approaching soon
		setAlertForClosestTarget(currentLocation, lastKnownLocation, trapLocations);

		
		// Find out how long this service should sleep and set the alarm for
		// that many milliseconds
		long timeToSleep = getTimeToSleep(currentLocation, trapLocations, lastKnownLocation);
		setAlarm(timeToSleep, currentLocation, trapLocations);

		stopSelf();
		
		return START_REDELIVER_INTENT;
	}


	/**
	 * This is called to modify the field {@link #trapLocations} based on the
	 * param currentLocation and the field {@link #lastKnownLocation}. This gets
	 * the users current bearing and makes sure that the bearing between the
	 * user and the trap is with a threshold of direction determined by
	 * {@link #PAST_POINT_VALID_BEARING_RANGE}
	 * 
	 * @param currentLocation
	 *            the user's current location
	 * @param trapLocations
	 * @param lastKnownLocation
	 */
	private void removePastTraps(SerializableLocation currentLocation,
			SerializableLocation lastKnownLocation, TrapLocations trapLocations) {

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
				"Removing traps that are out of range");

		float currentBearing = getCurrentBearing(currentLocation, lastKnownLocation);

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Current Bearing is: "
				+ currentBearing + " degrees east of north");

		// get the bounds that the bearing from current location to the trap
		// must be between for it to be a valid trap.
		float upperBearingBound = (float) ((currentBearing + .5 * PAST_POINT_VALID_BEARING_RANGE) % 360);
		float lowerBearingBound = (float) ((currentBearing - .5 * PAST_POINT_VALID_BEARING_RANGE) % 360);

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
				"Traps are valid between: " + lowerBearingBound
						+ " degrees and " + upperBearingBound + " degrees");

		// Go through every trap location, check that it is valid, if not,
		// remove it.
		Iterator<SerializableLocation> itor = trapLocations.getLocations()
				.iterator();

		while (itor.hasNext()) {
			SerializableLocation loc = itor.next();
			float bearingBetweenCurrentAndTrap = getBearingBetweenTwoLocations(
					loc, currentLocation);

			Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
					"Bearing to location is : " + bearingBetweenCurrentAndTrap
							+ " degrees east of north\n" + "for trap: \n"
							+ loc.toString());

			if (!(bearingBetweenCurrentAndTrap < upperBearingBound && bearingBetweenCurrentAndTrap > lowerBearingBound)) {
				itor.remove();

				Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
						"^^^Trap Removed");
			}
		}

	}

	/**
	 * This calculates the distance between the current location and each trap
	 * and puts that info in {@link #trapLocations}
	 * 
	 * @param currentLocation
	 *            user's current location
	 * @param trapLocations
	 * @param lastKnownLocation
	 */
	private void setUpdatedDistances(SerializableLocation currentLocation,
			SerializableLocation lastKnownLocation, TrapLocations trapLocations) {
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
				"Recalculating distances to points");
		for (SerializableLocation loc : trapLocations.getLocations()) {
			float[] result = new float[1];
			Location.distanceBetween(loc.getLatitude(), loc.getLongitude(),
					currentLocation.getLatitude(),
					currentLocation.getLongitude(), result);
			trapLocations.addDistance(loc, result[0]);
		}

	}

	/**
	 * This check to seee if the {@link #trapLocations} info is out of date or
	 * not. It uses three criteria in this order: Range from origin compared to
	 * the valid range for the server for the given points.
	 * 
	 * Distance to the farthest trap must be more than
	 * {@link #DISTANCE_TO_FURTHEST_TRAP_UPDATE_THRESHOLD}.
	 * 
	 * Current bearing must be in the valid bearing range given by the server in
	 * {@link #trapLocations}
	 * 
	 * @param currentLocation
	 *            the users current location
	 * @param trapLocations
	 * @param lastKnownLocation
	 * @return true if info is out of date, false if it's still valid
	 */
	private boolean infoIsOutOfDate(SerializableLocation currentLocation,
			SerializableLocation lastKnownLocation, TrapLocations trapLocations) {

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
				"Checking to see if info is out of date");

		SerializableLocation origin = trapLocations.getOriginalLocation();
		float[] results1 = new float[1];
		Location.distanceBetween(currentLocation.getLatitude(),
				currentLocation.getLongitude(), origin.getLatitude(),
				origin.getLongitude(), results1);
		float distanceFromOrigin = results1[0];

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
				"current location:" + currentLocation.getLatitude() + ", "
						+ currentLocation.getLongitude());
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "origin location:"
				+ origin.getLatitude() + ", " + origin.getLongitude());
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "distance:"
				+ distanceFromOrigin);
		// valid range check

		if (trapLocations.getRangeOfPointsFromOrigin() < distanceFromOrigin) {
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
					"Range from origin is invalid\n Current distance from origin: "
							+ distanceFromOrigin + " meters\nValid range: "
							+ trapLocations.getRangeOfPointsFromOrigin()
							+ " meters\n");
			return true;
		}

		// distance from most distant point

		float distanceToFarthestTrap = Collections.max(trapLocations
				.getDistanceCollection());

		if (distanceToFarthestTrap < DISTANCE_TO_FURTHEST_TRAP_UPDATE_THRESHOLD) {
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
					"Too close to furthest trap\n Current distance furthest Trap: "
							+ distanceToFarthestTrap + " meters\nValid range: "
							+ DISTANCE_TO_FURTHEST_TRAP_UPDATE_THRESHOLD
							+ " meters\n");

			return true;

		}
		// bearing check

		float upperBearingBound = (float) ((origin.getBearing() + .5 * trapLocations
				.getValidBearingRange()) % 360);
		float lowerBearingBound = (float) ((origin.getBearing() - .5 * trapLocations
				.getValidBearingRange()) % 360);
		float currentBearing = getCurrentBearing(currentLocation, lastKnownLocation);
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "currentBearing: "
				+ currentBearing);
		if (!(currentBearing > lowerBearingBound && currentBearing < upperBearingBound)) {
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
					"Heading in invalid bearing\n Current Bearing: "
							+ currentBearing + "degrees\nValid range: "
							+ lowerBearingBound + " to " + upperBearingBound
							+ " degrees\n");

			return true;
		}
		return false;
	}

	private void setAlertForClosestTarget(SerializableLocation currentLocation, SerializableLocation lastKnownLocation, TrapLocations trapLocations) {
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
				"Setting alarm for closest target");
		// SerializableLocation closestLoc =
		// trapLocations.getLocations().iterator().next();
		// float minDistance =
		// trapLocations.getDistanceFromLocation(closestLoc);
		// for (SerializableLocation loc : trapLocations.getLocations()) {
		// if (minDistance > trapLocations.getDistanceFromLocation(loc)){
		// closestLoc = loc;
		// }
		//
		// }
		//
		// trapLocations.removeLocation(closestLoc);
		//
		// Intent intentForAlert = new Intent(/*Alert Activity*/);
		//
		// intentForAlert
		// .setAction(/*Alert Activity Action*/);
		//
		// Bundle extraBundle = new Bundle();
		//
		// extraBundle.putSerializable(/*Alert Activity location key*/,
		// closestLoc);
		//
		// PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
		// intentForAlert, 0);
		//
		// // Set the broadcast alarm for a specified time and stop the service
		// AlarmManager alarmManager = (AlarmManager)
		// getSystemService(Service.ALARM_SERVICE);
		// alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
		// + /*Time until wake*/, pendingIntent);

	}

	/**
	 * Gets the users current bearing based on the bearing in currentLocation,
	 * or the calculated bearing between currentLocation and
	 * {@link #lastKnownLocation}. Returns -1 if no bearing could be determined.
	 * 
	 * @param currentLocation
	 *            the users current location
	 * @param lastKnownLocation 
	 * @return bearing in degrees east of north
	 */
	private float getCurrentBearing(SerializableLocation currentLocation, SerializableLocation lastKnownLocation) {

		if (currentLocation.hasBearing()) {
			return currentLocation.getBearing();
		}

		float bearing = getBearingBetweenTwoLocations(currentLocation,
				lastKnownLocation);

		if (bearing == Float.NaN) {
			return -1f;
		} else {
			return bearing;
		}

	}

	/**
	 * Gets the bearing between the start location and end location in degrees
	 * east of north
	 * 
	 * @param endLocation
	 *            the end location
	 * @param startLocation
	 *            start location
	 * @return degrees east of north
	 */
	private float getBearingBetweenTwoLocations(
			SerializableLocation endLocation, SerializableLocation startLocation) {		

		// results will be stored in float array
		float[] result1 = new float[1];

		// do the trig calculations and stuff
		Location.distanceBetween(startLocation.getLatitude(),
				startLocation.getLongitude(), startLocation.getLatitude(),
				endLocation.getLongitude(), result1);
		float distanceFromOldPointToRightTrianglePoint = result1[0];

		float[] result2 = new float[1];

		Location.distanceBetween(startLocation.getLatitude(),
				startLocation.getLongitude(), endLocation.getLatitude(),
				endLocation.getLongitude(), result2);
		float distanceFromOldPointToCurrentPoint = result2[0];

		float rawAngle = (float) Math
				.acos(distanceFromOldPointToRightTrianglePoint
						/ distanceFromOldPointToCurrentPoint);

		rawAngle = (float) Math.toDegrees(rawAngle);

		// figure out what quadrant this angle is in
		int quadrant;

		if (startLocation.getLatitude() < endLocation.getLatitude()) {

			if (startLocation.getLongitude() < endLocation.getLongitude()) {
				quadrant = 1;
			} else {
				quadrant = 2;
			}
		} else {
			if (startLocation.getLongitude() > endLocation.getLongitude()) {
				quadrant = 3;
			} else {
				quadrant = 4;
			}
		}

		// based on the quadrant give angle in degrees east of north

		float bearing = 361;

		switch (quadrant) {
		case 1:
			bearing = rawAngle;
			break;
		case 2:
			bearing = 180 - rawAngle;
			break;
		case 3:
			bearing = 180 + rawAngle;
			break;
		case 4:
			bearing = 360 - rawAngle;
			break;
		default:
			break;
		}
		return bearing;
	}

	/**
	 * Sets an alarm to wake up this service in timeToSleep milliseconds. Sends
	 * self currentLocation and {@link #trapLocations}.
	 * 
	 * @param timeToSleep
	 *            millis to sleep
	 * @param currentLocation
	 *            location to send self
	 * @param trapLocations 
	 */
	private void setAlarm(long timeToSleep, SerializableLocation currentLocation, TrapLocations trapLocations) {
		Intent intentForNextFix = new Intent(GetLocationService.class.getName());

		intentForNextFix
				.setAction(LocationRequestReceiver.GET_LOCATION_ACTION);

		Bundle extraBundle = new Bundle();
		
		extraBundle.putString(GetLocationService.LOCATION_TYPE_REQUEST, GetLocationService.SIMPLE_GPS_LOCATION_TYPE);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				intentForNextFix, 0);
		
		try {
			FileOutputStream fos = this.openFileOutput(GetTrapsService.TRAP_LOCATIONS_FILE_NAME, Context.MODE_PRIVATE);
			ObjectOutputStream os = new ObjectOutputStream(fos);
			os.writeObject(trapLocations);
			
			os.close();
			
			FileOutputStream fos2 = this.openFileOutput(GetTrapsService.LAST_KNOWN_LOCATION_FILE_NAME, Context.MODE_PRIVATE);
			ObjectOutputStream os2 = new ObjectOutputStream(fos2);
			os2.writeObject(currentLocation);
			
			os2.close();
			
		} catch (FileNotFoundException e) {
			serviceFailed();
			e.printStackTrace();
		} catch (IOException e) {
			serviceFailed();
			e.printStackTrace();
		}

		// Set the broadcast alarm for a specified time and stop the service
		AlarmManager alarmManager = (AlarmManager) getSystemService(Service.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
				+ timeToSleep, pendingIntent);
	}

	/**
	 * Finds the closest trap and gets the amount of time to sleep so that
	 * service can wake {@link #RATIO_OF_DISTANCE_TO_WAIT} ratio of the way
	 * there.
	 * 
	 * @param currentLocation
	 *            the current location
	 * @param trapLocations 
	 * @param lastKnownLocation 
	 * @return the number of millis to sleep
	 */
	private long getTimeToSleep(SerializableLocation currentLocation, TrapLocations trapLocations, SerializableLocation lastKnownLocation) {
		Collection<Float> distances = trapLocations.getDistanceCollection();
		Float dis = Collections.min(distances);
		Float speed = getCurrentSpeed(currentLocation, lastKnownLocation);

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Closest Trap is "
				+ dis + " meters away\n " + "Current speed is : " + speed
				+ " meters/second\n" + "Sleep for : "
				+ (long) ((long) (dis / speed) * RATIO_OF_DISTANCE_TO_WAIT)
				+ " seconds");

		return (long) ((long) (dis / speed) * 1000 * RATIO_OF_DISTANCE_TO_WAIT);
	}

	/**
	 * Gets the users current sleep based on the speed in currentLocation, or
	 * the calculated speed between currentLocation and
	 * {@link #lastKnownLocation}. Returns -1 if no speed could be determined.
	 * 
	 * @param currentLocation
	 *            the users current location
	 * @param lastKnownLocation 
	 * @return speed in meters per second
	 */
	private Float getCurrentSpeed(SerializableLocation currentLocation, SerializableLocation lastKnownLocation) {

		if (currentLocation.getSpeed() != 0.0f) {
			return currentLocation.getSpeed();
		}

		float[] result1 = new float[1];

		Location.distanceBetween(currentLocation.getLatitude(),
				currentLocation.getLongitude(),
				lastKnownLocation.getLatitude(),
				lastKnownLocation.getLongitude(), result1);
		float distanceFromLastKnown = result1[0];

		Float speed = distanceFromLastKnown
				/ (currentLocation.getTime() - lastKnownLocation.getTime())
				/ 1000;

		if (speed == Float.NaN) {
			return -1f;
		} else {
			return speed;
		}

	}

	/**
	 * If anything goes wrong, send out a broadcast that says so.
	 */
	public void serviceFailed() {
		Intent intent = new Intent();
		intent.setAction(TRAP_CALCULATIONS_FAILED_ACTION);
		sendBroadcast(intent);

		// calls destroy
		stopSelf();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}
