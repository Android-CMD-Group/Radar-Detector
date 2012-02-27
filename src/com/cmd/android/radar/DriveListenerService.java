package com.cmd.android.radar;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class DriveListenerService extends Service {

	private static final int NUMBER_OF_FIXES_THRESHOLD = 10;
	private BroadcastReceiver wifiChangedReceiver;
	private LocationManager locationManager;
	LocationListener locationListener;
	LocationDecider locationDecider;
	Location bestLastLocation;
	Handler handler;

	@Override
	public IBinder onBind(Intent intent) {
		// No need to use this yet, the service is so far, unbound
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(MainSettingsActivity.LOG_TAG,
				"DriveListenerService onStartCommand Called");
		// this is for testing without needing to walk away from wifi. delete it soon.
		showSomeMessage("Recieved wifi update");
		locationDecider = new LocationDecider();
		populateLocationList();
		
		while((locationDecider.numberOfLocations() < NUMBER_OF_FIXES_THRESHOLD)){
			//do nothing until we have enough fixes
		}
		
		bestLastLocation = locationDecider.getBestLocation();
		showSomeMessage("best fix at: " + bestLastLocation.getLongitude() + " " + bestLastLocation.getLatitude());
		
		handler = new Handler();
		class MyRunner implements Runnable {
			private float LOWEST_SPEED_THRESHOLD;
			private DriveListenerService caller;

			public MyRunner(DriveListenerService mCaller) {
				caller = mCaller;
			}

			public void run() {
				locationDecider = new LocationDecider();
				populateLocationList();
				
				while(!(locationDecider.numberOfLocations() < NUMBER_OF_FIXES_THRESHOLD)){
					//do nothing until we have enough fixes
				}
				
				Location bestNewLocation = locationDecider
						.getBestLocation();
				
				showSomeMessage("best fix at: " + bestNewLocation.getLongitude() + " " + bestNewLocation.getLatitude());
				if (isDriving(bestLastLocation, bestNewLocation)) {
					
				}
			}

			private boolean isDriving(
					Location bestLastLocation,
					Location bestNewLocation) {
				// 2680 is 20 miles per hour in meters per
				// minute after 5 minutes
				LOWEST_SPEED_THRESHOLD = 2680;
				float distance = bestLastLocation
						.distanceTo(bestNewLocation);
				float speed = distance
						/ (bestNewLocation.getTime() - bestLastLocation
								.getTime());
				if (distance > LOWEST_SPEED_THRESHOLD) {
					Toast.makeText(
							caller,
							"You're driving at speed: " + speed,
							Toast.LENGTH_LONG).show();
					return true;
				}
				return false;
			}
		}

		handler.postDelayed(new MyRunner(
				DriveListenerService.this), 50000);
		
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onCreate() {
		Log.d(MainSettingsActivity.LOG_TAG, "DriveListenerService created");
		super.onCreate();

		// Register a Broadcast Receiver which listens for a wifi connect OR
		// losing a wifi signal (NOT manually disconnecting
		final IntentFilter theFilter = new IntentFilter();
		theFilter.addAction("android.net.wifi.STATE_CHANGE");

		wifiChangedReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {

				// Only calls with disconnects
				// change this back to the lower example on
				// http://stackoverflow.com/questions/5365395/android-net-wifi-state-change-not-triggered-on-wifi-disconnect
				 if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
			          NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			          if( netInfo.isConnected()){
						showSomeMessage("Recieved wifi update");
						locationDecider = new LocationDecider();
						populateLocationList();
						
						while(!(locationDecider.numberOfLocations() < NUMBER_OF_FIXES_THRESHOLD)){
							//do nothing until we have enough fixes
						}
						
						bestLastLocation = locationDecider.getBestLocation();
						showSomeMessage("best fix at: " + bestLastLocation.getLongitude() + " " + bestLastLocation.getLatitude());
						
						handler = new Handler();
						class MyRunner implements Runnable {
							private float LOWEST_SPEED_THRESHOLD;
							private DriveListenerService caller;

							public MyRunner(DriveListenerService mCaller) {
								caller = mCaller;
							}

							public void run() {
								locationDecider = new LocationDecider();
								populateLocationList();
								
								while(!(locationDecider.numberOfLocations() < NUMBER_OF_FIXES_THRESHOLD)){
									//do nothing until we have enough fixes
								}
								
								Location bestNewLocation = locationDecider
										.getBestLocation();
								
								showSomeMessage("best fix at: " + bestNewLocation.getLongitude() + " " + bestNewLocation.getLatitude());
								if (isDriving(bestLastLocation, bestNewLocation)) {
									
								}
							}

							private boolean isDriving(
									Location bestLastLocation,
									Location bestNewLocation) {
								// 2680 is 20 miles per hour in meters per
								// minute after 5 minutes
								LOWEST_SPEED_THRESHOLD = 2680;
								float distance = bestLastLocation
										.distanceTo(bestNewLocation);
								float speed = distance
										/ (bestNewLocation.getTime() - bestLastLocation
												.getTime());
								if (distance > LOWEST_SPEED_THRESHOLD) {
									Toast.makeText(
											caller,
											"You're driving at speed: " + speed,
											Toast.LENGTH_LONG).show();
									return true;
								}
								return false;
							}
						}

						handler.postDelayed(new MyRunner(
								DriveListenerService.this), 50000);

					} else {
						if (locationManager != null) {
							locationManager.removeUpdates(locationListener);
						}
					}
				}

			}
		};

		// Register the receiver
		this.registerReceiver(this.wifiChangedReceiver, theFilter);
	}

	private void populateLocationList() {

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the network location
				// provider.
				pushToBestLoctionDecider(location);
				showSomeMessage("Got fix at " + location.getLongitude() + " " + location.getLatitude());
			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}
		};

		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
		
	}

	protected void pushToBestLoctionDecider(Location location) {
		locationDecider.addPossibleLocation(location);

		if (locationDecider.numberOfLocations() > NUMBER_OF_FIXES_THRESHOLD) {
			locationManager.removeUpdates(locationListener);
		}

	}

	private void showSomeMessage(String message) {
		Log.d(MainSettingsActivity.LOG_TAG,
				message);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// Unregister to save overhead
		this.unregisterReceiver(this.wifiChangedReceiver);
		locationManager.removeUpdates(locationListener);
	}
}
