package com.elug.worldlum.wallpaper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import com.elug.worldlum.EarthDraw;
import com.elug.worldlum.R;
import com.elug.worldlum.facebook.FacebookManager;

public class MyWallpaperPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private static final String TAG = "MyWallpaperPreferences";

	private FacebookManager mFacebookManager;
	private SharedPreferences mPrefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//getPreferenceManager().setSharedPreferencesName(MyWallpaperService.SHARED_PREFS_NAME);
		addPreferencesFromResource(R.xml.wallpaper_preferences);

		mFacebookManager = FacebookManager.getInstance(this);

		// Set shared preferences listener
		//mPrefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		mPrefs.registerOnSharedPreferenceChangeListener(this);
		// Simulate a preference change, in order to setup the engine
		onSharedPreferenceChanged(mPrefs, null);

	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, final String key) {
		if (key != null) {
			if (key.equalsIgnoreCase(getString(R.string.wallpaper_preference_facebook_friendslist_key))) {
				boolean showFacebookFriends = sharedPreferences.getBoolean(
						getString(R.string.wallpaper_preference_facebook_friendslist_key),
						getResources().getBoolean(R.bool.wallpaper_preference_facebook_friendslist_default));

				if (showFacebookFriends) {
					mFacebookManager.retrieveFacebookFriends(null, new Runnable() {
						@Override
						public void run() {
							CheckBoxPreference checkBoxPreference = (CheckBoxPreference) getPreferenceScreen()
									.findPreference(key);
							checkBoxPreference.setChecked(false);
						}
					});
				}
			}
		}

	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		mFacebookManager.authorizeCallback(requestCode, resultCode, data);
	}

}
