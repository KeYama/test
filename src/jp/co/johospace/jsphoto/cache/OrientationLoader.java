package jp.co.johospace.jsphoto.cache;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.media.ExifInterface;
import android.os.Handler;

public class OrientationLoader {
	private ExecutorService mPool;
	private Handler mHandler = new Handler();
	
	public interface OrientationCallback{
		public void onComplete(String path, int orientation);
		public void onError(String path);
	}
	
	public OrientationLoader(){
		mPool = Executors.newFixedThreadPool(1);
	}
	
	public void dispose(){
		mPool.shutdown();
	}
	
	public void loadOrientation(final String path, final OrientationCallback callback){
		mPool.execute(new Runnable() {
			
			@Override
			public void run() {
				try{
					final ExifInterface exif = new ExifInterface(path);

					mHandler.post(new Runnable() {
						
						@Override
						public void run() {
							callback.onComplete(path, 
									exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 
											ExifInterface.ORIENTATION_NORMAL));
						}
					});
				}catch(IOException e){
					mHandler.post(new Runnable() {
						
						@Override
						public void run() {
							callback.onError(path);
						}
					});
				}
			}
		});
	}
}
