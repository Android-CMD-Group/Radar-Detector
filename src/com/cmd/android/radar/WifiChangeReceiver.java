package com.cmd.android.radar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * This class catches two types of broadcast. The first is from wifi when it
 * goes out of range, the other from an alarm set in DriverServiceListener after
 * a location is grabbed. In both situations the DriveListenerService is
 * launched. This Receiver object is killed after about 10 seconds so no real
 * work should be done here.
 * 
 * @author satshabad
 * 
 */
public class WifiChangeReceiver extends BroadcastReceiver {

	// This is called when a NETWORK_STATE_CHANGED_ACTION or
	// TIMER_FOR_LOCATION_SLEEP broadcast is received from the system. This
	// method will start up the DriveListenerService Service.

	// NOTE:
	// NETWORK_STATE_CHANGED_ACTION is only broadcast when:
	// a) the user connects
	// to Wifi OR
	// b) when the user disconnects due to the signal fading (ie the
	// user drives away from the wifi signal)
	@Override
	public void onReceive(Context context, Intent originalIntent) {

		Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING, "Received Broadcast in WifichangeReceiver");

		// Check to see if the onReceive method was called because of a wifi
		// disconnect.
		if (originalIntent.getAction().equals(
				WifiManager.NETWORK_STATE_CHANGED_ACTION)) {

			Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING,
					"Broadcast is NETWORK_STATE_CHANGED_ACTION");

			NetworkInfo netInfo = originalIntent
					.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

			// CHANGE THIS TO !netInfo.isConnected() to let the service start
			// when Wifi is disconnected instead of on connect. This is changed
			// to help testing.
			if (netInfo.isConnected()) {
				Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING, "Launching service");

				// Launch the DriveListenerService
				Intent serviceIntent = new Intent(context,
						DriveListenerService.class);
				context.startService(serviceIntent);

			}
			// check to see if the broadcast was a TIMER_FOR_LOCATION_SLEEP
			// action
		} else if (originalIntent.getAction().equals(
				DriveListenerService.TIMER_FOR_LOCATION_SLEEP)) {

			Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING,
					"Broadcast is TIMER_FOR_LOCATION_SLEEP");

			// Launch the DriverListenerService
			Intent serviceIntent = new Intent(context,
					DriveListenerService.class);

			// The extras passed to the receiver should be passed on. It
			// contains the previous location object.
			serviceIntent.putExtras(originalIntent.getExtras());
			context.startService(serviceIntent);
		}
	}

}
