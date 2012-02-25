package com.cmd.android.radar;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class DriveListenerService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this,"Service created ...", Toast.LENGTH_LONG).show();
		Log.d(MainSettingsActivity.LOG_TAG, "DriveListenerService onStartCommand Called");
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onCreate() {
		Toast.makeText(this,"Service created ...", Toast.LENGTH_LONG).show();
		Log.d(MainSettingsActivity.LOG_TAG, "DriveListenerService created");
		super.onCreate();

	}
	
	

}
