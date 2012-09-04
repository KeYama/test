package jp.co.johospace.jsphoto.ux.view;

import java.util.LinkedHashMap;

import jp.co.johospace.jsphoto.util.BitmapUtils;
import jp.co.johospace.jsphoto.ux.thread.UXChannel;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

/**
 *
 * イメージ表示ビュー。以下の機能を持つ
 * ・イメージの非同期読み込み
 * ・invalidateのちらつき対策
 * ・サイズに応じてイメージの自動crop
 *
 * 使い方：
 * ・作成する。
 * ・Viewloaderをセット
 * ・loadImageで非同期読み込み
 * ・RecycleBinに入ったらRecycle
 * ・使い終わったらViewLoader、AsyncImageViewともにdispose
 *
 */
public class UXAsyncImageView extends View{
	private static Handler mHandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(android.os.Message msg) {
			((UXAsyncImageView)msg.obj).invalidate();
		}
	};


	private static int CAPACITY = 200;

	/** イメージ読み込みクラス */
	private UXViewLoader mViewLoader;
	/** 表示すべきビットマップ */
	private Bitmap mBitmap;
	private Bitmap mSaveBitmap;
	
	/** 今表示中のビットマップを表現する情報 */
	private Object mCurrentInfo;
	/** 方向 */
	private int mOrientation;
	/** キャンセル用のメッセージID */
	private long mLoadId = UXChannel.INVALID_ID;
	/** 回転情報を使用するかどうか */
	private boolean mUseRotation;

	/** Bitmap描画に使うPaint */
	private static Paint mBitmapPaint = new Paint();
	/** 再利用を行う箱 */
	private static RecycleBin mRecycleBin;

	private boolean mBorder;
	private int mBorderColor;
	private float mBorderWidth;
	private Paint mBorderPaint;

	public UXAsyncImageView(Context context) {
		super(context);
		initialize();
	}

	public UXAsyncImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	public static void setCacheCapacity(int capacity){
		CAPACITY = capacity;
	}

	private void initialize(){
		if(mRecycleBin == null){
			mRecycleBin = new RecycleBin();
		}
		if(mBorderPaint == null){
			mBorderPaint = new Paint();
			mBorderPaint.setStyle(Paint.Style.STROKE);
		}
	}

	public void setBorder(boolean flag, int color, float borderWidth){
		mBorder = flag;
		mBorderColor = color;
		mBorderWidth = borderWidth;
	}

	/**
	 * 破棄。
	 * 全体が不要になったら必ず呼ぶこと！！！！
	 */
	public static void dispose(){
		if(mRecycleBin != null){
			mRecycleBin.dispose();
			mRecycleBin = null;
		}
	}

	/**
	 * ビューローダーをセットする
	 *
	 * @param loader
	 */
	public void setViewLoader(UXViewLoader loader){
		mViewLoader = loader;
	}

	/**
	 * 指定情報とサイズヒントをもとにイメージ読み込み。非同期
	 *
	 * @param info
	 * @param size
	 */
	public synchronized void loadImage(Object info, int size){
		initialize();
		
		mHandler.sendMessageDelayed(mHandler.obtainMessage(0, this), 5000);

		if(mLoadId != UXChannel.INVALID_ID){
			mViewLoader.cancelImage(mLoadId);
			mLoadId = UXChannel.INVALID_ID;
		}

		if(mBitmap != null){
			recycle();
		}
		
		if(info != null){
			mCurrentInfo = info;
			mBitmap = mRecycleBin.getBitmap(info);
			mSaveBitmap = mRecycleBin.getDrawBitmap(info);
			mOrientation = mRecycleBin.getOrientation(info);
			if(mBitmap == null){
				mLoadId = mViewLoader.loadImage(this, info, size);
				if(mSaveBitmap != null)mSaveBitmap.recycle();
			}else{
				//invalidate();
			}
		}
		//invalidate();
	}

	public synchronized void onCompleteLoading(Object info, Bitmap bitmap, int orientation){
		
		if(info == mCurrentInfo && mCurrentInfo != null){//同一性判断
			mBitmap = bitmap;
			
			mSaveBitmap = null;
			mOrientation = orientation;
			Message msg = mHandler.obtainMessage(0, this);
			mHandler.sendMessage(msg);

		}else{
			bitmap.recycle();
		}
	}

	private static Rect tmpSrc = new Rect();
	private static Rect tmpDst = new Rect();
	private static Matrix tmpMat = new Matrix();
	private static Matrix oldMat = new Matrix();

	@Override
	protected synchronized void onDraw(Canvas canvas) {
		Bitmap bitmap = null;
		int orientation = 0;
		//synchronized (this){
			bitmap = mBitmap;
			orientation = mOrientation == -1 ? 0 : mOrientation;
			if(!mUseRotation)
				orientation = 0;
		//}

		if(bitmap != null){
			float width = getWidth();
			float height = getHeight();

			if(mSaveBitmap == null || mSaveBitmap.isRecycled())
				mSaveBitmap = BitmapUtils.cutdownBitmap(mBitmap, (int)width, (int)height);
			
			if(orientation == 90 || orientation == 270){
				float tmp = width;
				width = height;
				height = tmp;
			}

			float bitmapWidth = bitmap.getWidth();
			float bitmapHeight = bitmap.getHeight();

			if(height == 0)return;

			float ratio = width / height;
			float bitmapRatio = bitmapWidth / bitmapHeight;
			int srcWidth = 0;
			int srcHeight = 0;

			if(ratio < bitmapRatio){
				//縦を合わせる
				srcHeight = (int)bitmapHeight;
				srcWidth = (int)(srcHeight * ratio);
			}else{
				//横を合わせる
				srcWidth = (int)bitmapWidth;
				srcHeight = (int)(bitmapWidth / ratio);
			}

			int dx = ((int)bitmapWidth - srcWidth) / 2;
			int dy = ((int)bitmapHeight - srcHeight) / 2;

			tmpSrc.set(dx, dy, srcWidth + dx, srcHeight + dy);
			tmpDst.set(0, 0, (int)width, (int)height);

			canvas.getMatrix(oldMat);
			canvas.getMatrix(tmpMat);
			if(orientation == 90){
				tmpMat.preTranslate(height,  0);
				tmpMat.preRotate(orientation);
			}else if(orientation == 270){
				tmpMat.preTranslate(0, width);
				tmpMat.preRotate(orientation);
			}else{
				tmpMat.preTranslate(width/2, height/2);
				tmpMat.preRotate(orientation);
				tmpMat.preTranslate(-width/2, -height/2);
			}

			canvas.setMatrix(tmpMat);

			// TODO 暫定対処：確認して問題があれば改善する
			if(tmpSrc.width() < tmpDst.width() || tmpSrc.height() < tmpDst.height()) {
				canvas.drawBitmap(mSaveBitmap, tmpSrc, tmpDst, mBitmapPaint);
			} else {
				canvas.drawBitmap(mSaveBitmap, tmpDst.left, tmpDst.top, mBitmapPaint);
			}

			canvas.setMatrix(oldMat);

			if(mBorder){
				mBorderPaint.setColor(mBorderColor);
				mBorderPaint.setStrokeWidth(mBorderWidth);
				tmpDst.inset((int)mBorderWidth/2, (int)mBorderWidth/2);
				canvas.drawRect(tmpDst, mBorderPaint);
				tmpDst.inset((int)-mBorderWidth/2, (int)-mBorderWidth/2);
			}
		}
	}

	public synchronized void onFailedLoading(){
	}

	/**
	 *
	 * Bitmapのリサイクル指示。
	 * ListViewのRecycleListenerの通知を受け取って、
	 * 呼び出すこと。
	 *
	 */
	public synchronized void recycle(){
		if(mBitmap == null || mCurrentInfo == null)return;
		mRecycleBin.setBitmap(mCurrentInfo, mBitmap, mSaveBitmap, mOrientation);
		mBitmap = null;
		mCurrentInfo = null;
		mSaveBitmap = null;
		invalidate();
	}

	public void setAutoRotation(boolean flag){
		mUseRotation = flag;
		invalidate();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		
//		RecycleBin bin = mRecycleBin;
//		if (bin != null) {
//			bin.dispose();
//		}
		
		if (mDisposeImageResourceOnDetached) {
			disposeImageResources();
		}
	}

	public void disposeImageResources() {
		Bitmap[] bitmaps = {
				mBitmap, mSaveBitmap
		};
		for (Bitmap bitmap : bitmaps) {
			if (bitmap != null && !bitmap.isRecycled()) {
				bitmap.recycle();
			}
		}
		
		
		mBitmap = null;
		mSaveBitmap = null;
		mCurrentInfo = null;
		
//		System.out.println("native resource disposed.");/*$debug$*/
	}
	
	private boolean mDisposeImageResourceOnDetached = true;
	
	public void setDisposeImageResourceOnDetached(boolean b) {
		mDisposeImageResourceOnDetached = b;
	}
	
	public boolean getDisposeImageResourceOnDetached() {
		return mDisposeImageResourceOnDetached;
	}
	
	/**
	 *
	 * 再利用コンテナ
	 * 
	 */
	private class RecycleBin{
		LRUBitmapLinkedList mCache = new LRUBitmapLinkedList();
		LRUBitmapLinkedList mDrawCache = new LRUBitmapLinkedList();
		LRUOrientationLinkedList mOrientationCache = new LRUOrientationLinkedList();
		
		public void dispose(){
			for(Bitmap bitmap: mCache.values()){
				if(bitmap!=null && !bitmap.isRecycled())bitmap.recycle();
			}
			
			mCache.clear();
			
			for(Bitmap bitmap: mDrawCache.values()){
				if(bitmap!=null && !bitmap.isRecycled())bitmap.recycle();
			}
			
			mDrawCache.clear();
		}
		
		public Bitmap getBitmap(Object key){
			return mCache.remove(key);
		}
		
		public Bitmap getDrawBitmap(Object key){
			return mDrawCache.remove(key);
		}
		
		public int getOrientation(Object key){
			Integer ori = mOrientationCache.remove(key);
			if(ori == null)return -1;
			else return ori;
		}
		
		public void setBitmap(Object key, Bitmap bitmap, Bitmap drawBitmap, int rotate){
			mCache.put(key, bitmap);
			//描画前にキャッシュされるとnullの可能性がある
			if(drawBitmap != null)mDrawCache.put(key, drawBitmap);
			mOrientationCache.put(key, rotate);
		}
	}
	
	
	private class LRUOrientationLinkedList extends LinkedHashMap<Object, Integer>{
		public static final int CACHE_SIZE = 200;
		
		public LRUOrientationLinkedList(){
			super(CACHE_SIZE, 1, true);
		}

		@Override
		protected boolean removeEldestEntry(Entry<Object, Integer> eldest) {
			if(size() > CACHE_SIZE){
				return true;
			}
			
			return false;
		}
	}	
	
	private class LRUBitmapLinkedList extends LinkedHashMap<Object, Bitmap>{
		public static final int CACHE_SIZE = 200;
		
		public LRUBitmapLinkedList(){
			super(CACHE_SIZE, 1, true);
		}

		@Override
		protected boolean removeEldestEntry(Entry<Object, Bitmap> eldest) {
			if(size() > CACHE_SIZE){
				Bitmap bitmap = eldest.getValue();
				bitmap.recycle();
				return true;
			}
			
			return false;
		}
	}


//	/**
//	 *
//	 * 再利用コンテナ
//	 *
//	 */
//	private class RecycleBin{
//		private HashMap<Object, Bitmap> mBitmapMap = new HashMap<Object, Bitmap>();
//		private HashMap<Object, Bitmap> mDrawBitmapMap = new HashMap<Object, Bitmap>();
//		private HashMap<Object, Integer> mOrientationMap = new HashMap<Object, Integer>();
//		private int mUseCount = 0;
//
//		/**
//		 * 破棄メソッド
//		 * 内部で呼ばれる
//		 */
//		public void dispose(){
//			for(Bitmap bitmap: mBitmapMap.values()){
//				if(bitmap != null)
//					bitmap.recycle();
//			}
//			mBitmapMap.clear();
//			
//			for(Bitmap bitmap: mDrawBitmapMap.values()){
//				if(bitmap != null)
//					bitmap.recycle();
//			}
//			mDrawBitmapMap.clear();
//		}
//
//		/**
//		 * 指定キーのビットマップを得る
//		 *
//		 * @param key
//		 * @return なければNull
//		 */
//		public Bitmap getBitmap(Object key){
//			if(mBitmapMap.containsKey(key)){
//				Bitmap ret = mBitmapMap.remove(key);
//				mUseCount--;
//
//				return ret;
//			}else{
//				return null;
//			}
//		}
//		
//		public Bitmap getDrawBitmap(Object key){
//			if(mDrawBitmapMap.containsKey(key)){
//				Bitmap ret = mDrawBitmapMap.remove(key);
//				
//				return ret;
//			}else{
//				return null;
//			}
//		}
//		
//		/**
//		 * 指定キーの回転度を返します
//		 * @return なければ -1
//		 */
//		public int getOrientation(Object key){
//			if(mOrientationMap.containsKey(key)){
//				return mOrientationMap.remove(key);
//			}else{
//				return -1;
//			}
//		}
//
//		/**
//		 * Bitmapを追加する。
//		 * はみ出たらはみ出た分は破棄。
//		 *
//		 * @param key
//		 * @param bitmap
//		 */
//		public void setBitmap(Object key, Bitmap bitmap, Bitmap drawBitmap, int rotate){
//			if(key == null)throw new IllegalArgumentException("null");
//			if(bitmap == null)throw new IllegalArgumentException("null");
//			
//			if(mBitmapMap.containsKey(key)){
//				mBitmapMap.remove(key).recycle();
//			}
//
//			mBitmapMap.put(key, bitmap);
//			mDrawBitmapMap.put(key, drawBitmap);
//			mOrientationMap.put(key, rotate);
//			mUseCount++;
//
//			if(CAPACITY < mUseCount + mBitmapMap.size() && mBitmapMap.size() != 0){
//				int removeCount = mUseCount + mBitmapMap.size() - CAPACITY;
//				if(mBitmapMap.size() > removeCount)removeCount = mBitmapMap.size();
//
//				if(removeCount > 0){
//					Object[] ar = new Object[removeCount];
//					for(Object delKey: mBitmapMap.keySet()){
//						removeCount -= 1;
//						ar[removeCount] = delKey;
//						if(removeCount <= 0)break;
//					}
//					for(Object delKey: ar){
//						Bitmap toRecycle = mBitmapMap.remove(delKey);
//						Bitmap drawRecycle = mDrawBitmapMap.remove(delKey);
//						if(toRecycle != null)toRecycle.recycle();
//						if(drawRecycle != null)drawRecycle.recycle();
//					}
//				}
//			}
//		}
//	}
}
