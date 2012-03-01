package com.cmd.android.radar;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

public class WifiChangeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent originalIntent) {
		
		if(isMyServiceRunning(context)){
			return;
		}
		
		Log.d(MainSettingsActivity.LOG_TAG, "Received Broadcast");
		if (originalIntent.getAction().equals(
				WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
			Log.d(MainSettingsActivity.LOG_TAG, "Broadcast is NETWORK_STATE_CHANGED_ACTION");
			NetworkInfo netInfo = originalIntent
					.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			if (netInfo.isConnected()) {
				Log.d(MainSettingsActivity.LOG_TAG, "Launching service");
				Intent serviceIntent = new Intent(context,
						DriveListenerService.class);
				context.startService(serviceIntent);

			}
		} else if (originalIntent.getAction().equals(
				DriveListenerService.TIMER_FOR_LOCATION_SLEEP)) {
			Log.d(MainSettingsActivity.LOG_TAG, "Broadcast is TIMER_FOR_LOCATION_SLEEP");
			
			Intent serviceIntent = new Intent(context,
					DriveListenerService.class);
			
			serviceIntent.putExtras(originalIntent
					.getExtras());
			
			context.startService(serviceIntent);
		}
	}
	
	private boolean isMyServiceRunning(Context context) {
	    ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (DriveListenerService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}

}
