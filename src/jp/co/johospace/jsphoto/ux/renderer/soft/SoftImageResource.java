package jp.co.johospace.jsphoto.ux.renderer.soft;

import android.graphics.*;
import jp.co.johospace.jsphoto.ux.loader.UXMessage_RequestImage;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.ux.renderer.UXImageResource;
import jp.co.johospace.jsphoto.ux.thread.UXChannel;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;


/**
 * イメージのソフトウェア版
 */
class SoftImageResource implements UXImageResource {
	private Bitmap mBitmap;
	private UXChannel mLoaderChannel, mThumbnailChannel;
	private Object mInfo;
	private UXSoftRenderEngine mEngine;
	private int mWidthHint;
	private UXThumbnailLoader mLoader;
	private ImageCallback mCallback;

	private int mState;
	private long mLoadId;

	private static final int STATE_EMPTY = 0;
	private static final int STATE_REQUEST_IMAGE = 1;
	private static final int STATE_IMAGE = 2;

	private boolean mAutoRotation = true;
	private int mOrientation = 0;

	private static Paint mBitmapPaint = new Paint();
	
	@Override
	public boolean cancel(){
		if(mState == STATE_REQUEST_IMAGE){
			purgeMemory();
			return true;
		}
		
		return false;
	}

	public SoftImageResource(UXSoftRenderEngine engine, UXChannel loaderChannel, UXChannel thumbnailChannel){
		mEngine = engine;
		mThumbnailChannel = thumbnailChannel;
		mLoaderChannel = loaderChannel;

		mState = STATE_EMPTY;
	}

	@Override
	public void setAutoRotation(boolean flag) {
		mAutoRotation = flag;
	}

	@Override
	public void dispose() {
		purgeMemory();
		mEngine.unregisterResource(this);
	}

	@Override
	public void loadImage(Object info, int widthHint, UXThumbnailLoader loader, ImageCallback callback) {
		switch(mState){
		case STATE_EMPTY:
			mInfo = info;
			requestImage(info, widthHint, loader, callback);
			mState = STATE_REQUEST_IMAGE;
			break;

		case STATE_REQUEST_IMAGE:
			cancelImage();
			requestImage(info, widthHint, loader, callback);
			break;

		case STATE_IMAGE:
			purgeMemory();
			mInfo = info;
			mWidthHint = widthHint;
			requestImage(info, widthHint, loader, callback);
			mState = STATE_REQUEST_IMAGE;
			break;
		}
	}

	@Override
	public void setImage(Object info, Bitmap bitmap, byte[] compressed, int orientation) {
		switch(mState){
		case STATE_REQUEST_IMAGE:
			if(mInfo != info){
				bitmap.recycle();
				break;
			}
			if(bitmap == null && compressed == null){
				if(mCallback != null){
					mCallback.onFailed(this);
					mCallback = null;
				}
				break;
			}

			mBitmap = bitmap;
			mState = STATE_IMAGE;
			if(mCallback != null){
				mCallback.onLoad(this);
				mCallback = null;
			}
			mOrientation = orientation;
			break;

		case STATE_EMPTY:
		case STATE_IMAGE:
			if(bitmap != null)bitmap.recycle();
			break;
		}
	}

	private void requestImage(Object info, int widthHint, UXThumbnailLoader loader, ImageCallback callback){
		mInfo = info;
		mWidthHint = widthHint;
		mLoader = loader;
		mCallback = callback;

		if(mBitmap != null)return;
		mLoadId = mLoaderChannel.postMessage(UXMessage_RequestImage.create(mInfo, this, mWidthHint, mLoader));
	}

	@Override
	public boolean isValid() {
		return mBitmap != null;
	}

	@Override
	public void purgeMemory() {
		switch(mState){
		case STATE_EMPTY:
			break;
		case STATE_REQUEST_IMAGE:
			cancelImage();
			break;
		case STATE_IMAGE:
			//リソース解放は下で
			break;
		}

		if(mBitmap != null)mBitmap.recycle();
		mBitmap = null;
		mState = STATE_EMPTY;
	}

	private void cancelImage(){
		mLoaderChannel.cancelMessage(mLoadId);
		mThumbnailChannel.cancelMessage(mLoadId);
	}

	@Override
	public int getWidth() {
		if(mBitmap != null){
			return mBitmap.getWidth();
		}else{
			return -1;
		}
	}

	@Override
	public int getHeight() {
		if(mBitmap != null){
			return mBitmap.getHeight();
		}else{
			return -1;
		}
	}

	@Override
	public boolean draw(Rect src, Rect dst, float alpha) {
		if(mAutoRotation){
			draw(src, dst, alpha, mOrientation);
			return true;
		}

		if(mBitmap != null){
			Canvas c = mEngine.getSystemCanvas();
			mBitmapPaint.setAlpha((int)(alpha*0xff));
			c.drawBitmap(mBitmap, src, dst, mBitmapPaint);

			return true;
		}else{
			return false;
		}
	}

	@Override
	public void draw(Rect src, Rect dst, float alpha, int rotation) {
		if(mBitmap != null){
			Canvas c = mEngine.getSystemCanvas();
			mBitmapPaint.setAlpha((int)(alpha*0xff));

//			int w = mBitmap.getWidth();
//			int h = mBitmap.getHeight();
			int w = dst.width();
			int h = dst.height();

			c.save();

			Matrix m = c.getMatrix();
			m.preTranslate(dst.left, dst.top);
			m.preTranslate(w/2, h/2);
			m.preRotate(-rotation);
			m.preTranslate(-w / 2, -h / 2);
			c.setMatrix(m);
			dst.offset(-dst.left, -dst.top);
			c.drawBitmap(mBitmap, src, dst, mBitmapPaint);

			c.restore();
		}
	}
}
