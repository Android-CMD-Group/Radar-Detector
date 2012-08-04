package edu.cmd.radar.android.check;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GetTrapsReceiver extends BroadcastReceiver {

	public static final String GET_TRAPS_ACTION = "edu.cmd.radar.android.check.GET_TRAPS_ACTION";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		//TODO: verify that the location, speed, and bearing info is valid
		
		Intent serviceIntent = new Intent(context, GetTrapsService.class);
		serviceIntent.putExtras(intent);
		context.startService(serviceIntent);

	}

}
