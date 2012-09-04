package jp.co.johospace.jsphoto.ux.widget.memory.grid;

import android.graphics.Canvas;
import android.graphics.Paint;
import jp.co.johospace.jsphoto.ux.renderer.UXCanvasResource;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderEngine;
import jp.co.johospace.jsphoto.ux.widget.memory.UXMemoryDataSource;

/**
 *
 * ヘッダを表現するクラス
 *
 */
class Header{
	private UXCanvasResource mCanvas;
	private UXMemoryDataSource mSource;
	private int mCategory;

	private static Paint mPaint;
	private static int mFontAscent;

	UXCanvasResource.CanvasRenderer mRenderer = new UXCanvasResource.CanvasRenderer() {
		@Override
		public void draw(Canvas canvas) {
			canvas.drawText(mSource.getCategoryName(mCategory), 0, -mFontAscent, mPaint);
		}
	};

	Header(UXRenderEngine engine, UXMemoryDataSource source, int width, int height){
		if(mPaint == null){
			//width heightが共用という前提で使い回し
			mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			mPaint.setTextSize(height);
			mPaint.setColor(0xffffffff);
			mPaint.setStyle(Paint.Style.FILL_AND_STROKE);

			Paint.FontMetrics m = mPaint.getFontMetrics();
			mFontAscent = (int)m.ascent;
		}

		mSource = source;
		mCanvas = engine.createCanvasResource(width, height, mRenderer, true);
	}

	void reset(int category){
		mCategory = category;
		mCanvas.invalidate();
	}

	int getCategory(){
		return mCategory;
	}

	void dispose(){
		mPaint = null;
		if(mCanvas != null)mCanvas.dispose();
	}

	void draw(int x, int y){
		mCanvas.draw(x, y);
	}
}
