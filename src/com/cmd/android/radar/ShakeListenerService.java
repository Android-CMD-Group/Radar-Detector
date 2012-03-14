package com.cmd.android.radar;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

public class ShakeListenerService extends Service implements Shaker.Callback
{
	public static final String TIME_REPORTED_PREF_KEY = "TIME_REPORTED_PREF_KEY";
	private Shaker shaker = null;
	private Vibrator vib = null;

	@Override
	public void onCreate()
	{
		Log.d(MainSettingsActivity.LOG_TAG_SHAKE_LISTENER, "ShakeListenerService created.");
		super.onCreate();
		this.vib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
		this.shaker = new Shaker(this, this);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.d(MainSettingsActivity.LOG_TAG_SHAKE_LISTENER, "ShakeListenerService onStartCommand()");
		return super.onStartCommand(intent, flags, startId);
	}

	/**
	 * Called when the service is destroyed.
	 * Must close the shaker.
	 */
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		this.shaker.close();
	}

	/**
	 * Perform action when shaking begins.
	 */
	public void shakingStarted() 
	{
		vib.vibrate(700);
		Log.d(MainSettingsActivity.LOG_TAG_SHAKE_LISTENER, "Shaking started.");
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT, "Grabbing location");
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

	/**
	 * Vibrate and display a toast when shake stops
	 */
	public void shakingStopped()
	{
		Log.d(MainSettingsActivity.LOG_TAG_SHAKE_LISTENER, "Shaking stopped.");
		Context context = getApplicationContext();
		CharSequence text = "Shake Detected";
		int duration = Toast.LENGTH_SHORT;
		Toast.makeText(context, text, duration).show();
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
}