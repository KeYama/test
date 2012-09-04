package jp.co.johospace.jsphoto.fullscreen;

import jp.co.johospace.jsphoto.JorlleApplication;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;

/**
 * イメージを実際に描画するオブジェクト。
 * 位置の制御はImageScreenが行う
 * 
 */
public class ImageFrame {
	private Measure mMeasure;
	private ImageHolder mHolder;
	/** このImageFrameが配置されている仮想位置。仮想位置に関してはMeasureを参照 */
	private float mVirtualPosition;
	/** 描画するImageHolderの位置 */
	private int mIndex;
	private ImageScreen mScreen;
	private Matrix mMatrix;
	private Paint mPaint;
	
	private int mOrientation;
	
	
	public ImageFrame(Measure measure, ImageHolder holder, float position, int index, int orientation){
		mMeasure = measure;
		mHolder = holder;
		mVirtualPosition = position;
		mMatrix = new Matrix();
		mIndex = index;
		mScreen = new ImageScreen(this, measure);
		mPaint = new Paint();
		
		if(PreferenceUtil.getBooleanPreferenceValue(
				JorlleApplication.instance().getApplicationContext(), 
				ApplicationDefine.PREF_IMAGE_AUTO_ROTATION, true)){
			mOrientation = orientation;
		}else{
			mOrientation = ExifInterface.ORIENTATION_NORMAL;
		}
	}
	
	public Matrix getBaseMatrix(){
		Matrix mat = new Matrix();
		Bitmap b = getBitmap();
		if(b != null){
		switch(mOrientation){
			case ExifInterface.ORIENTATION_ROTATE_90:
				mat.preTranslate(b.getHeight(), 0);
				mat.preRotate(90);
				break;
				
			case ExifInterface.ORIENTATION_ROTATE_180:
				mat.preTranslate(b.getWidth(), b.getHeight());
				mat.preRotate(180);
				break;
				
			case ExifInterface.ORIENTATION_ROTATE_270:
				mat.preTranslate(0, b.getWidth());
				mat.preRotate(270);
				break;
			}
		}
		
		return mat;
	}
	
	public ImageScreen getScreen(){
		return mScreen;
	}
	
	public void draw(Canvas c, Matrix m){
		Bitmap b = getBitmap();
		if(b != null){
			float x = mMeasure.toWorldX(mVirtualPosition);
			mMatrix.reset();
			mMatrix.set(getBaseMatrix());
			mMatrix.postConcat(mScreen.getMatrix());
			mMatrix.postTranslate(x, 0);
			mMatrix.postConcat(m);
			
			c.drawBitmap(b, mMatrix, mPaint);
		}
	}
	
	public boolean process(){
		return mScreen.process();
	}
	
	/**
	 * ビットマップを得る。
	 * 拡大中であれば可能な限りフルイメージを得る
	 * 
	 * @return
	 */
	private Bitmap getBitmap(){
		if(mScreen.isZoom()){
			Bitmap b = mHolder.getFullImage(mIndex);
			if(b == null)return mHolder.getThumbnail(mIndex);
			else return b;
		}else{
			return mHolder.getThumbnail(mIndex);
		}
	}
	
	/**
	 * ビットマップの幅を問い合わせる
	 * 変更される可能性があるので、必要な時に都度問い合わせること
	 * 
	 * @return
	 */
	public float getWidth(){
		Bitmap b = getBitmap();
		if(b != null){
			switch(mOrientation){
			case ExifInterface.ORIENTATION_ROTATE_90:
			case ExifInterface.ORIENTATION_ROTATE_270:
				return b.getHeight();
				
			default:
				return b.getWidth();
			}
			//return b.getWidth();
		}else{
			return 0;
		}
	}
	
	/**
	 * ビットマップの高さを問い合わせる
	 * 変更される可能性があるので、必要な時に都度問い合わせること
	 * 
	 * @return
	 */
	public float getHeight(){
		Bitmap b = getBitmap();
		if(b != null){
			switch(mOrientation){
			case ExifInterface.ORIENTATION_ROTATE_90:
			case ExifInterface.ORIENTATION_ROTATE_270:
				return b.getWidth();
				
			default:
				return b.getHeight();
			}
			//return b.getHeight();
		}else{
			return 0;
		}
	}
	
	/**
	 * ビットマップが読み込まれ、描画準備が完了しているか否か
	 * 
	 * @return
	 */
	public boolean ready(){
		return mHolder.getThumbnail(mIndex) != null;
	}
	
	/**
	 * 前後のサムネイルクリア
	 * @param at
	 */
	public void clearThumbnailHolderOther(){
		if(mHolder!=null) {
			mHolder.clearThumbnailHolderOther(mIndex);
		}
	}
	
	/**
	 * 破棄
	 */
	public void dispose(){
		mHolder.dispose();
	}
}
