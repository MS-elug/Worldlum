package com.elug.worldlum.math;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * In euclidien base (O,x,y,z), P is a 3D point
 */

public class Point3D {
	public static final String TAG ="Point3D";
	
	/**
	 * Point = (x,y,z) in an euclidien space
	 */
	public double x,y,z;
	
	
	public Point3D()
	{
		x=0.0;
		y=0.0;
		z=0.0;
	};
	
	public Point3D(double x,double y,double z)
	{
		this.x=x;
		this.y=y;
		this.z=z;
	}

	public static Point3D convertPointGPS(PointGPS pointGPS) {
		double x, y, z;
		x =( pointGPS.altitude + PointGPS.EARTH_RADIUS) * Math.cos(Math.toRadians(pointGPS.longitude)) * Math.cos(Math.toRadians(pointGPS.latitude));
		y =( pointGPS.altitude + PointGPS.EARTH_RADIUS) * Math.sin(Math.toRadians(pointGPS.longitude)) * Math.cos(Math.toRadians(pointGPS.latitude));
		z =( pointGPS.altitude + PointGPS.EARTH_RADIUS) * Math.sin(Math.toRadians(pointGPS.latitude));
		return new Point3D(x, y, z);
	}
	
	public static Point3D convertSphericalPoint(SphericalPoint sphericalPoint) {
		double x, y, z;
		x = sphericalPoint.rho * Math.cos(Math.toRadians(sphericalPoint.theta)) * Math.sin(Math.toRadians(sphericalPoint.phi));
		y = sphericalPoint.rho * Math.sin(Math.toRadians(sphericalPoint.theta)) * Math.sin(Math.toRadians(sphericalPoint.phi));
		z = sphericalPoint.rho * Math.cos(Math.toRadians(sphericalPoint.phi));
		return new Point3D(x, y, z);
	}
	
	//--------------------------------------------------------------------------
	//Parcelable section
	//--------------------------------------------------------------------------
	public static final Parcelable.Creator<Point3D> CREATOR = new Parcelable.Creator<Point3D>() 
	{
		public Point3D createFromParcel(Parcel in) {
			return new Point3D(in);
		}

		public Point3D[] newArray(int size) {
			return new Point3D[size];
		}
	};
	
	public int describeContents() {
		return 0;
	}

	public Point3D(Parcel in)
	{
		x=in.readDouble();
		y=in.readDouble();
		z=in.readDouble();
	}
	
	public void writeToParcel(Parcel dest, int flag) {
		dest.writeDouble(x);
		dest.writeDouble(y);
		dest.writeDouble(z);
	}
}
