package com.cmd.android.radar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WifiChangeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(MainSettingsActivity.LOG_TAG, "Received");
		if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
			NetworkInfo netInfo = intent
					.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			if (netInfo.isConnected()) {
				Log.d(MainSettingsActivity.LOG_TAG, "Launching service");
				Intent serviceIntent = new Intent(context, 
						DriveListenerService.class);
				context.startService(serviceIntent);

			}
		}
	}
}
