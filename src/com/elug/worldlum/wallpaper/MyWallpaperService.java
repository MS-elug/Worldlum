package com.elug.worldlum.wallpaper;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.elug.worldlum.R;
import com.elug.worldlum.EarthDraw;

public class MyWallpaperService extends WallpaperService {

	private final Handler mHandler = new Handler();
	public static final int TIME_REFRESH_MAP = 5000;
	public static final int TIME_REFRESH_LIGHT= 60000;
	
	
	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public Engine onCreateEngine() {
		return new EarthLightEngine();
	}

	class EarthLightEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {

		private EarthDraw mEarthDraw;
		private SharedPreferences mPrefs;

		private final Runnable mDraw = new Runnable() {
			public void run() {
				drawFrame();
			}
		};

		private final Runnable mComputeLight = new Runnable() {
			public void run() {
				mEarthDraw.computeEarthAlphaLayer();
				// Reschedule the next redraw
				mHandler.removeCallbacks(mComputeLight);
				if (mVisible) {
					mHandler.postDelayed(mComputeLight, TIME_REFRESH_LIGHT);
				}
			}
		};

		private boolean mVisible;
		private long mStartTime;

		public EarthLightEngine() {
			mStartTime = SystemClock.elapsedRealtime();
			mEarthDraw = new EarthDraw();

			// Set shared preferences listener
			//mPrefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
			mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			mPrefs.registerOnSharedPreferenceChangeListener(this);
			// Simulate a preference change, in order to setup the engine
			onSharedPreferenceChanged(mPrefs, null);
		}

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);

			// By default we don't get touch events, so enable them.
			setTouchEventsEnabled(true);
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			mHandler.removeCallbacks(mDraw);
			mHandler.removeCallbacks(mComputeLight);
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			mVisible = visible;
			if (visible) {
				drawFrame();
				mHandler.post(mComputeLight);
			} else {
				mHandler.removeCallbacks(mDraw);
				mHandler.removeCallbacks(mComputeLight);
			}
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
			if (!isPreview()) {
				mEarthDraw.setSlideOffset(xOffset, yOffset);
			}
			drawFrame();
		}

		/*
		 * Draw one frame of the animation. This method gets called repeatedly
		 * by posting a delayed Runnable. You can do any drawing you want in
		 * here. This example draws a wireframe cube.
		 */
		void drawFrame() {
			final SurfaceHolder holder = getSurfaceHolder();

			Canvas canvas = null;
			try {
				canvas = holder.lockCanvas();
				if (canvas != null) {
					// draw something
					mEarthDraw.onDraw(canvas);
				}
			} finally {
				if (canvas != null)
					holder.unlockCanvasAndPost(canvas);
			}

			// Reschedule the next redraw
			mHandler.removeCallbacks(mDraw);
			if (mVisible) {
				mHandler.postDelayed(mDraw, TIME_REFRESH_MAP);
			}

			
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			mEarthDraw.init(getApplicationContext(), width, height,true);
			
			drawFrame();
			mHandler.post(mComputeLight);

		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {
			super.onSurfaceCreated(holder);

		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
			mVisible = false;
			mHandler.removeCallbacks(mDraw);
			mHandler.removeCallbacks(mComputeLight);
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

			boolean showFacebookFriends = sharedPreferences.getBoolean(
					getString(R.string.wallpaper_preference_facebook_friendslist_key),
					getResources().getBoolean(R.bool.wallpaper_preference_facebook_friendslist_default));

			boolean showSunIcon = sharedPreferences.getBoolean(getString(R.string.wallpaper_preference_show_sun_icon_key),
					getResources().getBoolean(R.bool.wallpaper_preference_show_sun_icon_default));
			boolean showTimeZone = sharedPreferences.getBoolean(getString(R.string.wallpaper_preference_map_show_time_zone_key),
					getResources().getBoolean(R.bool.wallpaper_preference_map_show_time_zone_default));
			boolean showSmoothLight = sharedPreferences.getBoolean(getString(R.string.wallpaper_preference_map_smooth_light_key),
					getResources().getBoolean(R.bool.wallpaper_preference_map_smooth_light_default));
			
			String follow = sharedPreferences.getString(getString(R.string.wallpaper_preference_follow_key),
					getString(R.string.wallpaper_preference_follow_default));
			if (follow.equalsIgnoreCase("NT_nothing")) {
				mEarthDraw.setSettingFollow(EarthDraw.SETTING_FOLLOW_NOTHING);
			} else if (follow.equalsIgnoreCase("NT_sun")) {
				mEarthDraw.setSettingFollow(EarthDraw.SETTING_FOLLOW_SUN);
			} else if (follow.equalsIgnoreCase("NT_user")) {
				mEarthDraw.setSettingFollow(EarthDraw.SETTING_FOLLOW_USER);
			} 

			mEarthDraw.setSettingShowFacebookFriends(showFacebookFriends);
			mEarthDraw.setSettingShowSunIcon(showSunIcon);
			mEarthDraw.setSettingShowTimeZone(showTimeZone) ;
			mEarthDraw.setSettingSmoothLight(showSmoothLight);
		}

	}

}
