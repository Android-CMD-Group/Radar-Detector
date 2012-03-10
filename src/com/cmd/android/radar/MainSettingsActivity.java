package com.cmd.android.radar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainSettingsActivity extends Activity {

	public static final String LOG_TAG = "Radar-Detector";
	
	// Buttons for starting and stopping the ShakeListenerService
	private Button startShakeListener;
	private Button stopShakeListener;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.d(MainSettingsActivity.LOG_TAG, "MainSettingsActivity created");
	}
	
	// Starts the ShakeListenerService
	public void launchShakeListener(View v) {
		startService(new Intent(this, ShakeListenerService.class));
	}
	
	// Kills the ShakeListenerService.
	// Look into binding to the activity.
	public void killShakeListener(View v) {
		this.stopService(new Intent(this, ShakeListenerService.class));
	}
}