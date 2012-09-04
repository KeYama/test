package jp.co.johospace.jsphoto.cache;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;

public class CacheLoader {
	private Channel mWorkerChannel;
	private OrientationLoader mOrientationLoader;
//	static int ic;
//	{
//		synchronized (getClass()) {
//			ic++;
//			System.out.println("new(" + ic + ")");
//		}
//	}
	
	/**
	 * スレッドを解放。アクティビティ終了時に必ず呼ぶ事。
	 */
	public void dispose(){
		if(mWorkerChannel != null)mWorkerChannel.dispose();
		mWorkerChannel = null;
		if(mOrientationLoader != null)mOrientationLoader.dispose();
		mOrientationLoader = null;
		
//		synchronized (getClass()) {
//			ic--;
//			System.out.println("dispose(" + ic + ")");
//		}
	}
	
	/**
	 * 指定されたパスの方向を読み込む
	 * 
	 * @param path
	 * @param callback
	 */
	public void loadOrientation(String path, OrientationLoader.OrientationCallback callback){
		if(mOrientationLoader == null){
			mOrientationLoader = new OrientationLoader();
		}
		mOrientationLoader.loadOrientation(path, callback);
	}
	
	/**
	 * サムネイルを非同期で読み込む
	 * 
	 * @param folder
	 * @param name
	 * @param callback
	 */
	public void loadThumbnail(String folder, String name, ImageCallback callback){		
		if(mWorkerChannel == null){
			mWorkerChannel = new Channel();
		}
		
		mWorkerChannel.putRequest(new RequestData(callback, folder, name));
	}
	
	/**
	 * サムネイルを非同期で読み込む。パス版
	 * 
	 * @param path
	 * @param callback
	 */
	public void loadThumbnail(String path, ImageCallback callback){
		File file = new File(path);
		loadThumbnail(file.getParent(), file.getName(), callback);
	}
	
	/**
	 * イメージの読み込みをキャンセル
	 * 
	 * @param folder
	 * @param name
	 * @param callback
	 */
	public void cancel(String folder, String name, ImageCallback callback){
		if(mWorkerChannel == null){
			mWorkerChannel = new Channel();
		}
		
		mWorkerChannel.remove(folder, name, callback);
	}
	
	/**
	 * イメージの読み込みをキャンセル。パス版
	 * 
	 * @param path
	 * @param c
	 */
	public void cancel(String path, ImageCallback c){
		File f = new File(path);
		cancel(f.getParent(), f.getName(), c);
	}
	
	protected Bitmap loadCache(RequestData data, BitmapFactory.Options opts)
			throws IOException {
		ImageCache.ImageData imageData = new ImageCache.ImageData();
		ImageCache.getImageCache(data.folder, data.name, imageData);
		Bitmap bitmap = BitmapFactory.decodeByteArray(imageData.data, 0, imageData.size, opts);
		return bitmap;
	}

	/**
	 * イメージの非同期読み込みコールバック
	 *
	 */
	public static interface ImageCallback{
		/**
		 * イメージが読み込まれたときに呼ばれるコールバック
		 * 
		 * @param bitmap
		 * @param folder
		 * @param name
		 */
		public void onLoadingImageComplete(BitmapDrawable bitmap, String folder, String name);
		/**
		 * キャッシュ生成に失敗したときに呼ばれるコールバック
		 * 
		 * @param e
		 * @param folder
		 * @param name
		 */
		public void onLoadingImageError(Exception e, String folder, String name);
	}
	
	protected class RequestData{
		public String folder;
		public String name;
		public ImageCallback callback;
		
		public RequestData(ImageCallback c, String p, String n){
			callback = c;
			folder = p;
			name = n;
		}
	}
	
	/**
	 * 実際のワーカスレッド
	 * 
	 * @author tshinsay
	 *
	 */
	private class WorkerThread extends Thread{
		private Channel mChannel;
		
		public WorkerThread(Channel channel){
			mChannel = channel;
		}
		
		@Override
		public void run(){
			RequestData data = null;
			while((data = mChannel.takeRequest()) != null){
				try{
					BitmapFactory.Options opts = new BitmapFactory.Options();
					opts.inScaled = false;
					opts.inPurgeable = true;
					
					Bitmap bitmap = loadCache(data, opts);
					
					setBitmap(data, bitmap);
				}catch(Exception e){
					onError(data, e);
				}
			}
		}
		
		private void setBitmap(final RequestData data, final Bitmap bitmap){
			final BitmapDrawable drawable = new BitmapDrawable(bitmap);
			
			if(!mChannel.post(new Runnable() {
				@Override
				public void run() {
					data.callback.onLoadingImageComplete(drawable, data.folder, data.name);
				}
			}, data)){
				bitmap.recycle();
			}
		}
		
		private void onError(final RequestData data, final Exception e){
			mChannel.post(new Runnable(){

				@Override
				public void run() {
					data.callback.onLoadingImageError(e, data.folder, data.name);
				}
				
			}, data);
		}
	};

	/**
	 * 通信チャンネル＆スレッドプール
	 * 
	 * @author tshinsay
	 *
	 */
	private class Channel{
		private Queue<RequestData> mQueue = new LinkedList<RequestData>();
		private WorkerThread mThread;
		private Handler mHandler = new Handler();
		private boolean mDisposed = false;
	
		public Channel(){
			mThread = new WorkerThread(this);
//			mThread.setPriority(4);
			mThread.start();
		}
		
		public synchronized void putRequest(RequestData data){
			if(mDisposed)return;
			mQueue.add(data);
			
			notifyAll();
		}
		
		public synchronized RequestData takeRequest(){
			if(mDisposed)return null;
			
			while(mQueue.peek() == null){
				try{
					wait();
				}catch(InterruptedException e){}
			}
			
			return mQueue.peek();
		}
		
		public synchronized boolean post(Runnable r, RequestData data){
			if(mDisposed)return false;
			if(!data.equals(mQueue.peek())){
				return false;
			}
			
			mQueue.remove();
			mHandler.post(r);
			return true;
		}
		
		public synchronized void dispose(){
			mDisposed = true;
			mHandler.removeCallbacksAndMessages(null);
			mQueue.clear();
			notifyAll();
		}
		
		public synchronized void remove(String folder, String name, ImageCallback c){
			for(RequestData data: mQueue){
				if(data.folder.equals(folder) && data.name.equals(name) && data.callback.equals(c)){
					mQueue.remove(data);
					return;
				}
			}
		}
	}
}
