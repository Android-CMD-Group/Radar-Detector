package edu.cmd.radar.android.ui;

import com.cmd.android.radar.R;

import edu.cmd.radar.android.shake.ShakeListenerService;

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

	public static final String LOG_TAG_SHAKE_LISTENER = "ShakeListenerService";

	/**
	 * Log tag used to show processes of finding out whether user is driving or
	 * not
	 */
	public static final String LOG_TAG_CHECK_FOR_DRIVING = "Radar-Detector.checkForDriving";


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