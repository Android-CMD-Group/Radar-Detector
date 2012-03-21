package edu.cmd.radar.android.check;

import java.util.List;

import android.location.Location;


public class TrapLocations {

	private List<Location> locationList;
	private List<Float> distanceList;
	private long timeStamp;
	
	public void addLocation(double lat, double lon, float accuracy, float speed){
		Location loc = new Location("CUSTOM_PROVIDER");
		loc.setLatitude(lat);
		loc.setLongitude(lon);
		loc.setSpeed(speed);
		loc.setAccuracy(accuracy);
		locationList.add(loc);
	}
	
	
	public List<Location> getLocationList() {
		return locationList;
	}
	public void setLocationList(List<Location> locationList) {
		this.locationList = locationList;
	}
	public List<Float> getDistanceList() {
		return distanceList;
	}
	public void addDistance(float d) {
		distanceList.add(d);
	}
	public long getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	
	
}
