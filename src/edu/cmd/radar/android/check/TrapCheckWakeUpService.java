package edu.cmd.radar.android.check;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

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
import edu.cmd.radar.android.location.SerializableLocation;
import edu.cmd.radar.android.location.SimpleLocationService;
import edu.cmd.radar.android.location.SpeedAndBearingLoactionService;
import edu.cmd.radar.android.ui.MainSettingsActivity;

public class TrapCheckWakeUpService extends Service {

	public static final String LAST_LOCATION_KEY = "LOCATION_KEY";
	public static final float RATIO_OF_DISTANCE_TO_WAIT = 0.666f;
	protected BroadcastReceiver simpleLocationReceiver;
	protected BroadcastReceiver serverInfoReceiver;
	private TrapLocations trapLocations;
	private SerializableLocation lastKnownLocation;
	public static final String TRAP_LOCATIONS_INFO_KEY = "TRAP_LOCATIONS_INFO_KEY";
	// 5 miles in meters
	private static final float DISTANCE_TO_FURTHEST_TRAP_UPDATE_THRESHOLD = 8046.72f;
	private static final double PAST_POINT_VALID_BEARING_RANGE = 120;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "TrapCheckWakeUpService onCommand called");
		Bundle extras = (Bundle) intent.getExtras();

		if (extras == null) {
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "No trapLocation info yet");
			startServiceToGetTrapInfo();
		} else {
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Traplocation info exists");
			trapLocations = (TrapLocations) intent.getExtras().getSerializable(
					TRAP_LOCATIONS_INFO_KEY);
			
			lastKnownLocation = (SerializableLocation) intent.getExtras()
					.getSerializable(LAST_LOCATION_KEY);
			IntentFilter filter = new IntentFilter();
			filter.addAction(SimpleLocationService.SIMPLE_LOCATION_OBTAINED_ACTION);

			simpleLocationReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent i) {
					unregisterReceiver(simpleLocationReceiver);
					Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Received location from simplelocation broadcast");
					SerializableLocation currentLocation = (SerializableLocation) i
							.getExtras().getSerializable(
									SimpleLocationService.LOCATION_KEY);

					locationRecieved(i, currentLocation);

				}

			};
			registerReceiver(simpleLocationReceiver, filter);
			Intent i = new Intent(this, SimpleLocationService.class);
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Starting SimpleLocationSevice to get a simple fix");
			startService(i);
		}

		return START_REDELIVER_INTENT;
	}

	private void startServiceToGetTrapInfo() {
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(TrapCheckServerPullService.TRAP_INFO_OBTAINED_ACTION);

		serverInfoReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent i) {
				unregisterReceiver(serverInfoReceiver);
				Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Got info from TrapCheckServerPullService broadcast");
				trapLocations = (TrapLocations) i.getExtras().getSerializable(
						TrapCheckServerPullService.NEW_TRAP_LOCATION_INFO_KEY);
				
				SerializableLocation currentLocation = (SerializableLocation) i
						.getExtras().getSerializable(
								SpeedAndBearingLoactionService.LOCATION_KEY);
				
				lastKnownLocation = currentLocation;
				locationRecieved(i, currentLocation);

			}

		};
		registerReceiver(serverInfoReceiver, filter);
		Intent i = new Intent(this, TrapCheckServerPullService.class);
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Starting TrapCheckServerPullService to get a hard fix and trap info");
		startService(i);
	}

	protected void locationRecieved(Intent i,
			SerializableLocation currentLocation) {
		
		//removePastTraps(currentLocation);

		setUpdatedDistances(currentLocation);

		if (infoOutOfDate(currentLocation)) {
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Info is out of date, get the info from the server");
			startServiceToGetTrapInfo();
			return;
		}
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Info is still valid");
		
		setAlertForClosestTarget(currentLocation);

		long timeToSleep = getTimeToSleep(currentLocation);
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Setting alarms for "+ timeToSleep/1000 + " seconds");
		setAlarm(timeToSleep, currentLocation);

		stopSelf();

	}

	private void removePastTraps(SerializableLocation currentLocation) {
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Removing traps that are out of range");
		float currentBearing = getCurrentBearing(currentLocation);

		float upperBearingBound = (float) ((currentBearing + .5 * PAST_POINT_VALID_BEARING_RANGE) % 360);
		float lowerBearingBound = (float) ((currentBearing - .5 * PAST_POINT_VALID_BEARING_RANGE) % 360);

		Iterator<SerializableLocation> itor = trapLocations.getLocations().iterator();

		
		
		while (itor.hasNext()) {
			SerializableLocation loc = itor.next();
			float bearingBetweenCurrentAndTrap = getBearingBetweenTwoLocations(
					loc, currentLocation);
			if (!(bearingBetweenCurrentAndTrap < upperBearingBound && bearingBetweenCurrentAndTrap > lowerBearingBound)) {
				itor.remove();
			}
		}

	}

	private void setAlertForClosestTarget(SerializableLocation currentLocation) {
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Setting alarm for closest target");
//		SerializableLocation closestLoc = trapLocations.getLocations().iterator().next();
//		float minDistance = trapLocations.getDistanceFromLocation(closestLoc);
//		for (SerializableLocation loc : trapLocations.getLocations()) {
//			if (minDistance > trapLocations.getDistanceFromLocation(loc)){
//				closestLoc = loc;
//			}
//			
//		}
//
//		trapLocations.removeLocation(closestLoc);
//		
//		Intent intentForAlert = new Intent(/*Alert Activity*/);
//
//		intentForAlert
//				.setAction(/*Alert Activity Action*/);
//
//		Bundle extraBundle = new Bundle();
//
//		extraBundle.putSerializable(/*Alert Activity location key*/,
//				closestLoc);
//
//		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
//				intentForAlert, 0);
//
//		// Set the broadcast alarm for a specified time and stop the service
//		AlarmManager alarmManager = (AlarmManager) getSystemService(Service.ALARM_SERVICE);
//		alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
//				+ /*Time until wake*/, pendingIntent);
		
		
	}

	private boolean infoOutOfDate(SerializableLocation currentLocation) {
		
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Checking to see if info is out of date");
		
		SerializableLocation origin = trapLocations.getOriginalLocation();
		float[] results1 = new float[1];
		Location.distanceBetween(currentLocation.getLatitude(),
				currentLocation.getLongitude(), origin.getLatitude(),
				origin.getLongitude(), results1);
		float distanceFromOrigin = results1[0];

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "current location:"+currentLocation.getLatitude()+", "+currentLocation.getLongitude());
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "origin location:"+origin.getLatitude()+", "+origin.getLongitude());
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "distance:"+distanceFromOrigin);
		// valid range check

		if (trapLocations.getRangeOfPointsFromOrigin() < distanceFromOrigin) {
			return true;
		}
		
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Range is still valid");
		// distance from most distant point
		
		
		
		float distanceToFurthestTrap = Collections.max(trapLocations
				.getDistanceCollection());

		if (distanceToFurthestTrap < DISTANCE_TO_FURTHEST_TRAP_UPDATE_THRESHOLD) {
			return true;

		}
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Still far enough from furthest trap");
		// bearing check

		float upperBearingBound = (float) ((origin.getBearing() + .5 * trapLocations
				.getValidBearingRange()) % 360);
		float lowerBearingBound = (float) ((origin.getBearing() - .5 * trapLocations
				.getValidBearingRange()) % 360);
		float currentBearing = getCurrentBearing(currentLocation);
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "currentBearing: "+currentBearing);
		if (!(currentBearing > lowerBearingBound && currentBearing < upperBearingBound)) {
			return true;
		}
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "still within valid bearing range");
		return false;
	}

	private float getCurrentBearing(SerializableLocation currentLocation) {

		if (currentLocation.hasBearing()) {
			return currentLocation.getBearing();
		}

		return getBearingBetweenTwoLocations(currentLocation, lastKnownLocation);

	}

	private float getBearingBetweenTwoLocations(
			SerializableLocation endLocation, SerializableLocation startLocation) {
		float[] result1 = new float[1];

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

	private void setAlarm(long timeToSleep, SerializableLocation currentLocation) {
		Intent intentForNextFix = new Intent(TrapCheckReceiver.class.getName());

		intentForNextFix
				.setAction(TrapCheckReceiver.CHECK_DISTANCE_FROM_TRAPS_ACTION);

		Bundle extraBundle = new Bundle();

		extraBundle.putSerializable(TrapCheckWakeUpService.LAST_LOCATION_KEY,
				currentLocation);

		extraBundle.putSerializable(TRAP_LOCATIONS_INFO_KEY, trapLocations);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				intentForNextFix, 0);

		// Set the broadcast alarm for a specified time and stop the service
		AlarmManager alarmManager = (AlarmManager) getSystemService(Service.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
				+ timeToSleep, pendingIntent);
	}

	private long getTimeToSleep(SerializableLocation currentLocation) {
		Collection<Float> distances = trapLocations
				.getDistanceCollection();
		Float dis = Collections.min(distances);
		return (long) ((long) (dis / getCurrentSpeed(currentLocation)) * 1000 * RATIO_OF_DISTANCE_TO_WAIT);
	}

	private Float getCurrentSpeed(SerializableLocation currentLocation) {

		if (currentLocation.getSpeed() != 0.0f){
			return currentLocation.getSpeed();
		}
		
		float[] result1 = new float[1];

		Location.distanceBetween(currentLocation.getLatitude(),
				currentLocation.getLongitude(), lastKnownLocation.getLatitude(),
				lastKnownLocation.getLongitude(), result1);
		float distanceFromLastKnown = result1[0];
		
		return distanceFromLastKnown/(currentLocation.getTime()-lastKnownLocation.getTime())/1000;
		
	}

	private void setUpdatedDistances(SerializableLocation currentLocation) {
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Recalculating distances to points");
		for (SerializableLocation loc : trapLocations.getLocations()) {
			float[] result = new float[1];
			Location.distanceBetween(loc.getLongitude(), loc.getLatitude(),
					currentLocation.getLatitude(),
					currentLocation.getLongitude(), result);
			trapLocations.addDistance(loc, result[0]);
		}

	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onDestroy() {
		try {
			unregisterReceiver(serverInfoReceiver);
			unregisterReceiver(simpleLocationReceiver);
		} catch (Exception e) {
			// TODO: handle exception
		}
		super.onDestroy();
	}
}
