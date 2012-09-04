package jp.co.johospace.jsphoto.ux.thread;

import java.util.ArrayList;
import java.util.Collection;

/**
 * チャンネルを流れるメッセージのベースクラス
 */
public class UXMessage {
	private long mId;

	private static ArrayList<Collection<?>> mRecycleBins;

	public static void registerRecycleBin(Collection<?> recycleBin){
		if(mRecycleBins == null){
			mRecycleBins = new ArrayList<Collection<?>>();
		}
		mRecycleBins.add(recycleBin);
	}

	public static void clear(){
		ArrayList<Collection<?>> recycleBins = mRecycleBins;
		if (recycleBins != null) {
			for(Collection<?> recycleBin: recycleBins){
				recycleBin.clear();
			}
		}
	}

	public void setId(long id){
		mId = id;
	}

	public long getId(){
		return mId;
	}

	public void recycleMessage(){
		//do nothing
	}
}
