package jp.co.johospace.jsphoto.ux.renderer.gl;

import jp.co.johospace.jsphoto.ux.renderer.UXCanvasResource;
import jp.co.johospace.jsphoto.ux.renderer.UXMessage_InvalidateCanvasResource;
import jp.co.johospace.jsphoto.ux.thread.UXChannel;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public class GLCanvasResource implements UXCanvasResource{

	private static final int DRAW_NOT_DRAW = 1;
	private static final int DRAW_CCOORDINATE = 2;
	private static final int DRAW_EXPANSION = 3;
	private static final int DRAW_9SCALE = 4;
	private UXGLRenderEngine mEngine;
	private int mTextureName;
	private int mResourceId;
	private int mTextureY;
	private int mTextureX;
	private int mTextureWidth;
	private int mTextureHeight;
	private int mWidth;
	private int mHeight;
	private UXChannel mRendererCannel;
	private CanvasRenderer mCanvasRenderer;
	private GLCanvas mGlCanvas;
	private int mDrawState;
	private DrawParam mDrawParam;
	private boolean isReCreate;
	private boolean isPurgeMemory;
	private boolean isDispose;

	private CanResParam mSetParam = new CanResParam() {

		@Override
		public void setParam(int textureName,int canvasId, int textureWidth, int textureHeight, int textureX, int textureY) {
			mTextureName = textureName;
			mResourceId = canvasId;
			mTextureWidth = textureWidth;
			mTextureHeight = textureHeight;
			mTextureX = textureX;
			mTextureY = textureY;
		}
	};

	public GLCanvasResource(UXGLRenderEngine engine, int width, int height,
			CanvasRenderer renderer, UXChannel rendererChannnel){
		mEngine = engine;
		mWidth = width;
		mHeight = height;
		mCanvasRenderer = renderer;
		mGlCanvas = engine.getRenderer().getGLCanvas();
		mRendererCannel = rendererChannnel;
		mDrawState = DRAW_NOT_DRAW;
		mDrawParam = new DrawParam();

		addTexture();
	}
	/**
	 * 画像を作成しテクスチャに設定する
	 */
	private void addTexture(){

		Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);

		//キャンバスレンダラーでテキストなどを描画
		mCanvasRenderer.draw(canvas);

		//テクスチャに画像を追加
		mGlCanvas.drawImageToTexture(mSetParam, bitmap);

		//bitmapを破棄
		if(bitmap != null)
			bitmap.recycle();
		canvas = null;
	}

	@Override
	public boolean draw9scale(Rect scale, Rect dst) {
		if(isPurgeMemory){
			resetting();
			return false;
		}
		if(isReCreate){
			addTexture();
			isReCreate = false;
		}
		mDrawState = DRAW_9SCALE;

		if(!scale.equals(mDrawParam.scale) && !dst.equals(mDrawParam.dst)){
			mDrawParam.set9Scale(scale, dst);
		}

		//テクスチャの範囲を設定
		Rect texRange = new Rect();
		texRange.left = mTextureX;
		texRange.top = mTextureY;
		texRange.right = mTextureX + mWidth;
		texRange.bottom = mTextureY + mHeight;

		return mEngine.getRenderer()
				.draw9scale(mTextureName, mTextureWidth, mTextureHeight, scale, dst, texRange);
	}

	@Override
	public void purgeMemory() {
		//GLCanvasにテクスチャが不要とメッセージを送る
		mGlCanvas.release(mTextureName, mResourceId);
		mDrawState = DRAW_NOT_DRAW;
 		isPurgeMemory = true;
	}

	@Override
	public void dispose() {
		isDispose = true;
		purgeMemory();
		mEngine.unregisterResource(this);
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
	public boolean isValid() {
		return mDrawState != DRAW_NOT_DRAW;
	}

	@Override
	public void invalidate() {
		mRendererCannel.postMessage(UXMessage_InvalidateCanvasResource.create(this));
	}

	/**
	 * 再描画する
	 */
	public void redraw(){
		if(isDispose)return;

		//テクスチャに画像を追加
		addTexture();

		switch (mDrawState) {
		case DRAW_NOT_DRAW:
			//drawしていない時は何もしない。
			break;

		case DRAW_CCOORDINATE:
			draw(mDrawParam.x, mDrawParam.y);
			break;

		case DRAW_EXPANSION:
			draw(mDrawParam.src, mDrawParam.dst, mDrawParam.alpha);
			break;

		case DRAW_9SCALE:
			draw9scale(mDrawParam.scale, mDrawParam.dst);
			break;
		}
		isReCreate = false;
	}

	private void resetting(){
		//ここで再設定する
		isPurgeMemory = false;
		invalidate();
	}

	@Override
	public boolean draw(Rect src, Rect dst, float alpha) {

		if(isPurgeMemory){
			resetting();
			return false;
		}

		if(isReCreate){
			addTexture();
			isReCreate = false;
		}

		mDrawState = DRAW_EXPANSION;

		//再描画用に引数を保存
		if(!src.equals(mDrawParam.src) && !dst.equals(mDrawParam.dst))
			mDrawParam.setExpansionDraw(src, dst, alpha);

		//テクスチャ内の位置に合わせる
		src.left += mTextureX;
		src.right += mTextureX;
		src.top += mTextureY;
		src.bottom += mTextureY;

		//レンダラーに渡して描画する
		mEngine.getRenderer().draw(mTextureName, mTextureWidth, mTextureHeight, src, dst, alpha, true);
		return true;	//描画できた時にtrue
	}

	@Override
	public boolean draw(int x, int y) {
		if(isPurgeMemory){
			resetting();
			return false;
		}
		if(isReCreate){
			addTexture();
			isReCreate = false;
		}

		mDrawState = DRAW_CCOORDINATE;
		mDrawParam.setCoordinateDraw(x, y);
		mEngine.getRenderer().draw(mTextureName, mTextureWidth, mTextureHeight,
				x, y, mWidth, mHeight, mTextureX, mTextureY);

		return true;
	}

	/**
	 * レンダラーでメモリが解放されたときに呼び出します。
	 * テクスチャに画像を設定し直し描画します。
	 */
	public void reCreate(){
		isReCreate = true;
		invalidate();
	}

	@Override
	public boolean isDirect() {
		//未実装
		return false;
	}

	@Override
	public boolean setDirect(boolean flag) {
		//未実装
		return false;
	}

	@Override
	public CanvasRenderer getRenderer() {
		return mCanvasRenderer;
	}

	@Override
	public void setRenderer(CanvasRenderer renderer) {
		mCanvasRenderer = renderer;
	}

	public interface CanResParam{
		void setParam(int textureWidth, int canvasId, int textureHeight, int textureName, int textureX, int textureY);
	}

	private class DrawParam{
		public Rect src = new Rect();
		public Rect dst = new Rect();
		public Rect scale = new Rect();
		public float alpha;
		public int x;
		public int y;

		public void setExpansionDraw(Rect src, Rect dsc, float alpha){
			//参照渡しを避けるために項目ごとに設定
			this.src.left = src.left;
			this.src.top = src.top;
			this.src.right = src.right;
			this.src.bottom = src.bottom;

			this.dst.left = dsc.left;
			this.dst.top = dsc.top;
			this.dst.right = dsc.right;
			this.dst.bottom = dsc.bottom;
			this.alpha = alpha;
		}
		public void setCoordinateDraw(int x, int y){
			this.x = x;
			this.y = y;
		}
		public void set9Scale(Rect scale, Rect dst){
			this.scale = scale;
			this.dst = dst;
		}
	}
}