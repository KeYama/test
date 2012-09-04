package jp.co.johospace.jsphoto.ux.renderer.gl;

import java.util.ArrayList;
import java.util.Collection;

import jp.co.johospace.jsphoto.ux.loader.UXCacheLoaderThread;
import jp.co.johospace.jsphoto.ux.loader.UXImageCompressor;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoaderThread;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailStoreThread;
import jp.co.johospace.jsphoto.ux.renderer.UXCanvasResource;
import jp.co.johospace.jsphoto.ux.renderer.UXCanvasResource.CanvasRenderer;
import jp.co.johospace.jsphoto.ux.renderer.UXImageResource;
import jp.co.johospace.jsphoto.ux.renderer.UXMessage_Invalidate;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderEngine;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderer;
import jp.co.johospace.jsphoto.ux.renderer.UXResource;
import jp.co.johospace.jsphoto.ux.thread.UXMessage;
import jp.co.johospace.jsphoto.ux.thread.UXMessage_End;
import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;

public class UXGLRenderEngine implements UXRenderEngine{
	private CustomGLSurfaceView mGLSurfaceView;
	private GLRenderer mGLRenderer;
	private UXThumbnailLoaderThread mLoaderThread;
	private UXThumbnailStoreThread mStoreThread;
	private UXCacheLoaderThread mCacheThread;
	private boolean mDontUnregister;
	private Color mColor;

	private MotionListener mMotionListener;

	private ArrayList<UXResource> mResources  = new ArrayList<UXResource>();

	TextureAtlas mAtlas;

	public UXGLRenderEngine(Context context, UXRenderer renderer){
		mGLSurfaceView = new CustomGLSurfaceView(context);
		mGLRenderer = new GLRenderer(this, renderer, mGLSurfaceView);
		mGLSurfaceView.setRenderer(mGLRenderer);
		mGLSurfaceView.setRenderMode(CustomGLSurfaceView.RENDERMODE_WHEN_DIRTY);

		mStoreThread = new UXThumbnailStoreThread();
		mLoaderThread = new UXThumbnailLoaderThread(mGLRenderer.getChannel(), mStoreThread.getChannel(), new UXImageCompressor());
		mCacheThread = new UXCacheLoaderThread(mGLRenderer.getChannel(), mLoaderThread.getChannel(), true);

		mStoreThread.start();
		mLoaderThread.start();
		mCacheThread.start();

		mAtlas = new TextureAtlas(this);

		mGLSurfaceView.setOnTouchListener(new MyTouchListener());
	}
	
	@Override
	public void addDecoderThread(int num){
		mLoaderThread.addThread(num);
	}

	@Override
	public void setBackgroundColor(int color) {
		postMessageToRenderThread(new GLRenderer.Message_ChangeColor(color));
	}

	public GLRenderer getRenderer(){
		return mGLRenderer;
	}

	private void postMessageToRenderThread(UXMessage msg){
		mGLRenderer.getChannel().postMessage(msg);
	}

	@Override
	public void drawRect(int left, int top, int right, int bottom, int color) {
		mGLRenderer.drawRect(left, top, right, bottom, color);
	}

	@Override
	public void invalidate() {
		postMessageToRenderThread(UXMessage_Invalidate.create());
	}

	@Override
	public int getWidth() {
		return mGLSurfaceView.getWidth();
	}

	@Override
	public int getHeight() {
		return mGLSurfaceView.getHeight();
	}

	public void reloadTextureAtlas(){
		mAtlas.repack();
	}

	public void clearTextureAtlas(){
		mAtlas.clearOnSurfaceChanged();
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

		mAtlas.repack();

		mDontUnregister = false;
	}

	@Override
	public UXCanvasResource createCanvasResource(int width, int height,
			CanvasRenderer renderer, boolean isDirect) {
		//GLCanvasResource res = new GLCanvasResource(this, width, height, renderer, getRenderer().getChannel());
		NewGLCanvasResource res = new NewGLCanvasResource(this, mAtlas, width, height, renderer, getRenderer().getChannel());
		mResources.add(res);
		return res;
	}

	@Override
	public UXImageResource createImageResource() {
		GLImageResource res = new GLImageResource(this, mCacheThread.getChannel(), mLoaderThread.getChannel());
		mResources.add(res);
		return res;
	}

	@Override
	public void setImageListener(ImageListener listener) {
		mGLRenderer.setImageListener(listener);
	}

	public void unregisterResource(UXResource res){
		if(mDontUnregister) return;
		mResources.remove(res);
	}

	public ArrayList<UXResource> getResources(){
		return mResources;
	}



	@Override
	public View getView() {
		return mGLSurfaceView;
	}

	@Override
	public void dispose() {
		mCacheThread.getChannel().postMessage(new UXMessage_End());
		mLoaderThread.getChannel().postMessage(new UXMessage_End());
		mGLRenderer.getChannel().postMessage(new UXMessage_End());
		mStoreThread.getChannel().postMessage(new UXMessage_End());

		//待つ必要無し
//		try {
//			mCacheThread.join();
//		} catch (InterruptedException e) {}
//		try {
//			mLoaderThread.join();
//		} catch (InterruptedException e) {}
//		try {
//			mStoreThread.join();
//		} catch (InterruptedException e) {}

		disposeAllResources(null);

		UXMessage.clear();
	}

	@Override
	public int getFps() {
		return mGLRenderer.getFps();
	}

	@Override
	public void setReorderFlag(boolean flag) {

	}

	@Override
	public void onResume() {
		mGLSurfaceView.onResume();
	}

	@Override
	public void onPause() {
		mGLSurfaceView.onPause();
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
