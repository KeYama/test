package jp.co.johospace.jsphoto.fullscreen;

import java.util.ArrayList;
import java.util.List;

import jp.co.johospace.jsphoto.fullscreen.loader.ImageLoaderRunner;
import android.graphics.Bitmap;


/**
 * 画像イメージの取得を透過的に行うクラス。
 * 
 * サムネイルは可能な限り連続した三つまでキャッシュ、フルイメージは一つまでキャッシュ。
 */
public class ImageHolder implements MessageDispatcher.Handler{
	private static final int MAX_THUMBNAIL_CACHE = 3;
	
	List<?> mTags;
	List<Integer> mThumbnails;
	ImageLoaderRunner mLoader;
	ThumbnailHolder[] mHolders;
	int mCurrentFullImage;
	Bitmap mFullBitmap;
	
	/**
	 * サムネイルの受け皿。
	 */
	public class ThumbnailHolder{
		private Bitmap mBitmap;
		private boolean mRequested;
		private int mIndex;
		private boolean mCleared;
		
		public ThumbnailHolder(int index){
			mIndex = index;
		}
		
		public void clear(){
			if(mBitmap != null)mBitmap.recycle();
			mBitmap = null;
			mRequested = false;
			mCleared = true;
		}
		
		public Bitmap getBitmap(){
			if(!mRequested){
				mRequested = true;
				mLoader.loadThumbnailImage(mTags.get(mIndex), this);
			}
			return mBitmap;
		}
		
		public void setBitmap(Bitmap b){
			if(mCleared){
				b.recycle();
			}else{
				mBitmap = b;
//				mRequested = false;
			}
		}
	}
	
	public void dispose(){
		for(ThumbnailHolder holder: mHolders){
			if(holder != null)holder.clear();
		}
		if(mFullBitmap != null)mFullBitmap.recycle();
	}
	
	public ImageHolder(ImageLoaderRunner loader, List<?> tags){
		mLoader = loader;
		mTags = tags;
		mHolders = new ThumbnailHolder[tags.size()];
		mThumbnails = new ArrayList<Integer>();
		mCurrentFullImage = -1;
	}
	
	public int size(){
		return mTags.size();
	}
	
	public Object getTag(int at){
		return mTags.get(at);
	}
	
	public void requestThumbnail(int at){
		getThumbnail(at);
	}
	
	public Bitmap getFullImage(int at){
		if(mCurrentFullImage != at){
			mCurrentFullImage = at;
			if(mFullBitmap != null)mFullBitmap.recycle();
			mFullBitmap = null;
			mLoader.loadFullImage(mTags.get(at), new Integer(at));
		}
		
		return mFullBitmap;
	}
	
	public Bitmap getThumbnail(int at){
		if(mThumbnails.contains(at)){
			return getThumbnailHolderAt(at).getBitmap();
		}
		
		if(mThumbnails.size() > MAX_THUMBNAIL_CACHE){
			//距離が一番遠いキャッシュを削除
			int d = Math.abs(at - mThumbnails.get(0));
			int apart = mThumbnails.get(0);
			
			for(Integer num: mThumbnails){
				int tmp = Math.abs(at - num);
				if(tmp > d){
					d = tmp;
					apart = num;
				}
			}
			
			mThumbnails.remove((Integer)apart);
//			getThumbnailHolderAt(apart).clear();
			clearThumbnailHolderAt(apart);
			mLoader.cancelRequest(mTags.get(apart));
		}
		
		mThumbnails.add(at);
		return getThumbnailHolderAt(at).getBitmap();
	}
	
	private ThumbnailHolder getThumbnailHolderAt(int at){
		if(mHolders[at] == null){
			return mHolders[at] = new ThumbnailHolder(at);
		}
		return mHolders[at];
	}
	
	private void clearThumbnailHolderAt(int at){
		if(mHolders[at] != null){
			ThumbnailHolder holder = mHolders[at];
			holder.clear();
			mHolders[at] = null;
		}
	}
	
	/** 指定したサムネイル以外をクリア */
	public void clearThumbnailHolderOther(int at){
		for(int i=0;i<mHolders.length;i++) {
			if(i!=at) {
				if(mHolders[i] != null){
					ThumbnailHolder holder = mHolders[i];
//					System.out.println("clear:" + holder.mBitmap);		/*$debug$*/
					holder.clear();
					mHolders[i] = null;
				}
			}
		}
	}
	
	@Override
	public boolean handleMessage(Object msg) {
		if(msg instanceof MessageChannel.LoadBitmap){
			MessageChannel.LoadBitmap bitmapMsg = (MessageChannel.LoadBitmap)msg;
			if(bitmapMsg.isFull){
				if(((Integer)bitmapMsg.info) != mCurrentFullImage){
					bitmapMsg.bitmap.recycle();
				}else{
					mFullBitmap = bitmapMsg.bitmap;
				}
			}else{
				((ThumbnailHolder)bitmapMsg.info).setBitmap(bitmapMsg.bitmap);
			}
			return true;
		}
		return false;
	}
}
