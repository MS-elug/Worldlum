package com.elug.worldlum.facebook;

import java.util.ArrayList;

public final class FacebookData {
	private static volatile FacebookData instance = null;

	

	
	private FacebookData() {
		super();
	}


	public final static FacebookData getInstance() {
		if (FacebookData.instance == null) {
			synchronized (FacebookData.class) {
				if (FacebookData.instance == null) {
					FacebookData.instance = new FacebookData();
				}
			}
		}
				
		return FacebookData.instance;
	}

}
