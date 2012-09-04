package jp.co.johospace.jsphoto.ux.renderer.soft;

import java.util.ArrayList;
import java.util.Collection;

import jp.co.johospace.jsphoto.ux.loader.UXCacheLoaderThread;
import jp.co.johospace.jsphoto.ux.loader.UXImageCompressor;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoaderThread;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailStoreThread;
import jp.co.johospace.jsphoto.ux.renderer.UXCanvasResource;
import jp.co.johospace.jsphoto.ux.renderer.UXImageResource;
import jp.co.johospace.jsphoto.ux.renderer.UXMessage_Invalidate;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderEngine;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderer;
import jp.co.johospace.jsphoto.ux.renderer.UXResource;
import jp.co.johospace.jsphoto.ux.thread.UXMessage;
import jp.co.johospace.jsphoto.ux.thread.UXMessage_End;
import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;

/**
 * ソフトウェアレンダラ
 */
public class UXSoftRenderEngine implements UXRenderEngine {
	private SurfaceView mSurfaceView;
	private SoftCanvas mCanvas;
	private SoftRenderThread mRenderThread;
	private UXThumbnailLoaderThread mLoaderThread;
	private UXThumbnailStoreThread mStoreThread;
	private UXCacheLoaderThread mCacheThread;
	private boolean mDontUnregister;

	private MotionListener mMotionListener;

	private ArrayList<UXResource> mResources  = new ArrayList<UXResource>();

	private static final int WIDTH_HINT = 150;

	public UXSoftRenderEngine(Context context, UXRenderer renderer){
		mSurfaceView = new SurfaceView(context);
		mRenderThread = new SoftRenderThread(this, renderer);

		mCanvas = new SoftCanvas(mSurfaceView, mRenderThread.getChannel());

		mStoreThread = new UXThumbnailStoreThread();
		mLoaderThread = new UXThumbnailLoaderThread(mRenderThread.getChannel(), mStoreThread.getChannel(), new UXImageCompressor());
		mCacheThread = new UXCacheLoaderThread(mRenderThread.getChannel(), mLoaderThread.getChannel(), true);

		mStoreThread.start();
		mRenderThread.start();
		mLoaderThread.start();
		mCacheThread.start();

		mSurfaceView.setOnTouchListener(new MyTouchListener());
	}
	
	@Override
	public void addDecoderThread(int num){
		mLoaderThread.addThread(num);
	}

	@Override
	public void drawRect(int left, int top, int right, int bottom, int color) {
		mRenderThread.drawRect(left, top, right, bottom, color);
	}

	@Override
	public void setBackgroundColor(int color) {
		mRenderThread.getChannel().postMessage(new SoftRenderThread.Message_ChangeBackgroundColor(color));
	}

	@Override
	public void setReorderFlag(boolean flag) {
		//do nothing
	}

	@Override
	public void setImageListener(ImageListener listener) {
		mRenderThread.setImageListener(listener);
	}

	@Override
	public int getFps() {
		return mRenderThread.getFps();
	}

	@Override
	public void dispose() {
		mCacheThread.getChannel().postMessage(new UXMessage_End());
		mRenderThread.getChannel().postMessage(new UXMessage_End());
		mLoaderThread.getChannel().postMessage(new UXMessage_End());
		mStoreThread.getChannel().postMessage(new UXMessage_End());

		try {
			mCacheThread.join();
		} catch (InterruptedException e) {}

		try{
			mRenderThread.join();
		}catch(InterruptedException e){}

		try {
			mLoaderThread.join();
		} catch (InterruptedException e) {}
		try {
			mStoreThread.join();
		} catch (InterruptedException e) {}

		disposeAllResources(null);

		UXMessage.clear();
	}

	@Override
	public void invalidate() {
		mRenderThread.getChannel().postSingleMessage(UXMessage_Invalidate.create());
	}

	public Canvas getSystemCanvas(){
		return mCanvas.getCanvas();
	}

	@Override
	public int getWidth() {
		return mCanvas.getWidth();
	}

	@Override
	public int getHeight() {
		return mCanvas.getHeight();
	}

	public SoftCanvas getCanvas() {
		return mCanvas;
	}

	@Override
	public void disposeAllResources(Collection<UXResource> without) {
		mDontUnregister = true;

		if(without != null){
			UXResource[] withoutArray = new UXResource[without.size()];
			without.toArray(withoutArray);
			int len = withoutArray.length;

			for(UXResource res: mResources){
				boolean withoutFlag = false;
				for(int n = 0; n < len; ++n){
					if(res == withoutArray[n]){
						withoutFlag = true;
						break;
					}
				}

				if(!withoutFlag){
					res.dispose();
				}
			}

			mResources.clear();
			for(int n = 0;n < len; ++n){
				mResources.add(withoutArray[n]);
			}
		}else{
			for(UXResource res: mResources){
				res.dispose();
			}
			mResources.clear();
		}

		mDontUnregister = false;
	}

	@Override
	public UXCanvasResource createCanvasResource(int width, int height,
												 UXCanvasResource.CanvasRenderer renderer, boolean isDirect) {
		UXCanvasResource res = new SoftCanvasResource(this, mRenderThread.getChannel(),
				width, height, renderer, isDirect);

		mResources.add(res);
		return res;
	}

	public void unregisterResource(UXResource res){
		if(mDontUnregister) return;
		mResources.remove(res);
	}


	@Override
	public UXImageResource createImageResource() {
		UXImageResource res = new SoftImageResource(this, mCacheThread.getChannel(), mLoaderThread.getChannel());
		mResources.add(res);
		return res;
	}

	@Override
	public View getView() {
		return mSurfaceView;
	}

	@Override
	public void onResume() {
		//do nothing
	}

	@Override
	public void onPause() {
		//do nothing
	}

	@Override
	public void setMotionListener(MotionListener listener) {
		mMotionListener = listener;
	}

	private class MyTouchListener implements View.OnTouchListener{
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			return mMotionListener.onMotion(motionEvent);
		}
	}
}
