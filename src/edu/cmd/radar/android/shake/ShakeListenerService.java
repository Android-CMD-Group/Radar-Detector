package edu.cmd.radar.android.shake;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;


import edu.cmd.radar.android.report.TrapLocationService;
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
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(MainSettingsActivity.LOG_TAG_SHAKE_LISTENER,
				"ShakeListenerService onStartCommand()");
		return super.onStartCommand(intent, flags, startId);
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
		if (!TrapLocationService.isRunning) {
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT, "Grabbing location");
			
			Intent i = new Intent(this, TrapLocationService.class);
			Bundle b = new Bundle();
			b.putLong(TIME_REPORTED_PREF_KEY, System.currentTimeMillis());
			i.putExtras(b);
			startService(i);
		}else{
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT, "Already grabbing location");
			Toast.makeText(this, "Already grabbing location", Toast.LENGTH_LONG).show();
		}

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