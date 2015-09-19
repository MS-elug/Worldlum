package com.elug.worldlum;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.location.Location;
import android.util.Log;

import com.elug.worldlum.facebook.FacebookFriend;
import com.elug.worldlum.util.LocationUtils;

public class Cache {
	public static Bitmap mSun;
	private static Bitmap mEarth,mEarthWallpaper;
	
	public static double cosLat[], sinLat[], cosLong[], sinLong[];

	private static ArrayList<FacebookFriend> mFriendList;
	private static Location mUserLastKnownLocation;
	
	public static Bitmap getEarth(boolean isWallpaper) {
		return isWallpaper?mEarthWallpaper:mEarth;
	}
	public static void setEarth(Bitmap earth,boolean isWallpaper) {
		if (isWallpaper==true){
			mEarthWallpaper=earth;
		}else{
			mEarth=earth;
		}
	}
	
	public static final String FILE_FACEBOOK_FRIEND_TMP= "FacebookFriend.tmpl";
	public static ArrayList<FacebookFriend> getFacebookFriendList() {
		if (mFriendList == null) {
			//try to load existing data
			mFriendList = CacheManager.getInstance().retrieveSerializable(FILE_FACEBOOK_FRIEND_TMP);
			if(mFriendList==null){
				Log.e("TAG","mFriendList NULL"); 
				//TODO download from facebook
				mFriendList = new ArrayList<FacebookFriend>();
			}
		}

		return mFriendList;
	}
	public static void setFacebookFriendList(ArrayList<FacebookFriend> friendList) {
		mFriendList=friendList;
		//And save it
		if(mFriendList!=null){
			CacheManager.getInstance().saveSerializable(mFriendList,FILE_FACEBOOK_FRIEND_TMP);
		}
	}
	
	
	public static final String FILE_USER_LOCATION_TMP= "UserLocation.tmpl";
	public static Location getUserLastKnownLocation() {
		if (mUserLastKnownLocation == null) {
			//try to load existing data
			mUserLastKnownLocation = (LocationSerializable) CacheManager.getInstance().retrieveSerializable(FILE_USER_LOCATION_TMP);

		}

		return mUserLastKnownLocation;
	}

	public static void setUserLastKnownLocation(Location userLocation) {
		mUserLastKnownLocation=userLocation;
		if(LocationUtils.isBetterLocation(userLocation, mUserLastKnownLocation)){
			mUserLastKnownLocation=userLocation;
			//And save it
			if(mUserLastKnownLocation!=null){
				CacheManager.getInstance().saveSerializable(new LocationSerializable(mUserLastKnownLocation),FILE_FACEBOOK_FRIEND_TMP);
			}
		}	
	}
	
}
