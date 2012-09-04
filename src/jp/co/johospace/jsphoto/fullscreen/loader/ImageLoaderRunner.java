package jp.co.johospace.jsphoto.fullscreen.loader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.co.johospace.jsphoto.fullscreen.MessageChannel;
import jp.co.johospace.jsphoto.fullscreen.MessageDispatcher;
import android.graphics.Bitmap;

/**
 * ImageLoaderを別スレッドで走らせるクラス
 * 
 */
public class ImageLoaderRunner {
	private MessageDispatcher mDispatcher;
	private ImageLoader mLoader;
	private ExecutorService mExecutor;
	
	private int mScreenWidth, mScreenHeight;
	
	public ImageLoaderRunner(MessageDispatcher channel, ImageLoaderFactory factory){
		mDispatcher = channel;
		mLoader = factory.create();
		mExecutor = Executors.newFixedThreadPool(1);
	}
	
	public synchronized void setScreenSize(int width, int height){
		mScreenWidth = width;
		mScreenHeight = height;
	}
	
	public synchronized void getScreenSize(int[] size){
		size[0] = mScreenWidth;
		size[1] = mScreenHeight;
	}
	
	public void loadFullImage(final Object tag, final Object info){
		mExecutor.execute(new Runnable(){

			@Override
			public void run() {
				Bitmap bitmap = mLoader.loadFullImage(tag);
				if(bitmap != null){
					mDispatcher.putMessage(new MessageChannel.LoadBitmap(bitmap, tag, info, true));
				}else{
					mDispatcher.putMessage(new MessageChannel.FailedLoadingBitmap(tag, info));
				}
			}
			
		});
	}
	
	public void loadThumbnailImage(final Object tag, final Object info){
		mExecutor.execute(new Runnable(){

			@Override
			public void run() {
				int[] size = new int[2];
				getScreenSize(size);
				Bitmap bitmap = mLoader.loadThumbnailImage(tag, size[0], size[1]);
				if(bitmap != null){
					mDispatcher.putMessage(new MessageChannel.LoadBitmap(bitmap, tag, info, false));
				}else{
					mDispatcher.putMessage(new MessageChannel.FailedLoadingBitmap(tag, info));
				}
			}
			
		});
	}
	
	public void cancelRequest(Object tag){
		mLoader.cancel(tag);
	}
}
