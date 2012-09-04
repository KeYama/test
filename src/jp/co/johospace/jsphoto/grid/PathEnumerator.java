package jp.co.johospace.jsphoto.grid;

import java.io.File;

import android.os.Handler;

/**
 * パスを再帰的に列挙するクラス
 * 
 * @author tshinsay
 *
 */
public class PathEnumerator {
	Handler mHandler = new Handler();
	PathCallback mCallback;
	
	public PathEnumerator(PathCallback callback){
		mCallback = callback;
	}
	
	public static interface PathCallback{
		public void onFind(File file);
		public void onComplete();
	}
	
	public synchronized void dispose(){
		mCallback = null;
	}
	
	/**
	 * 列挙を開始する
	 * 
	 * @param baseFile
	 * @param subfolder
	 */
	public void startEnumeration(final File baseFile, final boolean subfolder){
		new Thread(new Runnable(){
			@Override
			public void run(){
				findPath(baseFile, subfolder);
				mHandler.post(new Runnable() {
					
					@Override
					public void run() {
						if(mCallback != null) mCallback.onComplete();
					}
				});
			}
		}).start();
	}
	
	/**
	 * 再帰的にファイルを列挙する
	 * 
	 * @param baseFile
	 * @param subfolder
	 */
	private void findPath(File baseFile, boolean subfolder){
		File[] files = baseFile.listFiles();
		if(files == null)return;
		
		for(File file: files){
			if(!callOnFind(file)) break;
			if(subfolder && file.isDirectory()){
				findPath(file, subfolder);
			}
		}
	}
	
	/**
	 * 見つかったファイルをコールバックに渡す
	 * 
	 * @param file
	 * @return
	 */
	private synchronized boolean callOnFind(final File file){
		if(mCallback == null) return false;
		
		mHandler.post(new Runnable(){
			@Override
			public void run(){
				synchronized(PathEnumerator.this){
					if(mCallback != null)mCallback.onFind(file);
				}
			}
		});
		
		return true;
	}
}
