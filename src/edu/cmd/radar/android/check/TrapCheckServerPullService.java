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
	private BroadcastReceiver receiver;
	private SerializableLocation currentLocation;

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

		return START_STICKY;
	}

	protected void locationRecieved(Intent i) {
		currentLocation = (SerializableLocation) i.getExtras().getSerializable(
				SpeedAndBearingLoactionService.LOCATION_KEY);

		InputStream jsonStream = getTrapLocationsFromServer(currentLocation);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		copyInStreamToByteOutStream(jsonStream, baos);

		InputStream streamForFile = new ByteArrayInputStream(baos.toByteArray());

		writeJsonToFile(streamForFile);

		InputStream streamForObject = new ByteArrayInputStream(
				baos.toByteArray());

		TrapLocations trapLocations = streamToTrapLocation(streamForObject);

		try {
			baos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		long timeToSleep = getTimeToSleep(trapLocations);

		Intent intentForNextFix = new Intent(TrapCheckReceiver.class.getName());

		intentForNextFix.setAction(TrapCheckReceiver.CHECK_DISTANCE_FROM_TRAPS_ACTION);

		Bundle extraBundle = new Bundle();
		extraBundle.putSerializable(TrapCheckWakeUpService.LOCATION_KEY,
				currentLocation);

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

	private void copyInStreamToByteOutStream(InputStream jsonStream,
			ByteArrayOutputStream baos) {
		byte[] buffer = new byte[1024];
		int len;
		try {
			while ((len = jsonStream.read(buffer)) > -1) {
				baos.write(buffer, 0, len);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			baos.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			jsonStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeJsonToFile(InputStream streamForFile) {
		FileOutputStream file = null;
		try {
			file = this.openFileOutput(TrapCheckWakeUpService.TRAP_LOCATIONS_INFO_FILE_NAME,
					Context.MODE_PRIVATE);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int read = 0;
		byte[] bytes = new byte[1024];
		try {
			while ((read = streamForFile.read(bytes)) != -1) {
				file.write(bytes, 0, read);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			file.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			streamForFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private long getTimeToSleep(TrapLocations trapLocations) {

		ArrayList<Float> distances = (ArrayList<Float>) trapLocations
				.getDistanceList();
		Float dis = Collections.min(distances);
		ArrayList<Location> locList = (ArrayList<Location>) trapLocations
				.getLocationList();
		Location closest = locList.get(distances.indexOf(dis));

		return  (long) ((long) (dis / currentLocation.getSpeed()) * 1000 * TrapCheckWakeUpService.RATIO_OF_DISTANCE_TO_WAIT);
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}

	private InputStream getTrapLocationsFromServer(SerializableLocation loc) {

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

		InputStream stream = null;
		String result = "";

		StringEntity se = null;
		try {
			se = new StringEntity(toSend);
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
