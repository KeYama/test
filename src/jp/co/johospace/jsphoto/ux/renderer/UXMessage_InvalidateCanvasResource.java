package jp.co.johospace.jsphoto.ux.renderer;

import jp.co.johospace.jsphoto.ux.thread.UXMessage;

import java.util.ArrayList;

/**
 * 再描画メッセージ
 */
public class UXMessage_InvalidateCanvasResource extends UXMessage{
	private static ArrayList<UXMessage_InvalidateCanvasResource> mRecycleBin = new ArrayList<UXMessage_InvalidateCanvasResource>();

	static{
		registerRecycleBin(mRecycleBin);
	}

	public UXCanvasResource resource;

	public static synchronized UXMessage_InvalidateCanvasResource create(UXCanvasResource res){
		UXMessage_InvalidateCanvasResource msg;
		if(mRecycleBin.size() != 0){
			msg = mRecycleBin.remove(0);
		}else{
			msg = new UXMessage_InvalidateCanvasResource();
		}
		msg.resource = res;

		return msg;
	}

	private static synchronized void recycle(UXMessage_InvalidateCanvasResource msg){
		mRecycleBin.add(msg);
	}

	@Override
	public void recycleMessage() {
		recycle(this);
	}
}
