package edu.cmd.radar.android.check;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import edu.cmd.radar.android.location.SerializableLocation;


public class TrapLocations implements Serializable{

	private static final long serialVersionUID = 3714199198111591527L;
	private long timeStamp;
	private SerializableLocation originalLocation;
	private float rangeOfPointsFromOrigin;
	private HashMap<SerializableLocation, Float> locationToDistanceMap;
	private float validBearingRange;
	

	public void addLocation(double lat, double lon, float accuracy, float speed){
		SerializableLocation loc = new SerializableLocation();
		loc.setLatitude(lat);
		loc.setLongitude(lon);
		loc.setSpeed(speed);
		loc.setAccuracy(accuracy);
		locationToDistanceMap.put(loc, null);
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
	
	public Float addDistance(SerializableLocation loc, float dis) {
		return locationToDistanceMap.put(loc, dis);
	}
	
	public long getTimeStamp() {
		return timeStamp;
	}
	
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public Set<SerializableLocation> getLocations() {
		return locationToDistanceMap.keySet();
	}

	public Collection<Float> getDistanceCollection() {
		return locationToDistanceMap.values();
	}

	public float getValidBearingRange() {
		return validBearingRange;
	}

	public void setValidBearingRange(float validBearingRange) {
		this.validBearingRange = validBearingRange;
	}

	public void removeLocation(SerializableLocation loc) {
		locationToDistanceMap.remove(loc);
		
	}
	
	public float getDistanceFromLocation(SerializableLocation loc) {
		return locationToDistanceMap.get(loc);
	}
	
	
	
}
