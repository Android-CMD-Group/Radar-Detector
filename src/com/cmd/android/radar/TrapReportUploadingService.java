package com.cmd.android.radar;

import java.io.StringWriter;

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

import com.google.gson.stream.JsonWriter;

public class TrapReportUploadingService extends IntentService {

	//change this to appropriate uri
	private static final String TRAP_REPORT_URI = "http://192.168.1.3:8080/etc";

	public TrapReportUploadingService() {
		super("TrapReportUploadingService");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onHandleIntent(Intent intent) {

		Bundle b = intent.getExtras();
		Location location = (Location) b
				.get(android.location.LocationManager.KEY_LOCATION_CHANGED);

		StringWriter writer = new StringWriter();
		JsonWriter jsonWriter = new JsonWriter(writer);
		jsonWriter.beginObject();
		jsonWriter.name("loc");
		jsonWriter.beginArray();
		jsonWriter.value(location.getLongitude());
		jsonWriter.value(location.getLatitude());
		jsonWriter.endArray();
		jsonWriter.name("speed").value(location.getSpeed());
		jsonWriter.name("bearing").value(location.getBearing());
		jsonWriter.name("time reported").value(
				b.getLong(ShakeListenerService.TIME_REPORTED_PREF_KEY));
		jsonWriter.name("time of location").value(location.getTime());
		jsonWriter.name("id").value(
				Secure.getString(getContentResolver(), Secure.ANDROID_ID));

		jsonWriter.endObject();
		String toSend = writer.toString();
		StringEntity se = new StringEntity(toSend);

		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpPost httpost = new HttpPost(TRAP_REPORT_URI);
		httpost.setEntity(se);
	    httpost.setHeader("Content-type", "application/json");
	    
	    ResponseHandler responseHandler = new BasicResponseHandler();
	    httpclient.execute(httpost, responseHandler);

	}

}
