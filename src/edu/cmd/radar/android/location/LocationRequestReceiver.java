package edu.cmd.radar.android.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LocationRequestReceiver extends BroadcastReceiver {

	public static final String GET_LOCATION_ACTION = "edu.cmd.radar.android.location.GET_LOCATION_ACTION";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		Intent serviceIntent = new Intent(context, GetLocationService.class);
		serviceIntent.putExtras(intent);
		context.startService(serviceIntent);

	}

}
