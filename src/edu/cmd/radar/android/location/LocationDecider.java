package edu.cmd.radar.android.location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import android.location.Location;

/**
 * This class takes several locations and decides what might be the most
 * accurate one.
 * 
 * @author satshabad
 * 
 */
public class LocationDecider {

	/**
	 * List of possible locations
	 */
	ArrayList<Location> possibleLocationList;

	/**
	 * possible locations latitudes
	 */
	ArrayList<Double> latList;

	/**
	 * possible locations logitudes
	 */
	ArrayList<Double> longList;

	public LocationDecider() {

		possibleLocationList = new ArrayList<Location>();
		latList = new ArrayList<Double>();
		longList = new ArrayList<Double>();
	}

	/**
	 * Get the best possible location, but puts in custom serializable form for
	 * storage
	 * 
	 * @return the best location, ready for storage
	 */
	public SerializableLocation getBestLocationInSerializableForm() {

		return new SerializableLocation(getBestLocation());
	}

	/**
	 * Uses methods to pick the best possble location out of a list. Right no it
	 * uses the median lat combined with the median long. This is to avoid any
	 * initial outliers in the location set.
	 * 
	 * @return the best location
	 */
	public Location getBestLocation() {

		// sort
		Collections.sort(latList);
		Collections.sort(longList);

		// get median
		Double medianLat = latList.get(latList.size() / 2);
		Double medianLong = longList.get(longList.size() / 2);

		// return new location with median lat and long
		Location returnLocation = new Location("LOCATION_DECIDER");
		returnLocation.setAccuracy(possibleLocationList.get(latList.size() / 2).getAccuracy());
		returnLocation.setLatitude(medianLat);
		returnLocation.setLongitude(medianLong);
		returnLocation.setTime(getLatestLocationTime());

		return returnLocation;
	}

	/**
	 * Adds possible best location to list of locations
	 * 
	 * @param location
	 */
	public void addPossibleLocation(Location location) {
		latList.add(location.getLatitude());
		longList.add(location.getLongitude());
		possibleLocationList.add(location);
	}

	/**
	 * Gets the number of locations added
	 * 
	 * @return the number of locations
	 */
	public int numberOfLocations() {
		return possibleLocationList.size();
	}

	/**
	 * Runs through the location list and gets the time stamp of the latest one.
	 * 
	 * @return a long of time
	 */
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
