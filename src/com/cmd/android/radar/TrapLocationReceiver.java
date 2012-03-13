package com.cmd.android.radar;
import android.R.integer;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;


public class TrapLocationReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent originalIntent) {
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		
		Intent intent = new Intent("com.cmd.android.radar.TRAP_LOCATION_RECEIVED");
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
		    0, intent, 0);
		
		locationManager.removeUpdates(pendingIntent);
		
		
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT, "Received Location in TrapLocationReceiver");
		Intent serviceIntent = new Intent(context,
				TrapReportUploadingService.class);
		
		// The extras passed to the receiver should be passed on. It
		// contains the Location and the time of the shake.
		serviceIntent.putExtras(originalIntent.getExtras());
		context.startService(serviceIntent);
	}

}
