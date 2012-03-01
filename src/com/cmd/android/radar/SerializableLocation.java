package com.cmd.android.radar;

import java.io.Serializable;

import android.location.Location;

public class SerializableLocation implements Serializable{
	
	private float accuracy;
	private float bearing;
	private double latitude;
	private double longitude;
	private String provider;
	private float speed;
	private long time;

	public SerializableLocation(Location originalLocationObject){
		
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

	public Location returnEquivalentLocation(){
		Location returnLocation = new Location(provider);
		returnLocation.setAccuracy(accuracy);
		returnLocation.setBearing(bearing);
		returnLocation.setLatitude(latitude);
		returnLocation.setLongitude(longitude);
		returnLocation.setSpeed(speed);
		returnLocation.setProvider(provider);
		returnLocation.setTime(time);
		return returnLocation;
	}
	
	public boolean hasAccuracy(){
		return accuracy != 0.0;
	}
	
	public boolean hasBearing(){
		return bearing != 0.0;
	}
	
	public boolean hasSpeed(){
		return speed != 0.0f;
	}

	public float getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(float accuracy) {
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