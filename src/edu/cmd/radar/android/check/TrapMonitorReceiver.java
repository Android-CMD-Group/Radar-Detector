package edu.cmd.radar.android.check;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TrapMonitorReceiver extends BroadcastReceiver {

	public static final String CHECK_DISTANCE_FROM_TRAPS_ACTION = "edu.cmd.radar.android.check.CHECK_DISTANCE_FROM_TRAPS";

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent serviceIntent = new Intent(context, TrapMonitorService.class);
		serviceIntent.putExtras(intent);
		context.startService(serviceIntent);
	}

}
