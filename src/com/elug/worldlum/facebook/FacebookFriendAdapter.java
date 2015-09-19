package com.elug.worldlum.facebook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.elug.worldlum.ImageManager;
import com.elug.worldlum.R;
import com.elug.worldlum.R.id;
import com.elug.worldlum.R.layout;

public class FacebookFriendAdapter extends ArrayAdapter<FacebookFriend> {
	private ArrayList<FacebookFriend> mFacebookFriendList;
	private Activity mActivity;
	public ImageManager imageManager;

	public FacebookFriendAdapter(Activity activity, int textViewResourceId, List<FacebookFriend> facebookFriendList) {
		super(activity, textViewResourceId, facebookFriendList);
		mFacebookFriendList = new ArrayList<FacebookFriend>(facebookFriendList);
		
		Comparator<FacebookFriend> facebookFriendComparator = new Comparator<FacebookFriend>(){
			@Override
			public int compare(FacebookFriend friendA, FacebookFriend friendB) {
				if(friendA!=null && friendB!=null && friendA.name!=null && friendB.name!=null){
					return friendA.name.compareTo(friendB.name);
				}
				return 0;
			}
			
		};
		Collections.sort(mFacebookFriendList, facebookFriendComparator);
		
		mActivity = activity;

		imageManager = new ImageManager();
	}

	public static class ViewHolder {
		public TextView username;
		public TextView message;
		public ImageView image;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			LayoutInflater vi = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = vi.inflate(R.layout.listitem, null);
			holder = new ViewHolder();
			holder.username = (TextView) convertView.findViewById(R.id.username);
			holder.message = (TextView) convertView.findViewById(R.id.message);
			holder.image = (ImageView) convertView.findViewById(R.id.avatar);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		final FacebookFriend facebookFriend = mFacebookFriendList.get(position);
		if (facebookFriend != null) {

			String imageURL = "http://graph.facebook.com/" + facebookFriend.id + "/picture";
			holder.username.setText(facebookFriend.name);
			holder.message.setText("");
			holder.image.setTag(imageURL);
			imageManager.displayImage(imageURL, mActivity, holder.image);
		}

		return convertView;
	}
}