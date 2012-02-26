package com.cmd.android.radar;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class DriveListenerService extends Service {

	private BroadcastReceiver wifiChangedReceiver;
	private LocationManager locationManager;
	private String provider;

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
						if (userIsDriving()) {
							// TODO: launch shake listener
						}
					}
				}

			}
		};

		// Register the receiver
		this.registerReceiver(this.wifiChangedReceiver, theFilter);
	}

	private boolean userIsDriving() {


		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		LocationListener locationListener = new LocationListener() {
		    public void onLocationChanged(Location location) {
		    	// Called when a new location is found by the network location provider.
		    	if (location.hasSpeed()) {
					
				}
		    }
		    public void onStatusChanged(String provider, int status, Bundle extras) {}

		    public void onProviderEnabled(String provider) {}

		    public void onProviderDisabled(String provider) {}
		  };
		  
		  locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 40, locationListener);

		

		return false;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// Unregister to save overhead
		this.unregisterReceiver(this.wifiChangedReceiver);
	}
}
