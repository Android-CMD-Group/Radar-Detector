package com.cmd.android.radar;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

public class DriveListenerService extends Service {

	private BroadcastReceiver wifiChangedReceiver;

	@Override
	public IBinder onBind(Intent intent) {
		// No need to use this yet, the service is so far, unbound
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(MainSettingsActivity.LOG_TAG,
				"DriveListenerService onStartCommand Called");
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onCreate() {
		Log.d(MainSettingsActivity.LOG_TAG, "DriveListenerService created");
		super.onCreate();

		// Register a Broadcast Receiver which listens for a wifi connect OR
		// losing a wifi signal (NOT manually disconnecting
		final IntentFilter theFilter = new IntentFilter();
		theFilter.addAction("android.net.wifi.STATE_CHANGE");

		wifiChangedReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				
				// Only calls with disconnects
				if (intent.getAction().equals(
						WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
					NetworkInfo netInfo = intent
							.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
					if (netInfo.isConnected()) {
						//TODO: Check whether the user is driving or not.
					}
				}

			}
		};
		
		// Register the receiver
		this.registerReceiver(this.wifiChangedReceiver, theFilter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// Unregister to save overhead
		this.unregisterReceiver(this.wifiChangedReceiver);
	}
}
