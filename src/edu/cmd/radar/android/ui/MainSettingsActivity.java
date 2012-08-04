package edu.cmd.radar.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import edu.cmd.radar.android.R;
import edu.cmd.radar.android.check.GetTrapsService;
import edu.cmd.radar.android.check.TrapMonitorService;
import edu.cmd.radar.android.location.GetLocationService;
import edu.cmd.radar.android.location.LocationRequestReceiver;
import edu.cmd.radar.android.shake.ShakeListenerService;

public class MainSettingsActivity extends Activity {

	public static final String LOG_TAG = "Radar-Detector";
	/**
	 * Log tag used to show processes of finding out whether user is driving or
	 * not
	 */
	public static final String LOG_TAG_CHECK_FOR_DRIVING = "Radar-Detector.checkForDriving";
	public static final String LOG_TAG_TRAP_REPORT = "Radar-Detector.trapReport";
	public static final String LOG_TAG_SHAKE_LISTENER = "Radar-Detector.shakeListener";
	public static final String LOG_TAG_TRAP_CHECKER = "Radar-Detector.trapChecker";
	public static final String LOG_TAG_LOCATION = "Radar-Detector.location";


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.d(MainSettingsActivity.LOG_TAG, "MainSettingsActivity created");
		Intent broadcastIntent = new Intent();
		Bundle b = new Bundle();
		b.putString(GetLocationService.LOCATION_TYPE_REQUEST, GetLocationService.SPEED_AND_BEARING_LOCATION_TYPE);
		broadcastIntent.putExtras(b);
		broadcastIntent.setAction(LocationRequestReceiver.GET_LOCATION_ACTION);
		sendBroadcast(broadcastIntent);
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