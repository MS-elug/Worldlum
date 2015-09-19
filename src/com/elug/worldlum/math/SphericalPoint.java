package com.elug.worldlum.math;


import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * In euclidien base (O,x,y,z), P is a 3D point, it's spherical coordinate is : <br/>
 * <ul><li>rho=norm(OP)</li>
 * <li>theta=(x,OH) H is the projection of P on plan (O,x,y) 0<=theta<360 </li>
 * <li>phi=(z,OP)     0<=phi<180<br/></li>
 * </ul>
 */

public class SphericalPoint implements Parcelable{
	
	public static final String TAG ="SphericalPoint3D";
	
	public double rho,phi,theta;
	
	public SphericalPoint()
	{
		rho=0.0;
		phi=0.0;
		theta=0.0;		
	};
	public SphericalPoint(double rho,double phi,double theta)
	{
		this.rho=rho;
		this.phi=phi;
		this.theta=theta;
	}

	
	//--------------------------------------------------------------------------
	//Parcelable section
	//--------------------------------------------------------------------------

	public static final Parcelable.Creator<SphericalPoint> CREATOR = new Parcelable.Creator<SphericalPoint>() 
	{
		public SphericalPoint createFromParcel(Parcel in) {
			return new SphericalPoint(in);
		}

		public SphericalPoint[] newArray(int size) {
			return new SphericalPoint[size];
		}
	};
	
	public SphericalPoint(Parcel in)
	{
		rho=in.readDouble();
		phi=in.readDouble();
		theta=in.readDouble();
	}
	
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flag) {
		dest.writeDouble(rho);
		dest.writeDouble(phi);
		dest.writeDouble(theta);
	}
}
