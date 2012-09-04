package jp.co.johospace.jsphoto.ux.renderer.soft;

import android.graphics.*;
import jp.co.johospace.jsphoto.ux.renderer.UXCanvasResource;
import jp.co.johospace.jsphoto.ux.renderer.UXMessage_InvalidateCanvasResource;
import jp.co.johospace.jsphoto.ux.thread.UXChannel;

/**
 * ソフトウェア描画のキャンバスリソース
 */
class SoftCanvasResource implements UXCanvasResource{
	private CanvasRenderer mRenderer;
	private int mWidth;
	private int mHeight;
	private Bitmap mSource;
	private Canvas mCanvas;
	private UXSoftRenderEngine mEngine;
	private UXChannel mRenderChannel;
	private boolean mIsDirect;

	private static Paint mBitmapPaint = new Paint();


	public SoftCanvasResource(UXSoftRenderEngine engine, UXChannel renderChannel,
							  int width, int height, CanvasRenderer renderer, boolean isDirect){
		mEngine = engine;
		mRenderer = renderer;
		mWidth = width;
		mHeight = height;
		mIsDirect = isDirect;

		mRenderChannel = renderChannel;

		if(!mIsDirect)
			createCanvas();
	}

	public static final Rect tmpDst = new Rect();

	@Override
	public boolean draw9scale(Rect scale, Rect dst) {
		Canvas c = mEngine.getSystemCanvas();

		//左上
		tmpRect.set(0, 0, scale.top, scale.left);
		tmpDst.set(0, 0, scale.top, scale.left);
		c.drawBitmap(mSource, tmpRect, tmpDst, mBitmapPaint);

		//右上
		tmpRect.set(scale.right, scale.top, mSource.getWidth(), scale.top);
		tmpDst.set(0, 0, tmpRect.width(), tmpRect.height());
		tmpDst.offset(dst.width()-tmpRect.width(),0);
		c.drawBitmap(mSource, tmpRect, tmpDst, mBitmapPaint);

		//左下
		tmpRect.set(0, scale.bottom, scale.left, mSource.getHeight());
		tmpDst.set(0, 0, tmpRect.width(), tmpRect.height());
		tmpDst.offset(0, dst.height() - tmpRect.height());
		c.drawBitmap(mSource, tmpRect, tmpDst, mBitmapPaint);

		//右下
		tmpRect.set(scale.right, scale.bottom, mSource.getWidth(), mSource.getHeight());
		tmpDst.set(tmpRect);
		tmpDst.offset(dst.width() - tmpRect.width(), dst.height() - tmpRect.height());
		c.drawBitmap(mSource, tmpRect, tmpDst, mBitmapPaint);

		//動作確認用なのでここで打ち切り

		return true;
	}

	@Override
	public void dispose() {
		purgeMemory();
		mEngine.unregisterResource(this);
	}

	@Override
	public boolean isDirect() {
		return mIsDirect;
	}

	@Override
	public boolean setDirect(boolean flag) {
		if(!flag){
			purgeMemory();
		}
		mIsDirect = flag;
		return mIsDirect;
	}

	@Override
	public CanvasRenderer getRenderer() {
		return mRenderer;
	}

	@Override
	public void setRenderer(CanvasRenderer renderer) {
		mRenderer = renderer;
	}

	private void createCanvas(){
		if(mIsDirect)return;

		mSource = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(mSource);
	}

	@Override
	public void invalidate() {
		if(mCanvas == null && !mIsDirect){
			createCanvas();
		}
		mRenderChannel.postMessage(UXMessage_InvalidateCanvasResource.create(this));
	}

	public void redraw(){
		if(mIsDirect){
			return;
		}else{
			if(mCanvas != null){
				mSource.eraseColor(Color.argb(0,0,0,0));
				mRenderer.draw(mCanvas);
			}
		}
	}

	@Override
	public void purgeMemory() {
		if(mSource != null)mSource.recycle();
		mSource = null;
		mCanvas = null;
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
		return mCanvas != null;
	}

	private static final Rect tmpRect = new Rect();

	@Override
	public boolean draw(int x, int y) {
		if(mIsDirect){
			Canvas c = mEngine.getSystemCanvas();
			c.save();
				c.translate(x, y);

				tmpRect.set(0, 0, mWidth, mHeight);
				c.clipRect(tmpRect);

				if(mRenderer != null)
					mRenderer.draw(c);
			c.restore();

			return true;
		}else{
			Canvas c = mEngine.getSystemCanvas();
			if(mCanvas != null){
				mBitmapPaint.setAlpha(0xff);
				c.drawBitmap(mSource, x, y, mBitmapPaint);
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean draw(Rect src, Rect dst, float alpha) {
		if(mIsDirect){
			//未実装
			return false;
		}else{
			Canvas c = mEngine.getSystemCanvas();
			mBitmapPaint.setAlpha((int)(alpha*0xff));
			c.drawBitmap(mSource, src, dst, mBitmapPaint);

			return true;
		}
	}
}
