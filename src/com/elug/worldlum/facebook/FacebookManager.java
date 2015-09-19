package com.elug.worldlum.facebook;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.elug.worldlum.Cache;
import com.elug.worldlum.MyApplication;
import com.elug.worldlum.R;
import com.elug.worldlum.R.string;
import com.elug.worldlum.math.PointGPS;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.Facebook.DialogListener;

public final class FacebookManager {

	private static final String TAG = "FacebookManager";
	
	private static volatile FacebookManager instance = null;
	
	private Activity mContext;
	private Facebook mFacebook;
	private AsyncFacebookRunner mAsyncRunner;

	public static final String JSON_FACEBOOK_ERROR = "error";
	public static final String JSON_FACEBOOK_ERROR_MESSAGE = "message";
	public static final String JSON_FACEBOOK_ERROR_TYPE = "type";
	public static final String JSON_FACEBOOK_ERROR_TYPE_OAUTHEXCEPTION = "OAuthException";
	public static final String JSON_FACEBOOK_DATA = "data";
	public static final String JSON_FACEBOOK_NAME = "name";
	public static final String JSON_FACEBOOK_ID = "id";
	public static final String JSON_FACEBOOK_LOCATION = "location";
	public static final String JSON_FACEBOOK_LOCATION_ID = "id";
	public static final String JSON_FACEBOOK_LOCATION_LONGITUDE = "longitude";
	public static final String JSON_FACEBOOK_LOCATION_LATITUDE = "latitude";

	private FacebookManager(Activity context) {
		super();
		mContext = context;
		//Prepare facebook api
		mFacebook = new Facebook(mContext.getString(R.string.facebook_app_id));
	}
	
	private void setContext(Activity context){
		mContext = context;
	}

	public final static FacebookManager getInstance(Activity context) {
		
		if (FacebookManager.instance == null) {
			synchronized (FacebookManager.class) {
				if (FacebookManager.instance == null) {
					FacebookManager.instance = new FacebookManager(context);
				}
			}
		}
		FacebookManager.instance.setContext(context);
		
		return FacebookManager.instance;
	}
	
	private RequestListener facebookFriendsListRequestListener = new RequestListener() {

		@Override
		public void onMalformedURLException(MalformedURLException e, Object state) {
			e.printStackTrace();
		}

		@Override
		public void onIOException(IOException e, Object state) {
			e.printStackTrace();
		}

		@Override
		public void onFileNotFoundException(FileNotFoundException e, Object state) {
			e.printStackTrace();
		}

		@Override
		public void onFacebookError(FacebookError e, Object state) {
			e.printStackTrace();
		}

		@Override
		public void onComplete(String response, Object state) {
			try {

				Log.e(TAG, response);

				if (isJsonError(response)) {
					return;
				} else {

					synchronized (Cache.getFacebookFriendList()) {
						Cache.setFacebookFriendList(createFBFriends(response));
						
						// Prepare list of location id
						StringBuilder locationIDList = new StringBuilder();
						for (FacebookFriend facebookFriend : Cache.getFacebookFriendList()) {
							if (facebookFriend.locationId != null) {
								if (locationIDList.length() > 0) {
									locationIDList.append(",");
								}
								locationIDList.append(facebookFriend.locationId);
							}
						}
						
						Bundle parameters = new Bundle();
						parameters.putString("ids", locationIDList.toString());
						parameters.putString("fields", "location");
						mAsyncRunner.request("", parameters, "GET", facebookLocationListRequestListener, null);
					}			
				}

			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	};

	private RequestListener facebookLocationListRequestListener = new RequestListener() {

		@Override
		public void onMalformedURLException(MalformedURLException e, Object state) {
		}

		@Override
		public void onIOException(IOException e, Object state) {
		}

		@Override
		public void onFileNotFoundException(FileNotFoundException e, Object state) {
		}

		@Override
		public void onFacebookError(FacebookError e, Object state) {
		}

		@Override
		public void onComplete(String response, Object state) {
			Log.e(TAG, response);
			JSONObject jResponse;
			try {
				jResponse = new JSONObject(response);

				Iterator<String> iterator = jResponse.keys();
				while (iterator.hasNext()) {
					JSONObject jObject = jResponse.getJSONObject(iterator.next());
					String locationID = jObject.optString(JSON_FACEBOOK_ID);

					JSONObject location = jObject.optJSONObject(JSON_FACEBOOK_LOCATION);
					String latitudeString = location.optString(JSON_FACEBOOK_LOCATION_LATITUDE);
					String longitudeString = location.optString(JSON_FACEBOOK_LOCATION_LONGITUDE);


					synchronized (Cache.getFacebookFriendList()) {
						if (locationID != null && latitudeString != null && longitudeString != null) {
							try {
								double latitude = Double.valueOf(latitudeString);
								double longitude = Double.valueOf(longitudeString);

								for (FacebookFriend facebookFriend : Cache.getFacebookFriendList()) {
									if (facebookFriend.locationId != null
											&& locationID.equalsIgnoreCase(facebookFriend.locationId)) {
										facebookFriend.latitude = latitude;
										facebookFriend.longitude = longitude;
									}
								}
								
							} catch (NumberFormatException e) {
								e.printStackTrace();
							}
						}
					}
				}
				
			} catch (JSONException e) {
				e.printStackTrace();
			}

			

			//
			//			((Activity) mContext).runOnUiThread(new Runnable() {
			//				@Override
			//				public void run() {
			//					mEarthView.setFacebookFriendList(friendGPSCoordinateList);
			//				}
			//			});

		}
	};

	public void retrieveFacebookFriends(Runnable endRunnable,Runnable errorRunnable) {
		
		ConnectivityManager connectivityManager = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		if (networkInfo != null) {
			// System.out.println(networkInfo.getTypeName()); // mobile ou WIFI
			State networkState = networkInfo.getState();
			if (networkState.compareTo(State.CONNECTED) != 0) {
				Toast.makeText(mContext, mContext.getString(R.string.error_no_internet_connection_message), Toast.LENGTH_SHORT).show();
				if(errorRunnable!=null){
					errorRunnable.run();
				}
				return;
			}
		}

		
		refreshFacebookToken(new Runnable() {
			@Override
			public void run() {
				mAsyncRunner = new AsyncFacebookRunner(mFacebook);
				//Prepare request to retrieve friend list
				Bundle parameters = new Bundle();
				parameters.putString("access_token", mFacebook.getAccessToken());
				parameters.putString("fields", "id,name,location");
				mAsyncRunner.request("me/friends", parameters, facebookFriendsListRequestListener);

			}
		},endRunnable,errorRunnable);
	}

	
	public boolean checkTokenStatus(){
	
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		Resources resource = mContext.getResources();
		String defaultAccessToken = resource.getString(R.string.preference_facebook_token_default);
		final String facebookAccessTokenPrefKey = resource.getString(R.string.preference_facebook_token_key);

		String facebookAccessToken = sharedPreferences.getString(facebookAccessTokenPrefKey, defaultAccessToken);

		//Check if token is valid and ask for a new otken if needed
		if (facebookAccessToken.equalsIgnoreCase(defaultAccessToken)) {
			return false;
		}else{
			return true;
		}
	}
	
	/**
	 * Check if a token is already saved, if not get a new one
	 * runnable : the runnable when autorisation is complete
	 */
	private void refreshFacebookToken(final Runnable runnable,final Runnable endRunnable,final Runnable errorRunnable) {
		if (mFacebook != null) {
			final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
			Resources resource = mContext.getResources();
			String defaultAccessToken = resource.getString(R.string.preference_facebook_token_default);
			final String facebookAccessTokenPrefKey = resource.getString(R.string.preference_facebook_token_key);

			String facebookAccessToken = sharedPreferences.getString(facebookAccessTokenPrefKey, defaultAccessToken);

			//Check if token is valid and ask for a new otken if needed
			if (facebookAccessToken.equalsIgnoreCase(defaultAccessToken)) {
				mFacebook.setAccessToken(defaultAccessToken);
				mFacebook.authorize(mContext, new String[] { "publish_stream", "user_location", "friends_location",
						"offline_access" }, new DialogListener() {
					@Override
					public void onComplete(Bundle values) {
						Log.e(TAG,"onComplete");
						//Save the new access token
						SharedPreferences.Editor editor = sharedPreferences.edit();
						editor.putString(facebookAccessTokenPrefKey, mFacebook.getAccessToken());
						editor.commit();

						if (runnable != null) {
							runnable.run();
						}
						
						if (endRunnable != null) {
							endRunnable.run();
						}
					}

					@Override
					public void onFacebookError(FacebookError e) {
						Log.e(TAG,"onFacebookError");
						e.printStackTrace();
						if (errorRunnable != null) {
							errorRunnable.run();
						}
					}

					@Override
					public void onError(DialogError e) {
						Log.e(TAG,"onError");
						e.printStackTrace();
						if (errorRunnable != null) {
							errorRunnable.run();
						}
					}

					@Override
					public void onCancel() {
						Log.e(TAG,"onCancel");
						if (errorRunnable != null) {
							errorRunnable.run();
						}
					}
				});
			} else {
				Log.e(TAG,"Valid Toklen");
				mFacebook.setAccessToken(facebookAccessToken);
				if (runnable != null) {
					runnable.run();
				}
				if (endRunnable != null) {
					endRunnable.run();
				}
			}
		}
	}

	public void authorizeCallback(int requestCode, int resultCode, Intent data) {
		Log.e(TAG,"authorizeCallback");
		if (mFacebook != null) {
			mFacebook.authorizeCallback(requestCode, resultCode, data);
		}
	}

	public boolean isJsonError(String json) throws JSONException {

		JSONObject jObject = new JSONObject(json);
		JSONObject error = jObject.optJSONObject(JSON_FACEBOOK_ERROR);
		if (error != null) {
			//found an error
			String message = error.optString(JSON_FACEBOOK_ERROR_MESSAGE);
			String type = error.optString(JSON_FACEBOOK_ERROR_TYPE);
			if (type.trim().equalsIgnoreCase(JSON_FACEBOOK_ERROR_TYPE_OAUTHEXCEPTION)) {
				Resources resource = mContext.getResources();
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
				String defaultAccessToken = resource.getString(R.string.preference_facebook_token_default);
				String facebookAccessTokenPrefKey = resource.getString(R.string.preference_facebook_token_key);
				//Reset facebook token in order to get a new one

				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putString(facebookAccessTokenPrefKey, defaultAccessToken);
				editor.commit();
			}
			return true;
		}
		return false;
	}

	public static ArrayList<FacebookFriend> createFBFriends(String json) throws JSONException {

		ArrayList<FacebookFriend> fbFriendList = new ArrayList<FacebookFriend>();

		JSONObject jObject = new JSONObject(json);

		JSONArray dataArray = jObject.getJSONArray(JSON_FACEBOOK_DATA);
		for (int i = 0; i < dataArray.length(); i++) {
			JSONObject friend = dataArray.getJSONObject(i);
			JSONObject location = friend.optJSONObject(JSON_FACEBOOK_LOCATION);

			FacebookFriend facebookFriend = new FacebookFriend();
			facebookFriend.id = friend.optString(JSON_FACEBOOK_ID);
			facebookFriend.name = friend.optString(JSON_FACEBOOK_NAME);

			if (location != null) {
				String locID = location.optString(JSON_FACEBOOK_LOCATION_ID);
				if (locID != null && !locID.trim().isEmpty()) {
					facebookFriend.locationId = locID;
				}
			}
			fbFriendList.add(facebookFriend);

		}

		return fbFriendList;
	}
}
