package com.elug.worldlum;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
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

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

public class MyUncaughtExceptionHandler implements UncaughtExceptionHandler {
	PackageManager mManager;
	String mPackageName;
	private UncaughtExceptionHandler mDefaultUEH;

	/*
	 * if any of the parameters is null, the respective functionality
	 * will not be used
	 */
	public MyUncaughtExceptionHandler(PackageManager manager,String packageName) {
		mPackageName=packageName;
		mManager=manager;
		mDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
	}

	public void uncaughtException(Thread t, Throwable ex) {

		String stackTrace, versionCode, versionName, phoneModel, phoneProduct, androidVersion, country;

		try {
			PackageInfo info = mManager.getPackageInfo(mPackageName, 0);
			versionCode = String.valueOf(info.versionCode);
			versionName = info.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			mPackageName = "Error";
			versionCode = "-1";
			versionName = "Error";
		}
		// Device model
		phoneModel = Build.MODEL;
		// Device product
		phoneProduct = Build.PRODUCT;
		// Android version
		androidVersion = Build.VERSION.RELEASE;
		// get country
		country = Locale.getDefault().getCountry();
		// Stack trace
		Writer result = new StringWriter();
		PrintWriter printWriter = new PrintWriter(result);
		ex.printStackTrace(printWriter);
		stackTrace = result.toString();
		printWriter.close();

		//REQUEST
		HttpPost httppost = new HttpPost("http://www.e-lug.fr/worldlum/log.php");
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(); //On crée la liste qui contiendra tous nos paramètres
		nameValuePairs.add(new BasicNameValuePair("packageName", mPackageName));
		nameValuePairs.add(new BasicNameValuePair("phoneModel", phoneModel));
		nameValuePairs.add(new BasicNameValuePair("phoneProduct", phoneProduct));
		nameValuePairs.add(new BasicNameValuePair("androidVersion", androidVersion));
		nameValuePairs.add(new BasicNameValuePair("stackTrace", stackTrace));
		nameValuePairs.add(new BasicNameValuePair("versionCode", versionCode));
		nameValuePairs.add(new BasicNameValuePair("versionName", versionName));
		nameValuePairs.add(new BasicNameValuePair("country", country));

		HttpClient httpclient = new DefaultHttpClient();
		try {
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			httpclient.execute(httppost);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		mDefaultUEH.uncaughtException(t, ex);
	}

}
