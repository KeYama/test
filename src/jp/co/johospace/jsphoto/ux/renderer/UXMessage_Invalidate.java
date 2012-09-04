package jp.co.johospace.jsphoto.ux.renderer;

import jp.co.johospace.jsphoto.ux.thread.UXMessage;

import java.util.ArrayList;

/**
 * 再描画メッセージ
 */
public class UXMessage_Invalidate extends UXMessage{
	private static ArrayList<UXMessage_Invalidate> mRecycleBin = new ArrayList<UXMessage_Invalidate>();

	static{
		registerRecycleBin(mRecycleBin);
	}

	public static synchronized UXMessage_Invalidate create(){
		if(mRecycleBin.size() != 0){
			return mRecycleBin.remove(0);
		}else{
			return new UXMessage_Invalidate();
		}
	}

	private static synchronized void recycle(UXMessage_Invalidate msg){
		mRecycleBin.add(msg);
	}

	@Override
	public void recycleMessage() {
		recycle(this);
	}
}
