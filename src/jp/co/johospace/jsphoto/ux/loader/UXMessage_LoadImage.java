package jp.co.johospace.jsphoto.ux.loader;

import android.graphics.Bitmap;
import jp.co.johospace.jsphoto.ux.renderer.UXImageResource;
import jp.co.johospace.jsphoto.ux.thread.UXMessage;

import java.util.ArrayList;

/**
 * イメージ読み込みに成功した場合に送出されるメッセージ
 */
public class UXMessage_LoadImage extends UXMessage{
	private static ArrayList<UXMessage_LoadImage> mRecycleBin = new ArrayList<UXMessage_LoadImage>();

	static{
		registerRecycleBin(mRecycleBin);
	}

	public byte[] compressedImage;
	public Bitmap optBitmap;
	public Object info;
	public UXImageResource resource;
	public int orientation;


	public static synchronized UXMessage_LoadImage create(byte[] compressedImage, Bitmap optBitmap,
														  Object info, UXImageResource res, int orientation){
		UXMessage_LoadImage msg = null;
		if(mRecycleBin.size() != 0){
			msg = mRecycleBin.remove(0);
		}else{
			msg = new UXMessage_LoadImage();
		}

		msg.compressedImage = compressedImage;
		msg.optBitmap = optBitmap;
		msg.info = info;
		msg.resource = res;
		msg.orientation = orientation;

		return msg;
	}

	public static synchronized void recycle(UXMessage_LoadImage msg){
		mRecycleBin.add(msg);
	}

	@Override
	public void recycleMessage(){
		recycle(this);
	}
}
