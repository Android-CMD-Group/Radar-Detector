package edu.cmd.radar.android.report;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Log;

import com.google.gson.stream.JsonWriter;

import edu.cmd.radar.android.location.SerializableLocation;
import edu.cmd.radar.android.location.GetLocationService;
import edu.cmd.radar.android.shake.ShakeListenerService;
import edu.cmd.radar.android.ui.MainSettingsActivity;

/**
 * Service that handles intents one at a time until there are no more. Uploads
 * JSON to the server.
 */
public class TrapReportUploadingService extends IntentService {

	/**
	 * URI of server to upload JSON info to
	 */
	private static final String TRAP_REPORT_URI = "http://ec2-54-245-44-240.us-west-2.compute.amazonaws.com/trapreport";

	public TrapReportUploadingService() {
		// name of service
		super("TrapReportUploadingService");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT, "onStart of trapUploadingService called.");
		Bundle b = intent.getExtras();

		// location info
		SerializableLocation location = (SerializableLocation) b
				.getSerializable(GetLocationService.LOCATION_KEY);
		
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT, location.toString());
		

		// using GSON library, convert location into a JSON object string
		StringWriter writer = new StringWriter();
		JsonWriter jsonWriter = new JsonWriter(writer);
		try {
			jsonWriter.beginObject();
			jsonWriter.name("loc");
			jsonWriter.beginArray();
			jsonWriter.value(location.getLatitude());
			jsonWriter.value(location.getLongitude());

			jsonWriter.endArray();

			// not all location info may be valid
			if (location.hasAccuracy()) {
				jsonWriter.name("accuracy").value(location.getAccuracy());
			} else {
				jsonWriter.name("accuracy").nullValue();
			}

			if (location.hasSpeed()) {
				jsonWriter.name("speed").value(location.getSpeed());
			} else {
				jsonWriter.name("speed").nullValue();
			}

			if (location.hasBearing()) {
				jsonWriter.name("bearing").value(location.getBearing());
			} else {
				jsonWriter.name("bearing").nullValue();
			}

			// send the original time reported
			jsonWriter.name("timeReported").value(
					b.getLong("TIME_REPORTED"));

			// as well as the time that the first location was fixed
			jsonWriter.name("timeOfLocation").value(location.getTime());

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
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT, toSend);

		// encode for sending to server
		StringEntity se = null;
		try {
			se = new StringEntity(toSend);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		// use http client to make a POST request and put the JSON as the
		// message
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpPost httpost = new HttpPost(TRAP_REPORT_URI);
		httpost.setEntity(se);
		httpost.setHeader("Content-type", "application/json");

		// TODO Handle case when the phone does not have Internet. Write info to
		// file? send later.

		// Do nothing with the response for now. Later may want to save the info
		// for later and re-send
		ResponseHandler responseHandler = new BasicResponseHandler();
		try {
			httpclient.execute(httpost, responseHandler);
		} catch (ClientProtocolException e) {

			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
