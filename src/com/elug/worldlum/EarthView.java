package com.elug.worldlum;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.elug.worldlum.EarthDraw.ViewEvent;
import com.elug.worldlum.math.PointGPS;
import com.elug.worldlum.util.LocationUtils;

public class EarthView extends SurfaceView implements SurfaceHolder.Callback, SharedPreferences.OnSharedPreferenceChangeListener {
	public static final String TAG = "EarthView";
	public static final int TIME_REFRESH_LIGHT = 50000;

	private EarthDraw mEarthDraw;
	private SharedPreferences mPrefs;
	private MapClickListener mOnMapClickListener;
	private float mLastTouchX, mLastTouchY;
	private double mSlideOffset;

	private EarthViewThread mThread;
	// Define a listener that responds to location updates
	private LocationListener mLocationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			Cache.setUserLastKnownLocation(location);
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		public void onProviderEnabled(String provider) {
			Log.w(TAG, "provider enabled: " + provider);
		}

		public void onProviderDisabled(String provider) {
		}
	};
	
	public static interface MapClickListener {
		public void onMapClick(double longitude, double latitude);
	}

	public EarthView(Context context) {
		super(context);
		init();
	}

	public EarthView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public EarthView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		getHolder().addCallback(this);
		mEarthDraw = new EarthDraw();

		mEarthDraw.setOnViewEventListener(new ViewEvent() {

			@Override
			public void onNeedRefresh() {
				if (mThread != null) {
					mThread.mHandler.sendEmptyMessage(EarthViewThread.HANDLER_FORCE_REDRAW);
				}
			}

			@Override
			public void onNeedComputeMap() {
				if (mThread != null) {
					mThread.mHandler.sendEmptyMessage(EarthViewThread.HANDLER_FORCE_REDRAW_LIGHT);
				}
			}
		});

		

		// Acquire a reference to the system Location Manager
		LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
		// Register the listener with the Location Manager to receive location
		// updates
//			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
		
		Location lastKnownNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location lastKnownGPSLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		//Initialise
		mLocationListener.onLocationChanged(lastKnownNetworkLocation);
		mLocationListener.onLocationChanged(lastKnownGPSLocation);

		
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		mPrefs.registerOnSharedPreferenceChangeListener(this);

		//Init view from shared preferences
		Resources resource = getResources();
		String app_pref_key_facebook_friend = resource.getString(R.string.application_preference_facebook_friendslist_key);
		String app_pref_key_spider_map = resource.getString(R.string.application_preference_map_show_spider_map_key);
		String app_pref_key_smooth_light = resource.getString(R.string.application_preference_map_smooth_light_key);
		String app_pref_key_timezone = resource.getString(R.string.application_preference_map_show_time_zone_key);
		String app_pref_key_showsun = resource.getString(R.string.application_preference_show_sun_icon_key);

		boolean showSunIcon = mPrefs.getBoolean(app_pref_key_showsun,
				resource.getBoolean(R.bool.application_preference_show_sun_icon_default));

		boolean showTimeZone = mPrefs.getBoolean(app_pref_key_timezone,
				resource.getBoolean(R.bool.application_preference_map_show_time_zone_default));

		boolean showSmoothLight = mPrefs.getBoolean(app_pref_key_smooth_light,
				resource.getBoolean(R.bool.application_preference_map_smooth_light_default));

		boolean showFacebookFriends = mPrefs.getBoolean(app_pref_key_facebook_friend,
				resource.getBoolean(R.bool.application_preference_facebook_friendslist_default));
		
		boolean showSpiderMap = mPrefs.getBoolean(app_pref_key_spider_map,
				resource.getBoolean(R.bool.application_preference_map_show_spider_map_default));
		
		mEarthDraw.setSettingShowFacebookFriends(showFacebookFriends);
		mEarthDraw.setSettingShowSunIcon(showSunIcon);
		mEarthDraw.setSettingShowTimeZone(showTimeZone);
		mEarthDraw.setSettingSmoothLight(showSmoothLight);
		mEarthDraw.setSettingShowSpiderMap(showSpiderMap);
		
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);

		int action = event.getAction();
		int actionCode = action & MotionEvent.ACTION_MASK;

		switch (actionCode) {
			case MotionEvent.ACTION_DOWN:
				mLastTouchX = event.getX();
				mLastTouchY = event.getY();
				mSlideOffset = mEarthDraw.getSlideOffset();

				double longitude = mEarthDraw.getLongitudeFromScreenPosition((int) mLastTouchX);
				double latitude = mEarthDraw.getLatitudeFromScreenPosition((int) mLastTouchY);

				if (mOnMapClickListener != null) {
					mOnMapClickListener.onMapClick(longitude, latitude);
				}

				return true;
			case MotionEvent.ACTION_UP:
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				break;
			case MotionEvent.ACTION_POINTER_UP:
				break;
			case MotionEvent.ACTION_MOVE:
				mEarthDraw.setSlideOffset(mSlideOffset - ((event.getX() - mLastTouchX)) / getWidth(), 0.0);
				mThread.mHandler.sendEmptyMessage(EarthViewThread.HANDLER_FORCE_REDRAW);
				return true;
		}
		return false;
	}

	public void setOnMapClickListener(MapClickListener onMapClickListener) {
		mOnMapClickListener = onMapClickListener;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

		mEarthDraw.init(getContext(), width, height,false);
		// Prepare Thread
		if (mThread != null) {
			mThread.stopThread();
		}
		mThread = new EarthViewThread(getHolder(), mEarthDraw);
		mThread.start();

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mThread.stopThread();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

		if (key != null) {
			Resources resource = getResources();

			String app_pref_key_facebook_friend = resource.getString(R.string.application_preference_facebook_friendslist_key);
			String app_pref_key_spider_map = resource.getString(R.string.application_preference_map_show_spider_map_key);
			String app_pref_key_smooth_light = resource.getString(R.string.application_preference_map_smooth_light_key);
			String app_pref_key_timezone = resource.getString(R.string.application_preference_map_show_time_zone_key);
			String app_pref_key_showsun = resource.getString(R.string.application_preference_show_sun_icon_key);

			if (key.equalsIgnoreCase(app_pref_key_facebook_friend)) {
				boolean showFacebookFriends = sharedPreferences.getBoolean(app_pref_key_facebook_friend,
						resource.getBoolean(R.bool.application_preference_facebook_friendslist_default));
				mEarthDraw.setSettingShowFacebookFriends(showFacebookFriends);
			} else if (key.equalsIgnoreCase(app_pref_key_showsun)) {
				boolean showSunIcon = sharedPreferences.getBoolean(app_pref_key_showsun,
						resource.getBoolean(R.bool.application_preference_show_sun_icon_default));
				mEarthDraw.setSettingShowSunIcon(showSunIcon);
			} else if (key.equalsIgnoreCase(app_pref_key_timezone)) {
				boolean showTimeZone = sharedPreferences.getBoolean(app_pref_key_timezone,
						resource.getBoolean(R.bool.application_preference_map_show_time_zone_default));
				mEarthDraw.setSettingShowTimeZone(showTimeZone);
			} else if (key.equalsIgnoreCase(app_pref_key_smooth_light)) {
				boolean showSmoothLight = sharedPreferences.getBoolean(app_pref_key_smooth_light,
						resource.getBoolean(R.bool.application_preference_map_smooth_light_default));
				mEarthDraw.setSettingSmoothLight(showSmoothLight);
			} else if (key.equalsIgnoreCase(app_pref_key_spider_map)) {
				boolean showSpiderMap = mPrefs.getBoolean(app_pref_key_spider_map,
						resource.getBoolean(R.bool.application_preference_map_show_spider_map_default));
				mEarthDraw.setSettingShowSpiderMap(showSpiderMap);
			}

		}
	}
}
