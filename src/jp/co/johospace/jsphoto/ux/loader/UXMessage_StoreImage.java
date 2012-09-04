package jp.co.johospace.jsphoto.ux.loader;

import java.util.ArrayList;

import jp.co.johospace.jsphoto.ux.thread.UXMessage;

/**
 * イメージをキャッシュに保存する場合に送出されるメッセージ
 */
public class UXMessage_StoreImage extends UXMessage{
	private static ArrayList<UXMessage_StoreImage> mRecycleBin = new ArrayList<UXMessage_StoreImage>();

	static{
		registerRecycleBin(mRecycleBin);
	}

	public UXImageInfo imageInfo;
	public UXMessage_RequestImage requestImage;

	public static synchronized UXMessage_StoreImage create(UXMessage_RequestImage requestImage, UXImageInfo imageInfo){
		UXMessage_StoreImage msg = null;
		if(mRecycleBin.size() != 0){
			msg = mRecycleBin.remove(0);
		}else{
			msg = new UXMessage_StoreImage();
		}

		msg.imageInfo = imageInfo;
		msg.requestImage = requestImage;

		return msg;
	}

	public static synchronized void recycle(UXMessage_StoreImage msg){
		mRecycleBin.add(msg);
	}

	@Override
	public void recycleMessage(){
		recycle(this);
	}
}
