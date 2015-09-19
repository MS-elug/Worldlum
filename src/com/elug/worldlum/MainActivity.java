package com.elug.worldlum;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.elug.worldlum.EarthView.MapClickListener;
import com.elug.worldlum.facebook.FacebookData;
import com.elug.worldlum.facebook.FacebookFriend;
import com.elug.worldlum.facebook.FacebookFriendAdapter;
import com.elug.worldlum.util.LocationUtils;

public class MainActivity extends Activity {
	public static final String TAG = "EarthLightActivity";

	private EarthView mEarthView;
	private RelativeLayout mLoadingLayout;
	public static final double TOUCH_FRIEND_DISTANCE = 10.0; // in degres



	// POST MESSAGE ON USER WALL
	// Bundle parameters = new Bundle();
	// parameters.putString("message",
	// getString(R.string.facebook_action_post_message));
	// parameters.putString("name",
	// getString(R.string.facebook_action_post_name));
	// parameters.putString("link",
	// getString(R.string.facebook_action_post_link));
	// parameters.putString("description",
	// getString(R.string.facebook_action_post_description));
	// parameters.putString("picture",
	// getString(R.string.facebook_action_post_picture_url));
	// mAsyncRunner.request("me/feed", parameters,"POST",
	// facebookLocationListRequestListener,null);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.main);

		mEarthView = (EarthView) findViewById(R.id.earthView);
		mLoadingLayout  = (RelativeLayout) findViewById(R.id.loading_layout);
		mLoadingLayout.setVisibility(View.INVISIBLE);
		mEarthView.setOnMapClickListener(new MapClickListener() {
			@Override
			public void onMapClick(double longitude, double latitude) {
				List<FacebookFriend> friendList = Cache.getFacebookFriendList();

				if (friendList != null && friendList.size() > 0) {

					ArrayList<FacebookFriend> friendSelectedList = new ArrayList<FacebookFriend>();
					final ArrayList<String> friendIdList = new ArrayList<String>();
					for (FacebookFriend facebookFriend : friendList) {

						// Compute distance to the touched point
						double distance = Math.hypot(facebookFriend.latitude - latitude, facebookFriend.longitude - longitude);
						if (distance < TOUCH_FRIEND_DISTANCE) {
							friendSelectedList.add(facebookFriend);
						}

					}

					if (friendSelectedList.size() == 0) {
						return;
					}

					Context mContext = MainActivity.this;
					Dialog dialog = new Dialog(mContext);

					ListView friendListView = new ListView(mContext);
					friendListView
							.setAdapter(new FacebookFriendAdapter(MainActivity.this, R.layout.listitem, friendSelectedList));
					dialog.setContentView(friendListView);
					dialog.setTitle(getString(R.string.dialog_facebook_friend_title));
					dialog.show();

					friendListView.setOnItemClickListener(new OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView parent, View view, int position, long id) {
							//If facebook application already exist open in
							FacebookFriend facebookFriend = ((FacebookFriendAdapter) parent.getAdapter()).getItem(position);
							if (facebookFriend != null) {
								try {
									Intent intent = new Intent(Intent.ACTION_VIEW);
									intent.setClassName("com.facebook.katana", "com.facebook.katana.ProfileTabHostActivity");
									intent.putExtra("extra_user_id", Long.valueOf(facebookFriend.id));

									startActivity(intent);
									return;
								} catch (ActivityNotFoundException e) {
									e.printStackTrace();
								} catch (NumberFormatException e) {
									e.printStackTrace();
								}

								//Else open profile in navigator
								try {
									String url = "http://touch.facebook.com/profile.php?id=" + facebookFriend.id;
									Uri uri = Uri.parse(url);
									Intent intent = new Intent(Intent.ACTION_VIEW, uri);
									startActivity(intent);

									return;
								} catch (ActivityNotFoundException e) {
									e.printStackTrace();
								}

							}
						}
					});

				}
			}
		});


		
//		//print in logcat the hashkey for facebook
//		try {
//			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
//			for (Signature signature : info.signatures) {
//				MessageDigest md = MessageDigest.getInstance("SHA");
//				md.update(signature.toByteArray());
//				Log.i("PXR", "" + Base64.encodeBytes(md.digest()));
//
//			}
//		} catch (NameNotFoundException e) {
//		} catch (NoSuchAlgorithmException e) {
//		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.earth_light_activity, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.menu_exit:
				this.finish();
				return true;
			case R.id.menu_about:
				LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
				View layout = inflater.inflate(R.layout.about, null);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setView(layout);
				builder.setTitle(getString(R.string.about_title));
				AlertDialog alertDialog = builder.create();
				alertDialog.show();
				return true;
			case R.id.menu_share:
				Toast.makeText(this,"Not yet implemented", Toast.LENGTH_SHORT).show();
				return true;
			case R.id.menu_feedback:
				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                emailIntent.setType("plain/text");
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{ getString(R.string.feeback_email_to)});
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.feeback_email_subject));
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.feeback_email_body));
                startActivity(Intent.createChooser(emailIntent, "Send mail..."));
				return true;
			case R.id.menu_settings:
				Intent intent = new Intent(MainActivity.this, AppPreferences.class);
				startActivity(intent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

}