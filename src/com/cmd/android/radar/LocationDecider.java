package com.cmd.android.radar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import android.location.Location;

public class LocationDecider {

	ArrayList<Location> possibleLocationList;
	ArrayList<Double> latList;
	ArrayList<Double> longList;

	public LocationDecider() {

		possibleLocationList = new ArrayList<Location>();
		latList = new ArrayList<Double>();
		longList = new ArrayList<Double>();
	}

	public SerializableLocation getBestLocationInSerializableForm() {

		return new SerializableLocation(getBestLocation());
	}

	public Location getBestLocation() {

		Collections.sort(latList);
		Collections.sort(longList);
		Double medianLat = latList.get(latList.size() / 2);
		Double medianLong = longList.get(longList.size() / 2);
		Location returnLocation = new Location("LOCATION_DECIDER");
		returnLocation.setLatitude(medianLat);
		returnLocation.setLongitude(medianLong);
		returnLocation.setTime(getLatestLocationTime());

		return returnLocation;
	}

	public void addPossibleLocation(Location location) {
		latList.add(location.getLatitude());
		longList.add(location.getLongitude());
		possibleLocationList.add(location);
	}

	public int numberOfLocations() {
		return possibleLocationList.size();
	}

	private long getLatestLocationTime() {
		long latestTime = 0;
		for (Location location : possibleLocationList) {
			if (location.getTime() > latestTime) {
				latestTime = location.getTime();
			}
		}
		return latestTime;
	}

}
