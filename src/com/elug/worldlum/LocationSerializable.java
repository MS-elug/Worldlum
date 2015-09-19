package com.elug.worldlum;

import java.io.Serializable;

import android.location.Location;

public class LocationSerializable extends Location implements Serializable{
	public LocationSerializable(Location location) {
		super(location);
	}
}
