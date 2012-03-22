package edu.cmd.radar.android.check;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TrapCheckReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent serviceIntent = new Intent(context, TrapCheckWakeUpService.class);
		serviceIntent.putExtras(intent);
		context.startService(serviceIntent);
		

	}

}
