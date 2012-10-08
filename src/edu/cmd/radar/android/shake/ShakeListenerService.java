package edu.cmd.radar.android.shake;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;


import edu.cmd.radar.android.location.GetLocationService;
import edu.cmd.radar.android.report.TrapReportService;
import edu.cmd.radar.android.ui.MainSettingsActivity;

public class ShakeListenerService extends Service implements Shaker.Callback {
	public static final String TIME_REPORTED_PREF_KEY = "TIME_REPORTED_PREF_KEY";
	private Shaker shaker = null;
	private Vibrator vib = null;

	@Override
	public void onCreate() {
		Log.d(MainSettingsActivity.LOG_TAG_SHAKE_LISTENER,
				"ShakeListenerService created.");
		super.onCreate();
		this.vib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
		this.shaker = new Shaker(this, this);
	}

	@Override

	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.d(MainSettingsActivity.LOG_TAG_SHAKE_LISTENER, "ShakeListenerService onStartCommand()");
		return START_STICKY;
	}

	/**
	 * Called when the service is destroyed. Must close the shaker.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.shaker.close();
	}

	/**
	 * Perform action when shaking begins.
	 */
	public void shakingStarted() {
		
		vib.vibrate(700);
		Log.d(MainSettingsActivity.LOG_TAG_SHAKE_LISTENER, "Shaking started.");
			
		// Get the location and send it to the server
		Intent i = new Intent(this, TrapReportService.class);
		Bundle b = new Bundle();
		
		// due to gps startup time, send the server the actual report time so it can calculate the actual location.
		b.putLong(TIME_REPORTED_PREF_KEY, System.currentTimeMillis());
		i.putExtras(b);
		startService(i);
		
	}

	/**
	 * Display a toast when shake stops
	 */
	public void shakingStopped() {
		Log.d(MainSettingsActivity.LOG_TAG_SHAKE_LISTENER, "Shaking stopped.");
		Toast.makeText(this, "Shaking stopped", Toast.LENGTH_LONG).show();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}