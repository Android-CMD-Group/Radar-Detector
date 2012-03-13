package com.cmd.android.radar;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

public class ShakeListenerService extends Service implements Shaker.Callback
{
	static final String LOG_TAG = "ShakeListenerService";
	private static final double THRESHOLD = 2.25;
	private Shaker shaker = null;
	private Vibrator vib = null;

	@Override
	public void onCreate()
	{
		Log.d(LOG_TAG, "ShakeListenerService created.");
		super.onCreate();
		this.vib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
		this.shaker = new Shaker(this, this);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.d(LOG_TAG, "ShakeListenerService onStartCommand()");
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
		Log.d(LOG_TAG, "Shaking started.");
	}

	/**
	 * Vibrate and display a toast when shake stops
	 */
	public void shakingStopped()
	{
		Log.d(LOG_TAG, "Shaking stopped.");
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