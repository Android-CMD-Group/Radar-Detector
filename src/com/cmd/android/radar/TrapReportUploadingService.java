package com.cmd.android.radar;

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
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Log;

import com.google.gson.stream.JsonWriter;

public class TrapReportUploadingService extends IntentService {

	//change this to appropriate uri
	private static final String TRAP_REPORT_URI = "http://173.58.181.173:1188/Radar_Server/trap";

	public TrapReportUploadingService() {
		super("TrapReportUploadingService");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT, "Handling Intent");
		Bundle b = intent.getExtras();
		Location location = (Location) b
				.get(android.location.LocationManager.KEY_LOCATION_CHANGED);

		StringWriter writer = new StringWriter();
		JsonWriter jsonWriter = new JsonWriter(writer);
		try {
			jsonWriter.beginObject();
			jsonWriter.name("loc");
			jsonWriter.beginArray();
			jsonWriter.value(location.getLatitude());
			jsonWriter.value(location.getLongitude());

			jsonWriter.endArray();
			jsonWriter.name("speed").value(location.getSpeed());
			jsonWriter.name("bearing").value(location.getBearing());
			jsonWriter.name("timeReported").value(
					b.getLong(MainSettingsActivity.TIME_REPORTED_PREF_KEY));
			jsonWriter.name("timeOfLocation").value(location.getTime());
			jsonWriter.name("id").value(
					Secure.getString(getContentResolver(), Secure.ANDROID_ID));

			jsonWriter.endObject();
		} catch (IOException e) {
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT, "Problem writing JSON");
			e.printStackTrace();
		}
		String toSend = writer.toString();
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT, toSend);
		StringEntity se = null;
		try {
			se = new StringEntity(toSend);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpPost httpost = new HttpPost(TRAP_REPORT_URI);
		httpost.setEntity(se);
	    httpost.setHeader("Content-type", "application/json");
	    
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
