package edu.cmd.radar.android.check;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonMappingException;

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
import edu.cmd.radar.android.location.SerializableLocation;
import edu.cmd.radar.android.location.SimpleLocationService;
import edu.cmd.radar.android.location.SpeedAndBearingLoactionService;

public class TrapCheckWakeUpService extends Service {

	public static final String LOCATION_KEY = "LOCATION_KEY";
	public static final String LAST_TIME_KEY = "LAST_TIME_KEY";
	public static final float RATIO_OF_DISTANCE_TO_WAIT = 0.666f;
	protected BroadcastReceiver receiver;
	private TrapLocations trapLocations;
	private Location currentLocation;
	public static final String TRAP_LOCATIONS_INFO_KEY = "TRAP_LOCATIONS_INFO_KEY";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		trapLocations = (TrapLocations) intent.getExtras().getSerializable(
				TRAP_LOCATIONS_INFO_KEY);

		IntentFilter filter = new IntentFilter();
		filter.addAction(SimpleLocationService.SIMPLE_LOCATION_OBTAINED_ACTION);

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent i) {
				unregisterReceiver(receiver);

				locationRecieved(i);

			}

		};
		registerReceiver(receiver, filter);

		return START_REDELIVER_INTENT;
	}

	protected void locationRecieved(Intent i) {
		
		setUpdatedDistances(i);
		
		
		long timeToSleep = getTimeToSleep();

		setAlarm(timeToSleep);

		stopSelf();

	}

	private void setAlarm(long timeToSleep) {
		Intent intentForNextFix = new Intent(TrapCheckReceiver.class.getName());

		intentForNextFix
				.setAction(TrapCheckReceiver.CHECK_DISTANCE_FROM_TRAPS_ACTION);

		Bundle extraBundle = new Bundle();
		
		extraBundle.putSerializable(TrapCheckWakeUpService.LOCATION_KEY,
				new SerializableLocation(currentLocation));

		extraBundle.putSerializable(TRAP_LOCATIONS_INFO_KEY, trapLocations);
		
		extraBundle.putLong(TrapCheckWakeUpService.LAST_TIME_KEY,
				System.currentTimeMillis());
		intentForNextFix.putExtras(extraBundle);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				intentForNextFix, 0);

		// Set the broadcast alarm for a specified time and stop the service
		AlarmManager alarmManager = (AlarmManager) getSystemService(Service.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
				+ timeToSleep, pendingIntent);
	}

	private long getTimeToSleep() {
		ArrayList<Float> distances = (ArrayList<Float>) trapLocations
				.getDistanceList();
		Float dis = Collections.min(distances);
		ArrayList<SerializableLocation> locList = (ArrayList<SerializableLocation>) trapLocations
				.getLocationList();
		SerializableLocation closest = locList.get(distances.indexOf(dis));

		return (long) ((long) (dis / currentLocation.getSpeed()) * 1000 * RATIO_OF_DISTANCE_TO_WAIT);
	}

	private void setUpdatedDistances(Intent i) {
		SerializableLocation serloc = (SerializableLocation) i.getExtras()
				.getSerializable(SimpleLocationService.LOCATION_KEY);
		currentLocation = serloc.returnEquivalentLocation();

		List<Float> updatedDistances = new ArrayList<Float>();
		for (SerializableLocation loc : trapLocations.getLocationList()) {
			float[] result = new float[1];
			Location.distanceBetween(loc.getLongitude(), loc.getLatitude(),
					currentLocation.getLatitude(),
					currentLocation.getLongitude(), result);
			updatedDistances.add(result[0]);
		}

		trapLocations.setDistanceList(updatedDistances);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
