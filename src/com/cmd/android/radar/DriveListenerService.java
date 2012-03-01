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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class DriveListenerService extends Service {


	@Override
	public IBinder onBind(Intent intent) {
		// No need to use this yet, the service is so far, unbound
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(MainSettingsActivity.LOG_TAG,
				"DriveListenerService onStartCommand Called");
		// this is for testing without needing to walk away from wifi. delete it soon.
		return super.onStartCommand(intent, flags, startId);

	}

	@Override
	public void onCreate() {
		Log.d(MainSettingsActivity.LOG_TAG, "DriveListenerService created");
		super.onCreate();
	}
	
//	private boolean isDriving(
//			Location bestLastLocation,
//			Location bestNewLocation) {
//		// 2680 is 20 miles per hour in meters per
//		// minute after 5 minutes
//		LOWEST_SPEED_THRESHOLD = 2680;
//		float distance = bestLastLocation
//				.distanceTo(bestNewLocation);
//		float speed = distance
//				/ (bestNewLocation.getTime() - bestLastLocation
//						.getTime());
//		if (distance > LOWEST_SPEED_THRESHOLD) {
//			Toast.makeText(
//					caller,
//					"You're driving at speed: " + speed,
//					Toast.LENGTH_LONG).show();
//			return true;
//		}
//		return false;
//	}

	@Override
	public void onDestroy() {
		super.onDestroy();

	}
}
