package com.elug.worldlum.facebook;

import java.io.Serializable;

public class FacebookFriend implements Serializable{
	public String name, id, locationId;
	public double latitude=Double.NaN, longitude=Double.NaN;
}
