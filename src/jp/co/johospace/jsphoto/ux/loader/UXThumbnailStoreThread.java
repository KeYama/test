package jp.co.johospace.jsphoto.ux.loader;

import jp.co.johospace.jsphoto.ux.thread.UXChannel;
import jp.co.johospace.jsphoto.ux.thread.UXMessage;
import jp.co.johospace.jsphoto.ux.thread.UXMessage_End;

/**
 * イメージをキャッシュDBに格納するクラス
 */
public class UXThumbnailStoreThread extends Thread {
	private UXChannel mInputChannel;

	public UXThumbnailStoreThread(){
		mInputChannel = new UXChannel();
		
		setPriority(NORM_PRIORITY-1);
	}

	public UXChannel getChannel(){
		return mInputChannel;
	}

	@Override
	public void run(){
		UXMessage msg = null;

		while(true){
			msg = mInputChannel.waitForMessage();

			if(msg instanceof UXMessage_End){
				break;
			}
			if(msg instanceof UXMessage_StoreImage){
				storeImage((UXMessage_StoreImage)msg);
			}

			msg.recycleMessage();
		}
	}

	private void storeImage(UXMessage_StoreImage msg){
		msg.requestImage.loader.updateCachedThumbnail(msg.requestImage.info, msg.requestImage.widthHint, msg.imageInfo);
	}
}
