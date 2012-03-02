package com.cmd.android.radar;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ShakeListenerService extends Service {
	
	// Do not need at the moment
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
    @Override
    public void onCreate() {
    	
    }

    // Called every time the client starts the service
    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(MainSettingsActivity.LOG_TAG, 
				"ShakeListenerService launched. (" + ShakeListenerService.class.getName() + ")");
		return START_STICKY;
	}
}
