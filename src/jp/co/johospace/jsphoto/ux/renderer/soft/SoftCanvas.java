package jp.co.johospace.jsphoto.ux.renderer.soft;

import android.graphics.Canvas;
import android.graphics.Color;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import jp.co.johospace.jsphoto.ux.renderer.UXMessage_Invalidate;
import jp.co.johospace.jsphoto.ux.thread.UXChannel;

/**
 * ソフトウェアキャンバスオブジェクト
 */
class SoftCanvas implements SurfaceHolder.Callback{
	SurfaceHolder mHolder;
	boolean mCanDraw;
	int mWidth;
	int mHeight;
	Canvas mCanvas;
	UXChannel mRenderChannel;
	int mColor = Color.BLACK;

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}

	public SoftCanvas(SurfaceView view, UXChannel renderChannel){
		view.getHolder().addCallback(this);
		mHolder = view.getHolder();
		mRenderChannel = renderChannel;
	}

	public synchronized boolean begin() {
		if(mCanDraw){
			mCanvas = mHolder.lockCanvas();
			if(mCanvas != null)
				mCanvas.drawColor(mColor);
		}

		return mCanDraw && mCanvas != null;
	}

	public synchronized void end() {
		if(mCanvas != null){
			mHolder.unlockCanvasAndPost(mCanvas);
			mCanvas = null;
		}
	}

	public Canvas getCanvas(){
		return mCanvas;
	}

	@Override
	public void surfaceCreated(SurfaceHolder surfaceHolder) {
	}

	@Override
	public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
		synchronized (this){
			mWidth = width;
			mHeight = height;
			mCanDraw = true;
			mRenderChannel.postSingleMessage(UXMessage_Invalidate.create());
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
		synchronized (this){
			mCanDraw = false;
		}
	}

	public void setBackgroundColor(int color) {
		mColor = color;
	}
}
