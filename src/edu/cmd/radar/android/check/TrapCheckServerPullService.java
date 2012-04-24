package edu.cmd.radar.android.check;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.util.Log;

import com.google.gson.stream.JsonWriter;

import edu.cmd.radar.android.location.SerializableLocation;
import edu.cmd.radar.android.location.SpeedAndBearingLoactionService;
import edu.cmd.radar.android.ui.MainSettingsActivity;

/**
 * This class interfaces with the server to get the locations of nearby speed
 * traps. To do this the service must obtain the users location, speed, and,
 * bearing. This is then sent to the server and the reply is packaged and
 * broadcast. For more info go to:
 * https://github.com/Android-CMD-Group/Radar-Detector
 * /wiki/Design:-getting-info-on-traps-from-server
 * 
 * 
 * @author satshabad
 * 
 */
public class TrapCheckServerPullService extends Service {

	/**
	 * The "provider" of the locations gotten from the server
	 */
	private static final String SERVER_PROVIDER = "server";

	/**
	 * The URI of the server that gives the trap info
	 */
	private static final String TRAP_CHECK_URI = "http://192.168.1.3:1188/Radar_Server/check";

	/**
	 * Filter used to broadcast that this service has gotten info
	 */
	public static final String TRAP_INFO_OBTAINED_ACTION = "edu.cmd.radar.android.check.TRAP_INFO_OBTAINED";

	/**
	 * The key used to store the trap info in an intent
	 */
	public static final String NEW_TRAP_LOCATION_INFO_KEY = "NEW_TRAP_LOCATION_INFO_KEY";

	/**
	 * The broadcast receiver that catches the location from the started service
	 */
	private BroadcastReceiver receiver;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// This method starts another service that grabs the users location,
		// speed and bearing and then catches said services broadcast.

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
				"TrapCheckPullService is started");

		IntentFilter filter = new IntentFilter();
		filter.addAction(SpeedAndBearingLoactionService.SPEED_AND_BEARING_LOCATION_OBTAINED_ACTION);

		// get the users current location, speed, and bearing to send to the
		// server

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent i) {

				try {
					unregisterReceiver(receiver);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
						"Location received in TrapCheckPullService by broadcast from SpeedAndBearingLoactionService");
				locationRecieved(i);

			}

		};

		registerReceiver(receiver, filter);
		Intent i = new Intent(this, SpeedAndBearingLoactionService.class);

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
				"Starting SpeedAndBearingLoactionService from TrapCheckPullService to get fix");

		startService(i);

		return START_REDELIVER_INTENT;
	}

	/**
	 * This method is called only when a location fix has been received. it
	 * executes the main logic of the service, getting info from the server and
	 * sending a broadcast with the info packed inside of it.
	 * 
	 * @param i
	 */
	protected void locationRecieved(Intent i) {

		SerializableLocation currentLocation = (SerializableLocation) i
				.getExtras().getSerializable(
						SpeedAndBearingLoactionService.LOCATION_KEY);

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "\t\t\tlocation is:\n"
				+ currentLocation.toString());

		// Turn the current location into JSON format for easy sending to the
		// server

		String toSend = writeInfoToJsonString(currentLocation);

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Sending " + toSend
				+ " to server to get traps in local area");
		
		// Upload the given info to the server and get the JSON response.

		InputStream jsonStream = getTrapLocationsFromServer(toSend);

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
				"got raw info from server");

		// take that JSON response and turn it into a TrapLocations object
		
		TrapLocations trapLocations = streamToTrapLocation(jsonStream,
				currentLocation);

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
				"\t\t\tParesed info is\n" + trapLocations.toString());

		// Package the info up
		Bundle oldExtras = i.getExtras();
		oldExtras.putSerializable(NEW_TRAP_LOCATION_INFO_KEY, trapLocations);
		Intent intent = new Intent();
		intent.putExtras(oldExtras);

		// Send out a broadcast that this service is done.
		intent.setAction(TRAP_INFO_OBTAINED_ACTION);
		sendBroadcast(intent);

		// calls destroy
		stopSelf();
	}

	/**
	 * Uploads JSON info to server and receives information about nearby traps
	 * 
	 * @param jsonInfo the current location in JSON format
	 * @return a stream on JSON info
	 */
	private InputStream getTrapLocationsFromServer(String jsonInfo) {

		// THIS METHOD COULD USE SOME ERROR CATCHING
		
		InputStream stream = null;

		StringEntity se = null;
		try {
			se = new StringEntity(jsonInfo);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		// use http client to make a POST request and put the JSON as the
		// message
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpPost httpost = new HttpPost(TRAP_CHECK_URI);
		httpost.setEntity(se);
		httpost.setHeader("Content-type", "application/json");
		HttpResponse response = null;
		try {
			response = httpclient.execute(httpost);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		HttpEntity entity = response.getEntity();
		try {
			stream = entity.getContent();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return stream;
	}

	/**
	 * Takes a location and puts it into JSON format
	 * 
	 * @param loc the location
	 * @return the JSON string
	 */
	private String writeInfoToJsonString(SerializableLocation loc) {
		StringWriter writer = new StringWriter();
		JsonWriter jsonWriter = new JsonWriter(writer);
		try {
			jsonWriter.beginObject();
			jsonWriter.name("loc");
			jsonWriter.beginArray();
			jsonWriter.value(loc.getLatitude());
			jsonWriter.value(loc.getLongitude());

			jsonWriter.endArray();

			// not all location info may be valid
			if (loc.hasAccuracy()) {
				jsonWriter.name("accuracy").value(loc.getAccuracy());
			} else {
				jsonWriter.name("accuracy").nullValue();
			}

			if (loc.hasSpeed()) {
				jsonWriter.name("speed").value(loc.getSpeed());
			} else {
				jsonWriter.name("speed").nullValue();
			}

			if (loc.hasBearing()) {
				jsonWriter.name("bearing").value(loc.getBearing());
			} else {
				jsonWriter.name("bearing").nullValue();
			}
			// get the native android ID
			jsonWriter.name("id").value(
					Secure.getString(getContentResolver(), Secure.ANDROID_ID));

			jsonWriter.endObject();
		} catch (IOException e) {
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT,
					"Problem writing JSON");
			e.printStackTrace();
		}

		// convert to string
		String toSend = writer.toString();
		return toSend;
	}

	/**
	 * Turns stream of JSON info and the current location into a TrapInfo object
	 * 
	 * @param stream JSON from server
	 * @param currentLocation users loc
	 * @return nearby trap location in an object called TrapLocations
	 */
	private TrapLocations streamToTrapLocation(InputStream stream,
			SerializableLocation currentLocation) {
		JsonFactory jfactory = new JsonFactory();
		TrapLocations trapLocations = new TrapLocations();
		/*** read from file ***/
		try {
			JsonParser jParser = jfactory.createJsonParser(stream);

			while (jParser.nextToken() != JsonToken.END_OBJECT) {

				String fieldname = jParser.getCurrentName();
				if ("locations".equals(fieldname)) {

					jParser.nextToken(); // current token is "[", move next

					// array, loop until token equal to "]"
					while (jParser.nextToken() != JsonToken.END_ARRAY) {

						jParser.nextToken(); // current token is "[", move next
						double lat = jParser.getDoubleValue();
						jParser.nextToken();
						double lon = jParser.getDoubleValue();
						jParser.nextToken();
						float speed = jParser.getFloatValue();
						jParser.nextToken();
						float accuracy = jParser.getFloatValue();
						trapLocations.addLocation(lat, lon, accuracy, speed,
								SERVER_PROVIDER);
						jParser.nextToken();
					}

				}

				if ("distanceRange".equals(fieldname)) {
					jParser.nextToken();
					trapLocations.setRangeOfPointsFromOrigin(jParser
							.getFloatValue());
				}

				if ("bearingRange".equals(fieldname)) {
					jParser.nextToken();
					trapLocations.setValidBearingRange(jParser.getFloatValue());
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

		trapLocations.setOriginalLocation(currentLocation);
		trapLocations.setTimeStamp(System.currentTimeMillis());

		return trapLocations;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		try {
			unregisterReceiver(receiver);
		} catch (Exception e) {
		}
		super.onDestroy();
	}
}
