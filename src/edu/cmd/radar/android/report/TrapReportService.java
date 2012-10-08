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
import edu.cmd.radar.android.location.SerializableLocation;
import edu.cmd.radar.android.shake.ShakeListenerService;
import edu.cmd.radar.android.ui.MainSettingsActivity;

public class TrapReportService extends Service {

	/**
	 * Indicates if the service is running or not.
	 */

	private BroadcastReceiver receiver;
	private static long timeOriginallyReported;
	private Context serviceContext = this;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT,
				"onStart of trapReportService called.");

		if (intent.getExtras().getSerializable(GetLocationService.LOCATION_KEY) == null) {
			
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT,
					"No location passed");

			timeOriginallyReported = intent.getExtras().getLong(
					ShakeListenerService.TIME_REPORTED_PREF_KEY, -1);
			
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT,
					"Original time: "+timeOriginallyReported);

			if (timeOriginallyReported == -1) {
				Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT,
						"timeOriginallyReported == -1, this is very bad");
			}

			if (GetLocationService.isBusy == false) {
				
				Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT,
						"Location Service is free, sending it intent");

				Intent i = new Intent(this, GetLocationService.class);
				Bundle extras = new Bundle();
				extras.putString(GetLocationService.LOCATION_TYPE_REQUEST,
						GetLocationService.SIMPLE_GPS_LOCATION_TYPE);
				extras.putSerializable("CLASS_TO_SEND_BACK_TO",
						TrapReportService.class);
				i.putExtras(extras);
				startService(i);
				stopSelf();

			} else {
				
				Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT,
						"Location service is not free, registering reciever so when it is, we can use it");

				IntentFilter filter = new IntentFilter();
				filter.addAction(GetLocationService.LOCATION_SERVICE_IS_NOW_FREE_ACTION);

				receiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						
						if (GetLocationService.isBusy == false) {
						
							Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT,
									"Recieved broadcast serviceContext location service is free and is not busy, using it now.");
	
							unregisterReceiver(receiver);
							Intent i = new Intent(serviceContext, GetLocationService.class);
							Bundle extras = new Bundle();
							extras.putString(
									GetLocationService.LOCATION_TYPE_REQUEST,
									GetLocationService.SIMPLE_GPS_LOCATION_TYPE);
							extras.putSerializable("CLASS_TO_SEND_BACK_TO",
									TrapReportService.class);
							i.putExtras(extras);
							startService(i);
							stopSelf();
						
						}

					}
				};
				registerReceiver(receiver, filter);
			}
 
		} else {
			
			Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT,
					"Location was passed, sending to uploader");
			
			startUploader((SerializableLocation) intent.getExtras()
					.getSerializable(GetLocationService.LOCATION_KEY), timeOriginallyReported);
		}

		return START_REDELIVER_INTENT;
	}

	@Override
	public void onDestroy() {
		try {
			unregisterReceiver(receiver);
		} catch (Exception e) {
		}
		super.onDestroy();
	}

	protected void startUploader(SerializableLocation trapLocation,
			long timeReported) {
		
		Intent serviceIntent = new Intent(this,
				TrapReportUploadingService.class);
		Bundle b = new Bundle();

		// send loc to next service
		b.putSerializable(GetLocationService.LOCATION_KEY, trapLocation);

		// send original time of shake to next service
		b.putLong("TIME_REPORTED", timeReported);
		
		Log.d(MainSettingsActivity.LOG_TAG_TRAP_REPORT,
				""+timeReported);

		serviceIntent.putExtras(b);
		this.startService(serviceIntent);

		// calls destroy
		stopSelf();
	}
}
