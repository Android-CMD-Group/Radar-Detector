package edu.cmd.radar.android.check;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import com.google.gson.stream.JsonWriter;

import edu.cmd.radar.android.drive.WifiChangeReceiver;
import edu.cmd.radar.android.location.SerializableLocation;
import edu.cmd.radar.android.location.SpeedAndBearingLoactionService;
import edu.cmd.radar.android.shake.ShakeListenerService;
import edu.cmd.radar.android.ui.MainSettingsActivity;

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
import android.provider.Settings.Secure;
import android.util.Log;

public class TrapCheckServerPullService extends Service {

	private static final String TRAP_CHECK_URI = "http://domain/servlet/post";
	public static final String TRAP_INFO_OBTAINED_ACTION = "edu.cmd.radar.android.check.TRAP_INFO_OBTAINED";
	private static final String NEW_TRAP_LOCATION_INFO_KEY = null;
	private BroadcastReceiver receiver;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		IntentFilter filter = new IntentFilter();
		filter.addAction(SpeedAndBearingLoactionService.SPEED_AND_BEARING_LOCATION_OBTAINED_ACTION);

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent i) {
				
				unregisterReceiver(receiver);

				locationRecieved(i);

			}

		};
		registerReceiver(receiver, filter);
		Intent i = new Intent(this, SpeedAndBearingLoactionService.class);
		startService(i);

		return START_REDELIVER_INTENT;
	}

	protected void locationRecieved(Intent i) {
		
		SerializableLocation currentLocation = (SerializableLocation) i.getExtras().getSerializable(
				SpeedAndBearingLoactionService.LOCATION_KEY);

		String toSend = writeInfoToJsonString(currentLocation);
		
		InputStream jsonStream = getTrapLocationsFromServer(toSend);

		TrapLocations trapLocations = streamToTrapLocation(jsonStream, currentLocation);

		Intent intent = new Intent();

		intent.putExtras(i);
		intent.getExtras().putSerializable(NEW_TRAP_LOCATION_INFO_KEY, trapLocations);
		
		intent.setAction(TRAP_INFO_OBTAINED_ACTION);
		sendBroadcast(intent);

		// calls destroy
		stopSelf();
	}

	private InputStream getTrapLocationsFromServer(String jsonInfo) {



		InputStream stream = null;
		String result = "";

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

		// might not need
		// //convert response to string
		// try{
		// BufferedReader reader = new BufferedReader(new
		// InputStreamReader(is));
		// StringBuilder sb = new StringBuilder();
		// String line = null;
		// while ((line = reader.readLine()) != null) {
		// sb.append(line + "\n");
		// }
		// is.close();
		// result=sb.toString();
		// }catch(Exception e){
		// Log.e("log_tag", "Error converting result "+e.toString());
		// }

		return stream;
	}

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

	private TrapLocations streamToTrapLocation(InputStream stream, SerializableLocation currentLocation) {
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

				if ("distanceRange".equals(fieldname)) {
					jParser.nextToken();
					trapLocations.setRangeOfPointsFromOrigin(jParser.getFloatValue());
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
