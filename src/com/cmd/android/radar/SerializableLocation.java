package com.cmd.android.radar;

import java.io.Serializable;

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
		time = originalLocationObject.getTime();

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
		return accuracy != 0.0;
	}

	public boolean hasBearing() {
		return bearing != 0.0;
	}

	public boolean hasSpeed() {
		return speed != 0.0f;
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
