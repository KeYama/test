package jp.co.johospace.jsphoto.ux.loader;

import java.util.ArrayList;

import jp.co.johospace.jsphoto.ux.thread.UXChannel;
import jp.co.johospace.jsphoto.ux.thread.UXMessage;
import jp.co.johospace.jsphoto.ux.thread.UXMessage_End;

/**
 * イメージロードを行うクラス
 */
public class UXThumbnailLoaderThread extends Thread {
	private UXChannel mInputChannel;
	private UXChannel mOutputChannel;
	private UXChannel mStoreChannel;
	private UXImageCompressor mCompressor;
	private ArrayList<ThumbnailLoadThread> mThreads = new ArrayList<UXThumbnailLoaderThread.ThumbnailLoadThread>();
	private ArrayList<ThumbnailLoadThread> mFreeThread = new ArrayList<UXThumbnailLoaderThread.ThumbnailLoadThread>();
	private ArrayList<UXMessage_RequestImage> mMessagePool = new ArrayList<UXMessage_RequestImage>();

	public UXThumbnailLoaderThread(UXChannel out, UXChannel store, UXImageCompressor compressor){
		mInputChannel = new MyChannel();
		mOutputChannel = out;
		mStoreChannel = store;
		mCompressor = compressor;

		addThreadInternal(1);
		
		setPriority(NORM_PRIORITY-1);
	}
	
	private class MyChannel extends UXChannel{

		@Override
		public synchronized boolean cancelMessage(long id) {
			mInputChannel.postMessage(UXMessage_CancelLoading.create(id));
			return super.cancelMessage(id);
		}
		
	}
	
	public void addThread(int num){
		mInputChannel.postMessage(new UXMessage_AddThread(num));
	}
	
	private void addThreadInternal(int num){
		for(int n = 0; n < num; ++n){
			ThumbnailLoadThread th = new ThumbnailLoadThread(mOutputChannel, mInputChannel, mStoreChannel, mCompressor);
			mThreads.add(th);
			mFreeThread.add(th);
			th.setPriority(NORM_PRIORITY-1);
			th.start();
		}
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
				for(ThumbnailLoadThread th: mThreads){
					th.getChannel().postMessage(new UXMessage_End(), true);
				}
				break;
			}else if(msg instanceof UXMessage_AddThread){
				addThreadInternal(((UXMessage_AddThread)msg).toAdd);
			}else if(msg instanceof UXMessage_CancelLoading){
				removeRequestMessage(((UXMessage_CancelLoading)msg).id);
			}else if(msg instanceof UXMessage_RequestImage){
				mMessagePool.add((UXMessage_RequestImage)msg);
				msg = null;
			}else if(msg instanceof UXMessage_FinishLoading){
				mFreeThread.add(((UXMessage_FinishLoading)msg).thread);
			}
			
			checkLoading();

			if(msg != null)msg.recycleMessage();
		}
	}
	
	private void removeRequestMessage(long id){
		for(UXMessage_RequestImage msg: mMessagePool){
			if(msg.getId() == id){
				mMessagePool.remove(msg);
				return;
			}
		}
	}
	
	private void checkLoading(){
		if(mMessagePool.size() != 0 && mFreeThread.size() != 0){
			mFreeThread.remove(0).getChannel().postMessage(mMessagePool.remove(0));
		}
	}

	
	public static class UXMessage_AddThread extends UXMessage{
		public int toAdd;
		
		public UXMessage_AddThread(int num){
			toAdd = num;
		}
	}
	
	private static class UXMessage_FinishLoading extends UXMessage{
		private static ArrayList<UXMessage_FinishLoading> mRecycleBin = new ArrayList<UXMessage_FinishLoading>();
		
		static{
			registerRecycleBin(mRecycleBin);
		}
		
		public ThumbnailLoadThread thread;
		
		public static synchronized UXMessage_FinishLoading create(ThumbnailLoadThread argThread){
			UXMessage_FinishLoading msg = null;
			if(mRecycleBin.size() != 0){
				msg = mRecycleBin.remove(0);
			}else{
				msg = new UXMessage_FinishLoading();
			}
			
			msg.thread = argThread;
			
			return msg;
		}
		
		@Override
		public void recycleMessage(){
			synchronized(UXMessage_FinishLoading.class){
				mRecycleBin.add(this);
			}
		}
	}
	
	/** 実際にデコードを行うスレッド */
	private class ThumbnailLoadThread extends Thread{
		private UXChannel mOutputChannel, mParentChannel, mInputChannel, mStoreChannel;
		private UXImageCompressor mCompressor;
		
		public UXChannel getChannel(){
			return mInputChannel;
		}
		
		public ThumbnailLoadThread(UXChannel output, UXChannel parent, UXChannel store, UXImageCompressor compressor){
			mOutputChannel = output;
			mParentChannel = parent;
			mStoreChannel = store;
			
			mInputChannel = new UXChannel();
			mCompressor = compressor;
			
		}
		
		
		@Override
		public void run(){
			while (true) {
				try {
					UXMessage msg = mInputChannel.waitForMessage();
					if (msg instanceof UXMessage_End) {
						break;
					}
					
					try {
						if (msg instanceof UXMessage_RequestImage) {
							loadImage((UXMessage_RequestImage) msg);
						}
					} finally {
						try {
							mParentChannel.postMessage(UXMessage_FinishLoading
									.create(this));
						} finally {
							msg.recycleMessage();
						}
					}
				} catch (Exception e) {
					;
				}
			}
		}
		
		private void loadImage(UXMessage_RequestImage msg){
			//TODO: マスターサムネイル（すべての縮小元になる比較的大きなサムネイル）について考える
			UXImageInfo info = new UXImageInfo();
			UXImageInfo mTmpInfo = new UXImageInfo();

			boolean result;
			try {
				result = msg.loader.loadThumbnail(msg.info, msg.widthHint, info);
			} catch (Exception e) {
				result = false;
			}
			
			if(!result){
				mOutputChannel.postMessage(new UXMessage_FailedImage(msg.info, msg.imageResource));
			}else{
				try {
					mCompressor.compress(info, msg.widthHint, mTmpInfo);
					UXMessage_LoadImage msgLoad = UXMessage_LoadImage.create(mTmpInfo.compressedImage, mTmpInfo.bitmap,
							msg.info, msg.imageResource, info.orientation);
					notifyLoad(msgLoad);
					mOutputChannel.postMessage(msgLoad);
					
				} catch (Exception e) {
					mOutputChannel.postMessage(new UXMessage_FailedImage(msg.info, msg.imageResource));
					return;
				}

//				msg.loader.updateCachedThumbnail(msg.info, msg.widthHint, mTmpInfo);
				UXImageInfo tmp = new UXImageInfo();
				tmp.bitmap = mTmpInfo.bitmap;
				tmp.compressedImage = mTmpInfo.compressedImage;
				tmp.orientation = mTmpInfo.orientation;
				mStoreChannel.postMessage(UXMessage_StoreImage.create(msg, tmp));
			}
		}
	}
	
	protected void notifyLoad(UXMessage_LoadImage msg){
		
	}
}
