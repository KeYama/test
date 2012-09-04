package jp.co.johospace.jsphoto.ux.loader;

import java.util.ArrayList;

import jp.co.johospace.jsphoto.ux.thread.UXMessage;

public class UXMessage_CancelLoading extends UXMessage{
	private static ArrayList<UXMessage_CancelLoading> mRecycleBin = new ArrayList<UXMessage_CancelLoading>();
	
	static{
		registerRecycleBin(mRecycleBin);
	}
	
	public long id;
	
	public static synchronized UXMessage_CancelLoading create(long id){
		UXMessage_CancelLoading msg = null;
		if(mRecycleBin.size() != 0){
			msg = mRecycleBin.remove(0);
		}else{
			msg = new UXMessage_CancelLoading();
		}
		
		msg.id = id;
		
		return msg;
	}
	
	public void recycle(UXMessage_CancelLoading msg){
		synchronized(UXMessage_CancelLoading.class){
			mRecycleBin.add(msg);
		}
	}
}
