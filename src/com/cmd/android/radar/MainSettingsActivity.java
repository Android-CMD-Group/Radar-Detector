package com.cmd.android.radar;

import android.app.Activity;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

public class MainSettingsActivity extends Activity {

	public static final String LOG_TAG = "Radar-Detector";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Log.d(MainSettingsActivity.LOG_TAG, "MainSettingsActivity created");
		
        
	}
}