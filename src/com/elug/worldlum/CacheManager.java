package com.elug.worldlum;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public final class CacheManager {
	public static final String TAG = "CacheManager";

	public static final String DIR_BITMAP = "/bitmap/";
	public static final String DIR_SERIALIZABLE = "/serializable/";
	


	private static volatile CacheManager instance = null;

	private File cacheDir;

	//WARNING NEVER SAVE THE CONTEXT TO AVOID MEMORY LEACK

	private CacheManager() {
		super();
	}
	
	private void update(Context context){
		// Find the dir to save cached images
		String sdState = android.os.Environment.getExternalStorageState();
		if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) {
			File sdDir = android.os.Environment.getExternalStorageDirectory();
			cacheDir = new File(sdDir, context.getString(R.string.path_sd_save_cache));
		} else {
			cacheDir = context.getCacheDir();
		}

		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		
		File cacheBitmapDir = new File(cacheDir, DIR_BITMAP);
		if (!cacheBitmapDir.exists()) {
			cacheBitmapDir.mkdirs();
		}
		
		File cacheSerializableDir = new File(cacheDir, DIR_SERIALIZABLE);
		if (!cacheSerializableDir.exists()) {
			cacheSerializableDir.mkdirs();
		}
	}

	public final static CacheManager getInstance(Context context) {
		if (CacheManager.instance == null) {
			synchronized (CacheManager.class) {
				if (CacheManager.instance == null) {
					CacheManager.instance = new CacheManager();
				}
			}
		}

		CacheManager.instance.update(context);
		return CacheManager.instance;
	}

	public final static CacheManager getInstance() {
		if (CacheManager.instance == null) {
			Log.w(TAG,"CacheManager need first to be instantiated with the application context");
			return null;
		}
		return CacheManager.instance;
	}
	
	public void saveBitmap(Bitmap bmp, String cacheFileName) {
		
		Log.e(TAG,"saveBitmap=" + cacheFileName);
		File f = new File(cacheDir, DIR_BITMAP + cacheFileName);
		FileOutputStream out = null;

		try {
			out = new FileOutputStream(f);
			bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null)
					out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void saveSerializable(Serializable serializableObject, String cacheFileName) {
		
		Log.e(TAG,"saveSerializable=" + cacheFileName);
		Log.e(TAG,"cacheDir=" + cacheDir.getAbsolutePath());
		File f = new File(cacheDir, DIR_SERIALIZABLE + cacheFileName);
		FileOutputStream fos  = null;

		try {
			fos  = new FileOutputStream(f);
			ObjectOutputStream out = new ObjectOutputStream(fos);
			out.writeObject(serializableObject);
			out.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (fos != null)
					fos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param cacheFileName
	 * @return bitmap or null if no cache exists
	 */
	public Bitmap retrieveBitmap(String cacheFileName) {
		Log.e(TAG,"retrieveBitmap=" + cacheFileName);
		File f = new File(cacheDir, DIR_BITMAP  +cacheFileName);

		// Is the bitmap in our cache?
		Bitmap bitmap = BitmapFactory.decodeFile(f.getPath());

		return bitmap;

	}
	
	public <T> T retrieveSerializable(String cacheFileName) {
		Log.e(TAG,"retrieveSerializable=" + cacheFileName);
		File f = new File(cacheDir, DIR_SERIALIZABLE  +cacheFileName);

		T serializableObject=null;
		try {
			FileInputStream fis = new FileInputStream(f);
			ObjectInputStream in = new ObjectInputStream(fis);
			serializableObject = (T) in.readObject();
			in.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			serializableObject=null;
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			serializableObject=null;
		}

		return serializableObject;
	}

}
