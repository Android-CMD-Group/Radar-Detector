package edu.cmd.radar.android.location;

import java.io.Serializable;
import java.util.Date;

import android.location.Location;

/**
 * This is a work around class to help serialize location data in an android
 * compatible format. For the methods, see android location documentation, they
 * are the same.
 * 
 * @author satshabad
 * 
 */
public class SerializableLocation implements Serializable {

	private double accuracy;
	private float bearing;
	private double latitude;
	private double longitude;
	private String provider;
	private float speed;
	private long time;
	private boolean hasAccuracy;
	private boolean hasBearing;
	private boolean hasSpeed;

	public String toString() {
		String returnString = "-------------------------------------------------------------------------------\n";
		returnString += "Lat, Long:              " + latitude + ",    " + longitude + "\n";
		returnString += "Has Speed, Speed:       " + hasSpeed  +",    " + speed + " meters/second\n";
		returnString += "Has Accuracy, Accuracy: " + hasAccuracy + ", " + accuracy + " meters\n";
		returnString += "Has Bearing, Bearing:   " + hasBearing  +",  " + bearing + " degrees east from north\n";
		Date d = new Date(time);
		returnString += "Created at time: " + d.toString() + "\n";
		returnString += "Provided by:            " + provider + "\n";
		returnString += "-------------------------------------------------------------------------------\n";
		
		
		return returnString;
		
	}
	
	/**
	 * Takes a location obejct and copies its members into this new object
	 * 
	 * @param originalLocationObject
	 */
	public SerializableLocation(Location originalLocationObject) {

		accuracy = originalLocationObject.getAccuracy();
		bearing = originalLocationObject.getBearing();
		latitude = originalLocationObject.getLatitude();
		longitude = originalLocationObject.getLongitude();
		provider = originalLocationObject.getProvider();
		speed = originalLocationObject.getSpeed();
		// offset by one day due bug
		time = originalLocationObject.getTime()-86400000;
		hasAccuracy = originalLocationObject.hasAccuracy();
		hasBearing = originalLocationObject.hasBearing();
		hasSpeed = originalLocationObject.hasSpeed();

	}

	
	
	public SerializableLocation() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Converts #this to it's equiv Android based location object.
	 * 
	 * @return teh android based Location
	 */
	public Location returnEquivalentLocation() {
		Location returnLocation = new Location(provider);
		returnLocation.setAccuracy((float) accuracy);
		returnLocation.setBearing(bearing);
		returnLocation.setLatitude(latitude);
		returnLocation.setLongitude(longitude);
		returnLocation.setSpeed(speed);
		returnLocation.setProvider(provider);
		returnLocation.setTime(time);
		return returnLocation;
	}

	public boolean hasAccuracy() {
		return hasAccuracy;
	}

	public boolean hasBearing() {
		return hasBearing;
	}

	public boolean hasSpeed() {
		return hasSpeed;
	}

	public double getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(double accuracy) {
		this.accuracy = accuracy;
	}

	public float getBearing() {
		return bearing;
	}

	public void setBearing(float bearing) {
		this.bearing = bearing;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public float getSpeed() {
		return speed;
	}

	public void setSpeed(float speed) {
		this.speed = speed;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

}
