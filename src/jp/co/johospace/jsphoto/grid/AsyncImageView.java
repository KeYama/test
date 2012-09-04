package jp.co.johospace.jsphoto.grid;

import java.io.File;

import jp.co.johospace.jsphoto.cache.CacheLoader;
import jp.co.johospace.jsphoto.cache.OrientationLoader;
import jp.co.johospace.jsphoto.util.MediaUtil;
import jp.co.johospace.jsphoto.util.SizeConv;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.util.AttributeSet;
import android.view.View;

public class AsyncImageView extends View implements CacheLoader.ImageCallback{
	private /*static*/ CacheLoader mLoader;
	
	private static final int INVALID_ORIENTATION = -1;
	
	private String mPath;
	private Bitmap mBitmap;
	private Paint mPaint = new Paint();
	private Paint mRectPaint = new Paint();
	private boolean mCheck = false;
	private boolean mCheckMode = false;
	private boolean mFavorite = false;
	private boolean mGridMode = false;
	private boolean mAutoRotation = false;
	private int mImageOrientation = INVALID_ORIENTATION;
	
	static private Bitmap mBtimapFavorite; 
	static private Bitmap mBtimapCheckOn;
	static private Bitmap mBtimapCheckOff;
	
	private SizeConv sc;
	
	public AsyncImageView(Context context){
		this(context, null);
		sc = new SizeConv(context);
		mRectPaint.setColor(0xff555555);
		mRectPaint.setStyle(Paint.Style.STROKE);
		mRectPaint.setStrokeWidth(sc.getSize(2));
	}
	
	public AsyncImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		sc = new SizeConv(context);
		
//		if(mLoader == null){
//			mLoader = new CacheLoader();
//		}
	}
	
	public void setLoader(CacheLoader loader) {
		mLoader = loader;
	}
	
	public void setAutoRotation(boolean flag){
		mAutoRotation = flag;
	}
	
	public void setGridMode(boolean flag){
		mGridMode = flag;
	}
	
	public void setCheck(boolean flag){
		mCheck = flag;
		invalidate();
	}
	
	public void setCheckMode(boolean flag){
		mCheckMode = flag;
		invalidate();
	}
	
	public void setFavorite(boolean flag){
		mFavorite = flag;
		invalidate();
	}
	
	public void setBitmap(Bitmap b, String path){
		clearDrawable();
		mBitmap = b;
		mPath = path;
		getOrientation();
	}
	
	public void resetBitampNoRcyle(){
		mBitmap = null;
		mPath = null;
	}
	
	public String getPath(){
		return mPath;
	}
	
	public Bitmap getBitmap(){
		return mBitmap;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if(mBitmap != null && mImageOrientation != INVALID_ORIENTATION){
			if(false/*!mGridMode*/){

			}else{
				int bitmapHeight = mBitmap.getHeight();
				int bitmapWidth = mBitmap.getWidth();
				int span = 0;
				Rect src = null;
				if(bitmapWidth > bitmapHeight){
					//横長
					span = (bitmapWidth - bitmapHeight)/2;
					src = new Rect(span, 0, bitmapHeight+span, bitmapHeight);
				}else{
					//縦長
					span = (bitmapHeight - bitmapWidth)/2;
					src = new Rect(0, span, bitmapWidth, bitmapWidth+span);
				}
				Rect dst = new Rect(0, 0, getWidth(), getWidth());
				
				Matrix mat = canvas.getMatrix();
				if(mAutoRotation){
					//androidは右回転、一方Orientationは左回転。よって90=>270, 270=>90に
					//変更
					switch(mImageOrientation){
					case ExifInterface.ORIENTATION_ROTATE_90:
						mat.preTranslate(0, getWidth());
						mat.preRotate(270);
						break;
						
					case ExifInterface.ORIENTATION_ROTATE_180:
						mat.preTranslate(getWidth(), getWidth());
						mat.preRotate(180);
						break;
						
					case ExifInterface.ORIENTATION_ROTATE_270:
						mat.preTranslate(getWidth(), 0);
						mat.preRotate(90);
						break;
					}
				}
				
				canvas.save();
				canvas.setMatrix(mat);
				
				canvas.drawBitmap(mBitmap, src, dst, mPaint);
				if(mGridMode)
					canvas.drawRect(dst, mRectPaint);				
				
				canvas.restore();
				
//				float highSize = (mBitmap.getWidth() > mBitmap.getHeight())?
//						mBitmap.getWidth(): mBitmap.getHeight();
//				float lowSize = (mBitmap.getWidth() < mBitmap.getHeight())?
//						mBitmap.getWidth(): mBitmap.getHeight();
//						
//				float ratio = (float)getWidth() / highSize;
//				canvas.save();
//				Matrix matrix = new Matrix();
//				matrix.preTranslate(
//						(highSize - mBitmap.getWidth()) / 2 * ratio, 
//						(highSize - mBitmap.getHeight()) / 2 * ratio
//					);
//				matrix.preScale(ratio, ratio);
//				
//				//グリッド計算
//				RectF gridF = new RectF(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
//				matrix.mapRect(gridF);
//				
//				matrix.postConcat(canvas.getMatrix());
//				canvas.setMatrix(matrix);
//				canvas.drawBitmap(mBitmap, 0, 0, mPaint);
//				canvas.restore();
//				
//				//枠描画
//				if(mGridMode)
//					canvas.drawRect(gridF, mRectPaint);
			}
		}
		
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		float size = sc.getSize(32);
		float margin = sc.getSize(0);
		RectF rect = new RectF(margin, margin, margin + size, margin + size);
		
		if(mCheck && mCheckMode){
			Bitmap bitmap = getCheckOnBitmap();
			canvas.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), rect, paint);
			/*
			Drawable drawable = getContext().getResources().getDrawable(jp.co.johospace.jsphoto.R.drawable.btn_check_on);
			int height = drawable.getMinimumHeight();
			int width = drawable.getMinimumWidth();
			
			drawable.setBounds(0, 0, width, height);
			
			int thisWidth = getWidth();
			float scale = (float)(thisWidth/2) / width;
			
			Matrix m = canvas.getMatrix();
			m.preScale(scale, scale);
			canvas.save();
			canvas.setMatrix(m);
			drawable.draw(canvas);
			canvas.restore();
			*/
		}else if(mCheckMode){
			Bitmap bitmap = getCheckOffBitmap();
			canvas.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), rect, paint);

			/*
			Drawable drawable = getContext().getResources().getDrawable(jp.co.johospace.jsphoto.R.drawable.btn_check_on_disable);
			int height = drawable.getMinimumHeight();
			int width = drawable.getMinimumWidth();
			
			drawable.setBounds(0, 0, width, height);
			
			int thisWidth = getWidth();
			float scale = (float)(thisWidth/2) / width;
			
			Matrix m = canvas.getMatrix();
			m.preScale(scale, scale);
			canvas.save();
			canvas.setMatrix(m);
			drawable.draw(canvas);
			canvas.restore();			
			*/
		}
		if(mFavorite){
			if(mCheckMode){
				rect.top += size;// + sc.getSize(6);
				rect.bottom += size;// + sc.getSize(6);
			}
			Bitmap bitmap = getFavoriteBitmap();
			canvas.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), rect, paint);
			/*
			Drawable drawable = getContext().getResources().getDrawable(jp.co.johospace.jsphoto.R.drawable.btn_star_big_on);
			int height = drawable.getMinimumHeight();
			int width = drawable.getMinimumWidth();
			
			drawable.setBounds(0, 0, width, height);
			
			int thisWidth = getWidth();
			float scale = (float)(thisWidth/2) / width;
			
			Matrix m = canvas.getMatrix();
			m.preTranslate(0, thisWidth/2);
			m.preScale(scale, scale);
			
			
			canvas.save();
			canvas.setMatrix(m);
			drawable.draw(canvas);
			canvas.restore();		
			*/
		}
	}

	private synchronized Bitmap getFavoriteBitmap() {
		if(mBtimapFavorite==null) {
			mBtimapFavorite = BitmapFactory.decodeResource(getContext().getResources(), jp.co.johospace.jsphoto.R.drawable.btn_star_big_on);
		}
		return mBtimapFavorite;
	}
	
	private synchronized Bitmap getCheckOnBitmap() {
		if(mBtimapCheckOn==null) {
			mBtimapCheckOn = BitmapFactory.decodeResource(getContext().getResources(), jp.co.johospace.jsphoto.R.drawable.btn_check_on);
		}
		return mBtimapCheckOn;
	}
	
	private synchronized Bitmap getCheckOffBitmap() {
		if(mBtimapCheckOff==null) {
			mBtimapCheckOff = BitmapFactory.decodeResource(getContext().getResources(), jp.co.johospace.jsphoto.R.drawable.btn_check_on_disable);
		}
		return mBtimapCheckOff;
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, widthMeasureSpec);
	}

	/**
	 * 破棄メソッド。アクティビティ終了時に呼び出す事！！！
	 * 
	 */
	public /*static*/ void dispose(){
		if(mLoader != null)mLoader.dispose();
		mLoader = null;
		recycleDrawable();
	}
	
	/**
	 * 指定されたパスの画像を読み込む。非同期で読み込まれる。
	 * 
	 * @param path
	 */
	public void loadImage(String path){
		
		if (path == null) {
			clearDrawable();
			mPath = null;
			return;
		}
		
		//回転後のイメージが反映されなくなるので、コメントアウト
//		if(path.equals(mPath))return;
		
//		if (mLoader == null) mLoader = new CacheLoader();
		
		clearDrawable();
		if(mPath != null) mLoader.cancel(mPath, this);
		
		mPath = path;
		getOrientation();
		mLoader.loadThumbnail(path, this);
	}
	
	//画像方向を得る
	private void getOrientation(){
		if(!MediaUtil.getMimeTypeFromPath(mPath).equals("image/jpeg")){
			mImageOrientation = ExifInterface.ORIENTATION_NORMAL;
			return;
		}
		
		mLoader.loadOrientation(mPath, new OrientationLoader.OrientationCallback() {
			
			@Override
			public void onError(String path) {
				if(mLoader != null && path.equals(mPath)){
					mImageOrientation = ExifInterface.ORIENTATION_NORMAL;
					invalidate();
				}
			}
			
			@Override
			public void onComplete(String path, int orientation) {
				if(mLoader != null && path.equals(mPath)){
					mImageOrientation = orientation;
					invalidate();
				}
			}
		});
	}
	
	/**
	 * 現在のDrawableを削除
	 * 
	 */
	public void clearDrawable(){

		
		recycleDrawable();
//		setImageDrawable(null);
	}
	
	private void recycleDrawable(){
//		BitmapDrawable drawable = (BitmapDrawable)getDrawable();
//		if(drawable != null){
//			drawable.getBitmap().recycle();
//		}
		if(mBitmap != null){
			mBitmap.recycle();
			mBitmap = null;
		}
	}
	
	@Override
	protected void onDetachedFromWindow() {
		recycleDrawable();
	}

	@Override
	public void onLoadingImageComplete(BitmapDrawable bitmap, String folder,
			String name) {
		recycleDrawable();
		File pathFile = new File(new File(folder), name);
		
		if(pathFile.getPath().equals(mPath)){
			mBitmap = bitmap.getBitmap();
			//setImageDrawable(bitmap);
		}else{
			if(bitmap.getBitmap() != null)
				bitmap.getBitmap().recycle();
		}	
		invalidate();
	}

	@Override
	public void onLoadingImageError(Exception e, String folder, String name) {
		clearDrawable();
		invalidate();
	}

}