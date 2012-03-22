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

	protected static final String LOCATION_KEY = "LOCATION_KEY";
	public static final String LAST_TIME_KEY = "LAST_TIME_KEY";
	private static final float RATIO_OF_DISTANCE_TO_WAIT = 0.666f;
	protected BroadcastReceiver receiver;
	private TrapLocations trapLocations;
	private Location currentLocation;
	public static final String TRAP_LOCATIONS_INFO_FILE_NAME = "TRAP_LOCATIONS_INFO_FILE_NAME";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		FileInputStream file = null;

		try {
			file = this
					.openFileInput(TrapCheckWakeUpService.TRAP_LOCATIONS_INFO_FILE_NAME);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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

		trapLocations = streamToTrapLocation(file);

		return START_REDELIVER_INTENT;
	}

	protected void locationRecieved(Intent i) {
		setUpdatedDistances(i);
		convertToJSONFile();
		long timeToSleep = getTimeToSleep();
		
		Intent intentForNextFix = new Intent(TrapCheckReceiver.class.getName());

		intentForNextFix.setAction(TrapCheckReceiver.CHECK_DISTANCE_FROM_TRAPS_ACTION);

		Bundle extraBundle = new Bundle();
		extraBundle.putSerializable(TrapCheckWakeUpService.LOCATION_KEY,
				new SerializableLocation(currentLocation));

		extraBundle.putLong(TrapCheckWakeUpService.LAST_TIME_KEY, System.currentTimeMillis());
		intentForNextFix.putExtras(extraBundle);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				intentForNextFix, 0);

		// Set the broadcast alarm for a specified time and stop the service
		AlarmManager alarmManager = (AlarmManager) getSystemService(Service.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
				+ timeToSleep, pendingIntent);
		
		stopSelf();

	}

	private long getTimeToSleep() {
		ArrayList<Float> distances = (ArrayList<Float>) trapLocations
				.getDistanceList();
		Float dis = Collections.min(distances);
		ArrayList<Location> locList = (ArrayList<Location>) trapLocations
				.getLocationList();
		Location closest = locList.get(distances.indexOf(dis));

		return (long) ((long) (dis / currentLocation.getSpeed()) * 1000 * RATIO_OF_DISTANCE_TO_WAIT);
	}

	private void convertToJSONFile() {
		try {

			JsonFactory jfactory = new JsonFactory();

			/*** write to file ***/
			JsonGenerator jGenerator = jfactory.createJsonGenerator(new File(
					TrapCheckWakeUpService.TRAP_LOCATIONS_INFO_FILE_NAME), JsonEncoding.UTF8);
			jGenerator.writeStartObject();

			jGenerator.writeFieldName("locations");
			jGenerator.writeStartArray(); // [

			for (Location loc : trapLocations.getLocationList()) {
				jGenerator.writeStartArray();
				jGenerator.writeNumber(loc.getLatitude());
				jGenerator.writeNumber(loc.getLongitude());
				jGenerator.writeNumber(loc.getSpeed());
				jGenerator.writeNumber(loc.getAccuracy());
				jGenerator.writeEndArray();
			}

			jGenerator.writeEndArray();

			jGenerator.writeFieldName("distances");
			jGenerator.writeStartArray();

			for (Float distance : trapLocations.getDistanceList()) {
				jGenerator.writeNumber(distance);
			}

			jGenerator.writeEndArray();

			jGenerator.writeNumberField("timestamp",
					trapLocations.getTimeStamp());

			jGenerator.writeEndObject();

			jGenerator.close();

		} catch (JsonGenerationException e) {

			e.printStackTrace();

		} catch (JsonMappingException e) {

			e.printStackTrace();

		} catch (IOException e) {

			e.printStackTrace();

		}
	}

	private void setUpdatedDistances(Intent i) {
		SerializableLocation serloc = (SerializableLocation) i.getExtras()
				.getSerializable(SimpleLocationService.LOCATION_KEY);
		currentLocation = serloc.returnEquivalentLocation();

		List<Float> updatedDistances = new ArrayList<Float>();
		for (Location loc : trapLocations.getLocationList()) {
			updatedDistances.add(currentLocation.distanceTo(loc));
		}

		trapLocations.setDistanceList(updatedDistances);
	}

	private TrapLocations streamToTrapLocation(InputStream stream) {
		JsonFactory jfactory = new JsonFactory();
		TrapLocations trapLocations = new TrapLocations();
		/*** read from file ***/
		try {
			JsonParser jParser = jfactory.createJsonParser(stream);

			while (jParser.nextToken() != JsonToken.END_OBJECT) {

				String fieldname = jParser.getCurrentName();
				if ("locations".equals(fieldname)) {

					jParser.nextToken(); // current token is "[", move next

					// messages is array, loop until token equal to "]"
					while (jParser.nextToken() != JsonToken.END_ARRAY) {

						jParser.nextToken(); // current token is "[", move next
						double lat = jParser.getDoubleValue();
						jParser.nextToken();
						double lon = jParser.getDoubleValue();
						jParser.nextToken();
						float speed = jParser.getFloatValue();
						jParser.nextToken();
						float accuracy = jParser.getFloatValue();
						trapLocations.addLocation(lat, lon, accuracy, speed);
						jParser.nextToken();
					}

				}

				if ("distances".equals(fieldname)) {
					jParser.nextToken(); // current token is "[", move next
					// messages is array, loop until token equal to "]"
					while (jParser.nextToken() != JsonToken.END_ARRAY) {
						trapLocations.addDistance(jParser.getFloatValue());
					}
				}

				if ("timestamp".equals(fieldname)) {
					jParser.nextToken();
					trapLocations.setTimeStamp(jParser.getLongValue());

				}

			}
			jParser.close();

		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			stream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return trapLocations;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
