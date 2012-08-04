package edu.cmd.radar.android.check;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
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
import edu.cmd.radar.android.location.GetLocationService;
import edu.cmd.radar.android.ui.MainSettingsActivity;

/**
 * This service sends location, speed, and bearing info to the server and recieves back locations of traps. 
 * It packages those locations and broadcasts them off.
 * 
 * @author satshabad
 * 
 */
public class GetTrapsService extends Service {

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
	 * Filter used to broadcast that this service has gotten info
	 */
	public static final String OBTAINING_TRAP_INFO_FAILED_ACTION = "edu.cmd.radar.android.check.OBTAINING_TRAP_INFO_FAILED_ACTION";

	/**
	 * The key used to store the trap info in an intent
	 */
	public static final String NEW_TRAP_LOCATION_INFO_KEY = "NEW_TRAP_LOCATION_INFO_KEY";

	public static final String TRAP_LOCATIONS_FILE_NAME = "TRAP_LOCATIONS_FILE_NAME";

	public static final String LAST_KNOWN_LOCATION_FILE_NAME = "LAST_KNOWN_LOCATION_FILE_NAME";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
				"TrapCheckPullService is started");

		locationRecieved(intent);

		return START_REDELIVER_INTENT;
	}

	/**
	 * This method is called only when a location fix has been received. it
	 * executes the main logic of the service, getting info from the server and
	 * writes it to a file. Then it sends a broadcast indicating that the file is ready to be read.
	 * 
	 * @param i
	 */
	protected void locationRecieved(Intent i) {

		SerializableLocation currentLocation = (SerializableLocation) i
				.getExtras().getSerializable(
						GetLocationService.LOCATION_KEY);

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

		// package the info up and send out a broadcast that this service is
		// done. Then stop.


		
		try {
			FileOutputStream fos = this.openFileOutput(TRAP_LOCATIONS_FILE_NAME, Context.MODE_PRIVATE);
			ObjectOutputStream os = new ObjectOutputStream(fos);
			os.writeObject(trapLocations);
			
			os.close();
			
			FileOutputStream fos2 = this.openFileOutput(LAST_KNOWN_LOCATION_FILE_NAME, Context.MODE_PRIVATE);
			ObjectOutputStream os2 = new ObjectOutputStream(fos2);
			os2.writeObject(currentLocation);
			
			os2.close();
			
		} catch (FileNotFoundException e) {
			broadcastServiceFailed();
			e.printStackTrace();
		} catch (IOException e) {
			broadcastServiceFailed();
			e.printStackTrace();
		}
		
		Intent intent = new Intent();
		
		intent.setAction(TRAP_INFO_OBTAINED_ACTION);
		sendBroadcast(intent);

		// calls destroy
		stopSelf();
	}

	/**
	 * If anything goes wrong, send out a broadcast that says so.
	 */
	public void broadcastServiceFailed() {
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
				"Safely stopping TrapCheckPullService");
		Intent intent = new Intent();
		intent.setAction(OBTAINING_TRAP_INFO_FAILED_ACTION);
		sendBroadcast(intent);

		// calls destroy
		stopSelf();
	}

	/**
	 * Uploads JSON info to server and receives information about nearby traps
	 * 
	 * @param jsonInfo
	 *            the current location in JSON format
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
			broadcastServiceFailed();
			e.printStackTrace();
		} catch (IOException e) {
			broadcastServiceFailed();
			e.printStackTrace();
		}
		HttpEntity entity = response.getEntity();
		try {
			stream = entity.getContent();
		} catch (IllegalStateException e) {
			broadcastServiceFailed();
			e.printStackTrace();
		} catch (IOException e) {
			broadcastServiceFailed();
			e.printStackTrace();
		}

		return stream;
	}

	/**
	 * Takes a location and puts it into JSON format
	 * 
	 * @param loc
	 *            the location
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
			broadcastServiceFailed();
			e.printStackTrace();
		}

		// convert to string
		String toSend = writer.toString();
		return toSend;
	}

	/**
	 * Turns stream of JSON info and the current location into a TrapInfo object
	 * 
	 * @param stream
	 *            JSON from server
	 * @param currentLocation
	 *            users loc
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
			broadcastServiceFailed();
			e.printStackTrace();
		} catch (IOException e) {
			broadcastServiceFailed();
			e.printStackTrace();
		}

		try {
			stream.close();
		} catch (IOException e) {
			broadcastServiceFailed();
			e.printStackTrace();
		}

		trapLocations.setOriginalLocation(currentLocation);
		trapLocations.setTimeStamp(System.currentTimeMillis());

		return trapLocations;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}
