package edu.cmd.radar.android.check;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

public class TrapCheckServerPullService extends Service {

	private static final String TRAP_LOCATIONS_INFO_FILE_NAME = "TRAP_LOCATIONS_INFO_FILE_NAME";
	private static final String TRAP_CHECK_URI = "http://domain/servlet/get";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		
		InputStream jsonStream = getTrapLocationsFromServer();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		byte[] buffer = new byte[1024];
	    int len;
	    try {
			while ((len = jsonStream.read(buffer)) > -1 ) {
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
	    
	    InputStream streamForFile = new ByteArrayInputStream(baos.toByteArray()); 
	    
	    
		FileOutputStream file = null;
		try {
			file = this.openFileOutput(TRAP_LOCATIONS_INFO_FILE_NAME, Context.MODE_PRIVATE);
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
		
		InputStream streamForObject = new ByteArrayInputStream(baos.toByteArray());
		
		TrapLocations trapLocations = streamToTrapLocation(streamForObject);
		
		try {
			streamForFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			streamForObject.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			baos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		long timeToSleep = getTimeToSleep(trapLocations);
		
		
		return START_STICKY;
	}

	private long getTimeToSleep(TrapLocations trapLocations) {
		
		ArrayList<Float> distances = (ArrayList<Float>) trapLocations.getDistanceList();
		Float dis = Collections.min(distances);
		ArrayList<Location> locList = (ArrayList<Location>) trapLocations.getLocationList();
		Location closest = locList.get(distances.indexOf(dis));
		
		
		
		return 0;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}

	private InputStream getTrapLocationsFromServer() {

		InputStream stream = null;
		String result = "";

		// http post
		try {
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(TRAP_CHECK_URI);
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			stream = entity.getContent();

		} catch (Exception e) {
			Log.e("log_tag", "Error in http connection " + e.toString());
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

		return trapLocations;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
