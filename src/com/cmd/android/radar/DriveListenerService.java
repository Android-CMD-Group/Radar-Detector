package com.cmd.android.radar;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class DriveListenerService extends Service {
	
	private BroadcastReceiver wifiChangedReceiver;

	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(MainSettingsActivity.LOG_TAG, "DriveListenerService onStartCommand Called");
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onCreate() {
		Log.d(MainSettingsActivity.LOG_TAG, "DriveListenerService created");
		super.onCreate();
		final IntentFilter theFilter = new IntentFilter();
        theFilter.addAction("android.net.wifi.STATE_CHANGE");
        
        wifiChangedReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				
			}
		};
		this.registerReceiver(this.wifiChangedReceiver, theFilter);
	}
	
	@Override
    public void onDestroy() {
        super.onDestroy();
        
        this.unregisterReceiver(this.wifiChangedReceiver);
    }

	



}
