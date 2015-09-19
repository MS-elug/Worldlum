package com.elug.worldlum;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

import com.elug.worldlum.facebook.FacebookData;
import com.elug.worldlum.facebook.FacebookFriend;
import com.elug.worldlum.math.Point3D;
import com.elug.worldlum.math.PointGPS;
import com.elug.worldlum.math.Vector3D;
import com.elug.worldlum.timezone.*;
import com.elug.worldlum.util.LocationUtils;

public class EarthDraw {
	public static final String TAG = "EarthDraw";

	public static final String CACHE_FILENAME_MAP_PORTRAIT = "map_cache_portrait.png";
	public static final String CACHE_FILENAME_MAP_LANDSCAPE = "map_cache_landscape.png";

	// ----------------------------------------
	// View properties
	private Bitmap mEarthDay, mEarthNight;
	private PointGPS mSunPosition;
	private String mEquatorName, mTropicCapricornName, mTropicCancerName, mLoading;

	private ViewEvent mViewEventListener;

	public interface ViewEvent {
		public void onNeedRefresh();
		public void onNeedComputeMap();
	}

	private boolean mIsComputingMap = false, mIsLoadingFacebookFriends = false;
	private boolean mIsWallpaper;
	// ----------------------------------------
	// View layout properties
	private int mWidthScreen = -1, mHeightScreen = -1;
	private int mWidthMap = -1, mHeightMap = -1;
	private double mCoefPixToLongOnScreen = 1.0, mCoefPixToLatOnScreen = 1.0;
	private double mCoefPixToLongOnMap = 1.0, mCoefPixToLatOnMap = 1.0;
	private double mCoefLong = 1, mCoefLat = 1;

	// ----------------------------------------
	// Earth View position properties
	/**
	 * <b>ALWAYS</b> : -180<mUserViewLongitude<180
	 */
	private double mLongitudeOffset = 0, mSlideOffset = 0;

	// ----------------------------------------
	// Settings
	public static final int SETTING_FOLLOW_NOTHING = 0;
	public static final int SETTING_FOLLOW_SUN = 1;
	public static final int SETTING_FOLLOW_USER = 2;

	private static final int SPIDER_MAP_LINE_STEP = 100;
	
	private boolean mSettingShowSunIcon = false;
	private boolean mSettingSmoothLight = true;
	private boolean mSettingShowTimeZone = false;
	private boolean mSettingShowFacebookFriends = false;
	private boolean mSettingShowSpiderMap = false;
	private int mSettingFollow = SETTING_FOLLOW_NOTHING;

	private Thread mThreadComputeEarthAlphaLayer;
	private int mScreenOrientation;
	private Context mContext;
	
	public EarthDraw() {
	}

	public double getLongitudeOffset() {
		return mLongitudeOffset;
	}

	public void setLongitudeOffset(double longitudeOffset) {
		mLongitudeOffset = longitudeOffset;
	}

	public double getSlideOffset() {
		return (0.5 + mSlideOffset / (2 * 180));
	}

	public void setSlideOffset(double longitudeOffset, double latitudeOffset) {
		mSlideOffset = 2 * (longitudeOffset - 0.5) * 180;

		if (mSlideOffset < -180) {
			mSlideOffset += +360;
		} else if (mSlideOffset > 180) {
			mSlideOffset -= 360;
		}
	}

	public void init(Context context, int width, int height, boolean isWallpaper) {
		mContext = context;
		mIsWallpaper=isWallpaper;
		
		if (mEarthDay != null) {
			mEarthDay.recycle();
			mEarthDay = null;
		}
		if (mEarthNight != null) {
			mEarthNight.recycle();
			mEarthNight = null;
		}

		System.gc();

		mWidthScreen = width;
		mHeightScreen = height;

		mScreenOrientation = context.getResources().getConfiguration().orientation;
		if (mScreenOrientation == Configuration.ORIENTATION_PORTRAIT) {

			mWidthMap = mHeightScreen;
			mHeightMap = mWidthScreen;

			//TODO BETTER THIS COEF
			mCoefLat = 0.7;
			mCoefLong = 0.7;
			mCoefLong = (double) mWidthMap / (double) mWidthScreen;


		} else {
			mCoefLat = 1;
			mCoefLong = 1;

			mWidthMap = mWidthScreen;
			mHeightMap = mHeightScreen;

		}

		mCoefPixToLongOnScreen = (double) 360 / (double) (mWidthScreen * mCoefLong);
		mCoefPixToLatOnScreen = (double) 180 / (double) (mHeightScreen * mCoefLat);
		mCoefPixToLongOnMap = (double) 360 / (double) (mWidthMap);
		mCoefPixToLatOnMap = (double) 180 / (double) (mHeightMap);

		if (Cache.getEarth(mIsWallpaper) == null) {
			Cache.setEarth( Bitmap.createBitmap(mWidthMap, mHeightMap, Bitmap.Config.ARGB_8888), mIsWallpaper);
			Cache.cosLat = new double[mHeightMap];
			Cache.sinLat = new double[mHeightMap];
			Cache.cosLong = new double[mWidthMap];
			Cache.sinLong = new double[mWidthMap];
			for (int x = 0; x < mWidthMap; x++) {
				double longitude = Math.toRadians(x * mCoefPixToLongOnMap - 180);
				Cache.cosLong[x] = Math.cos(longitude);
				Cache.sinLong[x] = Math.sin(longitude);
			}
			for (int y = 0; y < mHeightMap; y++) {
				double latitude = Math.toRadians(-y * mCoefPixToLatOnMap - 90);
				Cache.cosLat[y] = Math.cos(latitude);
				Cache.sinLat[y] = Math.sin(latitude);
			}

			mIsComputingMap = true;
		}
		

		if (Cache.mSun == null) {
			Bitmap sunBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.sun);
			Cache.mSun = Bitmap.createBitmap(mWidthMap / 10, mWidthMap / 10, Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas();
			c.setBitmap(Cache.mSun);
			c.drawBitmap(sunBitmap, null, new Rect(0, 0, mWidthMap / 10, mWidthMap / 10), null);
			sunBitmap.recycle();
			sunBitmap = null;
			c = null;
			System.gc();
		}

		// Retrieve some Strings
		mEquatorName = context.getString(R.string.equator_name);
		mTropicCapricornName = context.getString(R.string.tropic_capricorn_name);
		mTropicCancerName = context.getString(R.string.tropic_cancer_name);
		mLoading = context.getString(R.string.loading);

		System.gc();

		computeEarthAlphaLayer();
	}

	public void onDraw(Canvas canvas) {
		canvas.save();

		switch (mSettingFollow) {
			case SETTING_FOLLOW_NOTHING:
			default:
				mLongitudeOffset = 0 + mSlideOffset;
				break;
			case SETTING_FOLLOW_SUN:
				if (mSunPosition != null) {
					computeSunPosition();
					mLongitudeOffset = mSunPosition.longitude + mSlideOffset;
				}
				break;
			case SETTING_FOLLOW_USER:
				if(Cache.getUserLastKnownLocation()!=null){
					mLongitudeOffset = Cache.getUserLastKnownLocation().getLongitude() + mSlideOffset;
				}else{
					mLongitudeOffset = 0 + mSlideOffset;
				}
				
				break;
		}

		if (mLongitudeOffset < -180) {
			mLongitudeOffset += +360;
		} else if (mLongitudeOffset > 180) {
			mLongitudeOffset -= 360;
		}

		if (mSlideOffset < -180) {
			mSlideOffset += +360;
		} else if (mSlideOffset > 180) {
			mSlideOffset -= 360;
		}

		Matrix matrix = new Matrix();
		matrix.setTranslate((float) mWidthScreen / 2, (float) mHeightScreen / 2);
		canvas.setMatrix(matrix);

		if (isLoading()) {
			canvas.drawARGB(255, 0, 0, 0);
			drawLoading(canvas);
		} else {
			drawEarth(canvas);
			if (mSettingShowSunIcon) {
				drawSun(canvas);
			}

			// Draw equator et tropiques
			drawEquator(canvas);
			drawTropics(canvas);

			drawLatitude(canvas);
		}

		canvas.restore();
	}

	/**
	 * Compute the sun GPS position
	 */
	private void computeSunPosition() {
		// get the current date
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"), Locale.getDefault());

		int mDay = calendar.get(Calendar.DAY_OF_YEAR);
		int mHour = calendar.get(Calendar.HOUR_OF_DAY);
		int mMinutes = calendar.get(Calendar.MINUTE);
		int mTotalMinutes = mMinutes + mHour * 60;

		float sunLongitude = -((float) 360 * (float) mTotalMinutes) / ((float) 24 * (float) 60) - (float) 180;
		float sunLatitude = (float) (-AstronomyConstants.ECLIPTIC_ANGLE * Math.cos(2 * Math.PI * (float) mDay / (float) 356));

		mSunPosition = new PointGPS(sunLatitude, sunLongitude, AstronomyConstants.SUN_DISTANCE);
	}

	public void computeEarthAlphaLayer() {
		if (mThreadComputeEarthAlphaLayer == null) {
			mThreadComputeEarthAlphaLayer = new Thread() {
				@Override
				public void run() {

					// Prepare the blend
					computeSunPosition();
					Point3D earthCenter = new Point3D(0, 0, 0);
					Point3D soleil = Point3D.convertPointGPS(mSunPosition);
					Vector3D dirSoleil = Vector3D.create(earthCenter, soleil);
					dirSoleil.normalize();

					byte[] valueAlpha = new byte[mWidthMap * mHeightMap];
					double value, px, py, pz;
					byte alpha;

					// double k=1.,offset=0;
					int x, y;
					for (y = 0; y < mHeightMap; ++y) {
						pz = Cache.sinLat[y];
						for (x = 0; x < mWidthMap; ++x) {

							px = Cache.cosLong[x] * Cache.cosLat[y];
							py = Cache.sinLong[x] * Cache.cosLat[y];

							// cross product
							value = px * dirSoleil.vx + py * dirSoleil.vy + pz * dirSoleil.vz;

							if (value < 0) {
								if (mSettingSmoothLight) {
									alpha = (byte) Math.min(-255 * 3 * value, 255);
								} else {
									alpha = (byte) 255;
								}
							} else {
								alpha = 0;
							}

							valueAlpha[x + y * mWidthMap] = alpha;
						}
					}

					// Resize bitmaps to fit the layout
					Bitmap earthOriginal = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.world_water);
					mEarthDay = Bitmap.createBitmap(mWidthMap, mHeightMap, Bitmap.Config.ARGB_8888);
					Canvas c = new Canvas();
					c.setBitmap(mEarthDay);
					c.drawBitmap(earthOriginal, null, new Rect(0, 0, mWidthMap, mHeightMap), null);
					earthOriginal.recycle();
					earthOriginal = null;
					c = null;
					System.gc();

					mEarthNight = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.earth_lights);

					int pixelColor;
					System.gc();
					int mImagePixels[] = new int[mWidthMap];
					for (y = 0; y < mHeightMap; y++) {
						//Get line pixels
						mEarthDay.getPixels(mImagePixels, 0, mWidthMap, 0, y, mWidthMap, 1);
						for (x = 0; x < mWidthMap; x++) {
							pixelColor = mImagePixels[x] & 0x00FFFFFF;
							mImagePixels[x] = pixelColor | ((valueAlpha[x + y * mWidthMap] << 24) & 0xFF000000);
						}
						mEarthDay.setPixels(mImagePixels, 0, mWidthMap, 0, y, mWidthMap, 1);
					}
					mImagePixels = null;

					// Combine maps
					Bitmap earth =Cache.getEarth(mIsWallpaper);
					synchronized (earth) {
						Canvas canvas = new Canvas();
						canvas.setBitmap(earth);
						canvas.drawBitmap(mEarthNight, null, new Rect(0, 0, mWidthMap, mHeightMap), null);
						canvas.drawBitmap(mEarthDay, null, new Rect(0, 0, mWidthMap, mHeightMap), null);

						Matrix matrix = new Matrix();
						matrix.setTranslate((float) mWidthMap / 2, (float) mHeightMap / 2);
						canvas.setMatrix(matrix);

						//-----
						//Draw Time Zone
						if (mSettingShowTimeZone) {
							Paint paintFill = new Paint();
							paintFill.setColor(Color.WHITE);
							paintFill.setAlpha(20);
							paintFill.setStyle(Style.FILL);
							paintFill.setAntiAlias(true);
							drawTimeZonePair(canvas, paintFill, 0);

							Paint paintStroke = new Paint();
							paintStroke.setColor(Color.WHITE);
							paintStroke.setAlpha(20);
							paintStroke.setStyle(Style.STROKE);
							paintStroke.setStrokeWidth(1);
							paintStroke.setAntiAlias(true);
							drawTimeZonePair(canvas, paintStroke, 0);
							drawTimeZoneImpair(canvas, paintStroke, 0);

							Paint paintText = new Paint();
							paintText.setColor(Color.GRAY);
							paintText.setAlpha(128);
							paintText.setStyle(Style.STROKE);
							paintText.setStrokeWidth(1);
							paintText.setAntiAlias(true);
							drawTimeZonePairText(canvas, paintText, 0);
						}

						//-----
						//Draw Longitude
						drawLongitude(canvas, mLongitudeOffset);

						drawFacebookFriendLine(canvas);
						drawFacebookFriendPosition(canvas);
						drawUserPosition(canvas);
						
					}

					if (mEarthNight != null) {
						mEarthNight.recycle();
						mEarthNight = null;
					}

					if (mEarthDay != null) {
						mEarthDay.recycle();
						mEarthDay = null;
					}

					System.gc();

					mIsComputingMap = false;

					synchronized (mThreadComputeEarthAlphaLayer) {
						mThreadComputeEarthAlphaLayer = null;
					}

					if (mViewEventListener != null) {
						synchronized (mViewEventListener) {
							mViewEventListener.onNeedRefresh();
						}
					}
				}
			};

			mThreadComputeEarthAlphaLayer.start();

		}

	}

	private void drawLoading(Canvas canvas) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);

		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		paint.setTextSize(50);
		paint.setAntiAlias(true);

		canvas.drawText(mLoading, 0 - 50 * mLoading.length() / 4, 0, paint);

		canvas.restore();

	}

	private void drawSun(Canvas canvas) {
		if (Cache.mSun != null && mSunPosition != null) {
			drawBitmapOnMap(canvas, Cache.mSun, mSunPosition.longitude, mSunPosition.latitude, mLongitudeOffset);
			drawBitmapOnMap(canvas, Cache.mSun, mSunPosition.longitude, mSunPosition.latitude, mLongitudeOffset - 360);
			drawBitmapOnMap(canvas, Cache.mSun, mSunPosition.longitude, mSunPosition.latitude, mLongitudeOffset + 360);
		}
	}

	private void drawEarth(Canvas canvas) {
		Bitmap earth =Cache.getEarth(mIsWallpaper);
		if (earth != null) {
			drawMapBitmap(canvas, earth);
		}
	}

	private void drawEquator(Canvas canvas) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);

		Paint paint = new Paint();
		paint.setColor(Color.GRAY);
		paint.setAlpha(128);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(1);
		paint.setPathEffect(new DashPathEffect(new float[] { 5, 5 }, 0));
		paint.setAntiAlias(true);

		canvas.drawLine(-mWidthScreen / 2, 0, mWidthScreen / 2, 0, paint);

		paint.setPathEffect(null);
		canvas.drawText(mEquatorName, -mWidthScreen / 2 + 10, -5, paint);
		canvas.restore();
	}

	private void drawTropics(Canvas canvas) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);

		Paint paint = new Paint();
		paint.setColor(Color.GRAY);
		paint.setAlpha(128);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(1);
		paint.setPathEffect(new DashPathEffect(new float[] { 10, 10 }, 0));
		paint.setAntiAlias(true);

		// Tropic of Capricorn
		canvas.drawLine(-mWidthScreen / 2, (int) (AstronomyConstants.ECLIPTIC_ANGLE / mCoefPixToLatOnScreen), mWidthScreen / 2,
				(int) (AstronomyConstants.ECLIPTIC_ANGLE / mCoefPixToLatOnScreen), paint);
		// Tropic of Cancer
		canvas.drawLine(-mWidthScreen / 2, (int) (-AstronomyConstants.ECLIPTIC_ANGLE / mCoefPixToLatOnScreen), mWidthScreen / 2,
				(int) (-AstronomyConstants.ECLIPTIC_ANGLE / mCoefPixToLatOnScreen), paint);

		paint.setPathEffect(null);
		canvas.drawText(mTropicCapricornName, -mWidthScreen / 2 + 10, -5
				+ (int) (AstronomyConstants.ECLIPTIC_ANGLE / mCoefPixToLatOnScreen), paint);
		canvas.drawText(mTropicCancerName, -mWidthScreen / 2 + 10, -5
				+ (int) (-AstronomyConstants.ECLIPTIC_ANGLE / mCoefPixToLatOnScreen), paint);
		canvas.restore();
	}

	private void drawLongitude(Canvas canvas, double longOffset) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);

		Paint paint = new Paint();
		paint.setColor(Color.GRAY);
		paint.setAlpha(128);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(1);
		paint.setAntiAlias(true);

		int startX, startY, stopX, stopY;
		for (int i = -180; i < 0; i += 30) {

			startX = (int) ((i - longOffset) / mCoefPixToLongOnMap);
			startY = (int) mHeightMap / 2;
			stopX = startX;
			stopY = startY;

			canvas.drawLine(startX, startY - 20, stopX, stopY, paint);
			canvas.drawText(String.valueOf(i) + "°", 5 + startX, -3 + startY, paint);

		}
		canvas.drawText("0°", (int) (0 - longOffset / mCoefPixToLongOnMap), -3 + mHeightMap / 2, paint);
		for (int i = 30; i <= 180; i += 30) {

			startX = (int) ((i - longOffset) / mCoefPixToLongOnMap);
			startY = (int) mHeightMap / 2;
			stopX = startX;
			stopY = startY;

			canvas.drawLine(startX, -20 + startY, stopX, stopY, paint);
			canvas.drawText(String.valueOf(i) + "°", startX - 30, -3 + startY, paint);
		}

		//redraw
		canvas.restore();
	}

	private void drawLatitude(Canvas canvas) {

		canvas.save(Canvas.MATRIX_SAVE_FLAG);

		Paint paint = new Paint();
		paint.setColor(Color.GRAY);
		paint.setAlpha(128);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(1);
		paint.setAntiAlias(true);

		int startX, startY, stopX, stopY;

		for (int i = -90; i <= 0; i += 30) {

			startX = (int) (mWidthScreen / 2);
			startY = (int) (-i / mCoefPixToLatOnScreen);
			stopX = startX;
			stopY = startY;

			canvas.drawLine(startX - 20, startY, startX, stopY, paint);
			canvas.drawText(String.valueOf(i) + "°", -20 + startX, -5 + startY, paint);

		}

		for (int i = 30; i <= 90; i += 30) {

			startX = (int) (mWidthScreen / 2);
			startY = (int) (-i / mCoefPixToLatOnScreen);
			stopX = startX;
			stopY = startY;

			canvas.drawLine(startX - 20, startY, startX, stopY, paint);
			canvas.drawText(String.valueOf(i) + "°", -20 + startX, 15 + startY, paint);
		}

		canvas.restore();
	}

	private void drawTimeZonePairText(Canvas canvas, Paint paint, double longOffset) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);

		int startX, startY;
		startY = (int) (-mHeightMap / 2) + 20;

		for (int i = -11; i < 12; i++) {
			startX = (int) ((i * 15 - longOffset) / mCoefPixToLongOnMap - paint.getTextSize());
			canvas.drawText(String.valueOf(i), 5 + startX, -3 + startY, paint);
		}

		startX = (int) (-mWidthMap / 2);
		canvas.drawText("-12", 5 + startX, -3 + startY, paint);
		startX = (int) (mWidthMap / 2 - 2 * paint.getTextSize());
		canvas.drawText("12", 5 + startX, -3 + startY, paint);

		canvas.restore();
	}

	private void drawTimeZonePair(Canvas canvas, Paint paint, double longOffset) {

		drawTimeZonePath(canvas, paint, TimeZoneP12.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP12.PATH_1, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP12.PATH_2, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP12.PATH_3, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneP10.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP10.PATH_1, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP10.PATH_2, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneP08.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP08.PATH_1, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneP06.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP06.PATH_1, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP06.PATH_2, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP06.PATH_3, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP06.PATH_4, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneP04.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP04.PATH_1, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP04.PATH_2, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP04.PATH_3, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP04.PATH_4, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneP02.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP02.PATH_1, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneP0.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP0.PATH_1, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP0.PATH_2, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneM02.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneM02.PATH_1, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneM04.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneM04.PATH_1, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneM06.PATH_0, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneM08.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneM08.PATH_1, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneM10.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneM10.PATH_1, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneM10.PATH_2, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneM10.PATH_3, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneM12.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneM12.PATH_1, longOffset);

	}

	private void drawTimeZoneImpair(Canvas canvas, Paint paint, double longOffset) {

		drawTimeZonePath(canvas, paint, TimeZoneP11.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP11.PATH_1, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneP09.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP09.PATH_1, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP09.PATH_2, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneP07.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP07.PATH_1, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneP05.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP05.PATH_1, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP05.PATH_2, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneP03.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneP03.PATH_1, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneP01.PATH_0, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneM01.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneM01.PATH_1, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneM01.PATH_2, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneM03.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneM03.PATH_1, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneM05.PATH_0, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneM07.PATH_0, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneM09.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneM09.PATH_1, longOffset);

		drawTimeZonePath(canvas, paint, TimeZoneM11.PATH_0, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneM11.PATH_1, longOffset);
		drawTimeZonePath(canvas, paint, TimeZoneM11.PATH_2, longOffset);
	}

	private void drawTimeZonePath(Canvas canvas, Paint paint, double[] pathCoordinate, double longOffset) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);

		Path path = new Path();
		path.moveTo((int) ((pathCoordinate[0] - longOffset) / mCoefPixToLongOnMap),
				-(int) (pathCoordinate[1] / mCoefPixToLatOnMap));

		for (int i = 2; i < pathCoordinate.length - 1; i += 2) {
			path.lineTo((int) ((pathCoordinate[i] - longOffset) / mCoefPixToLongOnMap),
					-(int) (pathCoordinate[i + 1] / mCoefPixToLatOnMap));
		}
		path.close();

		canvas.drawPath(path, paint);
		canvas.restore();
	}

	private void drawUserPosition(Canvas canvas) {
		Location userLocation =Cache.getUserLastKnownLocation();
		if (userLocation!=null) {
				Paint paint = new Paint();
				paint.setColor(Color.GREEN);
				paint.setAlpha(255);
				paint.setStyle(Style.FILL);
				paint.setAntiAlias(true);

				drawCircleOnMap(canvas, paint, 3, userLocation.getLongitude(), userLocation.getLatitude(), 0);
				drawCircleOnMap(canvas, paint, 3, userLocation.getLongitude(), userLocation.getLatitude(), -360);
				drawCircleOnMap(canvas, paint, 3, userLocation.getLongitude(), userLocation.getLatitude(), 360);
		}
	}
	
	private void drawFacebookFriendPosition(Canvas canvas) {
		if (mSettingShowFacebookFriends) {
			List<FacebookFriend> friendList = Cache.getFacebookFriendList();

			if (friendList != null) {

				Paint paint = new Paint();
				paint.setColor(Color.RED);
				paint.setAlpha(128);
				paint.setStyle(Style.FILL);
				paint.setAntiAlias(true);

				// Prepare list of gps point
				ArrayList<PointGPS> mFriendGPSCoordinateList = new ArrayList<PointGPS>();
				for (FacebookFriend facebookFriend : friendList) {
					if (!Double.isNaN(facebookFriend.latitude) && !Double.isNaN(facebookFriend.longitude)) {
						mFriendGPSCoordinateList.add(new PointGPS(facebookFriend.latitude, facebookFriend.longitude));
					}
				}

				for (PointGPS friendPointGPS : mFriendGPSCoordinateList) {
					drawCircleOnMap(canvas, paint, 5, friendPointGPS.longitude, friendPointGPS.latitude, 0);
					drawCircleOnMap(canvas, paint, 5, friendPointGPS.longitude, friendPointGPS.latitude, -360);
					drawCircleOnMap(canvas, paint, 5, friendPointGPS.longitude, friendPointGPS.latitude, 360);

				}
		
			}
		}
	}



	private void drawFacebookFriendLine(Canvas canvas) {
		Location userLocation =Cache.getUserLastKnownLocation();
		if (mSettingShowFacebookFriends && mSettingShowSpiderMap && userLocation!=null) {
			List<FacebookFriend> friendList = Cache.getFacebookFriendList();

			if (friendList != null) {

				

				// Prepare list of gps point
				ArrayList<PointGPS> mFriendGPSCoordinateList = new ArrayList<PointGPS>();
				for (FacebookFriend facebookFriend : friendList) {
					if (!Double.isNaN(facebookFriend.latitude) && !Double.isNaN(facebookFriend.longitude)) {
						mFriendGPSCoordinateList.add(new PointGPS(facebookFriend.latitude, facebookFriend.longitude));
					}
				}

				PointGPS userPosition = new PointGPS(userLocation.getLatitude(), userLocation.getLongitude());
				Point3D user3DPosition = Point3D.convertPointGPS(userPosition);
				double dX, dY, dZ, longitude[] = new double[SPIDER_MAP_LINE_STEP], latitude[] = new double[SPIDER_MAP_LINE_STEP];
				for (PointGPS friendPointGPS : mFriendGPSCoordinateList) {

					Point3D friend3DPosition = Point3D.convertPointGPS(friendPointGPS);

					dX = (friend3DPosition.x - user3DPosition.x) / (double) SPIDER_MAP_LINE_STEP;
					dY = (friend3DPosition.y - user3DPosition.y) / (double) SPIDER_MAP_LINE_STEP;
					dZ = (friend3DPosition.z - user3DPosition.z) / (double) SPIDER_MAP_LINE_STEP;

					for (int i = 0; i < SPIDER_MAP_LINE_STEP; i++) {

						PointGPS interGPS = PointGPS.convertPoint3D(new Point3D(user3DPosition.x + dX * i, user3DPosition.y + dY
								* i, user3DPosition.z + dZ * i));

						longitude[i] = interGPS.longitude;
						latitude[i] = interGPS.latitude;

					}

					Paint paint = new Paint();
					paint.setColor(Color.parseColor("#FF7400"));
					paint.setAlpha(255);
					paint.setStyle(Style.STROKE);
					paint.setStrokeWidth(2);
					paint.setAntiAlias(true);
					
					drawPathOnMap(canvas, paint, longitude, latitude, 0);
					
				}
			}
		}
	}

	// --------------------------------------------------------------
	// Tools to draw on Map
	// --------------------------------------------------------------
	private void drawCircleOnMap(Canvas canvas, Paint paint, float radius, double longitude, double latitude, double longOffset) {

		int cx = (int) ((longitude - longOffset) / mCoefPixToLongOnMap);
		int cy = (int) (-latitude / mCoefPixToLatOnMap);

		canvas.drawCircle(cx, cy, radius, paint);

	}

	private void drawPathOnMap(Canvas canvas, Paint paint, double longitude[], double latitude[], double longOffset) {

		if (longitude.length > 0 && latitude.length > 0 && longitude.length == latitude.length) {
			int nbrPoint = longitude.length;
			Path path = new Path();

			path.moveTo((float) ((longitude[0]-longOffset) / mCoefPixToLongOnMap), (float) (-latitude[0] / mCoefPixToLatOnMap));

			for (int i = 1; i < nbrPoint; i++) {
				
				if(i>0 && ( Math.abs(longitude[i-1] -longitude[i])>180)){
					canvas.drawPath(path, paint);
					path.moveTo((float) ((longitude[i]-longOffset) / mCoefPixToLongOnMap), (float) (-latitude[i] / mCoefPixToLatOnMap));
				}else{
				
					path.lineTo((float) ((longitude[i]-longOffset) / mCoefPixToLongOnMap), (float) (-latitude[i] / mCoefPixToLatOnMap));
				}
			
			}

			canvas.drawPath(path, paint);
		} else {
			Log.e(TAG, "drawPathOnMap error");
		}

	}

	private void drawBitmapOnMap(Canvas canvas, Bitmap bitmap, double longitude, double latitude, double longOffset) {
		int x = (int) ((longitude - longOffset) / mCoefPixToLongOnScreen);
		int y = (int) (-latitude / mCoefPixToLatOnScreen);

		canvas.drawBitmap(bitmap, (int) (x - bitmap.getWidth() / 2), y - bitmap.getHeight() / 2, null);
	}

	private void drawMapBitmap(Canvas canvas, Bitmap bitmap) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		Paint paint;
		if (mScreenOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			paint = new Paint();
			paint.setAntiAlias(true);

			if (mLongitudeOffset < 0) {
				Rect src, dst;
				int offset = (int) (-mLongitudeOffset / mCoefPixToLongOnScreen);

				src = new Rect(0, 0, mWidthMap - offset, mHeightMap);
				dst = new Rect(-mWidthScreen / 2 + offset, -mHeightScreen / 2, mWidthScreen / 2, mHeightScreen / 2);
				canvas.drawBitmap(bitmap, src, dst, paint);

				src = new Rect(mWidthMap - offset, 0, mWidthMap, mHeightMap);
				dst = new Rect(-mWidthScreen / 2, -mHeightScreen / 2, -mWidthScreen / 2 + offset, mHeightScreen / 2);
				canvas.drawBitmap(bitmap, src, dst, paint);

			} else {
				Rect src, dst;
				int offset = (int) (mLongitudeOffset / mCoefPixToLongOnScreen);

				src = new Rect(0, 0, offset, mHeightMap);
				dst = new Rect(mWidthScreen / 2 - offset, -mHeightScreen / 2, mWidthScreen / 2, mHeightScreen / 2);
				canvas.drawBitmap(bitmap, src, dst, paint);

				src = new Rect(offset, 0, mWidthMap, mHeightMap);
				dst = new Rect(-mWidthScreen / 2, -mHeightScreen / 2, mWidthScreen / 2 - offset, mHeightScreen / 2);
				canvas.drawBitmap(bitmap, src, dst, paint);
			}
		} else {
			paint = new Paint();
			paint.setAntiAlias(true);

			Rect src, dst;
			int offsetBitmap = (int) ((mLongitudeOffset + 180) / mCoefPixToLongOnMap);

			double windowScreenInBitmap = mWidthMap / mCoefLong;

			src = new Rect((int) (offsetBitmap - windowScreenInBitmap / 2), 0, (int) (offsetBitmap + windowScreenInBitmap / 2),
					mHeightMap);
			dst = new Rect(-mWidthScreen / 2, (int) (-mCoefLat * mHeightScreen / 2), mWidthScreen / 2, (int) (mCoefLat
					* mHeightScreen / 2));
			canvas.drawBitmap(bitmap, src, dst, paint);

			if ((offsetBitmap - windowScreenInBitmap / 2) < 0) {
				src = new Rect((int) (mWidthMap + offsetBitmap - windowScreenInBitmap / 2), 0,
						(int) (mWidthMap + offsetBitmap + windowScreenInBitmap / 2), mHeightMap);
				canvas.drawBitmap(bitmap, src, dst, paint);
			} else if ((offsetBitmap + windowScreenInBitmap / 2) > mWidthMap) {
				src = new Rect((int) (-mWidthMap + offsetBitmap - windowScreenInBitmap / 2), 0,
						(int) (-mWidthMap + offsetBitmap + windowScreenInBitmap / 2), mHeightMap);
				canvas.drawBitmap(bitmap, src, dst, paint);
			}

			paint = new Paint();
			paint.setColor(Color.BLACK);
			canvas.drawRect(new Rect(-mWidthScreen / 2, (int) (-mHeightScreen / 2), mWidthScreen / 2, (int) (-mCoefLat
					* mHeightScreen / 2)), paint);
			canvas.drawRect(new Rect(-mWidthScreen / 2, (int) (mCoefLat * mHeightScreen / 2), mWidthScreen / 2,
					(int) (mHeightScreen / 2)), paint);

		}
		canvas.restore();
	}

	// --------------------------------------------------------------
	// Settings Methods
	// --------------------------------------------------------------

	public boolean isSettingShowSunIcon() {
		return mSettingShowSunIcon;
	}

	public void setSettingShowSunIcon(boolean settingShowSunIcon) {
		mSettingShowSunIcon = settingShowSunIcon;
	}

	//	public boolean isSettingShowFacebookFriends() {
	//		return mSettingShowFacebookFriends;
	//	}
	//
	//	public void setSettingShowFacebookFriends(boolean settingShowFacebookFriends) {
	//		mSettingShowFacebookFriends = settingShowFacebookFriends;
	//	}

	/**
	 * @see SETTING_FOLLOW_NOTHING
	 * @see SETTING_FOLLOW_SUN
	 * @see SETTING_FOLLOW_USER
	 */
	public int isSettingFollow() {
		return mSettingFollow;
	}

	/**
	 * 
	 * @param settingFollow
	 * @see SETTING_FOLLOW_NOTHING
	 * @see SETTING_FOLLOW_SUN
	 * @see SETTING_FOLLOW_USER
	 */
	public void setSettingFollow(int settingFollow) {
		if (mSettingFollow != settingFollow) {
			if (mViewEventListener != null) {
				synchronized (mViewEventListener) {
					mViewEventListener.onNeedRefresh();
				}
			}
		}
		mSettingFollow = settingFollow;
	}

	public boolean isSettingSmoothLight() {
		return mSettingSmoothLight;
	}

	public void setSettingSmoothLight(boolean settingSmoothLight) {
		if (mSettingSmoothLight != settingSmoothLight) {
			mIsComputingMap = true;
			if (mViewEventListener != null) {
				synchronized (mViewEventListener) {
					mViewEventListener.onNeedComputeMap();
				}
			}
		}
		mSettingSmoothLight = settingSmoothLight;
	}

	public boolean isSettingShowTimeZone() {
		return mSettingShowTimeZone;
	}

	public void setSettingShowTimeZone(boolean settingShowTimeZone) {
		if (mSettingShowTimeZone != settingShowTimeZone) {
			mIsComputingMap = true;
			if (mViewEventListener != null) {
				synchronized (mViewEventListener) {
					mViewEventListener.onNeedComputeMap();
				}
			}
		}
		mSettingShowTimeZone = settingShowTimeZone;
	}

	public boolean isSettingShowFacebookFriendse() {
		return mSettingShowFacebookFriends;
	}

	public void setSettingShowFacebookFriends(boolean settingShowFacebookFriends) {
		if (mSettingShowFacebookFriends != settingShowFacebookFriends) {
			mIsComputingMap = true;
			if (mViewEventListener != null) {
				synchronized (mViewEventListener) {
					mViewEventListener.onNeedComputeMap();
				}
			}
		}
		mSettingShowFacebookFriends = settingShowFacebookFriends;
	}
	
	public void setSettingShowSpiderMap(boolean showSpiderMap){
		if (mSettingShowSpiderMap != showSpiderMap) {
			mIsComputingMap = true;
			if (mViewEventListener != null) {
				synchronized (mViewEventListener) {
					mViewEventListener.onNeedComputeMap();
				}
			}
		}
		mSettingShowSpiderMap=showSpiderMap;
	}

	/**
	 * Convert a x screen coordinate into a longitude position
	 * 
	 * @param x
	 *            position in screen
	 * @return longitude of this position
	 */
	public double getLongitudeFromScreenPosition(int x) {
		double longitude;

		longitude = ((x - mWidthScreen / 2) * mCoefPixToLongOnScreen + mLongitudeOffset);

		if (longitude < -180) {
			longitude += 360;
		} else if (longitude > 180) {
			longitude -= 360;
		}
		return longitude;
	}

	/**
	 * Convert a y screen coordinate into a latitude position
	 * 
	 * @param y
	 *            position in screen
	 * @return latitude of this position
	 */
	public double getLatitudeFromScreenPosition(int y) {
		double latitude;

		latitude = (-(y - mHeightScreen / 2) * mCoefPixToLatOnScreen);

		if (latitude < -90) {
			latitude = -90;
		} else if (latitude > 90) {
			latitude = 90;
		}
		return latitude;
	}

	public ViewEvent getViewEventListener() {
		return mViewEventListener;
	}

	public void setOnViewEventListener(ViewEvent viewEventListener) {
		mViewEventListener = viewEventListener;
	}

	public boolean isLoading() {
		return (mIsComputingMap || mIsLoadingFacebookFriends);
	}
	



}
