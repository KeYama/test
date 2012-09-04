package jp.co.johospace.jsphoto.ux.renderer.gl;

import jp.co.johospace.jsphoto.ux.loader.UXMessage_RequestImage;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.ux.renderer.UXImageResource;
import jp.co.johospace.jsphoto.ux.thread.UXChannel;
import android.graphics.Bitmap;
import android.graphics.Rect;

public class GLImageResource implements UXImageResource{
	private static final int STATE_NO_BIND = 0;
	private static final int STATE_REQUEST_IMAGE = 1;
	private static final int STATE_BIND = 2;

	private Object mInfo;
	private int mWidthHint;
	private UXThumbnailLoader mLoader;
	private ImageCallback mCallback;
	private long mLoadId;

	private UXChannel mLoaderChannel, mThumbnailChannel;
	private UXGLRenderEngine mEngine;
	private int mState;
	private int mTexturName;
	private int mWidth, mHeight;
	private int mTexWidth, mTexHeight;
	private boolean mIsBindTexture;
	public GLImageResource(UXGLRenderEngine engine, UXChannel loaderChannel, UXChannel thumbnailChannel){
		mLoaderChannel = loaderChannel;
		mThumbnailChannel = thumbnailChannel;
		mEngine = engine;

		mState = STATE_NO_BIND;
	}

	private int mOrientation;
	private boolean mRotation = true;
	
	@Override
	public boolean cancel(){
		if(mState == STATE_REQUEST_IMAGE){
			purgeMemory();
			return true;
		}
		return false;
	}

	/**
	 * 画像を再読み込みします。
	 */
	public void reloadImage(){
		if(mState == STATE_NO_BIND){
			loadImage(mInfo, mWidthHint, mLoader, mCallback);
		}
	}


	public void clearTexture(){
		//テクスチャを削除
		if(mState == STATE_BIND){
			mEngine.getRenderer().deleteTexture(mTexturName);
			mState = STATE_NO_BIND;
			mIsBindTexture = false;
		}
	}

	@Override
	public void setAutoRotation(boolean flag) {
		mRotation = flag;
	}

	@Override
	public void purgeMemory() {
		//ロードリクエストをキャンセル
		if(mState == STATE_REQUEST_IMAGE)
			cancelImage();

		//テクスチャを削除
		if(mState == STATE_BIND)
			mEngine.getRenderer().deleteTexture(mTexturName);
		mState = STATE_NO_BIND;
		mIsBindTexture = false;
	}

	@Override
	public void dispose() {
		//メモリを開放して、インスタンスも破棄する
		purgeMemory();

		//ヒモづけを解除
		mEngine.unregisterResource(this);
	}

	@Override
	public int getWidth() {
		if(!mIsBindTexture){
			return -1;
		}else{
			return mWidth;
		}
	}

	@Override
	public int getHeight() {
		if(!mIsBindTexture){
			return -1;
		}else{
			return mHeight;
		}
	}

	@Override
	public boolean isValid() {
		return mIsBindTexture;
	}

	/**
	 * glに画像をバインドします。
	 */
	@Override
	public void setImage(Object info, Bitmap bitmap, byte[] compressed, int orientation) {

		switch (mState) {
		case STATE_REQUEST_IMAGE:

			if(mInfo != info){
				break;
			}
			if(bitmap == null && compressed == null){
				if(mCallback != null){
					mCallback.onFailed(this);
					mCallback = null;
				}
				break;
			}

			//glに画像をバインドする
			GLTextureParam texParam = mEngine.getRenderer().bindImage(bitmap);
			mTexWidth = texParam.texWidth;
			mTexHeight = texParam.texHeight;
			mTexturName = texParam.texName;
			mIsBindTexture = true;

			mState = STATE_BIND;

			mWidth = bitmap.getWidth();
			mHeight = bitmap.getHeight();

			mOrientation = orientation;
			break;

		case STATE_BIND:
		case STATE_NO_BIND:
			break;
		}

		if(bitmap != null){
			bitmap.recycle();
			bitmap = null;
		}
	}

	@Override
	public void loadImage(Object info, int widthHint, UXThumbnailLoader loader,
			ImageCallback callback) {

		switch(mState){
		case STATE_NO_BIND:
			mInfo = info;
			requestImage(info, widthHint, loader, callback);
			mState = STATE_REQUEST_IMAGE;
			break;

		case STATE_REQUEST_IMAGE:
			cancelImage();
			requestImage(info, widthHint, loader, callback);
			break;

		case STATE_BIND:
			purgeMemory();
			mInfo = info;
			mWidthHint = widthHint;
			requestImage(info, widthHint, loader, callback);
			mState = STATE_REQUEST_IMAGE;
			break;
		}
	}

	/**
	 * 画像の読み込みをLoaderにリクエストします。
	 * @param info
	 * @param widthHint
	 * @param loader
	 * @param callback
	 */
	private void requestImage(Object info, int widthHint, UXThumbnailLoader loader, ImageCallback callback){
		mInfo = info;
		mWidthHint = widthHint;
		mLoader = loader;
		mCallback = callback;

		if(mIsBindTexture)return;//mTexturName != TEXTUR_INITIAL)return;
		mLoadId = mLoaderChannel.postMessage(UXMessage_RequestImage.create(mInfo, this, mWidthHint, mLoader));
	}

	/**
	 * 画像のロードをキャンセルします。
	 */
	private void cancelImage(){
		mLoaderChannel.cancelMessage(mLoadId);
		mThumbnailChannel.cancelMessage(mLoadId);
	}

	@Override
	public boolean draw(Rect src, Rect dst, float alpha) {
		if(mRotation){
			draw(src, dst, alpha, mOrientation);
			return true;
		}

		//テクスチャがバインドされているか判定
		if(mIsBindTexture){
			mEngine.getRenderer().draw(mTexturName, mTexWidth, mTexHeight, src, dst, alpha, false);
			return true;

		}else{
			return false;
		}
	}

	@Override
	public void draw(Rect src, Rect dst, float alpha, int rotation) {
		if(mIsBindTexture)//mTexturName != TEXTUR_INITIAL)
			mEngine.getRenderer().drawRotation(mTexturName, mTexWidth, mTexHeight, src, dst, alpha, rotation);
	}
}
