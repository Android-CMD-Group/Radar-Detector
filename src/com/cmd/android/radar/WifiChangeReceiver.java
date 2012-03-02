package com.cmd.android.radar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;


public class WifiChangeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
			NetworkInfo netInfo = intent
					.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			if (!netInfo.isConnected()) {
				//TODO: launch service to check if the user is driving or not
			}
		}
	}

}
