package jp.co.johospace.jsphoto.ux.loader;

import java.util.LinkedHashMap;

import jp.co.johospace.jsphoto.ux.thread.UXChannel;
import jp.co.johospace.jsphoto.ux.thread.UXMessage;
import jp.co.johospace.jsphoto.ux.thread.UXMessage_End;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * �C���[�W���[�h���s���N���X
 */
public class UXCacheLoaderThread extends Thread {
	private UXChannel mInputChannel;
	private UXChannel mOutputChannel;
	private UXChannel mThumbnailChannel;
	private boolean mNeedBitmap;
	
	private class CompressedCache{
		public byte[] cache;
		public int orientation;
	}
	
	private class LimitHashMap extends LinkedHashMap<Object, CompressedCache>{
		public static final int LIMIT = 1 * 1000 * 1000;
		public int mCurrent = 0;

		@Override
		public CompressedCache put(Object key, CompressedCache value) {
			mCurrent += value.cache.length;
			return super.put(key, value);
		}

		@Override
		protected boolean removeEldestEntry(
				Entry<Object, CompressedCache> eldest) {
			if(mCurrent > LIMIT){
				mCurrent -=eldest.getValue().cache.length;
				return true;
			}else{
				return false;
			}
		}
		
	}
	
	private LimitHashMap mMemoryCache;
	private int mWidth;

	public UXCacheLoaderThread(UXChannel out, UXChannel thumb, boolean needBitmap){
		mInputChannel = new UXChannel();
		mOutputChannel = out;
		mThumbnailChannel = thumb;
		mNeedBitmap = needBitmap;
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
			if(msg instanceof UXMessage_RequestImage){
				if(!loadImage((UXMessage_RequestImage)msg))
					msg = null;
			}

			if(msg!=null)msg.recycleMessage();
		}
	}

	private boolean loadImage(UXMessage_RequestImage msg){
		UXImageInfo info = new UXImageInfo();
		boolean result = false;

//		�I���������R���v���X�h�L���b�V��
//
//		if(mWidth != msg.widthHint){
//			mWidth = msg.widthHint;
//			mMemoryCache =  new LimitHashMap();
//		}
//		
//		if(mMemoryCache.containsKey(msg.info)){
//			CompressedCache cache = mMemoryCache.get(msg.info);
//			info.compressedImage = cache.cache;
//			info.orientation = cache.orientation;
//			result = true;
//		}else{
//			result = msg.loader.loadCachedThumbnail(msg.info, msg.widthHint, info);
//			if(result){
//				CompressedCache cache = new CompressedCache();
//				cache.cache = info.compressedImage;
//				cache.orientation = info.orientation;
//				mMemoryCache.put(msg.info, cache);
//			}
//		}
		
		result = msg.loader.loadCachedThumbnail(msg.info, msg.widthHint, info);

		
		if(result){
			Bitmap bitmap = null;
			if(mNeedBitmap){
				BitmapFactory.Options opts = new BitmapFactory.Options();
				opts.inPreferredConfig = Bitmap.Config.RGB_565;
				bitmap = BitmapFactory.decodeByteArray(info.compressedImage, 0, info.compressedImage.length, opts);
			}

			UXMessage_LoadImage msgLoad = UXMessage_LoadImage.create(info.compressedImage,
					bitmap, msg.info, msg.imageResource, info.orientation);
			notifyLoad(msgLoad);
			mOutputChannel.postMessage(msgLoad);
			return true;
		}else{
			mThumbnailChannel.repostMessage(msg, false);
			return false;
		}
	}
	
	protected void notifyLoad(UXMessage_LoadImage msg){
		
	}
}
