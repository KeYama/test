package jp.co.johospace.jsphoto.ux.renderer.gl;

import jp.co.johospace.jsphoto.ux.renderer.UXCanvasResource;
import jp.co.johospace.jsphoto.ux.renderer.UXMessage_InvalidateCanvasResource;
import jp.co.johospace.jsphoto.ux.thread.UXChannel;
import android.graphics.Rect;

/**
 *
 * キャンバスリソースリライト
 *
 */
class NewGLCanvasResource implements UXCanvasResource {

	private UXGLRenderEngine mEngine;
	private CanvasRenderer mRenderer;
	private UXChannel mRenderChannel;
	private TextureAtlas.Fragment mFragment;
	private TextureAtlas mAtlas;
	private int mWidth, mHeight;
	private boolean mDisposeFlag;

	public NewGLCanvasResource(UXGLRenderEngine engine, TextureAtlas atlas, int width, int height,
	                           CanvasRenderer renderer, UXChannel renderChannel){
		mEngine = engine;
		mRenderer = renderer;
		mRenderChannel = renderChannel;
		mAtlas = atlas;
		mWidth = width;
		mHeight = height;

		mFragment = mAtlas.createFragment(mRenderer, mWidth, mHeight);
		invalidate();
	}

	public void redraw(){
		if(!checkFragment()) return;
		mFragment.redraw(mEngine);
	}


	@Override
	public void invalidate() {
		mRenderChannel.postMessage(UXMessage_InvalidateCanvasResource.create(this));
	}

	@Override
	public boolean draw(int x, int y) {
		if(!checkFragment())return false;
		return mFragment.draw(mEngine, x, y);
	}

	@Override
	public boolean draw9scale(Rect scale, Rect dst) {
		if(!checkFragment()) return false;
		return mFragment.draw9scale(mEngine, scale, dst);
	}

	@Override
	public boolean isDirect() {
		return false;
	}

	@Override
	public boolean setDirect(boolean flag) {
		return false;
	}

	@Override
	public CanvasRenderer getRenderer() {
		return mRenderer;
	}

	@Override
	public void setRenderer(CanvasRenderer renderer) {
		mRenderer = renderer;
		mFragment.setRenderer(mRenderer);
	}

	@Override
	public void purgeMemory() {
		mFragment.dispose();
		mFragment = null;
	}

	@Override
	public void dispose() {
		mDisposeFlag = true;
		if(mFragment != null) mFragment.dispose();
		mFragment = null;
	}

	@Override
	public int getWidth() {
		return mWidth;
	}

	@Override
	public int getHeight() {
		return mHeight;
	}

	@Override
	public boolean draw(Rect src, Rect dst, float alpha) {
		if(!checkFragment())return false;
		return mFragment.draw(mEngine, src, dst, alpha);
	}

	@Override
	public boolean isValid() {
		return mFragment != null;
	}

	private boolean checkFragment(){
		if(mFragment != null)return true;
		if(mDisposeFlag) return false;

		mFragment = mAtlas.createFragment(mRenderer, mWidth, mHeight);
		invalidate();
		return true;
	}
}
