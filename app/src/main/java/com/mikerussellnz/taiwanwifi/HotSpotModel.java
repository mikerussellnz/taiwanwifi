package com.mikerussellnz.taiwanwifi;

import io.realm.RealmObject;

/**
 * Created by mike on 1/12/15.
 */
public class HotSpotModel extends RealmObject {
	private String name;
	private double lat;
	private double lon;
	private String address;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public String getAddress() { return address; }

	public void setAddress(String address) { this.address = address; }
}