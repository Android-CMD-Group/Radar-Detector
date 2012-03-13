package com.cmd.android.radar;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class MainSettingsActivity extends Activity {

	/**
	 * Log tag used to show processes of finding out whether user is driving or
	 * not
	 */
	public static final String LOG_TAG_CHECK_FOR_DRIVING = "Radar-Detector.checkForDriving";
	public static final String LOG_TAG_TRAP_REPORT = "Radar-Detector.trapReport";
	public static final String TIME_REPORTED_PREF_KEY = "TIME_REPORTED_PREF_KEY";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING, getPackageName());
		long time = System.currentTimeMillis();
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		
		Intent intent = new Intent("com.cmd.android.radar.TRAP_LOCATION_RECEIVED");
		Bundle bundle = new Bundle();
		bundle.putLong(TIME_REPORTED_PREF_KEY, time);
		intent.putExtras(bundle);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
		    0, intent, 0);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, pendingIntent);
	}
}