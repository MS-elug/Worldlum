package com.elug.worldlum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

public class EventManager {
	//Transformer en Singleton
	public static final String TAG ="EventManager";
	
	//Liste d'events generaliste, d'autres peuvent etre a definir
	public static final String EVENT_CLOSE_APPLICATION = "EVENT_CLOSE_APPLICATION";
	
	private Map<String,List<EventReceiver>> subscribersMap = new HashMap<String,List<EventReceiver>>();
	
	public interface EventReceiver{
		public void onEventReceived(String eventName);
	}
	
	
	public void addEventSubscriber(String eventName,EventReceiver eventReceiver){
		//TODO
		
		
	}
	
	/**
	 * Send the event fired to all subribers of this event
	 * @param eventName event to fire
	 */
	public void fireEvent(String eventName){
		List<EventReceiver> subscribers = subscribersMap.get(eventName);
		
		if(subscribers!=null && subscribers.size()>0){
			for(EventReceiver subscriber : subscribers){
				try{
					subscriber.onEventReceived(eventName);
				}catch(Exception e){
					Log.w(TAG,"Error with subscriber");
					e.printStackTrace();
				}
			}
		}else{
			Log.w(TAG,"Event not found :\"" + eventName+"\"");
		}
	}
	
	
}
