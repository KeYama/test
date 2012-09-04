package jp.co.johospace.jsphoto.ux.loader;

import jp.co.johospace.jsphoto.ux.renderer.UXImageResource;
import jp.co.johospace.jsphoto.ux.thread.UXMessage;

import java.util.ArrayList;

/**
 * イメージ読み込み要求メッセージ
 */
public class UXMessage_RequestImage extends UXMessage{
	private static ArrayList<UXMessage_RequestImage> mRecycleBin = new ArrayList<UXMessage_RequestImage>();

	static{
		registerRecycleBin(mRecycleBin);
	}

	public Object info;
	public UXImageResource imageResource;
	public int widthHint;
	public UXThumbnailLoader loader;

	public static synchronized UXMessage_RequestImage create(Object info, UXImageResource res,
															 int widthHint, UXThumbnailLoader mLoader){
		UXMessage_RequestImage msg = null;
		if(mRecycleBin.size() != 0){
			msg = mRecycleBin.remove(0);
		}else{
			msg = new UXMessage_RequestImage();
		}

		msg.info = info;
		msg.imageResource = res;
		msg.widthHint = widthHint;
		msg.loader = mLoader;

		return msg;
	}

	public static synchronized void recycle(UXMessage_RequestImage msg){
		mRecycleBin.add(msg);
	}

	@Override
	public void recycleMessage(){
		recycle(this);
	}
}
