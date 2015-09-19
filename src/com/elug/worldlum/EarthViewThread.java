package com.elug.worldlum;

import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

public class EarthViewThread extends Thread {
	private SurfaceHolder mSurfaceHolder;
	private EarthDraw mEarthDraw;

	public static final int TIME_REFRESH_MAP = 5000;
	public static final int TIME_REFRESH_LIGHT = 60000;

	public static final int HANDLER_FORCE_REDRAW = 0;
	public static final int HANDLER_FORCE_REDRAW_LIGHT = 1;
	public Handler mHandler;

	private final Runnable mDraw = new Runnable() {
		public void run() {
			Canvas canvas = null;
			try {
				canvas = mSurfaceHolder.lockCanvas(null);
				synchronized (mSurfaceHolder) {
					mEarthDraw.onDraw(canvas);
				}
			} finally {
				// do this in a finally so that if an exception is thrown
				// during the above, we don't leave the Surface in an
				// inconsistent state
				if (canvas != null) {
					mSurfaceHolder.unlockCanvasAndPost(canvas);
				}
			}
			mHandler.removeCallbacks(mDraw);
			mHandler.postDelayed(mDraw, TIME_REFRESH_MAP);
		}
	};

	private final Runnable mQuitLooper = new Runnable() {
		public void run() {
			mHandler.removeCallbacks(mDraw);
			mHandler.removeCallbacks(mComputeLight);
			Looper.myLooper().quit();
		}
	};

	private final Runnable mComputeLight = new Runnable() {
		public void run() {
			mEarthDraw.computeEarthAlphaLayer();
			// Reschedule the next redraw
			mHandler.removeCallbacks(mComputeLight);
			mHandler.postDelayed(mComputeLight, TIME_REFRESH_LIGHT);
			mHandler.sendEmptyMessage(EarthViewThread.HANDLER_FORCE_REDRAW);
		}
	};

	public EarthViewThread(SurfaceHolder surfaceHolder, EarthDraw earthDraw) {
		mSurfaceHolder = surfaceHolder;
		mEarthDraw = earthDraw;
	}

	public void stopThread() {
		if (mHandler != null) {
			mHandler.post(mQuitLooper);
		}
	}

	@Override
	public void run() {

		Looper.prepare();
		mHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
					case HANDLER_FORCE_REDRAW:
						mHandler.removeCallbacks(mDraw);
						mHandler.post(mDraw);
						break;
					case HANDLER_FORCE_REDRAW_LIGHT:
						mHandler.removeCallbacks(mComputeLight);
						mHandler.post(mComputeLight);
						break;
				}
			}
		};

		//start
		mHandler.post(mDraw);
		mHandler.post(mComputeLight);
		
		Looper.loop();
	}


}