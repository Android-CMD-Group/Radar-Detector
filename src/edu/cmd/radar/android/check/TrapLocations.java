package edu.cmd.radar.android.check;

import java.io.Serializable;
import java.util.List;

import edu.cmd.radar.android.location.SerializableLocation;


public class TrapLocations implements Serializable{

	private static final long serialVersionUID = 3714199198111591527L;
	private List<SerializableLocation> locationList;
	private List<Float> distanceList;
	private long timeStamp;
	private SerializableLocation originalLocation;
	private float rangeOfPointsFromOrigin;
	
	

	public void addLocation(double lat, double lon, float accuracy, float speed){
		SerializableLocation loc = new SerializableLocation();
		loc.setLatitude(lat);
		loc.setLongitude(lon);
		loc.setSpeed(speed);
		loc.setAccuracy(accuracy);
		locationList.add(loc);
	}
	
	public SerializableLocation getOriginalLocation() {
		return originalLocation;
	}

	public void setOriginalLocation(SerializableLocation originalLocation) {
		this.originalLocation = originalLocation;
	}

	public float getRangeOfPointsFromOrigin() {
		return rangeOfPointsFromOrigin;
	}

	public void setRangeOfPointsFromOrigin(float rangeOfPointsFromOrigin) {
		this.rangeOfPointsFromOrigin = rangeOfPointsFromOrigin;
	}
	public void setDistanceList(List<Float> distanceList) {
		this.distanceList = distanceList;
	}
	public List<SerializableLocation> getLocationList() {
		return locationList;
	}
	public void setLocationList(List<SerializableLocation> locationList) {
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
