package edu.cmd.radar.android.report;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import edu.cmd.radar.android.location.GetLocationService;
import edu.cmd.radar.android.location.LocationRequestReceiver;
import edu.cmd.radar.android.shake.ShakeListenerService;
import edu.cmd.radar.android.ui.MainSettingsActivity;

public class TrapReportService extends Service {

	/**
	 * Indicates if the service is running or not.
	 */
	public static boolean isRunning = false;
	private BroadcastReceiver receiver;
	private Intent originalIntent;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT, "onStart of trapReportService called.");
		originalIntent = intent;
		intent.getExtras().putString(GetLocationService.LOCATION_TYPE_REQUEST, GetLocationService.SPEED_AND_BEARING_LOCATION_TYPE);
		// lets system know not to restart this service
		TrapReportService.isRunning = true;
		IntentFilter filter = new IntentFilter();
		filter.addAction(LocationRequestReceiver.GET_LOCATION_ACTION);

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent i) {
				unregisterReceiver(receiver);
				startUploader(i);

			}
		};
		registerReceiver(receiver, filter);
		Intent i = new Intent(this, GetLocationService.class);
		startService(i);
		
		return START_REDELIVER_INTENT;
	}

	@Override
	public void onDestroy() {
		try {
			unregisterReceiver(receiver);
		} catch (Exception e) {
		}
		TrapReportService.isRunning = false;
		super.onDestroy();
	}

	protected void startUploader(Intent oldIntent) {
		Intent serviceIntent = new Intent(this,
				TrapReportUploadingService.class);
		Bundle b = new Bundle();

		// send loc to next service
		b.putSerializable(
				GetLocationService.LOCATION_KEY,
				oldIntent
						.getSerializableExtra(GetLocationService.LOCATION_KEY));

		// send original time of shake to next service. -1 is default return
		// value
		b.putLong(ShakeListenerService.TIME_REPORTED_PREF_KEY, originalIntent
				.getLongExtra(ShakeListenerService.TIME_REPORTED_PREF_KEY, -1));

		serviceIntent.putExtras(b);
		this.startService(serviceIntent);

		// calls destroy
		stopSelf();
	}
}
