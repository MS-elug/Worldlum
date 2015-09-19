package com.elug.worldlum.math;


import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Vector3D implements Parcelable{
	public static final String TAG ="Vector3D";
	
	/**
	 * Vector = (vx,vy,vz) in an euclidien space
	 */
	public double vx,vy,vz;
	
	public Vector3D()
	{
		vx=0.0;
		vy=0.0;
		vz=0.0;
	};
	
	public Vector3D(double vx,double vy,double vz)
	{
		this.vx=vx;
		this.vy=vy;
		this.vz=vz;
	}

	public double getNorm()
	{
		return Math.sqrt(vx*vx+vy*vy+vz*vz);
	}

	public void divide(double scalar)
	{
		if(Math.abs(scalar)>M.EPSILON){
			vx=vx/scalar;
			vy=vy/scalar;
			vz=vz/scalar;
		}else{
			vx=Double.NaN;
			vy=Double.NaN;
			vz=Double.NaN;
			Log.w(TAG,"Divide by number < M.Epsilon");
		}
	}
	
	/**
	 * Multiply the vector by a scalar number
	 */
	public void multiply(double scalar)
	{
		vx=vx*scalar;
		vy=vy*scalar;
		vz=vz*scalar;
	}

	/**
	 * Normalise the vector  to have an unit size
	 */
	public void normalize()
	{
		this.divide(this.getNorm());	
	}
	
	/**
	 * @param A
	 * @param B
	 * @return AxB
	 */
	static public Vector3D crossProduct(Vector3D A,Vector3D B)
	{
		return new Vector3D(A.vy*B.vz - A.vz*B.vy,A.vz*B.vx - A.vx*B.vz,A.vx*B.vy - A.vy*B.vx);
	}

	/**
	 * @param A
	 * @param B
	 * @return A.B
	 */
	static public double dotProduct(Vector3D A,Vector3D B)
	{
		return (A.vx*B.vx+A.vy*B.vy+A.vz*B.vz);
	}
	
	
	static public Vector3D create(Point3D start,Point3D end)
	{
		return new Vector3D(end.x-start.x,end.y-start.y,end.z-start.z);
	}
	
	//--------------------------------------------------------------------------
	//Parcelable section
	//--------------------------------------------------------------------------
	public static final Parcelable.Creator<Vector3D> CREATOR = new Parcelable.Creator<Vector3D>() 
	{
		public Vector3D createFromParcel(Parcel in) {
			return new Vector3D(in);
		}

		public Vector3D[] newArray(int size) {
			return new Vector3D[size];
		}
	};
	
	public int describeContents() {
		return 0;
	}

	public Vector3D(Parcel in)
	{
		vx=in.readDouble();
		vy=in.readDouble();
		vz=in.readDouble();
	}
	
	public void writeToParcel(Parcel dest, int flag) {
		dest.writeDouble(vx);
		dest.writeDouble(vy);
		dest.writeDouble(vz);
	}
}
