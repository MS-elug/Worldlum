package com.elug.worldlum;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.elug.worldlum.facebook.FacebookFriend;
import com.elug.worldlum.math.PointGPS;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

public class MyApplication extends android.app.Application {

	public static final String VERSION = "V1.0";
	
	
	
	@Override
	public void onCreate(){
		super.onCreate();
		Thread.setDefaultUncaughtExceptionHandler(new MyUncaughtExceptionHandler(getPackageManager(), getPackageName()));
		
		//Initialiaze CacheManager
		CacheManager.getInstance(getApplicationContext()); 
		

	}
}
