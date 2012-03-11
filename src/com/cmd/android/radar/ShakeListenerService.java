package com.cmd.android.radar;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

public class ShakeListenerService extends Service implements Shaker.Callback {
	static final String LOG_TAG = "ShakeListenerService";
	private static final double THRESHOLD = 2.25;
	private Shaker shaker = null;
	private Vibrator vib = null;

	@Override
	public void onCreate() {
		super.onCreate();
		this.vib = (Vibrator)this.getSystemService(VIBRATOR_SERVICE);
		shaker = new Shaker(this, THRESHOLD, 1000, this);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		shaker.close();
	}

	public void shakingStarted() {
		Log.d(LOG_TAG, "Shaking started!");
	}

	public void shakingStopped() {
		Log.d(LOG_TAG, "Shaking stopped!");
		vib.vibrate(700);
		
		Context context = getApplicationContext();
		CharSequence text = "Shake Detected";
		int duration = Toast.LENGTH_SHORT;
		Toast.makeText(context, text, duration).show();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}