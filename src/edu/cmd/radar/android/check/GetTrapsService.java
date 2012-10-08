package edu.cmd.radar.android.check;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

import edu.cmd.radar.android.location.GetLocationService;
import edu.cmd.radar.android.location.SerializableLocation;
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
	 * The URI of the server serviceContext gives the trap info
	 */
	private static final String TRAP_CHECK_URI = "http://ec2-54-245-44-240.us-west-2.compute.amazonaws.com/gettraps";
	//private static final String TRAP_CHECK_URI = "http://192.168.1.4:800/test";
	

	/**
	 * Filter used to broadcast serviceContext this service has failed to get info
	 */
	public static final String OBTAINING_TRAP_INFO_FAILED_ACTION = "edu.cmd.radar.android.check.OBTAINING_TRAP_INFO_FAILED_ACTION";

	/**
	 * The key used to store the trap info in an intent
	 */
	public static final String NEW_TRAP_LOCATION_INFO_KEY = "NEW_TRAP_LOCATION_INFO_KEY";

	public static final String TRAP_LOCATIONS_FILE_NAME = "TRAP_LOCATIONS_FILE_NAME";

	public static final String LAST_KNOWN_LOCATION_FILE_NAME = "LAST_KNOWN_LOCATION_FILE_NAME";

	private BroadcastReceiver receiver;
	
	private Context serviceContext = this;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
				"TrapCheckPullService is started");
		
		
		if (intent.getExtras().getSerializable(GetLocationService.LOCATION_KEY) == null){
			
			if(GetLocationService.isBusy == false){
				
				Intent i = new Intent(this, GetLocationService.class);
				Bundle extras = new Bundle();
				extras.putString(GetLocationService.LOCATION_TYPE_REQUEST,
						GetLocationService.SIMPLE_GPS_LOCATION_TYPE);
				extras.putSerializable("CLASS_TO_SEND_BACK_TO",
						GetTrapsService.class);
				i.putExtras(extras);
				startService(i);
				stopSelf();
				
			}else{
				
				IntentFilter filter = new IntentFilter();
				filter.addAction(GetLocationService.LOCATION_SERVICE_IS_NOW_FREE_ACTION);

				receiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						
						if (GetLocationService.isBusy == false) {
	
							unregisterReceiver(receiver);
							Intent i = new Intent(serviceContext, GetLocationService.class);
							Bundle extras = new Bundle();
							extras.putString(
									GetLocationService.LOCATION_TYPE_REQUEST,
									GetLocationService.SIMPLE_GPS_LOCATION_TYPE);
							extras.putSerializable("CLASS_TO_SEND_BACK_TO",
									GetTrapsService.class);
							i.putExtras(extras);
							startService(i);
							stopSelf();
						}

					}
				};
				
				registerReceiver(receiver, filter);

			}
			
			
		}else{
			sendLocationToServer((SerializableLocation) intent.getExtras().getSerializable(GetLocationService.LOCATION_KEY));
		}



		return START_REDELIVER_INTENT;
	}
	
	@Override
	public void onDestroy() {
		try{
			unregisterReceiver(receiver);
		}catch (Exception e) {
			// TODO: handle exception
		}
		super.onDestroy();
	}

	/**
	 * This method is called only when a location fix has been received. it
	 * executes the main logic of the service, getting info from the server and
	 * writes it to a file. Then it send an intent to TrapMonitorService
	 * 
	 * @param i
	 */
	protected void sendLocationToServer(SerializableLocation currentLocation) {

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "\t\t\tlocation is:\n"
				+ currentLocation.toString());

		// Turn the current location into JSON format for easy sending to the
		// server

		String toSend = writeInfoToJsonString(currentLocation);

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "Sending " + toSend
				+ " to server to get traps in local area");

		// Upload the given info to the server and get the JSON response.

		InputStream jsonStream = getTrapLocationsFromServer(toSend);
		
		BufferedReader br = new BufferedReader(new InputStreamReader(jsonStream));

		StringBuilder sb = new StringBuilder();

		String line;
		try {
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
				"got raw info from server");

		// take serviceContext JSON response and turn it into a TrapLocations object

		TrapLocations trapLocations = jsonStringToTrapLocation(sb.toString(),
				currentLocation);

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER,
				"\t\t\tParesed info is\n" + trapLocations.toString());

		// package the info up and send out a broadcast serviceContext this service is
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
		
		Intent intent = new Intent(this, TrapMonitorService.class);
		
		intent.putExtras(new Bundle());
		
		intent.getExtras().putSerializable(GetLocationService.LOCATION_KEY, currentLocation);
		
		startService(intent);
		

		// calls destroy
		stopSelf();
	}

	/**
	 * If anything goes wrong, send out a broadcast serviceContext says so.
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
	 * @param jsonString
	 *            JSON from server
	 * @param currentLocation
	 *            users loc
	 * @return nearby trap location in an object called TrapLocations
	 */
	private TrapLocations jsonStringToTrapLocation(String jsonString,
			SerializableLocation currentLocation) {
		
		TrapLocations trapLocations = new TrapLocations();
		
		JSONObject allTraps = null;
		try {
			allTraps = new JSONObject(jsonString);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		JSONArray traps = null;
		try {
			traps = allTraps.getJSONArray("traps");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			for(int i=0; i<(traps.length()); i++)
			{
			    JSONObject trap = traps.getJSONObject(i);
			    
			    JSONArray latLongArray = trap.getJSONArray("loc");
			    
			    trapLocations.addLocation(latLongArray.getDouble(0),latLongArray.getDouble(1), trap.getInt("accuracy"), trap.getInt("speed"), "server");
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		trapLocations.setOriginalLocation(currentLocation);
		trapLocations.setTimeStamp(System.currentTimeMillis());
			
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_CHECKER, "From Server: "+ trapLocations.toString());
		
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


}
