package jp.co.johospace.jsphoto.ux.view;

import java.util.HashMap;

import jp.co.johospace.jsphoto.ux.loader.UXCacheLoaderThread;
import jp.co.johospace.jsphoto.ux.loader.UXImageCompressor;
import jp.co.johospace.jsphoto.ux.loader.UXMessage_LoadImage;
import jp.co.johospace.jsphoto.ux.loader.UXMessage_RequestImage;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoaderThread;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailStoreThread;
import jp.co.johospace.jsphoto.ux.thread.UXChannel;
import jp.co.johospace.jsphoto.ux.thread.UXMessage;
import jp.co.johospace.jsphoto.ux.thread.UXMessage_End;

/**
 *
 * ビューにセットするローディング担当クラス
 *
 */
public class UXViewLoader {
	private static UXThumbnailLoaderThread mLoaderThread;
	private static UXThumbnailStoreThread mStoreThread;
	private static UXCacheLoaderThread mCacheThread;
	
	private UXThumbnailLoader mLoader;
	private static HashMap<Object, UXAsyncImageView> mViewMap;
	
	public UXViewLoader(UXThumbnailLoader loader){
		mLoader = loader;
		initialize();
	}
	
	public static void addThread(int num){
		initialize();
		mLoaderThread.addThread(num);
	}
	
	private static void initialize(){
		if(mLoaderThread == null){
			mStoreThread = new UXThumbnailStoreThread();
			mLoaderThread = new MyLoaderThread(mStoreThread.getChannel());
			mCacheThread = new MyCacheThread(mLoaderThread.getChannel(), true);
			mViewMap = new HashMap<Object, UXAsyncImageView>();
			
			mStoreThread.start();
			mLoaderThread.start();
			mCacheThread.start();
		}
	}
	
	public void cancelImage(long id){
		mLoaderThread.getChannel().cancelMessage(id);
	}

	public synchronized long loadImage(UXAsyncImageView view, Object info, int size){
		synchronized(UXViewLoader.class){
			mViewMap.put(info, view);
			return mCacheThread.getChannel().postMessage(UXMessage_RequestImage.create(info, null, size, mLoader));
		}
	}
	
	public static synchronized void onImage(UXMessage_LoadImage msg){
		UXAsyncImageView view = mViewMap.remove(msg.info);
		if(view != null)
			view.onCompleteLoading(msg.info, msg.optBitmap, msg.orientation);
	}
	
	public static void dispose(){
		if(mLoaderThread != null){
			mStoreThread.getChannel().postMessage(new UXMessage_End(), true);
			mLoaderThread.getChannel().postMessage(new UXMessage_End(), true);
			mCacheThread.getChannel().postMessage(new UXMessage_End(), true);
			
			mStoreThread = null;
			mLoaderThread = null;
			mCacheThread = null;
			mViewMap = null;
		}
	}
	
	
	private static  class NullChannel extends UXChannel{

		@Override
		public synchronized long postMessage(UXMessage message,
				boolean emergency) {
			return 0;
		}

		@Override
		public synchronized void repostMessage(UXMessage message,
				boolean emergency) {
		}

		@Override
		public long postMessage(UXMessage message) {
			return 0;
		}

		@Override
		public synchronized boolean postSingleMessage(UXMessage message) {
			return false;
		}

	}
	
	private static class MyCacheThread extends UXCacheLoaderThread{
		public MyCacheThread(UXChannel thumb, boolean needBitmap){
			super(new NullChannel(), thumb, needBitmap);
		}
		
		@Override
		protected void notifyLoad(UXMessage_LoadImage msg){
			onImage(msg);
		}
	}
	
	private static class MyLoaderThread extends UXThumbnailLoaderThread{
		
		public MyLoaderThread(UXChannel store){
			super(new NullChannel(), store, new UXImageCompressor());
		}
		
		
		@Override
		protected void notifyLoad(UXMessage_LoadImage msg){
			onImage(msg);
		}
	}
	
	
//	private static CacheLoaderThread mCacheThread;
//	private static ThumbnailLoaderThread mThumbnailThread;
//
//	private UXThumbnailLoader mLoader;
//
//	public UXViewLoader(UXThumbnailLoader loader){
//		mLoader = loader;
//		initialize();
//	}
//
//	private void initialize(){
//		if(mCacheThread == null){
//			mCacheThread = new CacheLoaderThread();
//			mThumbnailThread = new ThumbnailLoaderThread();
//			mCacheThread.setChannel(mThumbnailThread.getChannel());
//
//			mCacheThread.start();
//			mThumbnailThread.start();
//		}
//	}
//
//	public void cancelImage(long id){
//		if(mCacheThread!=null) {
//			mCacheThread.getChannel().cancelMessage(id);
//		}
//		if(mThumbnailThread!=null) {
//			mThumbnailThread.getChannel().cancelMessage(id);
//		}
//	}
//
//
//	public long loadImage(UXAsyncImageView view, Object info, int size){
//		StartLoading msg = new StartLoading();
//		msg.loader =mLoader;
//		msg.info = info;
//		msg.size = size;
//		msg.target = view;
//
//		if(mCacheThread!=null && mCacheThread.getChannel()!=null) {
//			return mCacheThread.getChannel().postMessage(msg);
//		} else {
//			return UXChannel.INVALID_ID;
//		}
//	}
//
//	/**
//	 *
//	 * ViewLoader共通リソース破棄。
//	 * すべてのViewLoaderを使用することがなくなったら必ず呼び出すこと！！
//	 *
//	 */
//	public static void dispose(){
//		if(mCacheThread != null){
//			mCacheThread.getChannel().postMessage(new UXMessage_End(), true);
//			//待つ必要はない
////			try {
////				mCacheThread.join();
////			} catch (InterruptedException e) {}
//
//			mCacheThread = null;
//
//			mThumbnailThread.getChannel().postMessage(new UXMessage_End(), true);
//
//			//待つ必要なし。
////			try {
////				mThumbnailThread.join();
////			} catch (InterruptedException e) {}
//
//			mThumbnailThread = null;
//		}
//	}
//
//
//	private static class StartLoading extends UXMessage{
//		public UXAsyncImageView target;
//		public Object info;
//		public UXThumbnailLoader loader;
//		public int size;
//	}
//
//	private class CacheLoaderThread extends Thread{
//		private UXChannel mInputChannel = new UXChannel();
//		private UXChannel mThumbnailChannel;
//
//		public CacheLoaderThread(){
//		}
//
//		public UXChannel getChannel(){
//			return mInputChannel;
//		}
//
//		public void setChannel(UXChannel thumbnail){
//			mThumbnailChannel = thumbnail;
//		}
//
//		@Override
//		public void run() {
//			UXMessage msg = null;
//			while(true){
//				msg = mInputChannel.waitForMessage();
//
//				if(msg instanceof UXMessage_End){
//					break;
//
//				}else if(msg instanceof StartLoading){
//					StartLoading m = (StartLoading)msg;
//					UXImageInfo info = new UXImageInfo();
//					if(m.loader.loadCachedThumbnail(m.info, m.size, info)){
//						info.bitmap = BitmapFactory.decodeByteArray(info.compressedImage, 0, info.compressedImage.length);
//						m.target.onCompleteLoading(m.info, info.bitmap, info.orientation);
//					}else{
//						mThumbnailChannel.repostMessage(m, false);
//					}
//				}
//			}
//		}
//	}
//
//	private class ThumbnailLoaderThread extends Thread{
//		private UXChannel mInputChannel = new UXChannel();
//
//		public UXChannel getChannel(){
//			return mInputChannel;
//		}
//
//		@Override
//		public void run() {
//			UXMessage msg = null;
//			while(true){
//				msg = mInputChannel.waitForMessage();
//
//				if(msg instanceof UXMessage_End){
//					break;
//
//				}else if(msg instanceof StartLoading){
//					StartLoading m = (StartLoading)msg;
//					UXImageInfo info = new UXImageInfo();
//					if(m.loader.loadThumbnail(m.info, m.size, info)){
//						ByteArrayOutputStream out = new ByteArrayOutputStream();
//						info.bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
//						info.compressedImage = out.toByteArray();
//						
//						m.loader.updateCachedThumbnail(m.info, m.size, info);
//
//						m.target.onCompleteLoading(m.info, info.bitmap, info.orientation);
//						try{
//							out.close();
//						}catch(IOException e){}
//						
//					}else{
//						m.target.onFailedLoading();
//					}
//				}
//			}
//		}
//	}
}
