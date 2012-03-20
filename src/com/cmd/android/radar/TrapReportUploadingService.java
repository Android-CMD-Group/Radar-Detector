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
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Log;

import com.google.gson.stream.JsonWriter;

import edu.cmd.radar.android.location.SerializableLocation;
import edu.cmd.radar.android.shake.ShakeListenerService;
import edu.cmd.radar.android.ui.MainSettingsActivity;

public class TrapReportUploadingService extends IntentService {

	//change this to appropriate uri
	private static final String TRAP_REPORT_URI = "http://173.58.181.173:1188/Radar-Server/trap";
	public TrapReportUploadingService() {
		super("TrapReportUploadingService");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT, "Handling Intent");
		Bundle b = intent.getExtras();
		SerializableLocation location = (SerializableLocation) b
				.getSerializable(TrapLocationService.LOCATION_KEY);
		
		StringWriter writer = new StringWriter();
		JsonWriter jsonWriter = new JsonWriter(writer);
		try {
			jsonWriter.beginObject();
			jsonWriter.name("loc");
			jsonWriter.beginArray();
			jsonWriter.value(location.getLatitude());
			jsonWriter.value(location.getLongitude());

			jsonWriter.endArray();
			
			if(location.hasAccuracy()){
				jsonWriter.name("accuracy").value(location.getAccuracy());
			}else{
				jsonWriter.name("accuracy").nullValue();
			}
			
			if(location.hasSpeed()){
				jsonWriter.name("speed").value(location.getSpeed());
			}else{
				jsonWriter.name("speed").nullValue();
			}
			
			if (location.hasBearing()){
				jsonWriter.name("bearing").value(location.getBearing());
			}else{
				jsonWriter.name("bearing").nullValue();
			}
			
			jsonWriter.name("timeReported").value(
					b.getLong(ShakeListenerService.TIME_REPORTED_PREF_KEY));
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
