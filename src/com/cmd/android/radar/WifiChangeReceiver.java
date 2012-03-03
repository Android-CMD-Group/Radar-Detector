package com.cmd.android.radar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
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
	
	// If we detect that we have connected back to wifi during sleep time, we cancel the timer. 
	
	// there is also a check to make sure the service is not called when it exists already.

	// NOTE:
	// NETWORK_STATE_CHANGED_ACTION is only broadcast when:
	// a) the user connects
	// to Wifi OR
	// b) when the user disconnects due to the signal fading (ie the
	// user drives away from the wifi signal)
	@Override
	public void onReceive(Context context, Intent originalIntent) {

		Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING,
				"Received Broadcast in WifichangeReceiver");

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
			if (!(netInfo.isConnected() || netInfo.isConnectedOrConnecting())) {

				if (doesPendingIntentExist(context) != null) {
					Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING,
							"Broadcast indicates disconnected, but alarm already exists...");
					return;
				}

				Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING,
						"Launching service");
				// Launch the DriveListenerService
				Intent serviceIntent = new Intent(context,
						DriveListenerService.class);
				context.startService(serviceIntent);

			} else if (netInfo.isConnected()){

				Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING,
						"Broadcast indicates connected");
				PendingIntent pendingIntent = doesPendingIntentExist(context);
				if (pendingIntent != null) {
					Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING,
							"Canceling alarm");
					AlarmManager am = (AlarmManager) context
							.getSystemService(Context.ALARM_SERVICE);
					am.cancel(pendingIntent);
					pendingIntent.cancel();

				}else{
					Log.d(MainSettingsActivity.LOG_TAG_CHECK_FOR_DRIVING,
							"No alarm to cancel");
				}

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

	private PendingIntent doesPendingIntentExist(Context context) {
		String pname = "com.cmd.android.radar";
		// manufacture an appropriate context
		Context mycontext = null;
		try {
			mycontext = context.createPackageContext(pname,
					Context.CONTEXT_IGNORE_SECURITY);
		} catch (NameNotFoundException e) {
			// handle exception here
			e.printStackTrace();
		}
		// and generate a pending intent
		
		Intent intentForNextFix = new Intent(
				"com.cmd.android.radar.WifiChangeReceiver");

		intentForNextFix.setAction(DriveListenerService.TIMER_FOR_LOCATION_SLEEP);
		
		PendingIntent pi = PendingIntent.getBroadcast(mycontext, 0, intentForNextFix, PendingIntent.FLAG_NO_CREATE);
		return pi;
	}

}
