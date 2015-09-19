package com.elug.worldlum.math;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * In euclidien base (O,x,y,z), P is a 3D point, it's GPS coordinate is : <br/>
 * <ul>
 * <li>latitude=(Oz,OP), -90<=latitude<90</li>
 * <li>longitude=(Ox,OP), -180<=longitude<180<br/>
 * </li>
 * </ul>
 */

public class PointGPS implements Parcelable {
	private final static String TAG = "PointGPS";
	
	/** in meters */
	public final static double EARTH_RADIUS = 6371000;

	public double latitude, longitude, altitude;

	public PointGPS() {
	};

	public PointGPS(double latitude, double longitude, double altitude) {
		this.latitude = latitude;

		this.altitude = altitude;

		if (longitude < -180) {
			this.longitude = longitude + 360;
		} else {
			this.longitude = longitude;
		}
	}

	/**
	 * by default altitude is set to 0
	 * 
	 * @param latitude
	 * @param longitude
	 */
	public PointGPS(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = 0;
		
		if (longitude < -180) {
			this.longitude = longitude + 360;
		} else {
			this.longitude = longitude;
		}
	}

	public PointGPS(Parcel in) {
		latitude = in.readDouble();
		longitude = in.readDouble();
		altitude = in.readDouble();
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getAltitude() {
		return altitude;
	}

	public PointGPS setRadius(double alt) {
		altitude = alt;
		return this;
	}

	public static PointGPS convertPoint3D(Point3D point3D) {
		double latitude, longitude, altitude;
		altitude = Math.sqrt(Math.pow(point3D.x, 2) + Math.pow(point3D.y, 2)
				+ Math.pow(point3D.z, 2));
		latitude = 90 - Math.toDegrees(Math.acos(point3D.z / altitude));
		if (point3D.y >= 0) {
			longitude = Math.toDegrees(Math.acos(point3D.x
					/ (Math.sqrt(Math.pow(point3D.x, 2)
							+ Math.pow(point3D.y, 2)))));
		} else {
			longitude =  - Math.toDegrees(Math.acos(point3D.x
					/ (Math.sqrt(Math.pow(point3D.x, 2)
							+ Math.pow(point3D.y, 2))))) ;//+360;
		}

		return new PointGPS(latitude, longitude, altitude);
	}

	public static PointGPS convertSphericalPoint(SphericalPoint sphericalPoint) {
		double latitude, longitude, altitude;

		latitude = 90 - sphericalPoint.phi;
		longitude = sphericalPoint.theta;
		altitude = sphericalPoint.rho - PointGPS.EARTH_RADIUS;

		return new PointGPS(latitude, longitude, altitude);
	}

	// --------------------------------------------------------------------------
	// Parcelable section
	// --------------------------------------------------------------------------

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flag) {
		dest.writeDouble(latitude);
		dest.writeDouble(longitude);
		dest.writeDouble(altitude);
	}

	public static final Parcelable.Creator<PointGPS> CREATOR = new Parcelable.Creator<PointGPS>() {
		public PointGPS createFromParcel(Parcel in) {
			return new PointGPS(in);
		}

		public PointGPS[] newArray(int size) {
			return new PointGPS[size];
		}
	};

}
